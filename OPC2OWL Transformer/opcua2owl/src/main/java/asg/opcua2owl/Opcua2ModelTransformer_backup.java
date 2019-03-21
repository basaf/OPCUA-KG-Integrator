package asg.opcua2owl;

import java.util.List;
import java.util.Vector;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ModelFactory;
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

public class Opcua2ModelTransformer_backup {
	String endpointUrl = null;
	
	// the OpcaUaClient object used to connect to the server
	OpcUaClient client = null;
	
	// a list of nodes that already have been processed avoids endless loops
	private Vector<NodeId> processedNodes = null;
	
	// maximum depth of the OPC UA address space to convert starting from each initialNodeId
	int maxDepth = -1;
	
	// only nodes of on of the 
	Vector<String> namespaces = null;
	
	// namespace Table of the OPC UA server
	String[] namespaceArray = new String[0];
	
	// the resulting OntModel
	OntModel ontModel = null;
	
	// Logging
	Logger logger = null;

	
	public Opcua2ModelTransformer_backup() {
		this.processedNodes = new Vector<NodeId>();
		this.endpointUrl = "opc.tcp://localhost:48030";
	}

	/*
	 * @param endpointUrl endpoint URL of the OPC UA endpoint used for transformation
	 */
	public Opcua2ModelTransformer_backup(String endpointUrl, Logger logger) {
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
	public OntModel transform(Vector<NodeId> rootNodeIds, int maxDepth, Vector<String> namespaces, String baseNamespace) throws Exception {
		// create an empty model for our knowledge base
		ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		
		// add the <ontology rdf:about = " ... ">
		Ontology baseOnt = ontModel.createOntology(baseNamespace);
		// add the xmlns="<baseNamespace>
		ontModel.setNsPrefix("", baseNamespace + "#");
		// ontModel.setNsPrefix("myprefix", baseNamespace);
		
		// Import all other namespaces
		for(String namespace : namespaces) {
			if(namespace.equals(baseNamespace))
				continue;
			Resource ontToBeImported = ontModel.createResource(namespace);
			baseOnt.addImport(ontToBeImported);
		}
		
		this.namespaces = namespaces;

		// connect to OPC UA server
		client = HelperFunctions.connectToServer(endpointUrl);
		
		// get the namespace table from the server
		namespaceArray = HelperFunctions.getNamespaceArray(client);
		
		// Attributes are not supported in ontologies. We use an Annotation Property (or Annotation Properties) instead.
		/*
		AnnotationProperty hasAttribute = ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#HasAttribute");
		hasAttribute.addSubProperty(ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#HasNodeId"));
		*/
		
		this.maxDepth = maxDepth;
		// start the recursive conversion process for each nodeId in intialNodeIds
		System.out.println("Converting...");
		for (NodeId initialNodeId : rootNodeIds) {
			transformRecursive(initialNodeId, 0);
		}
		
		return ontModel;
	}

	private void transformRecursive(NodeId nodeId, int currentDepth) throws Exception {

		if (processedNodes.contains(nodeId))
			return;
		if (currentDepth > maxDepth)
			return;
		if (!namespaces.contains(namespaceArray[nodeId.getNamespaceIndex().intValue()]))
			return;
		if (currentDepth == maxDepth) {
			// transform only this node
			transformNode(client.getAddressSpace().getNodeInstance(nodeId).get());
		}
		else {
			// continue browsing child nodes if the MAX_DEPTH has not been reached yet
			BrowseDescription browseDescription = new BrowseDescription(
					nodeId, // nodeId
					BrowseDirection.Forward, // browseDirection
					Identifiers.References, // referenceTypeId
					true, // includeSubtypes
					uint(NodeClass.ObjectType.getValue() | NodeClass.Object.getValue()), // nodeClassMask
					uint(BrowseResultMask.All.getValue()) // resultMask
					);
	
			BrowseResult browseResult = client.browse(browseDescription).get();
	
			List<ReferenceDescription> references = toList(browseResult.getReferences());
	
			for (ReferenceDescription rd : references) {
				// collect the nodes
				NodeId sourceNodeId = nodeId;
				NodeId referenceTypeNodeId = rd.getReferenceTypeId();
				if (rd.getNodeId().getServerIndex() != 0) { // nodes on another server are not supported
					logger.error("ExpandedNodeIds to nodes on another server are not supported");
					continue;
				}
				NodeId targetNodeId = rd.getNodeId().local().get();
				
				UaNode sourceNode = client.getAddressSpace().getNodeInstance(sourceNodeId).get();
				UaNode referenceTypeNode = client.getAddressSpace().getNodeInstance(referenceTypeNodeId).get();
				UaNode targetNode = client.getAddressSpace().getNodeInstance(targetNodeId).get();
				
				// convert the tripple
				transformTriple(sourceNode, 
						referenceTypeNode, 
						targetNode);
				
				// continue transformation process recursively
				transformRecursive(targetNodeId, currentDepth + 1);
			}
		}
		processedNodes.add(nodeId);
	}
	
	private void transformTriple(UaNode subjectNode, UaNode referenceNode, UaNode objectNode) throws Exception {
		System.out.println(subjectNode.getBrowseName().get().getName() + " " + referenceNode.getBrowseName().get().getName() + " " + objectNode.getBrowseName().get().getName());
		OntResource subject = transformNode(subjectNode);
		OntResource property = transformNode(referenceNode);
		OntResource object = transformNode(objectNode);
		if(subject != null && property != null && object != null)
			subject.addProperty((OntProperty)property, object);
	}

	/*
	 * Creates a class/instance/... for the OPC UA node
	 * and adds the node's attributes to the ontology model.
	 * No properties to other resources in the ontology model are created here.
	 */
	private OntResource transformNode(UaNode node) throws Exception {
		// convert node from OPC UA to OWL
		// System.out.println("Converting node " + nodeId.toString());
		String nodeId = node.getNodeId().get().toParseableString();
		String browseName = node.getBrowseName().get().getName().replace("<", "").replace(">", "");
		String displayName = node.getDisplayName().get().getText().replace("<", "").replace(">", "");
		int namespaceIndex = node.getNodeId().get().getNamespaceIndex().intValue();
		
		OntResource ontResource = null;

		/*
		System.out.println("NodeId: " + node.getNodeId().get());
		System.out.println("\tNodeClass: " + node.getNodeClass().get());
		System.out.println("\tBrowseName: " + node.getBrowseName().get());
		System.out.println("\tDisplayName: " + node.getDisplayName().get());
		System.out.println("\tDescription: " + node.getDescription().get());
		*/

		switch(node.getNodeClass().get()) {
		case DataType: 
			ontResource = ontModel.createClass(namespaceArray[namespaceIndex] + "#" + browseName);
		case Method: 
			ontResource = ontModel.createOntResource(namespaceArray[namespaceIndex] + "#" + nodeId);
			break;
		case Object:
			Vector<UaNode> typeNodes = HelperFunctions.getTypeNodes(client, node, logger);
			for(UaNode typeNode : typeNodes) {
				OntClass c = (OntClass) transformNode(typeNode);
				ontResource = c.createIndividual(namespaceArray[namespaceIndex] + "#" + nodeId);
			}
			break;
		case ObjectType: 
			ontResource = ontModel.createClass(namespaceArray[namespaceIndex] + "#" + browseName);
			break;
		case ReferenceType: 
			ontResource = ontModel.createObjectProperty(namespaceArray[namespaceIndex] + "#" + browseName);
			break;
		case Variable: 
			ontResource = ontModel.createOntResource(namespaceArray[namespaceIndex] + "#" + nodeId);
			break;
		case VariableType: 
			ontResource = ontModel.createClass(namespaceArray[namespaceIndex] + "#" + browseName);
			break;
		case View: 
			break;
			default: logger.error("Node class {} not supported at the moment", node.getNodeClass());
		}
		
		if(ontResource != null) {
			transformAttributes(node, ontResource);
			ontResource.addLabel(displayName, null);
		}
		return ontResource;
	}
	
	private void transformAttributes(UaNode node, Resource resource) throws Exception {
		// Add base node class attributes
		resource.addProperty(
				ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#NodeId"), 
				ontModel.createLiteral(node.getNodeId().get().toString())
				);
		resource.addProperty(
				ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#NodeClass"), 
				ontModel.createLiteral(node.getNodeClass().get().toString())
				);
		resource.addProperty(
				ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#BrowseName"), 
				ontModel.createLiteral(node.getBrowseName().get().toString())
				);
		resource.addProperty(
				ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#DisplayName"), 
				ontModel.createLiteral(node.getDisplayName().get().toString())
				);
		resource.addProperty(
				ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#Description"), 
				ontModel.createLiteral(node.getDescription().get().toString())
				);
		resource.addProperty(
				ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#WriteMask"), 
				ontModel.createLiteral(node.getWriteMask().get().toString())
				);
		resource.addProperty(
				ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#UserWriteMask"), 
				ontModel.createLiteral(node.getUserWriteMask().get().toString())
				);
		
		switch(node.getNodeClass().get()) {
		case DataType: 
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#IsAbstract"), 
					ontModel.createTypedLiteral(((UaDataTypeNode) node).getIsAbstract().get())
					);
			break;
		case Method: 
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#Executable"), 
					ontModel.createTypedLiteral(((UaMethodNode) node).getExecutable().get())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#UserExecutable"), 
					ontModel.createTypedLiteral(((UaMethodNode) node).getUserExecutable().get())
					);
			break;
		case Object:
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#EventNotifier"), 
					ontModel.createTypedLiteral(((UaObjectNode) node).getEventNotifier().get())
					);
			break;
		case ObjectType: 
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#IsAbstract"), 
					ontModel.createTypedLiteral(((UaObjectTypeNode) node).getIsAbstract().get())
					);
			break;
		case ReferenceType: 
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#IsAbstract"), 
					ontModel.createTypedLiteral(((UaReferenceTypeNode) node).getIsAbstract().get())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#Symmetric"), 
					ontModel.createTypedLiteral(((UaReferenceTypeNode) node).getSymmetric().get())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#InverseName"), 
					ontModel.createTypedLiteral(((UaReferenceTypeNode) node).getInverseName().get())
					);
			break;
		case Variable: 
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#Value"), 
					ontModel.createLiteral(client.readValue(0, null, node.getNodeId().get()).toString())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#DataType"), 
					ontModel.createTypedLiteral(((UaVariableNode) node).getDataType().get().toString())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#ValueRank"), 
					ontModel.createTypedLiteral(((UaVariableNode) node).getValueRank().get())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#ArrayDimensions"), 
					ontModel.createTypedLiteral(((UaVariableNode) node).getArrayDimensions().get())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#AccessLevel"), 
					ontModel.createTypedLiteral(((UaVariableNode) node).getAccessLevel().get())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#UserAccessLevel"), 
					ontModel.createTypedLiteral(((UaVariableNode) node).getUserAccessLevel().get())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#MinimumSamplingInterval"), 
					ontModel.createTypedLiteral(((UaVariableNode) node).getMinimumSamplingInterval().get())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#Historizing"), 
					ontModel.createTypedLiteral(((UaVariableNode) node).getHistorizing().get())
					);
			break;
		case VariableType: 
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#Value"), 
					ontModel.createLiteral(client.readValue(0, null, node.getNodeId().get()).toString())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#DataType"), 
					ontModel.createTypedLiteral(((UaVariableTypeNode) node).getDataType().get().toString())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#ValueRank"), 
					ontModel.createTypedLiteral(((UaVariableTypeNode) node).getValueRank().get())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#ArrayDimensions"), 
					ontModel.createTypedLiteral(((UaVariableTypeNode) node).getArrayDimensions().get())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#IsAbstract"), 
					ontModel.createTypedLiteral(((UaVariableTypeNode) node).getIsAbstract().get())
					);
			break;
		case View: 
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#ContainsNoLoops"), 
					ontModel.createTypedLiteral(((UaViewNode) node).getContainsNoLoops().get())
					);
			resource.addProperty(
					ontModel.createAnnotationProperty("http://opcfoundation.org/UA/#EventNotifier"), 
					ontModel.createTypedLiteral(((UaViewNode) node).getEventNotifier().get())
					);
			break;
			default: logger.error("Node class {} not supported at the moment", node.getNodeClass());
		}

	}
}
