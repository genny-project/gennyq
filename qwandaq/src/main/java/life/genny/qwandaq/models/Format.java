package life.genny.qwandaq.models;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriterFactory;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.stream.JsonGenerator;

/**
 * Format
 */
public class Format {

	/**
	 * Build a string.
	 * @param args Arguments
	 * @return String
	 */
	public static String string(Object... args) {

		StringBuilder builder = new StringBuilder();
		Arrays.stream(args).forEach(a -> {
			builder.append(a.toString());
		});

		return builder.toString();
	}

	/**
	 * Get a prettified json string.
	 * @param json Json String
	 * @return String
	 */
	public static String pretty(String json) {

		Jsonb jsonb = JsonbBuilder.create();
		return pretty(jsonb.fromJson(json, JsonObject.class));
	}

	/**
	 * Get a prettified json string.
	 * @param json Json Object
	 * @return String
	 */
	public static String pretty(JsonObject json) {

		Map<String, Boolean> config = new HashMap<>();
		config.put(JsonGenerator.PRETTY_PRINTING, true);

		JsonWriterFactory writerFactory = Json.createWriterFactory(config);
		Writer writer = new StringWriter();
		writerFactory.createWriter(writer).write(json);

		return writer.toString();
	}
}
