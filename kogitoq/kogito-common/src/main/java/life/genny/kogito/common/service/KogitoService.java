package life.genny.kogito.common.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.ServiceToken;

@ApplicationScoped
public class KogitoService {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	ServiceToken serviceToken;

	/**
	 * Call a kogito api to delete a process.
	 * 
	 * @param workflowCode
	 * @param processId
	 */
	public void deleteProcess(final String workflowCode, final String processId) {
		log.info(
				"Pretending to delete the process, but really allowing the workflow to go through to a terminating end");

		TimeUnit.MINUTES.sleep(1); // wait a minute to let workflow finish

		// final String url = "http://localhost:8080/" + workflowCode + "/" + processId;
		// log.debug("Deleting process " + url);
		// log.debug("token=" + serviceToken.getToken());

		// HttpClient client = HttpClient.newHttpClient();
		// HttpRequest request = HttpRequest.newBuilder()
		// .uri(URI.create(url))
		// .header("Authorization", "Bearer " + serviceToken.getToken())
		// .DELETE()
		// .build();

		// HttpResponse<String> response = null;
		// try {
		// response = client.send(request,
		// HttpResponse.BodyHandlers.ofString());
		// log.debug(response.statusCode() + ":" + response.body());
		// } catch (IOException | InterruptedException e) {
		// log.error(e.getLocalizedMessage());
		// }
		// return response.statusCode() == 200 ? Response.ok().build() :
		// Response.serverError().build();
	}

}
