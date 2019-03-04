package virtualOPCEndpoint;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;

public class OPCUAConnector implements ExtConnector{

	private String endpoint=null;
	
	public OPCUAConnector(String endpoint) {
		this.endpoint=endpoint; 

	}
	
	public String readValue(Integer NamespaceIndex,String NodeIDString) {
		String value_s=null;
		
		try {
			//TODO: decide how to select endpoint from ontology information
			EndpointDescription[] endpoints =
					  UaTcpStackClient.getEndpoints(endpoint)
					    .get();
			
			OpcUaClientConfigBuilder cfg = new OpcUaClientConfigBuilder();
			cfg.setEndpoint(endpoints[0]);
		
			//connect to endpoint
			OpcUaClient client = new OpcUaClient(cfg.build());
			client.connect().get();
			//System.out.println("connected!");
		
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

	public String readhistData() {
		// TODO Auto-generated method stub
		return null;
	}

}
