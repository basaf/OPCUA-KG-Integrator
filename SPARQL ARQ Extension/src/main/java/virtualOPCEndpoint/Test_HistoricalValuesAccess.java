package virtualOPCEndpoint;

import java.io.InputStream;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;


public class Test_HistoricalValuesAccess {

	public static void main(String[] args) {	
		String inputFileName = "../Ontologies/SimpleTestOntology.owl";
		
		Model model=ModelFactory.createDefaultModel();	
		Model tempModel=ModelFactory.createDefaultModel();
		
		// use the FileManager to find the input file
		 InputStream in = FileManager.get().open(inputFileName);
		 
		if (in == null) {
		    throw new IllegalArgumentException(
		                                 "File: " + inputFileName + " not found");
		}

		// read the RDF/XML file
		model.read(in, null);

		GetHistoricalValues.init();

		//----------start Query Data-----------------------------
		
		 String queryString = 	
				    "PREFIX opc: <http://auto.tuwien.ac.at/opcua.owl#>\r\n" +
			  		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n" + 
					"SELECT * \r\n"+
			  		"WHERE {\r\n" +	
			  		" ?a opc:nodeId ?c.\r\n"+
			  		" ?a opc:histValues (?timeStamp ?value \"2018-03-18 11:28:00\").\r\n"+
			  		
			  		//" values ?startTime {now()}.\r\n"+

			  	//"bind( now() as ?age ). \r\n" +		
			  	  "}" ;
 
		  Query query = QueryFactory.create(queryString);
		  
		 
		  //Ececute
		  try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
		    ResultSet results = qexec.execSelect() ;
		    
		    ResultSetFormatter.out(System.out,results,query);	    
//		    ResultSetFormatter.outputAsJSON(System.out,results);
		  }
		}
	
}
