package life.genny.qwandaq.utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.time.Duration;
import java.net.http.HttpResponse;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import life.genny.qwandaq.exception.GennyRuntimeException;
import life.genny.qwandaq.exception.runtime.response.GennyResponseException;
import life.genny.qwandaq.models.GennyToken;

/**
 * A Static utility class for standard HTTP requests.
 * 
 * @author Jasper Robison
 * @author Bryn Meachem
 */
public class HttpUtils {

	static Jsonb jsonb = JsonbBuilder.create();

	public static final String PUT = "PUT";
	public static final String POST = "POST";
	public static final String GET = "GET";
	public static final String DELETE = "DELETE";

	public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(2);

	private static Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Create and send a PUT request.
	 *
	 * @param uri   The target URI of the request.
	 * @param body  The json string to use as the body.
	 * @param token The token to use in authorization.
	 * @return The returned response object.
	 */
	// We should be moving to using these methods beause the genny token should
	// always be well defined!
	public static HttpResponse<String> put(String uri, String body, GennyToken token) {
		return put(uri, body, token.getToken());
	}

	/**
	 * Create and send a PUT request.
	 *
	 * @param uri   The target URI of the request.
	 * @param body  The json string to use as the body.
	 * @param token The token to use in authorization.
	 * @return The returned response object.
	 */
	@Deprecated
	public static HttpResponse<String> put(String uri, String body, String token) {

		HttpRequest request = HttpRequest.newBuilder()
				.uri(createURI(uri))
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.timeout(DEFAULT_TIMEOUT)
				.PUT(HttpRequest.BodyPublishers.ofString(body))
				.build();

		HttpResponse<String> response = null;
		try {
			response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
			return response;
		} catch (IOException | InterruptedException e) {
			GennyResponseException.newBuilder(uri)
					.setRequestBody(body)
					.setToken(token)
					.setRequestType(PUT)
					.includeRequest(request)
					.fromHttpResponse(response)
					.setAssociatedException(e)
					.build()
					.printStackTrace();
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
	// We should be moving to using these methods beause the genny token should
	// always be well defined!
	public static HttpResponse<String> post(String uri, String body, GennyToken token) {
		return post(uri, body, token.getToken());
	}

	/**
	 * Create and send a POST request.
	 *
	 * @param uri   The target URI of the request.
	 * @param body  The json string to use as the body.
	 * @param token The token to use in authorization.
	 * @return The returned response object.
	 */
	@Deprecated
	public static HttpResponse<String> post(String uri, String body, String token) {
		return post(uri, body, MediaType.APPLICATION_JSON, token);
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
	// We should be moving to using these methods beause the genny token should
	// always be well defined!
	public static java.net.http.HttpResponse<String> post(String uri, String body, String contentType, GennyToken token) {
		return post(uri, body, contentType, token.getToken());
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
	@Deprecated
	public static java.net.http.HttpResponse<String> post(String uri, String body, String contentType, String token) {

		Builder requestBuilder = HttpRequest.newBuilder()
				.uri(createURI(uri))
				.setHeader(HttpHeaders.CONTENT_TYPE, contentType)
				.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
				.timeout(DEFAULT_TIMEOUT);

		if (token != null)
			requestBuilder.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

		HttpRequest request = requestBuilder
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		
		try {
			return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			GennyResponseException.newBuilder(uri)
					.setRequestBody(body)
					.setToken(token)
					.setRequestType(POST)
					.includeRequest(request)
					.setAssociatedException(e)
					.build()
					.printStackTrace();
		}

		return null;
	}

	/**
	 * Create and send a GET request.
	 *
	 * @param uri   The target URI of the request.
	 * @param token The token to use in authorization.
	 * @return The returned response object.
	 */
	// We should be moving to using these methods beause the genny token should
	// always be well defined!
	public static HttpResponse<String> get(String uri, GennyToken token) {
		return get(uri, token.getToken());
	}

	/**
	 * Create and send a GET request.
	 *
	 * @param uri   The target URI of the request.
	 * @param token The token to use in authorization.
	 * @return The returned response object.
	 */
	@Deprecated
	public static HttpResponse<String> get(String uri, String token) {

		HttpRequest request = HttpRequest.newBuilder()
				.uri(createURI(uri))
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.timeout(DEFAULT_TIMEOUT)
				.GET().build();

		HttpResponse<String> response = null;
		try {
			response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
			return response;
		} catch (IOException | InterruptedException e) {
			GennyResponseException.newBuilder(uri)
					.setToken(token)
					.setRequestType(GET)
					.includeRequest(request)
					.fromHttpResponse(response)
					.setAssociatedException(e)
					.build()
					.printStackTrace();
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
	// We should be moving to using these methods beause the genny token should
	// always be well defined!
	public static HttpResponse<String> delete(String uri, GennyToken token) {
		return delete(uri, token.getToken());
	}

	/**
	 * Create and send a DELETE request.
	 *
	 * @param uri   The target URI of the request.
	 * @param token The token to use in authorization.
	 * @return The returned response object.
	 */
	@Deprecated
	public static HttpResponse<String> delete(String uri, String token) {

		HttpRequest request = HttpRequest.newBuilder()
				.uri(createURI(uri))
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.timeout(DEFAULT_TIMEOUT)
				.DELETE().build();

		HttpResponse<String> response = null;
		try {
			response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
			return response;
		} catch (IOException | InterruptedException e) {
			GennyResponseException.newBuilder(uri)
					.setToken(token)
					.setRequestType(DELETE)
					.includeRequest(request)
					.fromHttpResponse(response)
					.setAssociatedException(e)
					.build()
					.printStackTrace();
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
	@Deprecated
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

	/**
	 * Extract the token from a header and handle different scenarios such as:
	 * - The authorization parameter does not have spaces
	 * - The authorization parameter does not start with bearer
	 * - The authorization parameter starts with bearer
	 * - The authorization parameter only has bearer
	 * - The authorization parameter only has token
	 * - The authorization parameter starts with bearer and join by space with a
	 * token
	 * @param authorization Value of the authorization header normally with this
	 *                      format: Bearer eydsMSklo30...
	 * @return token Token extracted or the same token if nothing found to extract
	 */
	public static GennyToken extractGennyTokenFromHeaders(String authorization) {
		return new GennyToken(extractTokenFromHeaders(authorization));
	}

	/**
	 * Create a URI object from a uri string.
	 * TODO: Need to work on this more.
	 * @param uri The uri string
	 * @return A URI object
	 */
	private static URI createURI(String uri) throws IllegalArgumentException {
		try {
			log.info("Creating URI: " + uri);
			return URI.create(uri);
		} catch(IllegalArgumentException e) {
			throw new GennyRuntimeException("Bad Uri: [" + uri + "]", e) {};
		}
	}
}
