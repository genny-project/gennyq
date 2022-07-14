package life.genny.qwandaq.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.CacheName;
import life.genny.qwandaq.datatype.CapabilityMode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.handlers.RuleFlowGroupWorkItemHandler;
import life.genny.qwandaq.message.MessageData;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.message.QEventDropdownMessage;
import life.genny.qwandaq.message.QSearchMessage;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.common.key.cache.CacheKey;
import life.genny.qwandaq.serialization.key.baseentity.BaseEntityKey;

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
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	CapabilityUtils capabilityUtils;

	@Inject
	ServiceToken serviceToken;

	@Inject
	UserToken userToken;

	/**
	 * Evaluate any conditional filters for a {@link SearchEntity}
	 *
	 * @param searchBE the SearchEntity to evaluate filters of
	 * @return SearchEntity
	 */
	public SearchEntity evaluateConditionalFilters(SearchEntity searchBE) {

		List<String> shouldRemove = new ArrayList<>();

		for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {

			if (!ea.getAttributeCode().startsWith("CND_")) {
				// find Conditional Filters
				EntityAttribute cnd = searchBE.findEntityAttribute("CND_" + ea.getAttributeCode()).orElse(null);

				if (cnd != null) {

					log.info("Condition found for " + ea.getAttributeCode() + " with value: "
							+ cnd.getValue().toString());
					String[] condition = cnd.getValue().toString().split(":");

					String capability = condition[0];
					String mode = condition[1];

					// check for NOT operator
					Boolean not = capability.startsWith("!");
					capability = not ? capability.substring(1) : capability;

					// check for Capability
					Boolean hasCap = capabilityUtils.hasCapabilityThroughPriIs(capability,
							CapabilityMode.getMode(mode));

					// XNOR operator
					if (!(hasCap ^ not)) {
						shouldRemove.add(ea.getAttributeCode());
					}
				}
			}
		}

		// remove unwanted attrs
		shouldRemove.stream().forEach(item -> {
			searchBE.removeAttribute(item);
		});

		return searchBE;
	}

	/**
	 * Perform a table like search in Genny using a {@link SearchEntity} code.
	 * The respective {@link SearchEntity} will be fetched from the cache befor
	 * processing.
	 *
	 * @param code    the code of the SearchEntity to grab from cache and search
	 */
	public void searchTable(String code) {

		String searchCode = code;
		if (searchCode.startsWith("CNS_")) {
			searchCode = searchCode.substring(4);
		} else if (!searchCode.startsWith("SBE_")) {
			searchCode = "SBE_" + searchCode;
		}

		log.info("SBE CODE   ::   " + searchCode);

		BaseEntityKey beKey = new BaseEntityKey(userToken.getProductCode(), searchCode);
		SearchEntity searchEntity = CacheUtils.getObject(CacheName.BASEENTITY, beKey, SearchEntity.class);

		if (searchEntity == null) {
			log.error("Could not fetch " + searchCode + " from cache!!!");
			return;
		}

		if (code.startsWith("CNS_")) {
			searchEntity.setCode(code);
		}

		searchTable(searchEntity);
	}

	/**
	 * Perform a table like search in Genny using a {@link SearchEntity}.
	 *
	 * @param searchEntity the SearchEntity to search
	 */
	public void searchTable(SearchEntity searchEntity) {

		if (searchEntity == null) {
			log.error("SearchBE is null (searchTable)");
			return;
		}

		// update code to session search code
		searchEntity = getSessionSearch(searchEntity);

		// // Add any necessary extra filters
		// List<EntityAttribute> filters = getUserFilters(searchEntity);

		// if (!filters.isEmpty()) {
		// 	log.info("Found " + filters.size() + " additional filters for " + searchEntity.getCode());

		// 	for (EntityAttribute filter : filters) {
		// 		searchEntity.getBaseEntityAttributes().add(filter);
		// 	}
		// }

		CacheKey cacheKey = new CacheKey(userToken.getProductCode(), "LAST-SEARCH:" + searchEntity.getCode());
		// TODO: Decide where to cache LAST-SEARCH
		CacheUtils.putObject(CacheName.BASEENTITY, cacheKey, searchEntity);

		// ensure column and action indexes are accurate
		searchEntity.updateColumnIndex();
		searchEntity.updateActionIndex();

		// package and send search message to fyodor
		QSearchMessage searchBeMsg = new QSearchMessage(searchEntity);
		searchBeMsg.setToken(userToken.getToken());
		searchBeMsg.setDestination("webcmds");
		KafkaUtils.writeMsg("search_events", searchBeMsg);
	}

	/**
	 * A method to fetch any additional {@link EntityAttribute} filters for a given
	 * {@link SearchEntity}
	 * from the SearchFilters rulegroup.
	 *
	 * @param searchBE the SearchEntity to get additional filters for
	 * @return List
	 */
	public List<EntityAttribute> getUserFilters(SearchEntity searchBE) {

		List<EntityAttribute> filters = new ArrayList<>();

		Map<String, Object> facts = new ConcurrentHashMap<>();
		facts.put("serviceToken", serviceToken);
		facts.put("userToken", userToken);
		facts.put("searchBE", searchBE);

		Map<String, Object> results = new RuleFlowGroupWorkItemHandler()
				.executeRules(
						beUtils,
						facts,
						"SearchFilters",
						"SearchUtils:getUserFilters");

		if (results != null) {

			Object obj = results.get("payload");

			if (obj instanceof QBulkMessage) {
				QBulkMessage bulkMsg = (QBulkMessage) results.get("payload");

				// Check if bulkMsg not empty
				if (bulkMsg.getMessages().length > 0) {

					// Get the first QDataBaseEntityMessage from bulkMsg
					QDataBaseEntityMessage msg = bulkMsg.getMessages()[0];

					// Check if msg is not empty
					if (!msg.getItems().isEmpty()) {

						// Extract the baseEntityAttributes from the first BaseEntity
						Set<EntityAttribute> filtersSet = msg.getItems().get(0).getBaseEntityAttributes();
						filters.addAll(filtersSet);
					}
				}
			}
		} else {
			log.error("results are null");
			filters = new ArrayList<EntityAttribute>();
		}
		return filters;
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
		Attribute primaryAttribute = qwandaUtils.getAttribute(primaryAttrCode);

		Answer ans = new Answer(baseBE.getCode(), baseBE.getCode(), finalAttributeCode, "");
		Attribute att = new Attribute(finalAttributeCode, primaryAttribute.getName(), primaryAttribute.getDataType());
		ans.setAttribute(att);

		for (int i = 0; i < calFields.length - 1; i++) {
			String attributeCode = calFields[i];
			String calBe = be.getValueAsString(attributeCode);

			if (calBe != null && !calBe.isBlank()) {
				String calVal = BaseEntityUtils.cleanUpAttributeValue(calBe);
				String[] codeArr = calVal.split(",");

				for (String code : codeArr) {
					if (code.isBlank()) {
						log.error("code from Calfields is empty calVal[" + calVal + "] skipping calFields=[" + calFields
								+ "] - be:" + baseBE.getCode());
						continue;
					}

					BaseEntity associatedBe = beUtils.getBaseEntityByCode(code);
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
	 * Get a session search for a given SearchEntity
	 *
	 * @param searchEntity the searchEntity
	 * @return SearchEntity
	 */
	public SearchEntity getSessionSearch(SearchEntity searchEntity) {

		// don't bother if the code is already a session search
		if (searchEntity.getCode().contains(userToken.getJTI().toUpperCase())) {
			return searchEntity;
		}

		// we need to set the searchEntity's code to session search code
		String sessionSearchCode = searchEntity.getCode() + "_" + userToken.getJTI().toUpperCase();
		log.info("sessionSearchCode  ::  " + searchEntity.getCode());

		// update code and any nested codes
		searchEntity.setCode(sessionSearchCode);

		searchEntity.getBaseEntityAttributes().stream()
				.filter(ea -> ea.getAttributeCode().startsWith("SBE_"))
				.forEach(ea -> {
					ea.setAttributeCode(ea.getAttributeCode() + "_" + userToken.getJTI().toUpperCase());
				});

		// put/update in the cache
		BaseEntityKey searchEntityKey = new BaseEntityKey(userToken.getProductCode(), searchEntity.getCode());
		CacheUtils.putObject(CacheName.BASEENTITY, searchEntityKey, searchEntity);

		return searchEntity;
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
		String cleanCode = BaseEntityUtils.cleanUpAttributeValue(dropdownValue);
		BaseEntity target = beUtils.getBaseEntityByCode(cleanCode);

		BaseEntity project = beUtils.getBaseEntityByCode("PRJ_" + productCode.toUpperCase());

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

			BaseEntityKey beKey = new BaseEntityKey(userToken.getProductCode(), bucketMapCode);
			SearchEntity baseSearch = CacheUtils.getObject(CacheName.BASEENTITY, beKey, SearchEntity.class);

			if (baseSearch == null) {
				log.error("SearchEntity " + bucketMapCode + " is NULL in cache!");
				continue;
			}

			// handle Pre Search Mutations
			JsonArray preSearchMutations = bucketMap.getJsonArray("mutations");

			for (Object m : preSearchMutations) {

				JsonObject mutation = (JsonObject) m;

				JsonArray conditions = mutation.getJsonArray("conditions");

				if (jsonConditionsMet(conditions, target)) {

					log.info("Pre Conditions met for : " + conditions.toString());

					String attributeCode = mutation.getString("attributeCode");
					// String operator = mutation.getString("operator");
					String value = mutation.getString("value");

					// TODO: allow for regular filters too
					// SearchEntity.StringFilter stringFilter =
					// SearchEntity.convertOperatorToStringFilter(operator);
					SearchEntity.StringFilter stringFilter = SearchEntity.StringFilter.EQUAL;
					String mergedValue = MergeUtils.merge(value, ctxMap);
					log.info("Adding filter: " + attributeCode + " "
							+ stringFilter.toString() + " " + mergedValue);
					baseSearch.addFilter(attributeCode, stringFilter, mergedValue);
				}
			}

			// perform Search
			baseSearch.setPageSize(100000);
			log.info("Performing search for " + baseSearch.getCode());
			List<BaseEntity> results = beUtils.getBaseEntitys(baseSearch);

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
								log.info("Testing conditions: " + conditions.toString());
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
				beKey = new BaseEntityKey(productCode, targetedBucketCode + "_" + sessionCode);
				SearchEntity searchBE = CacheUtils.getObject(CacheName.BASEENTITY, beKey,
						SearchEntity.class);

				if (searchBE == null) {
					log.error("Null SBE in cache for " + targetedBucketCode);
					continue;
				}

				// // Attach any extra filters from SearchFilters rulegroup
				// List<EntityAttribute> filters = getUserFilters(searchBE);

				// if (!filters.isEmpty()) {
				// 	log.info("User Filters are NOT empty");
				// 	log.info("Adding User Filters to searchBe  ::  " + searchBE.getCode());
				// 	for (EntityAttribute filter : filters) {
				// 		searchBE.getBaseEntityAttributes().add(filter);
				// 	}
				// } else {
				// 	log.info("User Filters are empty");
				// }

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
				KafkaUtils.writeMsg("webcmds", msg);

				// update and send the SearchEntity
				// updateBaseEntity(searchBE, "PRI_TOTAL_RESULTS",
				// Long.valueOf(finalResultList.size()) + "");

				Attribute attribute = qwandaUtils.getAttribute("PRI_TOTAL_RESULTS");
				try {
					searchBE.addAnswer(
							new Answer(searchBE, searchBE, attribute, Long.valueOf(finalResultList.size()) + ""));
				} catch (BadDataException e) {
					log.error("Could not update total results");
				}

				beKey = new BaseEntityKey(productCode, searchBE.getCode());
				CacheUtils.putObject(CacheName.BASEENTITY, beKey, searchBE);

				if (searchBE != null) {
					log.info("Sending Search Entity : " + searchBE.getCode());
				}
				
				QDataBaseEntityMessage searchMsg = new QDataBaseEntityMessage(searchBE);
				searchMsg.setToken(userToken.getToken());
				searchMsg.setReplace(true);
				KafkaUtils.writeMsg("webcmds", searchMsg);
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
			// String operator = condition.getString("operator");
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

			if (!ea.getValue().toString().toUpperCase().equals(value.toUpperCase())) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Perform a dropdown search through dropkick.
	 *
	 * @param ask       the ask to perform dropdown search for
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
		KafkaUtils.writeMsg("events", msg);
	}

}
