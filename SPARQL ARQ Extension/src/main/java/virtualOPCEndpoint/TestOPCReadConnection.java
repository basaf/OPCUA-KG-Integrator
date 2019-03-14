package virtualOPCEndpoint;

public class TestOPCReadConnection {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ExtConnector opcCon= new OPCUAConnector("opc.tcp://G-StationHP:53530/OPCUA/SimulationServer");
		
		//test valueRead
		String val=opcCon.readValue(2, "MyLevel");	
		System.out.println("Retruned value = " + val);
		
		//test histRead
		String val2=opcCon.readhistData(2, "MyLevel","s","s");	
		System.out.println("Retruned value = " + val2);
	}

}
