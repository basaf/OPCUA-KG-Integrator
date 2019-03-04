package virtualOPCEndpoint;

public class TestOPCReadConnection {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ExtConnector opcCon= new OPCUAConnector("opc.tcp://G-StationHP:53530/OPCUA/SimulationServer");
		
		String val=opcCon.readValue(2, "MyLevel");
		
		
		System.out.println("Retruned value = " + val);
	}

}
