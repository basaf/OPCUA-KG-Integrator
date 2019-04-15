package virtualOPCEndpoint;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;

public class TestEndpointRetrieval {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

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
					"SELECT ?value \r\n"+
			  		"WHERE {\r\n" +	
					
						"?endpoint rdfs:label \"EndpointUrl\".\r\n"+
						"?endpoint uaBase:Value ?value.\r\n"+
					    "FILTER(REGEX(STR(?endpoint), \"^http://auto.tuwien.ac.at/packed-bed-regenerator-session.owl#\"))\r\n"+
			  	  "}" ;



		
		  Query query = QueryFactory.create(queryString);		  
		 
		  String firstResult=null;
		  //Ececute
		  try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
		    ResultSet results = qexec.execSelect() ;
    
		    
		    firstResult= results.next().toString();
		    
		    ResultSetFormatter.out(System.out,results,query);	    
		  }
		  
		  System.out.println(firstResult);
		  

			
			String patternEndpoint="opc.tcp:.*\\/";
			
			Pattern rEndpoint=Pattern.compile(patternEndpoint);
	
			
			Matcher m=rEndpoint.matcher(firstResult);
			
			String enpointUrl=null;
			if (m.find()) {
				enpointUrl=m.group(0);
			}
			System.out.println(enpointUrl);

		  
	}
	

}
