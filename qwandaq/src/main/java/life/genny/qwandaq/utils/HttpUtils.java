package life.genny.qwandaq.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import org.jboss.logging.Logger;



/**
 * A Static utility class for standard HTTP requests.
 * 
 * @author Jasper Robison
 */
public class HttpUtils {

	static final Logger log = Logger.getLogger(CacheUtils.class);

	static Jsonb jsonb = JsonbBuilder.create();

	/**
	 * Create and send a PUT request.
	 *
	 * @param uri   The target URI of the request.
	 * @param body  The json string to use as the body.
	 * @param token The token to use in authorization.
	 * @return The returned response object.
	 */
	public static java.net.http.HttpResponse<String> put(String uri, String body, String token) {

		HttpClient client = HttpClient.newHttpClient();

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(uri))
				.setHeader("Content-Type", "application/json")
				.setHeader("Authorization", "Bearer " + token)
				.PUT(HttpRequest.BodyPublishers.ofString(body))
				.build();

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return response;
		} catch (IOException | InterruptedException e) {
			log.error(e);
		}

		return null;
	}

	/**
	 * Create and send a POST request.
	 *
	 * @param uri   The target URI of the request.
	 * @param body  The json string to use as the body.
	 * @param token The token to use in authorization.
	 * @return The returned response object.
	 */
	public static HttpResponse<String> post(String uri, String body, String token) {

		return post(uri, body, "application/json", token);
	}

	/**
	 * Create and send a POST request.
	 *
	 * @param uri         The target URI of the request.
	 * @param body        The json string to use as the body.
	 * @param contentType The contentType to use in the header. Default:
	 *                    "application/json"
	 * @param token       The token to use in authorization.
	 * @return The returned response object.
	 */
	public static java.net.http.HttpResponse<String> post(String uri, String body, String contentType, String token) {

		HttpClient client = HttpClient.newHttpClient();

		Builder requestBuilder = null;
		
		try {
		requestBuilder = HttpRequest.newBuilder()
				.uri(URI.create(uri))
				.setHeader("Content-Type", contentType);
	} catch (IllegalArgumentException e) {
		log.error("bad URI ->["+uri+"]");
		log.error(e);
		}
		if (token != null) {
			requestBuilder.setHeader("Authorization", "Bearer " + token);
		}

		HttpRequest request = requestBuilder
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return response;
		} catch (IOException | InterruptedException e) {
			log.error("Error getting response back from " + uri);
			log.error(e);
			e.printStackTrace();
		}

		log.error("Null response from: " + uri);
		log.error("Payload: " + body);

		return null;
	}

	/**
	 * Create and send a GET request.
	 *
	 * @param uri   The target URI of the request.
	 * @param token The token to use in authorization.
	 * @return The returned response object.
	 */
	public static HttpResponse<String> get(String uri, String token) {

		HttpClient client = HttpClient.newHttpClient();

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(uri))
				.setHeader("Content-Type", "application/json")
				.setHeader("Authorization", "Bearer " + token)
				.GET().build();

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return response;
		} catch (IOException | InterruptedException e) {
			log.error(e);
		}

		return null;
	}

	/**
	 * Create and send a DELETE request.
	 *
	 * @param uri   The target URI of the request.
	 * @param token The token to use in authorization.
	 * @return The returned response object.
	 */
	public static HttpResponse<String> delete(String uri, String token) {

		HttpClient client = HttpClient.newHttpClient();

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(uri))
				.setHeader("Content-Type", "application/json")
				.setHeader("Authorization", "Bearer " + token)
				.DELETE().build();

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return response;
		} catch (IOException | InterruptedException e) {
			log.error(e);
		}

		return null;
	}

	/**
	 * Build an error message json string from a msg string.
	 *
	 * @param msg The error message used to construct the json.
	 * @return A stringified json object containing an error msg and status.
	 */
	public static String error(String msg) {

		JsonObject json = Json.createObjectBuilder()
				.add("status", "failed")
				.add("error", msg)
				.build();

		return jsonb.toJson(json);
	}

	/**
	 * Build an ok status json string;
	 *
	 * @return A stringified json object containing an ok status.
	 */
	public static String ok() {

		JsonObject json = Json.createObjectBuilder()
				.add("status", "ok")
				.build();

		return jsonb.toJson(json);
	}

	/**
	 * Extract the token from a header and handle different scenarios such as:
	 * - The authorization parameter does not have spaces
	 * - The authorization parameter does not start with bearer
	 * - The authorization parameter starts with bearer
	 * - The authorization parameter only has bearer
	 * - The authorization parameter only has token
	 * - The authorization parameter starts with bearer and join by space with a
	 * token
	 *
	 *
	 * @param authorization Value of the authorization header normally with this
	 *                      format: Bearer eydsMSklo30...
	 *
	 * @return token Token extracted or the same token if nothing found to extract
	 */
	public static String extractTokenFromHeaders(String authorization) {

		String[] authValues = authorization.split(" ");

		if (authValues.length < 2) {
			if (authValues.length != 0
					&& !authValues[0].equalsIgnoreCase("bearer")
					&& authValues[0].length() > 5) {

				return authValues[0];
			}
		}

		return authValues[1];
	}
}
