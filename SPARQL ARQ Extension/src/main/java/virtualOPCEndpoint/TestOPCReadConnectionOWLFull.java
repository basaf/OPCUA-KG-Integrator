package virtualOPCEndpoint;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class TestOPCReadConnectionOWLFull {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ExtConnector opcCon= new OPCUAConnector("opc.tcp://localhost:48030");
		
		//test valueRead
		
		String val=opcCon.readValue(2, 6023);	
		System.out.println("Retrieve single value 2: " + val);
		System.out.println("");
		

		

	}
}
