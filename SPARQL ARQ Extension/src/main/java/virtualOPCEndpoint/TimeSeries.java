package virtualOPCEndpoint;

import java.util.Date;
import java.util.List;

public class TimeSeries {

	private List<Date> timeStamps;
	private List<String> values;
	
	public TimeSeries(List<Date> timeStamp, List<String> values) {
		this.timeStamps=timeStamp;
		this.values=values;
	}
	
	public int size() {
		return timeStamps.size();
	}

	public List<Date> getTimeStamps() {
		return timeStamps;
	}
	public Date getTimeStamp(int index) {
		return timeStamps.get(index);
	}

	public List<String> getValues() {
		return values;
	}
	public String getValue(int index) {
		return values.get(index);
	}
	
}
