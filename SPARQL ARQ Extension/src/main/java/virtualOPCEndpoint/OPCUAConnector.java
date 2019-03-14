package virtualOPCEndpoint;

import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;

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
		String value_s=null;
		
		try {
			OpcUaClient client=connectToEndpoint();
		
			//defining node id
			NodeId nodeId = new NodeId(NamespaceIndex, NodeIDString);
			//read data with client
			DataValue value =
					  client.readValue(0, TimestampsToReturn.Both, nodeId)
					    .get();
			//convert value to String
			value_s=value.getValue().getValue().toString();		
			
		}catch(Exception e) {
			System.out.println("ERROR: " +e.getMessage());
		}
		finally {
			
		}
			return value_s;
		
		
	}

	public String readhistData(Integer NamespaceIndex,String NodeIDString, String startTime, String stopTime) {
		
		OpcUaClient client=connectToEndpoint();
		
		//defining node id
		NodeId nodeId = new NodeId(NamespaceIndex, NodeIDString);
		
		HistoryReadDetails historyReadDetails = new ReadRawModifiedDetails(
                false,
                DateTime.MIN_VALUE,
                DateTime.now(),
                UInteger.MAX,//uint(0), // both work
                true
        );
		
        HistoryReadValueId histReadValueId= new HistoryReadValueId(nodeId, null, QualifiedName.NULL_VALUE, ByteString.NULL_VALUE);
        
        
        //unbedingt false -> keine Ahnung warum!
        try {
		HistoryReadResponse hist = client.historyRead(historyReadDetails, TimestampsToReturn.Both, false, Arrays.asList(histReadValueId)).get();
	
		HistoryReadResult[] historyReadResults = hist.getResults();
        HistoryReadResult historyReadResult = historyReadResults[0];
        HistoryData historyData = historyReadResult.getHistoryData().decode();
        List<DataValue> dataValues = l(historyData.getDataValues());
        
        dataValues.forEach(v -> System.out.println("value=" + v));
        
        }
        catch (Exception e) {
        	System.out.println("ERROR: " +e.getMessage());
        }
		return "s";
	}

}
