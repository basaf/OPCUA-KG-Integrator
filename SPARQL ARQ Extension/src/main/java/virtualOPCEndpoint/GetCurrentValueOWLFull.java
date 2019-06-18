package virtualOPCEndpoint;


import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.Lock;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.iterator.QueryIterNullIterator;
import org.apache.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import org.apache.jena.sparql.engine.iterator.QueryIterSingleton;
import org.apache.jena.sparql.pfunction.*;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.VCARD;


public class GetCurrentValueOWLFull implements PropertyFunctionFactory{
	
	public static void init() {
		//register custom property function 
		final PropertyFunctionRegistry reg = PropertyFunctionRegistry.chooseRegistry(ARQ.getContext());
		reg.put("http://auto.tuwien.ac.at/opcua.owl#value2", new GetCurrentValueOWLFull());
		PropertyFunctionRegistry.set(ARQ.getContext(), reg);
	}
    
	@Override
	public PropertyFunction create(final String uri)
    {   
    	return new PFuncSimple() {
			
			@Override
			public QueryIterator execEvaluated(Binding parent, Node subject, Node predicate, Node object,
					ExecutionContext execCxt) {
				
				//get datapoint URI
				String datapointURI=subject.getURI().toString();
				
				//create the model from the context
				Graph graph=execCxt.getActiveGraph();
				Model model =ModelFactory.createModelForGraph(graph);			
				
				//find db, table name and column
//				 String queryString = 	
//						    "PREFIX opc: <http://auto.tuwien.ac.at/opcua.owl#>\r\n" + 
//					  		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n" + 
//							"SELECT ?nsIndex ?id \r\n" +
//					  		"WHERE {\r\n" +	
//					  		"<"+datapointURI+">" + " opc:nodeId ?NodeId.\r\n"+
//					  		"?NodeId opc:namespaceIndex ?nsIndex.\r\n"+
//					  		"?NodeId opc:identifierType ?idType.\r\n"+
//					  		"?NodeId opc:identifier ?id.\r\n"+
//					  	  "}" ;
//				
//				ResultSet results = null;						
//				List<Binding> bindings = new ArrayList<>() ;			
//				Query query = QueryFactory.create(queryString) ;
				
				
				String subjectString=subject.toString();
				
				String patternNs="ns=(\\d+)";
				String patternId="id=(\\d+)";
				
				Pattern rNs=Pattern.compile(patternNs);
				Pattern rId=Pattern.compile(patternId);
				
				Matcher m=rNs.matcher(subjectString);
				
				String ns=null;
				if (m.find()) {
					ns=m.group(1);
				}
		
				
				m=rId.matcher(subjectString);
				
				String id=null;
				if (m.find()) {
					id=m.group(1);
				}
		
				

				
									
				List<Binding> bindings = new ArrayList<>() ;

				if(ns!=null && id!=null) {		
		    			   
				  //Retrieve data 
				    //TODO: change to dynamic opc-endpoint identification from information stored in the ontology
					ExtConnector opcCon= new OPCUAConnector("opc.tcp://localhost:48030"); 
							
					String currentVal=opcCon.readValue( Integer.parseInt(ns), Integer.parseInt(id));
					
					// create string literal
					if (currentVal != null) {
						//Node bNode=NodeFactory.createLiteral(currentVal);
						
						Node bNode=NodeFactory.createLiteral(currentVal, XSDDatatype.XSDdecimal);
						final Binding b = BindingFactory.binding(parent, Var.alloc(object),bNode);
						bindings.add(b);
					}
				}   
					
				
				return new QueryIterPlainWrapper(bindings.iterator(), execCxt) ;
								
			}


		};
        
    }

	

}
