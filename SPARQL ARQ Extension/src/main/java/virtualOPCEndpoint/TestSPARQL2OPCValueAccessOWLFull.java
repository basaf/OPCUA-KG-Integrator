package virtualOPCEndpoint;

import java.io.InputStream;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.*;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.iterator.QueryIterNullIterator;
import org.apache.jena.sparql.engine.iterator.QueryIterSingleton;
import org.apache.jena.sparql.pfunction.*;
import org.apache.jena.util.FileManager;

public class TestSPARQL2OPCValueAccessOWLFull {

	public static void main(String[] args) {	
		String inputFileName1 = "../Ontologies/opcua.owl";
		String inputFileName2 = "../Ontologies/packed-bed-regenerator.owl";
		String inputFileName3 = "../Ontologies/packed-bed-regenerator-session.owl";
		
		Model model1=ModelFactory.createDefaultModel();	
		Model model2=ModelFactory.createDefaultModel();
		Model model3=ModelFactory.createDefaultModel();
		Model model=ModelFactory.createDefaultModel();
		// use the FileManager to find the input file
		InputStream in1 = FileManager.get().open(inputFileName1);
		InputStream in2 = FileManager.get().open(inputFileName2);
		InputStream in3 = FileManager.get().open(inputFileName3);
		
		if (in1 == null) {
		    throw new IllegalArgumentException(
		                                 "File: " + inputFileName1 + " not found");
		}
		model1.read(in1, null);

		if (in2 == null) {
		    throw new IllegalArgumentException(
		                                 "File: " + inputFileName2 + " not found");
		}
		model2.read(in2, null);
		
		if (in3 == null) {
		    throw new IllegalArgumentException(
		                                 "File: " + inputFileName3 + " not found");
		}
		model3.read(in3, null);
		
		model.add(model1);
		model.add(model2);
		model.add(model3);
		
		GetCurrentValueOWLFull.init();
			
		
		//----------start Query Data-----------------------------
		
		 String queryString = 	
				    "PREFIX uaBase: <http://auto.tuwien.ac.at/opcua.owl#>\r\n" +
				    "PREFIX j.0: <http://auto.tuwien.ac.at/packed-bed-regenerator.owl#>\r\n"+
			  		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n" + 
			  		"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\r\n"+ 
					"SELECT (avg(?value) as ?average) \r\n"+
			  		"WHERE {\r\n" +	
					
			  		"?a rdfs:label \"PackedBedRegenerator\".\r\n"+
			  		"?a uaBase:HasComponent* ?c.\r\n"+  		
			  		"?c rdfs:label \"BulkContainer\".\r\n"+		
			  		"?c uaBase:HasComponent ?tempSensors.\r\n"+
			  		"?tempSensors rdfs:label ?label.\r\n"+
			  		"?tempSensors uaBase:HasComponent ?currentTemp.\r\n"+
			  		
			  		"?currentTemp uaBase:value2 ?value.\r\n"+
			  	  "}" ;



		
		  Query query = QueryFactory.create(queryString);		  
		 
		  //Ececute
		  try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
		    ResultSet results = qexec.execSelect() ;
		    
		    ResultSetFormatter.out(System.out,results,query);	    
		  }
		}
	
	
}
