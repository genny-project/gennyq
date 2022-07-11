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

import life.genny.qwandaq.constants.CacheName;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.common.key.cache.CacheKey;

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
    public String fetchProcessId(String table, String field, String value) {

		return fetchProcessId(table, Map.of(field, value));
	}

	/**
	 * Fetch the Process Id of from a GraphQL table using a query.
	 *
	 * @param table The Table to query
	 * @param queryMap A map of key-value pairs used in the query
	 * @return The process id
	 */
    public String fetchProcessId(String table, Map<String, String> queryMap) {

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
	 * @param table The table to query from
	 * @param field The field to query
	 * @param value The value for the query to match
	 * @param returns The fields to return
	 * @return A JsonArray of process instance variable objects
	 */
    public JsonArray queryTable(String table, String field, String value, String... returns) {

        return queryTable(table, Map.of(field, value), returns);
    }

	/**
	 * Query a table in the GraphQL data-index.
	 *
	 * @param table The table to query from
	 * @param queryMap A map of key-value pairs used in the query
	 * @param returns The fields to return
	 * @return A JsonArray of process instance variable objects
	 */
    public JsonArray queryTable(String table, Map<String, String> queryMap, String... returns) {

		String body = performGraphQLQuery(table, queryMap, returns);

		JsonObject bodyObj = jsonb.fromJson(body, JsonObject.class);
		JsonObject dataObj = bodyObj.getJsonObject("data");

		if (dataObj == null) {
			log.error("No data field found in: " + body);
			return null;
		}
		return dataObj.getJsonArray(table);
	}

	/**
	 * Perform a Query on table in the GraphQL data-index.
	 *
	 * @param table The table to query from
	 * @param queryMap A map of key-value pairs used in the query
	 * @param returns The fields to return
	 * @return The response body
	 */
    public String performGraphQLQuery(String table, Map<String, String> queryMap, String... returns) {

		// setup fields to query on
		String queryFields = queryMap.entrySet().stream()
			.map(e -> String.format("%s : { equal: \"%s\" }", e.getKey(), e.getValue()))
			.collect(Collectors.joining(", "));

		// create full query string
        String query = String.format("query { %s ( where: { %s }){ %s }}", 
				table, queryFields, String.join(" ", returns));

        String uri = GennySettings.dataIndexUrl() + "/graphql";
        HttpResponse<String> response = HttpUtils.post(uri, query, "application/GraphQL", userToken);

        return response.body();
    }

	/**
	 * Fetch the targetCode stored in the processInstance 
	 * for the given processId.
	 *
	 * @param processId The processId to check
	 * @return The associated TargetCode
	 */
	public String fetchProcessInstanceTargetCode(String processId) {

		log.info("Fetching targetCode for processId : " + processId);
		CacheKey targetCodeKey = new CacheKey(userToken.getProductCode(), processId+":TARGET_CODE");

		// check in cache first
		String targetCode = CacheUtils.getObject(CacheName.METADATA, targetCodeKey, String.class);
		if (targetCode != null) {
			return targetCode;
		}

		JsonArray array = queryTable("ProcessInstances", "id", processId, "variables");
		JsonObject variables = jsonb.fromJson(array.getJsonObject(0).getString("variables"), JsonObject.class);

		// grab the targetCode from process questions variables
		targetCode = variables.getString("targetCode");
		CacheUtils.putObject(CacheName.METADATA, targetCodeKey, targetCode);

		return targetCode;
	}

}
