package life.genny.qwandaq.utils;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.message.MessageData;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
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

	static Logger log = Logger.getLogger(SearchUtils.class);
	static Jsonb jsonb = JsonbBuilder.create();

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
		String uri = GennySettings.fyodorServiceUrl() + "/api/search/fetch";
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
			// deserialise and grab entities
			Page results = jsonb.fromJson(response.body(), Page.class);
			return results.getItems();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Call the Fyodor API to fetch a list of codes
	 * associated with result entities.
	 *
	 * @param searchEntity A {@link SearchEntity} object used to determine the
	 *                     results
	 * @return A list of code strings
	 */
	public List<String> searchBaseEntityCodes(SearchEntity searchEntity) {

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
			log.error("Bad response status " + status + " from " + uri);
		}

		try {
			// deserialise and grab entities
			Page results = jsonb.fromJson(response.body(), Page.class);
			return results.getCodes();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
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

		if (!code.startsWith(Prefix.SBE))
			throw new DebugException("Code " + code + " does not represent a SearchEntity");

		log.info("SBE CODE   ::   " + code);

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
		searchBeMsg.setDestination(GennyConstants.EVENT_WEBCMDS);
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
	 * @param baseBE    the baseBE to get associated column for
	 * @param calEACode the calEACode to get
	 * @return Answer
	 */
	public Answer getAssociatedColumnValue(BaseEntity baseBE, String calEACode) {

		String[] calFields = calEACode.substring("COL__".length()).split("__");
		if (calFields.length == 1) {
			log.error("CALS length is bad for :" + calEACode);
			return null;
		}

		String linkBeCode = calFields[calFields.length - 1];
		BaseEntity be = baseBE;
		Optional<EntityAttribute> associateEa = null;
		String finalAttributeCode = calEACode.substring("COL_".length());

		// Fetch The Attribute of the last code
		String primaryAttrCode = calFields[calFields.length - 1];
		Attribute primaryAttribute = cm.getAttribute(primaryAttrCode);

		Answer ans = new Answer(baseBE.getCode(), baseBE.getCode(), finalAttributeCode, "");
		Attribute att = new Attribute(finalAttributeCode, primaryAttribute.getName(), primaryAttribute.getDataType());
		ans.setAttribute(att);

		for (int i = 0; i < calFields.length - 1; i++) {
			String attributeCode = calFields[i];
			String calBe = be.getValueAsString(attributeCode);

			if (calBe != null && !calBe.isBlank()) {
				String calVal = beUtils.cleanUpAttributeValue(calBe);
				String[] codeArr = calVal.split(",");

				for (String code : codeArr) {
					if (code.isBlank()) {
						log.error("code from Calfields is empty calVal[" + calVal + "] skipping calFields=[" + calFields
								+ "] - be:" + baseBE.getCode());
						continue;
					}

					BaseEntity associatedBe = beUtils.getBaseEntity(code);
					if (associatedBe == null) {
						log.warn("associatedBe DOES NOT exist ->" + code);
						return null;
					}

					if (i == (calFields.length - 2)) {
						associateEa = associatedBe.findEntityAttribute(linkBeCode);

						if (associateEa != null && (associateEa.isPresent() || ("PRI_NAME".equals(linkBeCode)))) {
							String linkedValue = null;
							if ("PRI_NAME".equals(linkBeCode)) {
								linkedValue = associatedBe.getName();
							} else {
								linkedValue = associatedBe.getValueAsString(linkBeCode);
							}
							if (!ans.getValue().isEmpty()) {
								linkedValue = ans.getValue() + "," + linkedValue;
							}
							ans.setValue(linkedValue);
						} else {
							log.warn("No attribute present");
						}
					}
					be = associatedBe;
				}
			} else {
				log.warn("Could not find attribute value for " + attributeCode + " for entity " + be.getCode());
				return null;
			}
		}

		return ans;
	}

	/**
	 * @param dropdownValue the dropdownValue to perform for
	 */
	public void performQuickSearch(String dropdownValue) {

		Instant start = Instant.now();

		String productCode = userToken.getProductCode();
		String sessionCode = userToken.getJTI().toUpperCase();

		// convert to entity list
		log.info("dropdownValue = " + dropdownValue);
		String cleanCode = beUtils.cleanUpAttributeValue(dropdownValue);
		BaseEntity target = beUtils.getBaseEntity(cleanCode);

		BaseEntity project = beUtils.getBaseEntity("PRJ_" + productCode.toUpperCase());

		if (project == null) {
			log.error("Null project Entity!!!");
			return;
		}

		String jsonStr = project.getValue("PRI_BUCKET_QUICK_SEARCH_JSON", null);

		if (jsonStr == null) {
			log.error("Null Bucket Json!!!");
			return;
		}

		// init merge contexts
		HashMap<String, Object> ctxMap = new HashMap<>();
		ctxMap.put("TARGET", target);

		JsonObject json = jsonb.fromJson(jsonStr, JsonObject.class);
		JsonArray bucketMapArray = json.getJsonArray("buckets");

		for (Object bm : bucketMapArray) {

			JsonObject bucketMap = (JsonObject) bm;

			String bucketMapCode = bucketMap.getString("code");

			SearchEntity baseSearch = cm.getObject(productCode, bucketMapCode, SearchEntity.class);

			if (baseSearch == null) {
				log.error("SearchEntity " + bucketMapCode + " is NULL in cache!");
				continue;
			}

			// handle Pre Search Mutations
			JsonArray preSearchMutations = bucketMap.getJsonArray("mutations");

			for (Object m : preSearchMutations) {

				JsonObject mutation = (JsonObject) m;

				JsonArray conditions = mutation.getJsonArray("conditions");

				if (Boolean.TRUE.equals(jsonConditionsMet(conditions, target))) {

					log.info("Pre Conditions met for : " + conditions.toString());

					String attributeCode = mutation.getString("attributeCode");
					String operator = mutation.getString("operator");
					String value = mutation.getString("value");

					// TODO: allow for regular filters too
					// SearchEntity.StringFilter stringFilter =
					// SearchEntity.convertOperatorToStringFilter(operator);
					Operator filter = Operator.EQUALS;
					String mergedValue = MergeUtils.merge(value, ctxMap);
					log.info("Adding filter: " + attributeCode + " "
							+ filter + " " + mergedValue);
					baseSearch.add(new Filter(attributeCode, filter, mergedValue));
				}
			}

			// perform Search
			baseSearch.setPageSize(100000);
			log.info("Performing search for " + baseSearch.getCode());
			List<BaseEntity> results = searchBaseEntitys(baseSearch);

			JsonArray targetedBuckets = bucketMap.getJsonArray("targetedBuckets");

			if (targetedBuckets == null) {
				log.error("No targetedBuckets field for " + bucketMapCode);
			}

			// handle Post Search Mutations
			for (Object b : targetedBuckets) {

				JsonObject bkt = (JsonObject) b;

				String targetedBucketCode = bkt.getString("code");

				if (targetedBucketCode == null) {
					log.error("No code field present in targeted bucket!");
				} else {
					log.info("Handling targeted bucket " + targetedBucketCode);
				}

				JsonArray postSearchMutations = bkt.getJsonArray("mutations");

				log.info("postSearchMutations = " + postSearchMutations);

				List<BaseEntity> finalResultList = new ArrayList<>();

				for (BaseEntity item : results) {

					if (postSearchMutations != null) {

						for (Object m : postSearchMutations) {

							JsonObject mutation = (JsonObject) m;

							JsonArray conditions = mutation.getJsonArray("conditions");

							if (conditions == null) {
								if (jsonConditionMet(mutation, item)) {
									log.info("Post condition met");
									finalResultList.add(item);
								}
							} else {
								log.info("Testing conditions: " + conditions);
								if (jsonConditionsMet(conditions, target) && jsonConditionMet(mutation, item)) {
									log.info("Post condition met");
									finalResultList.add(item);
								}
							}
						}

					} else {
						finalResultList.add(item);
					}
				}

				// fetch each search from cache
				SearchEntity searchBE = cm.getObject(productCode, targetedBucketCode + "_" + sessionCode,
						SearchEntity.class);

				if (searchBE == null) {
					log.error("Null SBE in cache for " + targetedBucketCode);
					continue;
				}

				// process the associated columns
				List<EntityAttribute> cals = searchBE.findPrefixEntityAttributes("COL__");

				for (BaseEntity be : finalResultList) {

					for (EntityAttribute calEA : cals) {

						Answer ans = getAssociatedColumnValue(be, calEA.getAttributeCode());

						if (ans != null) {
							try {
								be.addAnswer(ans);
							} catch (BadDataException e) {
								e.printStackTrace();
							}
						}
					}
				}

				// send the results
				log.info("Sending Results: " + finalResultList.size());
				QDataBaseEntityMessage msg = new QDataBaseEntityMessage(finalResultList);
				msg.setToken(userToken.getToken());
				msg.setReplace(true);
				msg.setParentCode(searchBE.getCode());
				KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);

				// update and send the SearchEntity
				// updateBaseEntity(searchBE, "PRI_TOTAL_RESULTS",
				// Long.valueOf(finalResultList.size()) + "");

				Attribute attribute = cm.getAttribute("PRI_TOTAL_RESULTS");
				searchBE.addAnswer(
					new Answer(searchBE, searchBE, attribute, Long.valueOf(finalResultList.size()) + ""));
				cm.putObject(productCode, searchBE.getCode(), searchBE);

				log.info("Sending Search Entity : " + searchBE.getCode());

				QDataBaseEntityMessage searchMsg = new QDataBaseEntityMessage(searchBE);
				searchMsg.setToken(userToken.getToken());
				searchMsg.setReplace(true);
				KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, searchMsg);
			}
		}

		Instant end = Instant.now();
		log.info("Finished Quick Search: " + Duration.between(start, end).toMillis() + " millSeconds.");
	}

	/**
	 * Evaluate whether a set of conditions are met for a specific BaseEntity.
	 *
	 * @param conditions the conditions to check
	 * @param target     the target entity to check against
	 * @return Boolean
	 */
	public Boolean jsonConditionsMet(JsonArray conditions, BaseEntity target) {

		// TODO: Add support for roles and context map

		if (conditions != null) {

			// log.info("Bulk Conditions = " + conditions.toString());

			for (Object c : conditions) {

				JsonObject condition = (JsonObject) c;

				if (!jsonConditionMet(condition, target)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Evaluate whether the condition is met for a specific BaseEntity.
	 *
	 * @param condition the condition to check
	 * @param target    the target entity to check against
	 * @return Boolean
	 */
	public Boolean jsonConditionMet(JsonObject condition, BaseEntity target) {

		// TODO: Add support for roles and context map

		if (condition != null) {

			// log.info("Single Condition = " + condition.toString());

			String attributeCode = condition.getString("attributeCode");
			String operator = condition.getString("operator");
			String value = condition.getString("value");

			EntityAttribute ea = target.findEntityAttribute(attributeCode).orElse(null);

			if (ea == null) {
				log.info(
						"Could not evaluate condition: Attribute "
								+ attributeCode
								+ " for "
								+ target.getCode()
								+ " returned Null!");
				return false;
			} else {
				log.info("Found Attribute " + attributeCode + " for " + target.getCode());
			}
			log.info(ea.getValue().toString() + " = " + value);

			return ea.getValue().toString().equalsIgnoreCase(value);
		}

		return true;
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
		msg.setAttributeCode(ask.getQuestion().getAttribute().getCode());

		// publish to events for dropkick
		msg.setToken(userToken.getToken());
		KafkaUtils.writeMsg(KafkaTopic.EVENTS, msg);
	}


}
