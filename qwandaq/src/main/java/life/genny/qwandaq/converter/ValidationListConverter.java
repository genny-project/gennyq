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

		JsonArray jsonArray = jsonb.fromJson(String.format("[%s]", joined), JsonArray.class);

		Validation v = new Validation();
		v.setCode(jsonArray.getString(0));
		v.setName(jsonArray.getString(1));
		v.setRegex(jsonArray.getString(2));

		JsonArray validationGroups = jsonb.fromJson(jsonArray.getString(3), JsonArray.class);
		v.setSelectionBaseEntityGroupList(Arrays.asList(validationGroups.toArray(new String[0])));

		v.setMultiAllowed("TRUE".equalsIgnoreCase(jsonArray.getString(4)));
		v.setRecursiveGroup("TRUE".equalsIgnoreCase(jsonArray.getString(5)));

		List<Validation> validations = new ArrayList<>();
		validations.add(v);
		return validations;
	}

}
	
