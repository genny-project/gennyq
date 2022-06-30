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

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.UserToken;

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
	 * Query a table in the GraphQL data-index.
	 *
	 * @param table The table to query from
	 * @param field The field to query
	 * @param value The value for the query to match
	 * @param returns The fields to return
	 * @return The response body
	 */
    public String queryTable(String table, String field, String value, String... returns) {

        return queryTable(table, Map.of(field, value), returns);
    }

	/**
	 * Query a table in the GraphQL data-index.
	 *
	 * @param table The table to query from
	 * @param queryMap A map of key-value pairs used in the query
	 * @param returns The fields to return
	 * @return The response body
	 */
    public String queryTable(String table, Map<String, String> queryMap, String... returns) {

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
	 * Fetch the Process Id of from a GraphQL table using a query.
	 *
	 * @param table The Table to query
	 * @param queryMap A map of key-value pairs used in the query
	 * @return The process id
	 */
    public String fetchProcessId(String table, Map<String, String> queryMap) {

		String body = queryTable(table, queryMap, "id");
		if (StringUtils.contains(body, "Error id")) {
			log.error("No processId found");
			return null;
		}

		// isolate the id
		JsonObject json = jsonb.fromJson(body, JsonObject.class);
		JsonObject data = json.getJsonObject("data");
		JsonArray jsonArray = data.getJsonArray(table);

		if (jsonArray == null || jsonArray.isEmpty()) {
			log.error("No processId found");
			return null;
		}

		JsonObject firstItem = jsonArray.getJsonObject(0);
		return firstItem.getString("id");
    }
}
