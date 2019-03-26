package virtualOPCEndpoint;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestOPCReadConnection {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ExtConnector opcCon= new OPCUAConnector("opc.tcp://G-StationHP:53530/OPCUA/SimulationServer");
		
		//test valueRead
		
		String val=opcCon.readValue(2, "MyLevel");	
		System.out.println("Retrieve single value from MyLevel: " + val);
		System.out.println("");
		//test histRead
		DateFormat inFormat=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		String startTimeString = "2019-03-18 19:00:00";
		
		Date startTime;
		Date stopTime = new Date();
		
		
		try {
			startTime = inFormat.parse(startTimeString);
			System.out.println("Retrieve history from "+ startTime +" to " +stopTime);
			
			TimeSeries series=opcCon.readhistData(2, "MySwitch", startTime, stopTime);	
			
			if (series.size() > 0) {
				System.out.println("First (1) entry: " + series.getValues().get(0) + " @ " + series.getTimeStamp(0));
				System.out.println("Last ("+ series.size() +") entry: " + series.getValues().get(series.size()-1) + " @ " + series.getTimeStamp(series.size()-1));
			}
			else
				System.out.println("No history!");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
