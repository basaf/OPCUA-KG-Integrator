package virtualOPCEndpoint;

import java.util.Date;
public interface ExtConnector {

	public String readValue(Integer NamespaceIndex,String NodeIDString);
	public String readValue(Integer NamespaceIndex,Integer NodeIDString);
	public TimeSeries readhistData(Integer NamespaceIndex,String NodeIDString, Date startTime, Date stopTime);
}
