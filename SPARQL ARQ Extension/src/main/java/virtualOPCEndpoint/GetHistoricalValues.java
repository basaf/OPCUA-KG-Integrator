package virtualOPCEndpoint;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import org.apache.jena.sparql.pfunction.PFuncSimpleAndList;
import org.apache.jena.sparql.pfunction.PropFuncArg;
import org.apache.jena.sparql.pfunction.PropertyFunction;
import org.apache.jena.sparql.pfunction.PropertyFunctionFactory;
import org.apache.jena.sparql.pfunction.PropertyFunctionRegistry;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;

import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.ParseException;

public class GetHistoricalValues implements PropertyFunctionFactory {

	public static void init() {
		//register custom property function 
		final PropertyFunctionRegistry reg = PropertyFunctionRegistry.chooseRegistry(ARQ.getContext());
		reg.put("http://auto.tuwien.ac.at/opcua.owl#histValues", new GetHistoricalValues());
		PropertyFunctionRegistry.set(ARQ.getContext(), reg);
	}
	
	@Override
	public PropertyFunction create(String uri) {
		// TODO Auto-generated method stub
		return new PFuncSimpleAndList() {

					
			@Override
			public QueryIterator execEvaluated(Binding binding, Node subject, Node predicate, PropFuncArg object,
					ExecutionContext execCxt) {
				// TODO Auto-generated method stub

				int c=object.getArgListSize();
								
				switch (c) {
					case 3: // histValues (?time ?val "startTimeString")
						DateFormat inFormat=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
						String startTimeString=object.getArg(2).toString();
						
						
						try {
							startTimeString=startTimeString.replaceAll("\"", "");
							Date startTime =inFormat.parse(startTimeString);
							System.out.println(startTime);
							Date stopTime=new Date();
	
							
							//TimeSeries series=opcCon.readhistData(2, "MyLevel", startTime, stopTime);
							
							
							//get datapoint URI
							String datapointURI=subject.getURI().toString();
							
							//create the model from the context
							Graph graph=execCxt.getActiveGraph();
							Model model =ModelFactory.createModelForGraph(graph);			
							
							//find db, table name and column
							 String queryString = 	
									    "PREFIX opc: <http://auto.tuwien.ac.at/opcua.owl#>\r\n" + 
								  		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n" + 
										"SELECT ?nsIndex ?id \r\n" +
								  		"WHERE {\r\n" +	
								  		"<"+datapointURI+">" + " opc:nodeId ?NodeId.\r\n"+
								  		"?NodeId opc:namespaceIndex ?nsIndex.\r\n"+
								  		"?NodeId opc:identifierType ?idType.\r\n"+
								  		"?NodeId opc:identifier ?id.\r\n"+
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
								
								
									
									//String currentVal=opcCon.readValue( Integer.parseInt(qsol.get("nsIndex").toString()), qsol.get("id").toString());
									TimeSeries series=opcCon.readhistData(Integer.parseInt(qsol.get("nsIndex").toString()), qsol.get("id").toString(), startTime, stopTime);
									
									//serialize to json
									// create string literal
									for (int i=0; i<series.size();i++) {
										
										Node timeNode=NodeFactory.createLiteral(series.getTimeStamp(i).toString());
										Node valueNode=NodeFactory.createLiteral(series.getValue(i));
										//Node bnode=NodeFactory.createBlankNode();
										
										BindingMap bindingMap = new BindingHashMap(binding);
										
										
										//final Binding timeBinding = BindingFactory.binding(oB, Var.alloc(object.getArg(0)),timeNode);
										bindingMap.add(Var.alloc(object.getArg(0)),timeNode);
										//final Binding valueBinding = BindingFactory.binding(oB, Var.alloc(object.getArg(1)),valueNode);
										bindingMap.add(Var.alloc(object.getArg(1)),valueNode);
										//bindings.add(timeBinding);
										//bindings.add(valueBinding);
										bindings.add(bindingMap);
									}
									}   
							}
							
							return new QueryIterPlainWrapper(bindings.iterator(), execCxt) ;
							
							
							
							
							
							
							
							
							
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						//readhistData(Integer NamespaceIndex,String NodeIDString, Date startTime, Date stopTime);
						
						break;
					case 2: break;
					default: break; //invalid
				}
				for (Node i : object.getArgList()) {
					System.out.println(i);
				}
				return null;
			}
			
		};
		

	}
	

}
