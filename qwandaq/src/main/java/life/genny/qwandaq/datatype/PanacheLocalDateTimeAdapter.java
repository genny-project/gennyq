package life.genny.qwandaq.datatype;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.bind.adapter.JsonbAdapter;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PanacheLocalDateTimeAdapter implements JsonbAdapter<LocalDateTime, JsonValue> {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	public PanacheLocalDateTimeAdapter() {}

	/** 
	 * @param obj the object to adapt to json
	 * @return JsonValue
	 * @throws Exception if something goes wrong
	 */
	@Override
	public JsonValue adaptToJson(LocalDateTime obj) throws Exception {

		String dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
		String localDateTimeStr = obj.format(dateFormatter);
		return Json.createValue(localDateTimeStr);

	}


	/** 
	 * @param obj the object to adapt from json
	 * @return LocalDateTime
	 * @throws Exception if something goes wrong
	 */
	@Override
	public LocalDateTime adaptFromJson(JsonValue obj) throws Exception {

		String dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
		String str = obj.toString();
		str = str.substring(0,str.length()-1);
		String value = str.substring(1);
		LocalDateTime ret = dateFormatter.parse(value, LocalDateTime::from);

		return ret;
	}


	/** 
	 * @param dateTimeStr the datetime string to convert
	 * @param zoneOffset the zone offset to use in conversion
	 * @return LocalDateTime
	 */
	static public LocalDateTime getLocalDateTimeFromString(final String dateTimeStr, ZoneOffset zoneOffset) {

		TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(dateTimeStr);
		Instant i = Instant.from(ta);
		LocalDateTime dt  = LocalDateTime.ofInstant(i, zoneOffset);
		return dt;
	}
}
