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

public class TestSPARQL2OPCValueAccess {
	
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


	//register custom property function 
	//GetDBValues.init();
	GetCurrentValue.init();
		
//	Dataset ds = DatasetFactory.createTxnMem();
//	ds.addNamedModel("ds", model);
//	ds.addNamedModel("tempModel", tempModel);
	
	//----------start Query Data-----------------------------
	
	 String queryString = 	
			    "PREFIX opc: <http://auto.tuwien.ac.at/opcua.owl#>\r\n" +
		  		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n" + 
				"SELECT * \r\n"+
		  		"WHERE {\r\n" +	
		  		" ?a opc:nodeId ?c.\r\n"+
		  		" ?a opc:value ?value.\r\n"+
		  	  "}" ;

//	 String queryString = 	
//			    "PREFIX opc: <http://auto.tuwien.ac.at/opcua.owl#>\r\n" + 
//		  		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n" + 
//				"SELECT * \r\n"+
//		  		"WHERE {\r\n" +	
//		  		" ?a ?b ?c.\r\n"+
//		  	  "}" ;

	  Query query = QueryFactory.create(queryString);
	  
	 
	  //Ececute
	  try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
	    ResultSet results = qexec.execSelect() ;
	    
	    ResultSetFormatter.out(System.out,results,query);	    
//	    ResultSetFormatter.outputAsJSON(System.out,results);
	  }
	}
}
