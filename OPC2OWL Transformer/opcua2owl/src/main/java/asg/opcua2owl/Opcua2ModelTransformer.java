package asg.opcua2owl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaDataTypeNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaReferenceTypeNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableTypeNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaViewNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.slf4j.Logger;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.toList;

public class Opcua2ModelTransformer {
	String endpointUrl = null;
	
	// the OpcaUaClient object used to connect to the server
	OpcUaClient client = null;
	
	// a list of nodes that already have been processed avoids endless loops
	private Vector<NodeId> processedNodes = null;
	
	// maximum depth of the OPC UA address space to convert starting from each initialNodeId
	int maxDepth = -1;
	
	// namespace Table of the OPC UA server
	String[] namespaceArray = new String[0];
	
	// the resulting OntModel
	Map<String, Model> models = null;
	
	// Logging
	Logger logger = null;
	
	private final String uaBaseNs = "http://opcfoundation.org/UA/";

	public Opcua2ModelTransformer() {
		// the following map keeps the order of entries
		models = Collections.synchronizedMap(new LinkedHashMap<String, Model>());
		this.processedNodes = new Vector<NodeId>();
		this.endpointUrl = "opc.tcp://localhost:48030";
	}

	/*
	 * @param endpointUrl endpoint URL of the OPC UA endpoint used for transformation
	 */
	public Opcua2ModelTransformer(String endpointUrl, Logger logger) {
		// the following map keeps the order of entries
		models = Collections.synchronizedMap(new LinkedHashMap<String, Model>());
		processedNodes = new Vector<NodeId>();
		this.endpointUrl = endpointUrl;
		this.logger = logger;
	}

	/*
	 * Transforms an OPC UA address space to an ontology
	 * @param initialNodeIds a list of OPC UA NodeIds to start the transformation process from
	 * @param maxDepth maximum depth of the OPC UA address space to convert starting from each initialNodeId. use 0 to convert only a single node
	 * @param namespaces the OPC UA namespaces to be imported into the OntModel
	 * @param namespaces the OPC UA namespace that actually is transformed and forms the base of the OntModel
	 */
	public Map<String, Model> transform(Vector<NodeId> rootNodeIds, int maxDepth) throws Exception {
		// connect to OPC UA server
		client = HelperFunctions.connectToServer(endpointUrl);
		
		// get the namespace table from the server
		namespaceArray = HelperFunctions.getNamespaceArray(client);

		// create a model for each namespace
		for(String ns : namespaceArray) {
			Model model = ModelFactory.createDefaultModel();
			// set the prefix for the uaBase namespace
			model.setNsPrefix("uaBase", uaBaseNs + "#");
			models.put(ns, model);
		}
		
		this.maxDepth = maxDepth;

		// start the recursive conversion process for each nodeId in intialNodeIds
		for (NodeId initialNodeId : rootNodeIds) {
			transformRecursive(initialNodeId, 0);
		}
		
		client.disconnect();
		
		return models;
	}

	private void transformRecursive(NodeId nodeId, int currentDepth) throws Exception {

		if (processedNodes.contains(nodeId))
			return;
		if (currentDepth > maxDepth)
			return;

		// transform this node 
		Resource sourceResource = transformNode(client.getAddressSpace().getNodeInstance(nodeId).get());
		transformAttributes(client.getAddressSpace().getNodeInstance(nodeId).get(), sourceResource);
		processedNodes.add(nodeId);

		if (currentDepth == maxDepth) {
			return;
		}

		// continue browsing child nodes if the MAX_DEPTH has not been reached yet
		BrowseDescription browseDescription = new BrowseDescription(
				nodeId, // nodeId
				BrowseDirection.Both, // browseDirection
				Identifiers.References, // referenceTypeId
				true, // includeSubtypes
				uint(// nodeClassMask
						NodeClass.ObjectType.getValue() | 
						NodeClass.Object.getValue() |
						NodeClass.DataType.getValue() |
						NodeClass.Method.getValue() |
						NodeClass.ReferenceType.getValue() |
						NodeClass.Variable.getValue() |
						NodeClass.VariableType.getValue() | 
						NodeClass.View.getValue()
				), 
				uint(BrowseResultMask.All.getValue()) // resultMask
				);

		BrowseResult browseResult = client.browse(browseDescription).get();

		List<ReferenceDescription> references = toList(browseResult.getReferences());

		for (ReferenceDescription rd : references) {
			if (rd.getNodeId().getServerIndex() != 0) { // nodes on another server are not supported
				logger.error("ExpandedNodeIds to nodes on another server are not supported");
				continue;
			}

			// collect the nodes
			UaReferenceTypeNode referenceNode = (UaReferenceTypeNode) client.getAddressSpace().getNodeInstance(rd.getReferenceTypeId()).get();
			UaNode targetNode = client.getAddressSpace().getNodeInstance(rd.getNodeId().local().get()).get();
			
			Property property = transformReference(referenceNode, rd.getIsForward());
			Resource targetResource = transformNode(targetNode);
			
			// we want to keep the namespace with the lower index clean from other namespaces
			String sourceNodeNs = namespaceArray[nodeId.getNamespaceIndex().intValue()];
			String targetNodeNs = namespaceArray[targetNode.getNodeId().get().getNamespaceIndex().intValue()];
			String sourceResourceName = sourceResource.getURI().substring(sourceResource.getURI().indexOf('#')+1);
			if(nodeId.getNamespaceIndex().intValue() < targetNode.getNodeId().get().getNamespaceIndex().intValue()) {
				// add sourceResoure also to the other model
				sourceResource = models.get(targetNodeNs).createResource(sourceNodeNs + "#" + sourceResourceName);
			}
			sourceResource.addProperty(property, targetResource);			
			// continue transformation process recursively
			transformRecursive(targetNode.getNodeId().get(), currentDepth + 1);
		}

	}
	
	private Property transformReference(UaReferenceTypeNode referenceNode, boolean isForward) throws Exception
	{
		Property property = null;

		// get the corresponding model
		int namespaceIndex = referenceNode.getNodeId().get().getNamespaceIndex().intValue();
		String namespace = namespaceArray[namespaceIndex];
		Model model = models.get(namespace);
		
		// add the property
		if(isForward)
			property = model.createProperty(namespace + "#" + referenceNode.getBrowseName().get().getName());
		else
			property = model.createProperty(namespace + "#" + referenceNode.getInverseName().get().getText());
		
		return property;
	}

	int dots = 0;
	private Resource transformNode(UaNode node) throws Exception
	{
		System.out.print(".");
		if (dots > 100) {
			System.out.println();
			dots = 0;
		}
		dots++;
		
		Resource resource;
		
		// get the corresponding model
		int namespaceIndex = node.getNodeId().get().getNamespaceIndex().intValue();
		String namespace = namespaceArray[namespaceIndex];
		Model model = models.get(namespace);
		String nameString = node.getNodeId().get().toString();
		nameString = nameString.substring(nameString.indexOf('{') + 1, nameString.indexOf('}')).replace(" ",  "");
		
		// add the resource
		if(node.getNodeClass().get().toString().endsWith("Type"))
			resource = model.createResource(namespace + "#" + node.getBrowseName().get().getName());
		else
			resource = model.createResource(namespace + "#" + nameString);
		
		return resource;
	}
	
	private void transformAttributes(UaNode node, Resource resource) throws Exception {
		Model uaBaseModel = models.get(uaBaseNs);
		
		// Add base node class attributes
		resource.addLiteral(
				uaBaseModel.createProperty(uaBaseNs +"#NodeId"), 
				node.getNodeId().get().toString()
				);
		resource.addLiteral(
				uaBaseModel.createProperty(uaBaseNs +"#NodeClass"), 
				node.getNodeClass().get().toString()
				);
		resource.addLiteral(
				uaBaseModel.createProperty(uaBaseNs +"#BrowseName"), 
				node.getBrowseName().get().getName()
				);
		resource.addLiteral(
				uaBaseModel.createProperty(uaBaseNs +"#DisplayName"), 
				node.getDisplayName().get().getText()
				);
		try {
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#Description"), 
					node.getDescription().get().getText()
					);		}
		catch (Exception e) {
			// ignore if reading an optional argument fails
		}
		try {
		resource.addLiteral(
				uaBaseModel.createProperty(uaBaseNs +"#WriteMask"), 
				node.getWriteMask().get().toString()
				);		}
		catch (Exception e) {
			// ignore if reading an optional argument fails
		}

		try {
		resource.addLiteral(
				uaBaseModel.createProperty(uaBaseNs +"#UserWriteMask"), 
				node.getUserWriteMask().get().toString()
				);		}
		catch (Exception e) {
			// ignore if reading an optional argument fails
		}

		
		switch(node.getNodeClass().get()) {
		case DataType: 
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#IsAbstract"), 
					((UaDataTypeNode) node).getIsAbstract().get()
					);
			break;
		case Method: 
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#Executable"), 
					((UaMethodNode) node).getExecutable().get()
					);
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#UserExecutable"), 
					((UaMethodNode) node).getUserExecutable().get()
					);
			break;
		case Object:
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#EventNotifier"), 
					((UaObjectNode) node).getEventNotifier().get().byteValue()
					);
			break;
		case ObjectType: 
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#IsAbstract"), 
					((UaObjectTypeNode) node).getIsAbstract().get()
					);
			break;
		case ReferenceType: 
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#IsAbstract"), 
					((UaReferenceTypeNode) node).getIsAbstract().get()
					);
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#Symmetric"), 
					((UaReferenceTypeNode) node).getSymmetric().get()
					);
			try {
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#InverseName"), 
					((UaReferenceTypeNode) node).getInverseName().get().getText()
					);			}
			catch (Exception e) {
				// ignore if reading an optional argument fails
			}

			break;
		case Variable: 
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#Value"), 
					client.readValue(0, null, node.getNodeId().get()).get().toString()
					);
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#DataType"), 
					((UaVariableNode) node).getDataType().get().toString()
					);
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#ValueRank"), 
					((UaVariableNode) node).getValueRank().get()
					);
			try {
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#ArrayDimensions"), 
					((UaVariableNode) node).getArrayDimensions().get().toString()
					);
			}
			catch (Exception e) {
				// ignore if reading an optional argument fails
			}
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#AccessLevel"), 
					((UaVariableNode) node).getAccessLevel().get().byteValue()
					);
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#UserAccessLevel"), 
					((UaVariableNode) node).getUserAccessLevel().get().byteValue()
					);
			try {
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#MinimumSamplingInterval"), 
					((UaVariableNode) node).getMinimumSamplingInterval().get()
					);			}
			catch (Exception e) {
				// ignore if reading an optional argument fails
			}

			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#Historizing"), 
					((UaVariableNode) node).getHistorizing().get()
					);
			break;
		case VariableType: 
			try {
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#Value"), 
					client.readValue(0, null, node.getNodeId().get()).get().toString()
					);			
			}
			catch (Exception e) {
				// ignore if reading an optional argument fails
			}

			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#DataType"), 
					((UaVariableTypeNode) node).getDataType().get().toString()
					);
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#ValueRank"), 
					((UaVariableTypeNode) node).getValueRank().get()
					);
			try {
				resource.addLiteral(
						uaBaseModel.createProperty(uaBaseNs +"#ArrayDimensions"), 
						((UaVariableTypeNode) node).getArrayDimensions().get().toString()
						);
			}
			catch (Exception e) {
				// ignore if reading an optional argument fails
			}
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#IsAbstract"), 
					((UaVariableTypeNode) node).getIsAbstract().get()
					);
			break;
		case View: 
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#ContainsNoLoops"), 
					((UaViewNode) node).getContainsNoLoops().get()
					);
			resource.addLiteral(
					uaBaseModel.createProperty(uaBaseNs +"#EventNotifier"), 
					((UaViewNode) node).getEventNotifier().get().byteValue()
					);
			break;
			default: logger.error("Node class {} not supported at the moment", node.getNodeClass());
		}

	}
}
