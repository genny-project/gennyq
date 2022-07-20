package life.genny.qwandaq.converter;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.validation.Validation;

@Converter
public class ValidationListConverter implements AttributeConverter<List<Validation>, String> {

	private static final Logger log = Logger.getLogger(ValidationListConverter.class);

	Jsonb jsonb = JsonbBuilder.create();

	/** 
	 * Convert a List of Validations to a Database Column
	 *
	 * @param list the List of Validations to convert
	 * @return String
	 */
	@Override
	public String convertToDatabaseColumn(final List<Validation> list) {

		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Validation v : list) {
			builder
				.add(v.getCode())
				.add(v.getName())
				.add(v.getRegex())
				.add(v.getSelectionBaseEntityGroupList().toString())
				.add((v.getMultiAllowed() ? "TRUE" : "FALSE"))
				.add((v.getRecursiveGroup() ? "TRUE" : "FALSE"));
		}
		String value = builder.build().toString();
		value = StringUtils.removeStart(value, "[");
		value = StringUtils.removeEnd(value, "]");
		
		if (value.length() >= 512) {
			log.error("Error -> field > 512 " + value + ":" + value.length());
		}

		return value;
	}

	/** 
	 * Convert a validation string to a List of Validations
	 *
	 * @param joined the string of validations to convert
	 * @return List&lt;Validation&gt;
	 */
	@Override
	public List<Validation> convertToEntityAttribute(String joined) {

		log.info(joined);
		if (StringUtils.isEmpty(joined)) {
			return new ArrayList<Validation>();
		}

		List<String> lst = stringToList(joined);

		Validation v = new Validation();
		v.setCode(lst.get(0));
		v.setName(lst.get(1));
		v.setRegex(lst.get(2));
		v.setSelectionBaseEntityGroupList(stringToList(lst.get(3)));
		v.setMultiAllowed("TRUE".equalsIgnoreCase(lst.get(4)));
		v.setRecursiveGroup("TRUE".equalsIgnoreCase(lst.get(5)));

		List<Validation> validations = new ArrayList<>();
		validations.add(v);
		return validations;
	}

	/**
	 * Helper function to convert string to list of strings
	 * @param str The string to convert
	 * @return The list if strings
	 */
	public List<String> stringToList(String str) {
		// remove brackets
		str = StringUtils.removeStart(str, "[");
		str = StringUtils.removeEnd(str, "]");
		// remove first and last quotes
		str = StringUtils.removeStart(str, "\"");
		str = StringUtils.removeEnd(str, "\"");

		return Arrays.asList(str.split("\",\""));
	}

}
	
