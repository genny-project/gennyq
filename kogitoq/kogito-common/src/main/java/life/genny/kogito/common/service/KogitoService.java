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
	public Response deleteProcess(final String workflowCode, final String processId) {

		final String url = "http://localhost:8080/" + workflowCode + "/" + processId;
		log.debug("Deleting process " + url);

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Authorization", "Bearer " + serviceToken.getToken())
				.DELETE()
				.build();

		HttpResponse<String> response = null;
		try {
			response = client.send(request,
					HttpResponse.BodyHandlers.ofString());
			log.debug(response.statusCode() + ":" + response.body());
		} catch (IOException | InterruptedException e) {
			log.error(e.getLocalizedMessage());
		}
		return response.statusCode() == 200 ? Response.ok().build() : Response.serverError().build();
	}

}
