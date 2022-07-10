package life.genny.dropkick.streams;

import io.quarkus.runtime.StartupEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.CapabilityUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.GraphQLUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.MergeUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;




@ApplicationScoped
public class TopologyProducer {

	private static final Logger log = Logger.getLogger(TopologyProducer.class);

	class ProcessBeAndDef {
		ProcessBeAndDef(BaseEntity be, String defCode) {
			this.processBE = be;
			this.defCode = defCode;
		}

		public BaseEntity processBE;
		public String defCode;
	}

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
	CapabilityUtils capabilityUtils;

	@Inject
	GraphQLUtils gqlUtils;

	Jsonb jsonb = JsonbBuilder.create();

	void onStart(@Observes StartupEvent ev) {

		if (service.showValues()) {
			log.info("Default dropdown size  : " + defaultDropDownSize);
		}

		service.fullServiceInit();
		log.info("[*] Finished Startup!");
	}

	@Produces
	public Topology buildTopology() {

		// Read the input Kafka topic into a KStream instance.
		StreamsBuilder builder = new StreamsBuilder();
		builder
				.stream("events", Consumed.with(Serdes.String(), Serdes.String()))
				.peek((k, v) -> scope.init(v))
				.peek((k, v) -> log.debug("Consumed message: " + v))

				.filter((k, v) -> isValidDropdownMessage(v))
				.peek((k, v) -> log.debug("Processing valid message: " + v))

				.mapValues(v -> fetchDropdownResults(v))

				.filter((k, v) -> v != null)
				.peek((k, v) -> log.debug("Sending results: " + v))

				// write using KafkaUtils for bridge switching
				.foreach((k, v) -> KafkaUtils.writeMsg("webdata", v));

		return builder.build();
	}

	/**
	 * Check if dropdown message is valid and has all necessary fields.
	 *
	 * @param data Message to check
	 * @return Boolean value determining validity
	 */
	public Boolean isValidDropdownMessage(String data) {

		JsonObject json = jsonb.fromJson(data, JsonObject.class);

		// Check to make sure it has an event type
		if (!json.containsKey("event_type")) {
			return false;
		}

		String eventType = json.getString("event_type");

		// Check the event type is a dropdown event
		if (!eventType.equals("DD")) {
			return false;
		}

		JsonObject dataJson = json.getJsonObject("data");

		// Check attribute code exists
		if (!json.containsKey("attributeCode")) {
			log.error("No Attribute code in message " + data);
			return false;
		}

		// Check Source exists
		if (!dataJson.containsKey("sourceCode")) {
			log.error("Missing sourceCode in Dropdown Message [" + dataJson.toString() + "]");
			return false;
		}

		// Check Target exists
		if (!dataJson.containsKey("targetCode")) {
			log.error("Missing targetCode in Dropdown Message [" + dataJson.toString() + "]");
			return false;
		}

		// Grab info required to find the DEF
		String attributeCode = json.getString("attributeCode");
		String targetCode = dataJson.getString("targetCode");
		String processId = dataJson.getString("processId");

		BaseEntity target = null;
		BaseEntity defBE = null;

		if (!StringUtils.isBlank(processId)) {
			// This means that the target should come from the graphql
			ProcessBeAndDef processBeAndDef = fetchProcessInstanceProcessBE(processId);
			if (processBeAndDef == null) {
				log.error("Could not find process instance for processId [" + processId + "]");
				return false;
			}
			target = processBeAndDef.processBE;
			defBE = beUtils.getBaseEntityByCode(processBeAndDef.defCode);
		} else {

			target = beUtils.getBaseEntityByCode(targetCode);
			if (target == null) {
				return false;
			}
		}

		// Find the DEF
		if (defBE == null) {
			defBE = defUtils.getDEF(target);
		}
		if (defBE == null) {
			log.error("No DEF found for target " + targetCode);
			return false;
		}

		// Check if attribute code exists as a SER for the DEF
		Optional<EntityAttribute> searchAttribute = defBE.findEntityAttribute("SER_" + attributeCode);

		for (EntityAttribute ea : defBE.getBaseEntityAttributes()) {
			log.info(ea.getBaseEntityCode()+"   EA="+ea.getAttributeCode());
		}

		if (!searchAttribute.isPresent()) {
			log.info("Target: " + target.getCode() + ", Definition: " + defBE.getCode() + ", No attribute found for SER_" + attributeCode);
			return false;
		}

		// Parse search json to object
		String searchValue = searchAttribute.get().getValueString();
		JsonObject searchJson = jsonb.fromJson(searchValue, JsonObject.class);
		log.info("Attribute exists in " + defBE.getCode() + " for SER_" + attributeCode + " --> " + searchValue);

		if (searchJson.containsKey("enabled")) {

			Boolean isEnabled = searchJson.getBoolean("enabled");
			log.info("Search Json Enabled = " + isEnabled);

			return isEnabled;
		}

		return true;
	}

	
	/**
	 * Fetch the targetCode stored in the processInstance 
	 * for the given processId.
	 */
	public ProcessBeAndDef fetchProcessInstanceProcessBE(String processId) {
		BaseEntity processBe = null;
		String defCode = null;
		String processBeStr = null;

		log.info("Fetching processBE for processId : " + processId);

		// check in cache first (But not ready yet, processQuestions would need to save the processBe into cache every answer received)
	/* 	String processBeStr = CacheUtils.getObject(userToken.getProductCode(), processId+":PROCESS_BE", String.class);
		if (processBeStr != null) {
			processBe = jsonb.fromJson(processBeStr, BaseEntity.class);
			return processBe;
		} */

	JsonArray array = gqlUtils.queryTable("ProcessInstances", "id", processId, "variables");
	if (array.isEmpty()) {
			log.error("Nothing found for processId: " + processId);
			return null;
		}
		JsonObject variables = jsonb.fromJson(array.getJsonObject(0).getString("variables"), JsonObject.class);

		// grab the targetCode from process questions variables
		processBeStr = variables.getString("processBEJson");
		defCode = variables.containsKey("defCode")?variables.getString("defCode"):null;
		processBe = jsonb.fromJson(processBeStr, BaseEntity.class);
		
		if (defCode == null) {
			BaseEntity defBE = defUtils.getDEF(processBe);
			defCode = defBE.getCode();
		}
		
		ProcessBeAndDef processBeAndDef = new ProcessBeAndDef(processBe, defCode);
		
		return processBeAndDef;
	}

	/**
	 * Fetch and return the results for this dropdown. Will return null
	 * if items can not be fetched for this message. This null must
	 * be filtered by streams builder.
	 *
	 * @param data
	 * @return
	 */
	public String fetchDropdownResults(String data) {

		JsonObject jsonStr = jsonb.fromJson(data, JsonObject.class);

		JsonObject dataJson = jsonStr.getJsonObject("data");

		String attrCode = jsonStr.getString("attributeCode");
		String sourceCode = dataJson.getString("sourceCode");
		String targetCode = dataJson.getString("targetCode");
		String searchText = dataJson.getString("value");
		String parentCode = dataJson.getString("parentCode");
		String questionCode = dataJson.getString("questionCode");
		String processId = dataJson.getString("processId");

		log.info(attrCode + ":" + parentCode + ":[" + searchText + "]");

		BaseEntity source = beUtils.getBaseEntityByCode(sourceCode);

		if (source == null) {
			log.error("Source Entity is NULL!");
			return null;
		}

		BaseEntity target = null;
		BaseEntity defBE = null;

		if (!StringUtils.isBlank(processId)) {
			ProcessBeAndDef processBeAndDef = fetchProcessInstanceProcessBE(processId);
			target = processBeAndDef.processBE;
			defBE = beUtils.getBaseEntityByCode(processBeAndDef.defCode);
		} else {
			target = beUtils.getBaseEntityByCode(targetCode);
		}

		if (target == null) {
			log.error("Target Entity is NULL!");
			return null;
		}

		if (defBE == null) {
			defBE = defUtils.getDEF(target);
		}

		log.info("Target DEF is " + defBE.getCode() + " : " + defBE.getName());
		log.info("Attribute is " + attrCode);

		// Because it is a drop down event we will search the DEF for the search
		// attribute
		Optional<EntityAttribute> searchAttribute = defBE.findEntityAttribute("SER_" + attrCode);

		if (!searchAttribute.isPresent()) {
			log.error("No present search attribute for " + defBE.getCode());
			return null;
		}

		String searchValue = searchAttribute.get().getValueString();
		log.info("Search Attribute Value = " + searchValue);

		JsonObject searchValueJson = null;
		try {
			searchValueJson = jsonb.fromJson(searchValue, JsonObject.class);
		} catch (JsonbException e1) {
			e1.printStackTrace();
		}


		log.info("SearchValueJson="+searchValueJson);
			
		
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

		if (source ==null) {
			log.error("Source is NULL!");
			return null;
		}

		if (target == null) {
			log.error("Target is NULL!");
			return null;
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
						if (!capabilityUtils.conditionMet(cond.toString().replaceAll("\"", ""))) {
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
										log.error(ANSIColour.RED + "A Parent value is missing for " + paramSourceCode
												+ ", Not sending dropdown results" + ANSIColour.RESET);
										return null;
									}

									paramSourceCode = MergeUtils.merge(paramSourceCode, ctxMap);
								}

								String paramTargetCode = null;
								if (json.containsKey("targetCode")) {
									paramTargetCode = json.getString("targetCode");

									if (!MergeUtils.contextsArePresent(paramTargetCode, ctxMap)) {
										log.error(ANSIColour.RED + "A Parent value is missing for " + paramTargetCode
												+ ", Not sending dropdown results" + ANSIColour.RESET);
										return null;
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

								if (logic != null && logic.equals("AND")) {
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
		searchBE = defUtils.mergeFilterValueVariables(searchBE, ctxMap);

		if (searchBE == null) {
			log.error(ANSIColour.RED + "Cannot Perform Search!!!" + ANSIColour.RESET);
			return null;
		}

		// Perform search and evaluate columns
		List<BaseEntity> results = beUtils.getBaseEntitys(searchBE);
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage();

		if (results == null) {

			log.error(ANSIColour.RED + "Dropdown search returned NULL!" + ANSIColour.RESET);
			return null;

		} else if (results.size() > 0) {

			msg = new QDataBaseEntityMessage(results);
			log.info("DROPDOWN :Loaded " + msg.getItems().size() + " baseentitys");

			for (BaseEntity item : msg.getItems()) {

				if (item.getValueAsString("PRI_NAME") == null) {
					log.warn("DROPDOWN : item: " + item.getCode() + " ===== " + item.getValueAsString("PRI_NAME"));
				} else {
					log.info("DROPDOWN : item: " + item.getCode() + " ===== " + item.getValueAsString("PRI_NAME"));
				}
			}
		} else {
			log.info("DROPDOWN :Loaded NO baseentitys");
		}

		// Set all required message fields and return msg
		msg.setParentCode(parentCode);
		msg.setQuestionCode(questionCode);
		msg.setToken(userToken.getToken());
		msg.setLinkCode("LNK_CORE");
		msg.setLinkValue("ITEMS");
		msg.setReplace(true);
		msg.setShouldDeleteLinkedBaseEntities(false);
		msg.setTotal(Long.getLong(""+msg.getItems().size()));
		return jsonb.toJson(msg);
	}

}
