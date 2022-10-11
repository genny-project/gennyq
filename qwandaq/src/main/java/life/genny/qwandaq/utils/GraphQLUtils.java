package life.genny.qwandaq.utils;

import java.lang.invoke.MethodHandles;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.exception.checked.GraphQLException;
import life.genny.qwandaq.graphql.ProcessData;

/*
 * A non-static utility class used for operations regarding the GraphQL data-index.
 * 
 * @author Adam Crow
 * @author Jasper Robison
 */
@ApplicationScoped
public class GraphQLUtils {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
	private static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	/**
	 * Fetch the Process Id of from a GraphQL table using a query.
	 *
	 * @param table The Table to query
	 * @param field The field to query on
	 * @param value The value the field must equal
	 * @return The process id
	 */
	public String fetchProcessId(String table, String field, String value) throws GraphQLException {
		return fetchProcessId(table, Map.of(field, value));
	}

	/**
	 * Fetch the Process Id of from a GraphQL table using a query.
	 *
	 * @param table    The Table to query
	 * @param queryMap A map of key-value pairs used in the query
	 * @return The process id
	 */
	public String fetchProcessId(String table, Map<String, String> queryMap) throws GraphQLException {

		JsonArray array = queryTable(table, queryMap, "id");

		if (array == null || array.isEmpty()) {
			log.error("No Id found in " + table);
			return null;
		}

		if (array.size() > 1) {
			log.warn("Found more than one Id for this query... defaulting to first in list!");
		}

		return array.getJsonObject(0).getString("id");
	}

	/**
	 * Query a table in the GraphQL data-index.
	 *
	 * @param table   The table to query from
	 * @param field   The field to query
	 * @param value   The value for the query to match
	 * @param returns The fields to return
	 * @return A JsonArray of process instance variable objects
	 */
	public JsonArray queryTable(String table, String field, String value, String... returns) throws GraphQLException {

		return queryTable(table, Map.of(field, value), returns);
	}

	/**
	 * Query a table in the GraphQL data-index.
	 *
	 * @param table    The table to query from
	 * @param queryMap A map of key-value pairs used in the query
	 * @param returns  The fields to return
	 * @return A JsonArray of process instance variable objects
	 */
	public JsonArray queryTable(String table, Map<String, String> queryMap, String... returns) throws GraphQLException {

		String body = performGraphQLQuery(table, queryMap, returns);
		log.debug("GraphQL Response Body: " + body);

		JsonObject bodyObj = jsonb.fromJson(body, JsonObject.class);
		JsonObject dataObj = bodyObj.getJsonObject("data");

		if (dataObj == null) {
			log.error("No data field found in: " + body);
			return null;
		}
		if (dataObj.containsKey(table)) {
			JsonArray retArray = null;
			try {
				retArray = dataObj.getJsonArray(table);
				return retArray;
			} catch (Exception e) {
				log.error("No " + table + " field found in: " + body + " with queryMap_> " + queryMap);
				return null;
			}

		}
		log.warnf("No table %s found in %s", table, body);
		return null;
	}

	/**
	 * Perform a Query on table in the GraphQL data-index.
	 *
	 * @param table    The table to query from
	 * @param queryMap A map of key-value pairs used in the query
	 * @param returns  The fields to return
	 * @return The response body
	 */
	public String performGraphQLQuery(String table, Map<String, String> queryMap, String... returns) throws GraphQLException {

		// setup fields to query on
		String queryFields = queryMap.entrySet().stream()
				.map(e -> String.format("%s : { equal: \"%s\" }", e.getKey(), e.getValue()))
				.collect(Collectors.joining(", "));


		// create full query string in this format (String.format is incredibly slow)
		// "query { <table> ( where: { <queryFields> }){ <returns> }}"
		String query = new StringBuilder("query { ")
			.append(table)
			.append(" ( where: { ")
			.append(queryFields)
			.append(" }){ ")
			.append(String.join(" ", returns))
			.append(" }}")
			.toString();

		log.info("GraphQL Query: " + query);

		String uri = GennySettings.dataIndexUrl() + "/graphql";
		HttpResponse<String> response = HttpUtils.post(uri, query, "application/GraphQL", userToken);

		if (response == null)
			throw new GraphQLException("Bad response from graphql");

		return response.body();
	}

	/**
	 * Fetch the targetCode stored in the processInstance
	 * for the given processId.
	 *
	 * @param processId The processId to check
	 * @return The associated TargetCode
	 */
	public String fetchProcessInstanceTargetCode(String processId) throws GraphQLException {

		log.info("Fetching targetCode for processId : " + processId);

		// check in cache first
		String targetCode = CacheUtils.getObject(userToken.getProductCode(), processId + ":TARGET_CODE", String.class);
		if (targetCode != null) {
			return targetCode;
		}

		JsonArray array = queryTable("ProcessInstances", "id", processId, "variables");
		JsonObject variables = jsonb.fromJson(array.getJsonObject(0).getString("variables"), JsonObject.class);

		// grab the targetCode from process questions variables
		targetCode = variables.getString("targetCode");
		CacheUtils.putObject(userToken.getProductCode(), processId + ":TARGET_CODE", targetCode);

		return targetCode;
	}

	/**
	 * Fetch the targetCode stored in the processInstance
	 * for the given processId.
	 * 
	 * @param processId The id of the process to fetch for
	 * @return The process data
	 */
	public ProcessData fetchProcessData(String processId) throws GraphQLException {

		log.info("Fetching processBE for processId : " + processId);
		String key = String.format("%s:PROCESS_DATA", processId);

		// check cache first
		ProcessData processData = CacheUtils.getObject(userToken.getProductCode(), key, ProcessData.class);
		if (processData != null) {
			return processData;
		}

		// otherwise query graphql
		JsonArray array = queryTable("ProcessInstances", "id", processId, "variables");
		if (array.isEmpty()) {
			log.error("Nothing found for processId: " + processId);
			return null;
		}

		// grab json and deserialise
		JsonObject variables = jsonb.fromJson(array.getJsonObject(0).getString("variables"), JsonObject.class);
		String processJson = variables.getJsonObject("processData").toString();
		processData = jsonb.fromJson(processJson, ProcessData.class);

		// cache the object for quicker retrieval
		CacheUtils.putObject(userToken.getProductCode(), key, processData);

		return processData;
	}

}
