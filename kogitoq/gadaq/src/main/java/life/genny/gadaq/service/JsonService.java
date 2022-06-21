package life.genny.gadaq.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.Json;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.Service2ServiceData;

@ApplicationScoped
public class JsonService {

	private static final Logger log = Logger.getLogger(JsonService.class);

	Jsonb jsonb = JsonbBuilder.create();

	/**
	 * Create the json payload for calling ProcessQuestions.
	 *
	 * @param questionCode The question
	 * @param sourceCode The source
	 * @param targetCode The target
	 * @param pcmCode The PCM code
	 */
	public Service2ServiceData createProcessQuestionsJson(String questionCode, String sourceCode, String targetCode, String pcmCode) {

		JsonObject payload = Json.createObjectBuilder()
			.add("questionCode", questionCode)
			.add("sourceCode", sourceCode)
			.add("targetCode", targetCode)
			.add("pcmCode", pcmCode)
			.build();	

		Service2ServiceData data = new Service2ServiceData();
		data.setProcessId("no-id");

		return data;
	}
}
