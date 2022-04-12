package life.genny.qwandaq.datatype;

import java.time.LocalDateTime;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {
	
	/** 
	 * @param s the string to unmarshal
	 * @return LocalDateTime
	 * @throws Exception if something goes wrong
	 */
	@Override
	public LocalDateTime unmarshal(String s) throws Exception {
		return LocalDateTime.parse(s);
	}

	
	/** 
	 * @param dateTime the datetime to marshal
	 * @return String
	 * @throws Exception if something goes wrong
	 */
	@Override
	public String marshal(LocalDateTime dateTime) throws Exception {
		return dateTime.toString();
	}
}
