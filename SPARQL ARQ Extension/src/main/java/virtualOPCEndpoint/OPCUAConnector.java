package virtualOPCEndpoint;

import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryData;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadDetails;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadResult;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadRawModifiedDetails;
import java.util.Date;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OPCUAConnector implements ExtConnector{

	private String endpoint=null;
	
	public OPCUAConnector(String endpoint) {
		this.endpoint=endpoint; 

	}
	
	private OpcUaClient connectToEndpoint() {
		//TODO: decide how to select endpoint from ontology information
		OpcUaClient client=null;
		try {
		EndpointDescription[] endpoints =
				  UaTcpStackClient.getEndpoints(endpoint)
				    .get();
		
		OpcUaClientConfigBuilder cfg = new OpcUaClientConfigBuilder();
		cfg.setEndpoint(endpoints[0]);
	
		//connect to endpoint
		client = new OpcUaClient(cfg.build());
		client.connect().get();
		//System.out.println("connected!");
		}
		catch(Exception e) {
			System.out.println("ERROR: " +e.getMessage());
		}
		
		return client;
	}
	
	public String readValue(Integer NamespaceIndex,String NodeIDString) {
		
		NodeId nodeId = new NodeId(NamespaceIndex, NodeIDString);	
		return getValue(nodeId);
				
	}
	
	public String readValue(Integer NamespaceIndex,Integer NodeIDString) {
		
		NodeId nodeId = new NodeId(NamespaceIndex, NodeIDString);	
		return getValue(nodeId);		
	}
	
	private String getValue(NodeId nodeId) {
		String value_s=null;
		try {
			OpcUaClient client=connectToEndpoint();
		
			//read data with client
			DataValue value =
					  client.readValue(0, TimestampsToReturn.Both, nodeId)
					    .get();
			//convert value to String
			value_s=value.getValue().getValue().toString();		
			
			client.disconnect();
		}catch(Exception e) {
			System.out.println("ERROR: " +e.getMessage());
		}

		
		return value_s;
	}

	public TimeSeries readhistData(Integer NamespaceIndex,String NodeIDString, Date startTime, Date stopTime) {
		
		OpcUaClient client=connectToEndpoint();
		
		//defining node id
		NodeId nodeId = new NodeId(NamespaceIndex, NodeIDString);
		
		DateTime startDateTime= new DateTime(startTime);
		DateTime stopDateTime=new DateTime(stopTime);
		
		HistoryReadDetails historyReadDetails = new ReadRawModifiedDetails(
                false,
               // DateTime.MIN_VALUE,
                startDateTime,
                stopDateTime,
                uint(0),//UInteger.MAX,//uint(0), // both work
                true
        ); 
		
        HistoryReadValueId histReadValueId= new HistoryReadValueId(nodeId, null, QualifiedName.NULL_VALUE, ByteString.NULL_VALUE);
        
        
        //unbedingt false -> keine Ahnung warum!
        TimeSeries timeSeries=null;
        try {
		HistoryReadResponse hist = client.historyRead(historyReadDetails, TimestampsToReturn.Source, false, Arrays.asList(histReadValueId)).get();
	
		HistoryReadResult[] historyReadResults = hist.getResults();
        HistoryReadResult historyReadResult = historyReadResults[0];
        HistoryData historyData = historyReadResult.getHistoryData().decode();
        List<DataValue> dataValues = l(historyData.getDataValues());
        

//        System.out.println("First Element: " + dataValues.get(0).getValue().getValue() +" @ " + dataValues.get(0).getSourceTime().getJavaDate());
//        System.out.println("Last Element: " + dataValues.get(dataValues.size()-1).getValue().getValue() +" @ " + dataValues.get(0).getSourceTime().getJavaDate());
//        System.out.println(dataValues.size());
        
        List<Date> timeStamps= new ArrayList<>();
        List<String> values = new ArrayList<>();	
        
        dataValues.forEach(value -> {
        	
        	timeStamps.add(value.getSourceTime().getJavaDate());
        	values.add(value.getValue().getValue().toString());
        });
        
        timeSeries= new TimeSeries(timeStamps, values);
        client.disconnect();
        
        }
        catch (Exception e) {
        	System.out.println("ERROR: " +e.getMessage());
        }
		return timeSeries;
	}

}
