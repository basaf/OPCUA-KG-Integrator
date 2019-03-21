package asg.opcua2owl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class App 
{
	// OPC UA server endpoint url
	private static final String ENDPOINT_URL = "opc.tcp://localhost:48030";
	private static final int MAX_DEPTH = 30;
	private static final String uaBaseNs = "http://opcfoundation.org/UA/";
	private static final String outputDir = "C:\\Users\\user1\\Desktop\\VirtualEndpointsProofOfConcept\\Git\\VirtualOPCUA2SPQARL\\Ontologies\\";
	

	// Logging
	private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main( String[] args ) throws Exception
    {
        System.out.println( "Opcua2Owl" );

    	Vector<NodeId> rootNodeIds = new Vector<NodeId>();
    	rootNodeIds.add(new NodeId(0, 84));
        
        System.out.print("Transforming OPC UA address space ...");
        Opcua2ModelTransformer opcua2ModelTransformer = new Opcua2ModelTransformer(ENDPOINT_URL, logger);
        Map<String, Model> models = opcua2ModelTransformer.transform(rootNodeIds, MAX_DEPTH);
        System.out.println();
        
        int i = 0;
        // publish models
        /*
        for(String ns : models.keySet()) {
        	System.out.println("Publishing ns" + i + " ...");
    		HelperFunctions.publishModelAsRdfXml(outputDir + "ns" + i + ".rdf", models.get(ns), logger);
        	i++;
        }
        */
       
        // postProcess and publish models
        Map<String, String> nsSubstitutionTable = Collections.synchronizedMap(new LinkedHashMap<String, String>());
        nsSubstitutionTable.put(uaBaseNs, "http://auto.tuwien.ac.at/opcua");
        nsSubstitutionTable.put("http://auto.tuwien.ac.at/PackedBedRegenerator/", "http://auto.tuwien.ac.at/packed-bed-regenerator");
        nsSubstitutionTable.put("urn:packed-bed-regenerator-opcua-server:ASG:ASG_NS", "http://auto.tuwien.ac.at/packed-bed-regenerator-session-information");
        PostProcessor postProcessor = new OWLPostProcessor();
        i = 0;
        for(String ns : models.keySet()) {
            System.out.println("Post processing ns" + i + " ...");
        	OntModel ontModel = postProcessor.process(models, ns, false, logger);
        	ontModel = postProcessor.substitudeNamespaces(ontModel, nsSubstitutionTable);
        	String substitutedNs = nsSubstitutionTable.get(ns);
        	String filename = substitutedNs.substring(substitutedNs.lastIndexOf('/') + 1);
        	System.out.println("Publishing ns " + filename + ".owl" + " ...");
    		HelperFunctions.publishModelAsRdfXml(outputDir + filename + ".owl", ontModel, logger);
        	i++;
        }
    }
}
