package life.genny.dropkick.live.data;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.MergeUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;

@ApplicationScoped
public class InternalConsumer {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@ConfigProperty(name = "genny.default.dropdown.size", defaultValue = "25")
	Integer defaultDropDownSize;

	@Inject
	GennyScopeInit scope;

	@Inject
	Service service;

	@Inject
	UserToken userToken;

	@Inject
	DefUtils defUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	CapabilitiesManager capMan;

	@Inject
	SearchUtils searchUtils;

	Jsonb jsonb = JsonbBuilder.create();

	void onStart(@Observes StartupEvent ev) {

		if (service.showValues()) {
			log.info("Default dropdown size  : " + defaultDropDownSize);
		}

		service.fullServiceInit(true);
		log.info("[*] Finished Startup!");
	}

	/**
	 * Consume incoming answers for inference
	 */
	@Incoming("events")
	@Blocking
	public void getData(String event) {

		Instant start = Instant.now();
		
		// deserialise message
		JsonObject jsonStr = jsonb.fromJson(event, JsonObject.class);
	

		if (!jsonStr.getString("event_type").equals("DD")) {
			return; // TODO: This should not get here?
		}

		// init scope and process msg
		scope.init(event);
		log.debug("Consumed message: " + event);

		JsonObject dataJson = jsonStr.getJsonObject("data");

		// grab necessarry info
		String attrCode = jsonStr.getString("attributeCode");
		String sourceCode = dataJson.getString("sourceCode");
		String targetCode = dataJson.getString("targetCode");
		String searchText = dataJson.getString("value");
		String parentCode = dataJson.getString("parentCode");
		String questionCode = dataJson.getString("questionCode");
		String processId = dataJson.getString("processId");

		log.info(attrCode + ":" + parentCode + ":[" + searchText + "]");

		BaseEntity source = beUtils.getBaseEntity(sourceCode);

		BaseEntity target = null;
		BaseEntity defBE = null;

		if (!StringUtils.isBlank(processId)) {
			ProcessData processData = qwandaUtils.fetchProcessData(processId);
			if (processData == null) {
				log.error("Process data not found for processId: " + processId);
				return;
			}
			target = qwandaUtils.generateProcessEntity(processData);
			defBE = beUtils.getBaseEntity(processData.getDefinitionCode());
		} else {
			target = beUtils.getBaseEntity(targetCode);
			defBE = defUtils.getDEF(target);
		}

		log.info("Target DEF is " + defBE.getCode() + " : " + defBE.getName());
		log.info("Attribute is " + attrCode);

		// Because it is a drop down event we will search the DEF for the search
		// attribute
		Optional<EntityAttribute> searchAttribute = defBE.findEntityAttribute("SER_" + attrCode);
		if (searchAttribute.isEmpty()) {
			throw new ItemNotFoundException(String.format("%s -> %s", defBE.getCode(), "SER_"+attrCode));
		}

		String searchValue = searchAttribute.get().getValueString();
		log.info("Search Attribute Value = " + searchValue);

		JsonObject searchValueJson = jsonb.fromJson(searchValue, JsonObject.class);
		log.info("SearchValueJson = " + searchValueJson);

		Integer pageStart = 0;
		Integer pageSize = searchValueJson.containsKey("dropdownSize") ? searchValueJson.getInt("dropdownSize")
				: GennySettings.defaultDropDownPageSize();
		Boolean searchingOnLinks = false;

		SearchEntity searchBE = new SearchEntity("SBE_DROPDOWN", " Search")
				.addColumn("PRI_CODE", "Code")
				.addColumn("PRI_NAME", "Name");

		Map<String, Object> ctxMap = new ConcurrentHashMap<>();

		if (source != null) {
			ctxMap.put("SOURCE", source);
		}
		if (target != null) {
			ctxMap.put("TARGET", target);
		}

		JsonArray jsonParms = searchValueJson.getJsonArray("parms");
		int size = jsonParms.size();

		for (int i = 0; i < size; i++) {

			JsonObject json = null;

			try {

				json = jsonParms.getJsonObject(i);

				// conditionals
				Boolean conditionsAreMet = true;
				if (json.containsKey("conditions")) {
					JsonArray conditions = json.getJsonArray("conditions");
					for (Object cond : conditions) {
						if (!capMan.conditionMet(cond.toString().replaceAll("\"", ""))) {
							conditionsAreMet = false;
						}
					}
				}

				if (conditionsAreMet) {

					String attributeCode = json.getString("attributeCode");

					// Filters
					if (attributeCode != null) {

						Attribute att = qwandaUtils.getAttribute(attributeCode);

						String val = json.getString("value");

						String logic = null;
						if (json.containsKey("logic")) {
							logic = json.getString("logic");
						}

						String filterStr = null;
						if (val.contains(":")) {
							String[] valSplit = val.split(":");
							filterStr = valSplit[0];
							val = valSplit[1];
						}

						DataType dataType = att.getDataType();

						if (dataType.getClassName().equals("life.genny.qwanda.entity.BaseEntity")) {

							// These represent EntityEntity
							if (attributeCode.equals("LNK_CORE") || attributeCode.equals("LNK_IND")) {

								log.info("Adding CORE/IND DTT filter");
								// This is used for the sort defaults
								searchingOnLinks = true;

								// For using the search source and target and merge any data
								String paramSourceCode = null;
								if (json.containsKey("sourceCode")) {
									paramSourceCode = json.getString("sourceCode");

									// These will return True by default if source or target are null
									if (!MergeUtils.contextsArePresent(paramSourceCode, ctxMap)) {
										throw new DebugException(
												String.format("A Parent value is missing for %s, Not sending dropdown results", paramSourceCode));
									}

									paramSourceCode = MergeUtils.merge(paramSourceCode, ctxMap);
								}

								String paramTargetCode = null;
								if (json.containsKey("targetCode")) {
									paramTargetCode = json.getString("targetCode");

									if (!MergeUtils.contextsArePresent(paramTargetCode, ctxMap)) {
										throw new DebugException(
												String.format("A Parent value is missing for %s, Not sending dropdown results", paramTargetCode));
									}

									paramTargetCode = MergeUtils.merge(paramTargetCode, ctxMap);
								}

								log.info("attributeCode = " + json.getString("attributeCode"));
								log.info("val = " + val);
								log.info("link paramSourceCode = " + paramSourceCode);
								log.info("link paramTargetCode = " + paramTargetCode);

								// Set Source and Target if found it parameter
								if (paramSourceCode != null) {
									searchBE.setSourceCode(paramSourceCode);
								}
								if (paramTargetCode != null) {
									searchBE.setTargetCode(paramTargetCode);
								}

								// Set LinkCode and LinkValue
								searchBE.setLinkCode(att.getCode());
								searchBE.setLinkValue(val);
							} else {
								// This is a DTT_LINK style that has class = baseentity --> Baseentity_Attribute
								// TODO equals?
								SearchEntity.StringFilter stringFilter = SearchEntity.StringFilter.LIKE;
								if (filterStr != null) {
									stringFilter = SearchEntity.convertOperatorToStringFilter(filterStr);
								}
								log.info("Adding BE DTT filter");

								if ("AND".equals(logic)) {
									searchBE.addAnd(attributeCode, stringFilter, val);
								} else if (logic != null && logic.equals("OR")) {
									searchBE.addOr(attributeCode, stringFilter, val);
								} else {
									searchBE.addFilter(attributeCode, stringFilter, val);
								}

							}

						} else if (dataType.getClassName().equals("java.lang.String")) {
							SearchEntity.StringFilter stringFilter = SearchEntity.StringFilter.LIKE;
							if (filterStr != null) {
								stringFilter = SearchEntity.convertOperatorToStringFilter(filterStr);
							}
							log.info("Adding string DTT filter");

							if (logic != null && logic.equals("AND")) {
								searchBE.addAnd(attributeCode, stringFilter, val);
							} else if (logic != null && logic.equals("OR")) {
								searchBE.addOr(attributeCode, stringFilter, val);
							} else {
								searchBE.addFilter(attributeCode, stringFilter, val);
							}
						} else {
							SearchEntity.Filter filter = SearchEntity.Filter.EQUALS;
							if (filterStr != null) {
								filter = SearchEntity.convertOperatorToFilter(filterStr);
							}
							log.info("Adding Other DTT filter");
							searchBE.addFilterAsString(attributeCode, filter, val);
						}
					}
				}

				// sorts
				String sortBy = null;
				if (json.containsKey("sortBy")) {
					sortBy = json.getString("sortBy");
				}
				if (sortBy != null) {
					String order = json.getString("order");
					SearchEntity.Sort sortOrder = order.equals("DESC") ? SearchEntity.Sort.DESC : SearchEntity.Sort.ASC;
					searchBE.addSort(sortBy, sortBy, sortOrder);
				}

			} catch (Exception e) {
				log.error(e);
				log.error("DROPDOWN :Bad Json Value ---> " + json.toString());
				continue;
			}
		}

		// default to sorting by name if no sorts were specified and if not searching
		// for EntityEntitys
		Boolean hasSort = searchBE.getBaseEntityAttributes().stream()
				.anyMatch(item -> item.getAttributeCode().startsWith("SRT_"));
		if (!hasSort && !searchingOnLinks) {
			searchBE.addSort("PRI_NAME", "Name", SearchEntity.Sort.ASC);
		}

		// Filter by name wildcard provided by user
		searchBE.addFilter("PRI_NAME", SearchEntity.StringFilter.LIKE, searchText + "%")
				.addOr("PRI_NAME", SearchEntity.StringFilter.LIKE, "% " + searchText + "%");

		searchBE.setRealm(userToken.getProductCode());
		searchBE.setPageStart(pageStart);
		searchBE.setPageSize(pageSize);

		// Capability Based Conditional Filters
		// searchBE = SearchUtils.evaluateConditionalFilters(beUtils, searchBE);

		// Merge required attribute values
		// NOTE: This should correct any wrong datatypes too

		// TODO Hack to get around baseentityUtils thinking that processBE is cached.

		searchBE = defUtils.mergeFilterValueVariables(searchBE, ctxMap);
		if (searchBE != null) {

			// Perform search and evaluate columns
			List<BaseEntity> results = searchUtils.searchBaseEntitys(searchBE);
			QDataBaseEntityMessage msg = new QDataBaseEntityMessage();

			if (results == null)
				throw new DebugException("Dropdown search returned null");

			if (results.isEmpty())
				log.info("DROPDOWN : NO RESULTS");

			msg = new QDataBaseEntityMessage(results);
			log.info("DROPDOWN :Loaded " + msg.getItems().size() + " baseentitys");

			for (BaseEntity item : msg.getItems()) {
				String logStr = String.format("DROPDOWN : item: %s ===== %s", item.getCode(),
						item.getValueAsString("PRI_NAME"));

				if (item.getValueAsString("PRI_NAME") == null)
					log.warn(logStr);
				else
					log.info(logStr);
			}

			// Set all required message fields and return msg
			msg.setParentCode(parentCode);
			msg.setQuestionCode(questionCode);
			msg.setToken(userToken.getToken());
			msg.setLinkCode("LNK_CORE");
			msg.setLinkValue("ITEMS");
			msg.setReplace(true);
			msg.setShouldDeleteLinkedBaseEntities(false);
			KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);

		} else {
			log.error("DROPDOWN : SearchBE is null");
			//throw new DebugException("searchBE is null");
		}

		// log duration
		scope.destroy();
		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
	}

}
