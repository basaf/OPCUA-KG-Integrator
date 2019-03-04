package virtualOPCEndpoint;

//Custom property function

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
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



public class GetCurrentValue implements PropertyFunctionFactory {
	
	public static void init() {
		//register custom property function 
		final PropertyFunctionRegistry reg = PropertyFunctionRegistry.chooseRegistry(ARQ.getContext());
		reg.put("http://auto.tuwien.ac.at/opcua.owl#currentValue", new GetCurrentValue());
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
				 String queryString = 	
						    "PREFIX opc: <http://auto.tuwien.ac.at/opcua.owl#>\r\n" + 
					  		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n" + 
							"SELECT ?nsIndex ?NodeId \r\n" +
					  		"WHERE {\r\n" +	
					  		"<"+datapointURI+">" + " opc:NodeId ?NodeId.\r\n"+
					  		"?NodeId opc:NamespaceIndex ?nsIndex.\r\n"+
					  		"?NodeId opc:IdentifierType ?idType.\r\n"+
					  		"?NodeId opc:Identifier ?id.\r\n"+
					  	  "}" ;
				
				ResultSet results = null;
				
				
				List<Binding> bindings = new ArrayList<>() ;
				
				Query query = QueryFactory.create(queryString) ;
				
				try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
				    results = qexec.execSelect() ; 					    		
				
				
					if(results.hasNext()) { //query only if database entries exist
						  
					    QuerySolution qsol=results.next();
					    			   
					  //Retrieve data 
					    //TODO: change to dynamic opc-endpoint identification from information stored in the ontology
						ExtConnector opcCon= new OPCUAConnector("opc.tcp://G-StationHP:53530/OPCUA/SimulationServer"); 
						
						//String val=opcCon.readValue(2, "MyLevel");//
						String currentVal=opcCon.readValue( Integer.parseInt(qsol.get("nsIndex").toString()), qsol.get("id").toString());
						
						//serialize to json
						// create string literal
						Node bNode=NodeFactory.createLiteral(currentVal);
						final Binding b = BindingFactory.binding(parent, Var.alloc(object),bNode);
						bindings.add(b);
						
						}   
					}
				
				return new QueryIterPlainWrapper(bindings.iterator(), execCxt) ;
									
			}


		};
        
    }

}
