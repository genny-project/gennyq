package life.genny.qwandaq.utils;

import java.net.http.HttpResponse;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.message.MessageData;
import life.genny.qwandaq.message.QEventDropdownMessage;
import life.genny.qwandaq.message.QSearchMessage;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.Page;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;

/**
 * A utility class used for performing table
 * searches and search related operations.
 * 
 * @author Jasper Robison
 */
@ApplicationScoped
public class SearchUtils {

	static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	Logger log;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	CapabilitiesManager capabilityUtils;

	@Inject
	ServiceToken serviceToken;

	@Inject
	UserToken userToken;

	@Inject
	CacheManager cm;

	/**
	 * Call the Fyodor API to fetch a list of {@link BaseEntity}
	 * objects using a {@link SearchEntity} object.
	 *
	 * @param searchEntity A {@link SearchEntity} object used to determine the
	 *                     results
	 * @return A list of {@link BaseEntity} objects
	 */
	public List<BaseEntity> searchBaseEntitys(SearchEntity searchEntity) {

		// build uri, serialize payload and fetch data from fyodor
		String uri = GennySettings.fyodorServiceUrl() + "/api/search";
		String json = jsonb.toJson(searchEntity);
		HttpResponse<String> response = HttpUtils.post(uri, json, userToken);

		if (response == null) {
			log.error("Null response from " + uri);
			return null;
		}

		Integer status = response.statusCode();

		if (Response.Status.Family.familyOf(status) != Response.Status.Family.SUCCESSFUL) {
			log.errorf("Bad response status %s from %s.\nResponse body: %s", status, uri, response.body());
			return null;
		}

		try {
			// deserialise and grab entities
			Page results = jsonb.fromJson(response.body(), Page.class);
			return results.getItems();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Call the Fyodor API to fetch a count of {@link BaseEntity}
	 * objects using a {@link SearchEntity} object.
	 *
	 * @param searchEntity A {@link SearchEntity} object used to determine the
	 *                     results
	 * @return A count of items
	 */
	public Long countBaseEntitys(SearchEntity searchEntity) {

		// build uri, serialize payload and fetch data from fyodor
		String uri = GennySettings.fyodorServiceUrl() + "/api/search/count";
		String json = jsonb.toJson(searchEntity);
		HttpResponse<String> response = HttpUtils.post(uri, json, userToken);

		if (response == null) {
			log.error("Null response from " + uri);
			return null;
		}

		Integer status = response.statusCode();

		if (Response.Status.Family.familyOf(status) != Response.Status.Family.SUCCESSFUL) {
			log.error("Bad response status " + status + " from " + uri);
		}

		try {
			// deserialise and return count
			Long results = jsonb.fromJson(response.body(), Long.class);
			return results;
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Perform a table like search in Genny using a {@link SearchEntity} code.
	 * The respective {@link SearchEntity} will be fetched from the cache befor
	 * processing.
	 *
	 * @param code the code of the SearchEntity to grab from cache and search
	 */
	public void searchTable(String code) {

		if (!code.startsWith(Prefix.SBE_))
			throw new DebugException("Code " + code + " does not represent a SearchEntity");

		log.debug("Performing Table Search: " + code);

		// fetch search from cache
		String sessionCode = sessionSearchCode(code);
		SearchEntity searchEntity = cm.getObject(userToken.getProductCode(), "LAST-SEARCH:" + sessionCode, SearchEntity.class);
		if (searchEntity == null) {
			searchEntity = cm.getObject(userToken.getProductCode(), code, SearchEntity.class);
			searchEntity.setCode(sessionCode);
		}

		searchTable(searchEntity);
	}

	/**
	 * Perform a table like search in Genny using a {@link SearchEntity}.
	 *
	 * @param searchEntity the SearchEntity to search
	 */
	public void searchTable(SearchEntity searchEntity) {

		if (searchEntity == null)
			throw new NullPointerException("searchEntity");

		cm.putObject(userToken.getProductCode(), "LAST-SEARCH:" + searchEntity.getCode(),
				searchEntity);

		// remove JTI from code
		searchEntity.setCode(removeJTI(searchEntity.getCode()));

		// package and send search message to fyodor
		QSearchMessage searchBeMsg = new QSearchMessage(searchEntity);
		searchBeMsg.setToken(userToken.getToken());
		KafkaUtils.writeMsg(KafkaTopic.SEARCH_EVENTS, searchBeMsg);
	}

	/**
	 * @param searchEntity
	 * @return
	 */
	public String sessionSearchCode(SearchEntity searchEntity) {
		return sessionSearchCode(searchEntity.getCode());
	}

	/**
	 * @param code
	 * @return
	 */
	public String sessionSearchCode(String code) {
		// TODO: optimise this (contains for long patterns is inefficient)
		String jti = userToken.getJTI().toUpperCase();
		return (code.contains(jti) ? code : new StringBuilder(code).append("_").append(jti).toString());
	}

	/**
	 * @param searchEntity
	 * @return
	 */
	public String removeJTI(SearchEntity searchEntity) {
		return removeJTI(searchEntity.getCode());
	}

	/**
	 * @param code
	 * @return
	 */
	public String removeJTI(String code) {
		String jti = userToken.getJTI().toUpperCase();
		return code.replace("_".concat(jti), "");
	}

	/**
	 * @param searchBE the SearchEntity to send filter questions for
	 */
	public void sendFilterQuestions(SearchEntity searchBE) {
		log.error("Function not complete!");
	}

	/**
	 * Perform a dropdown search through dropkick.
	 *
	 * @param ask the ask to perform dropdown search for
	 */
	public void performDropdownSearch(Ask ask) {

		// setup message data
		MessageData messageData = new MessageData();
		messageData.setCode(ask.getQuestion().getCode());
		messageData.setSourceCode(ask.getSourceCode());
		messageData.setTargetCode(ask.getTargetCode());
		messageData.setValue("");

		// setup dropdown message and assign data
		QEventDropdownMessage msg = new QEventDropdownMessage();
		msg.setData(messageData);
		msg.setAttributeCode(ask.getQuestion().getAttributeCode());

		// publish to events for dropkick
		msg.setToken(userToken.getToken());
		KafkaUtils.writeMsg(KafkaTopic.EVENTS, msg);
	}

}
