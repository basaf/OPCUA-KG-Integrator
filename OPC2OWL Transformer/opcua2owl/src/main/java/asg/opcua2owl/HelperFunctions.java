package asg.opcua2owl;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.toList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.UaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.slf4j.Logger;

import de.uni_stuttgart.vis.vowl.owl2vowl.Owl2Vowl;

public class HelperFunctions {
	
	public static void listStatements(Model model, Logger logger) {
		// list the statements in the Model
		StmtIterator iter = model.listStatements();

		// print out the predicate, subject and object of each statement
		while (iter.hasNext()) {
		    Statement stmt      = iter.nextStatement();  // get next statement
		    Resource  subject   = stmt.getSubject();     // get the subject
		    Property  predicate = stmt.getPredicate();   // get the predicate
		    RDFNode   object    = stmt.getObject();      // get the object

		    logger.info(subject.toString());
		    logger.info(" " + predicate.toString() + " ");
		    if (object instanceof Resource) {
		    	logger.info(object.toString());
		    } else {
		        // object is a literal
		    	logger.info(" \"" + object.toString() + "\"");
		    }

		    logger.info(" .");
		} 
	}
	
	public static void listClasses(OntModel ontModel, Logger logger) {
		
		// list the statements in the Model
		ExtendedIterator<OntClass> iter = ontModel.listClasses();

		// print out the predicate, subject and object of each statement
		logger.info("ontModel classes: ");
		while (iter.hasNext()) {
			OntClass c = iter.next();
			logger.info(c.toString());
		}
	}
	
	public static void publishModelAsVowlJson(String outputFile, Model model, Logger logger) throws FileNotFoundException {
		// serialize the model as RDF/XML
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		model.write(out, "RDF/XML");
		
		// convert the model to vowl (json) format
		Owl2Vowl owl2Vowl = new Owl2Vowl(new ByteArrayInputStream(out.toByteArray()));
		PrintWriter vowlFilePrintWriter = new PrintWriter(outputFile);

		// push the vowl model to our web server
		vowlFilePrintWriter.write(owl2Vowl.getJsonAsString());
		
		vowlFilePrintWriter.close();
	}
	
	
	public static void publishModelAsRdfXml(String outputFile, Model model, Logger logger) throws FileNotFoundException {
		FileOutputStream rdfxmlFileOutputStream = new FileOutputStream(outputFile);
		model.write(rdfxmlFileOutputStream, "RDF/XML-ABBREV");		
	}

	public static void publishModelAsTtl(String outputFile, Model model, Logger logger) throws FileNotFoundException {
		FileOutputStream rdfxmlFileOutputStream = new FileOutputStream(outputFile);
		model.write(rdfxmlFileOutputStream, "Ttl");		
	}

	/*
	 * The connectToServer method connects to an OPC UA server and returns the corresponding client object.
	 * @param endpointUrl OPC UA endpoint url, e.g. "opc.tcp://localhost:48030"
	 * @return OpcUaClient object, which is connected to the server specified by endpointUrl
	 */
	public static OpcUaClient connectToServer(String endpointUrl) throws Exception {
		// Discovery available endpoints on the OPC UA Server
		EndpointDescription[] endpoints;

		try {
			endpoints = UaTcpStackClient.getEndpoints(endpointUrl).get();
		} catch (Throwable ex) {
			// try the explicit discovery endpoint as well
			String discoveryUrl = endpointUrl + "/discovery";
			endpoints = UaTcpStackClient.getEndpoints(discoveryUrl).get();
		}

		// Choose an endpoint with security policy: None
		EndpointDescription endpoint;
		endpoint = Arrays.stream(endpoints)
				.filter(e -> e.getSecurityPolicyUri().equals(SecurityPolicy.None.getSecurityPolicyUri())).findFirst()
				.orElseThrow(() -> new Exception("no desired endpoints returned"));

		// Create an OPC UA client configuration (and stick with the standard values mostly)
		OpcUaClientConfigBuilder configBuilder = OpcUaClientConfig.builder();
		configBuilder.setEndpoint(endpoint);

		// Create an OPC UA client
		OpcUaClientConfig config = configBuilder.build();
		OpcUaClient client = new OpcUaClient(config);

		// Connect to an existing OPC UA server in a synchronous way (note the .get())
		client.connect().get();
		
		return client;
	}
	
	public static DataValue readAttribute(OpcUaClient client, NodeId nodeId, AttributeId attributeId) throws Exception {
		DataValue dv = null;
    	List<NodeId> nodeIdsToRead = new ArrayList<NodeId>();
    	nodeIdsToRead.add(nodeId);
    	List<UInteger> attributesToRead = new ArrayList<UInteger>();
    	attributesToRead.add(AttributeId.Description.uid());
    	List<DataValue> res = client.read(0, null, nodeIdsToRead, attributesToRead).get(); // synchronous read
    	if(res.size() == 1)
    		dv = res.get(0); // res should only contain a single value, because only one attributeId is read from one node
    	return dv;
	}
	
	public static String[] getNamespaceArray(OpcUaClient client) throws Exception{
		// TODO: make this code nicer
        VariableNode node = client.getAddressSpace().createVariableNode(Identifiers.Server_NamespaceArray);
        DataValue value = node.readValue().get();
        Variant v = value.getValue();
        Object o = v.getValue();
        String[] stringArray = (String[]) o;
		
		return stringArray;
	}
	
	/*
	 * Sets the property identified via uri1 equivalent to the property identified via uri 2
	 * Adds a property identified via uri2 for all properties identified via uri1
	 */
	public static void setEquivalentProperty(OntModel model, String uri1, String uri2, boolean inverse, Logger logger) {
		OntProperty property1 = model.getOntProperty(uri1);
		OntProperty property2 = model.createObjectProperty(uri2);
		if(property1 == null) {
			logger.info("Property does not exist. No equivalent properties added: " + uri1);
			return;
		}
		if(property2 == null) {
			logger.error("Property does not exist: " + uri2);
			return;
		}
		if(inverse)
			property1.addInverseOf(property2);
		else
			property1.addEquivalentProperty(property2);

		Hashtable<Resource, Resource> subjectObjectPairs = new Hashtable<Resource, Resource>();
		
		StmtIterator stmts = model.listStatements();
		while(stmts.hasNext()) {
			Statement stmt = stmts.next();
			if(stmt.getPredicate().getURI().toString().equals(uri1)) {
				if(inverse)
					subjectObjectPairs.put(stmt.getObject().asResource(), stmt.getSubject());
				else
					subjectObjectPairs.put(stmt.getSubject(), stmt.getObject().asResource());
			}
		}
		for(Resource subject : subjectObjectPairs.keySet()) {
			Resource object = subjectObjectPairs.get(subject);
			subject.addProperty(property2, object);
		}
	}
	
	public static Vector<UaNode> getTypeNodes(UaClient client, UaNode node, Logger logger) throws Exception {
		Vector<UaNode> typeNodes = new Vector<UaNode>();
		
		// continue browsing child nodes if the MAX_DEPTH has not been reached yet
		BrowseDescription browseDescription = new BrowseDescription(
				node.getNodeId().get(), // nodeId
				BrowseDirection.Forward, // browseDirection
				Identifiers.HasTypeDefinition, // referenceTypeId
				true, // includeSubtypes
				uint(NodeClass.ObjectType.getValue() | NodeClass.VariableType.getValue() | NodeClass.DataType.getValue()), // nodeClassMask
				uint(BrowseResultMask.All.getValue()) // resultMask
				);

		BrowseResult browseResult = client.browse(browseDescription).get();

		List<ReferenceDescription> references = toList(browseResult.getReferences());

		for (ReferenceDescription rd : references) {
			NodeId targetNodeId = rd.getNodeId().local().get();
			typeNodes.add(client.getAddressSpace().getNodeInstance(targetNodeId).get());
		}
		return typeNodes;
	}
	
	public static Vector<UaNode> getSupertypeNodes(UaClient client, UaNode node, Logger logger) throws Exception {
		Vector<UaNode> supertypeNodes = new Vector<UaNode>();
		
		// continue browsing child nodes if the MAX_DEPTH has not been reached yet
		BrowseDescription browseDescription = new BrowseDescription(
				node.getNodeId().get(), // nodeId
				BrowseDirection.Inverse, // browseDirection
				Identifiers.HasSubtype, // referenceTypeId
				true, // includeSubtypes
				uint(NodeClass.ObjectType.getValue() | NodeClass.VariableType.getValue() | NodeClass.DataType.getValue()), // nodeClassMask
				uint(BrowseResultMask.All.getValue()) // resultMask
				);

		BrowseResult browseResult = client.browse(browseDescription).get();

		List<ReferenceDescription> references = toList(browseResult.getReferences());

		for (ReferenceDescription rd : references) {
			NodeId targetNodeId = rd.getNodeId().local().get();
			supertypeNodes.add(client.getAddressSpace().getNodeInstance(targetNodeId).get());
		}
		return supertypeNodes;
	}
}
