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
	private static final int MAX_DEPTH = 40;
	private static final String uaBaseNsUaStyle = "http://opcfoundation.org/UA/";
	private static final String uaBaseNsRdfStyle = "http://opcfoundation.org/UA/#";
	private static final String outputDir = "C:\\Users\\user1\\Desktop\\VirtualEndpointsProofOfConcept\\Git\\VirtualOPCUA2SPQARL\\Ontologies\\";
	

	// Logging
	private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main( String[] args ) throws Exception
    {
        System.out.println( "Opcua2Owl" );

    	Vector<NodeId> rootNodeIds = new Vector<NodeId>();
    	rootNodeIds.add(new NodeId(0, 58));
        
        System.out.print("Transforming OPC UA address space ...");
        Opcua2ModelTransformer opcua2ModelTransformer = new Opcua2ModelTransformer(ENDPOINT_URL, logger);
        Map<String, Model> models = opcua2ModelTransformer.transform(rootNodeIds, MAX_DEPTH);
        System.out.println();
        
        int i = 0;
        
        /*
        // publish models as plain rdf
        for(String ns : models.keySet()) {
        	System.out.println("Publishing ns" + i + " " + ns + " ...");
    		HelperFunctions.publishModelAsRdfXml(outputDir + "ns" + i + ".rdf", models.get(ns), logger);
        	i++;
        }
        */

        // postProcess and publish models
        Map<String, String> nsSubstitutionTable = Collections.synchronizedMap(new LinkedHashMap<String, String>());
        nsSubstitutionTable.put(uaBaseNsRdfStyle, "http://auto.tuwien.ac.at/~ontologies/opcua.owl#");
        nsSubstitutionTable.put("http://auto.tuwien.ac.at/PackedBedRegenerator/#", "http://auto.tuwien.ac.at/~ontologies/packed-bed-regenerator.owl#");
        nsSubstitutionTable.put("urn:pauker-nb:ASG:ASG_NS#", "http://auto.tuwien.ac.at/~ontologies/session.owl#");
        PostProcessor postProcessor = new OWLPostProcessor();
        i = 0;
        for(String nsRdfStyle : models.keySet()) {
            System.out.println("Post processing ns" + i + " " + nsRdfStyle + " ...");
        	OntModel ontModel = postProcessor.process(models, nsRdfStyle, false, logger);
        	ontModel = postProcessor.substitudeNamespaces(ontModel, nsSubstitutionTable);
        	String substitutedNs = nsSubstitutionTable.get(nsRdfStyle);
        	String filename = substitutedNs.substring(substitutedNs.lastIndexOf('/') + 1).replace("#", "");
        	System.out.println("Publishing ns " + filename + " ...");
    		HelperFunctions.publishModelAsRdfXml(outputDir + filename, ontModel, logger);
        	i++;
        }

    }
}
