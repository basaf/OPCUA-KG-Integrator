package virtualOPCEndpoint;

public interface ExtConnector {

	public String readValue(Integer NamespaceIndex,String NodeIDString);
	public String readhistData(Integer NamespaceIndex,String NodeIDString, String startTime, String stopTime);
}
