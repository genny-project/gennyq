package life.genny.dropkick.live.data;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.clause.Or;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.*;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
	CacheManager cm;

	@Inject
	DefUtils defUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	EntityAttributeUtils beaUtils;

	@Inject
	CapabilitiesManager capMan;

	@Inject
	SearchUtils searchUtils;

	@Inject
	FilterUtils filter;

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
	public void getEvent(String event) {

		Instant start = Instant.now();

		// deserialise message
		JsonObject jsonStr = jsonb.fromJson(event, JsonObject.class);

		if (!"DD".equals(jsonStr.getString("event_type"))) {
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
		String parentCode = dataJson.getString("parentCode");
		String searchText = dataJson.getString("value");
		String questionCode = dataJson.getString("questionCode");
		String processId = dataJson.getString("processId");

		log.info(attrCode + "::[" + searchText + "]");

		if(filter.validFilter(attrCode)) {
			return;
		}

		BaseEntity source = beUtils.getBaseEntity(sourceCode);

		BaseEntity target = null;
		List<BaseEntity> definitions = new ArrayList<>();
		BaseEntity definition = null; // For wip usage
    
    target = beUtils.getBaseEntity(targetCode);
    // TODO: This will break if it is an old target without a processId
    BaseEntity def = defUtils.getDEF(target);
    definitions.add(def);

		if (!StringUtils.isBlank(processId)) {
			ProcessData processData = qwandaUtils.fetchProcessData(processId);
			if (processData == null) {
				log.error("Process data not found for processId: " + processId);
				return;
			}
			BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
      // update transient target with process enity information
      log.debug("Adding processentity attributes to target");
      for (EntityAttribute ea : processEntity.getBaseEntityAttributes()) {
        EntityAttribute copy = ea;
        copy.setBaseEntityCode(target.getCode());
        target.addAttribute(copy);
      }

      // add process entity defs to list
			List<String> defCodes = processData.getDefCodes();
			for (String defCode : defCodes) {
				BaseEntity processDef = beUtils.getBaseEntity(defCode, true);
				definitions.add(processDef);
			}
    }

		Optional<EntityAttribute> searchEA = Optional.empty();
		for (BaseEntity defBE : definitions) {
			log.info("Target DEF is " + defBE.getCode() + " : " + defBE.getName());
			searchEA = defBE.findEntityAttribute("SER_" + attrCode);
			if (searchEA.isPresent()) {
				definition = defBE;
				break;
			}
		}
		if (definition == null) {
			log.info("Attribute is " + attrCode);
			if (definitions.size() == 1) {
				definition = definitions.get(0);
				log.warn("Only one definition found for target, using " + definition.getCode());
			} else {
				log.warn("Multiple definitions found for target, using first one");
				definition = definitions.get(0);
			}
		}

		// grab search entity
		String productCode = userToken.getProductCode();
		String searchAttributeCode = new StringBuilder("SBE_SER_").append(attrCode).toString();
		String key = new StringBuilder(definition.getCode()).append(":").append(searchAttributeCode).toString();
		log.info("key="+key);
		SearchEntity searchEntity = cm.getObject(productCode, key, SearchEntity.class);

		if (searchEntity == null)
			throw new ItemNotFoundException("Search Entity with key=" + key);

		log.debug("Using Search Entity: " + searchEntity);
		// Filter by name wildcard provided by user
		searchEntity.add(new Or(
			new Filter(Attribute.PRI_NAME, Operator.LIKE, searchText + "%"),
			new Filter(Attribute.PRI_NAME, Operator.LIKE, "% " + searchText + "%")));

		searchEntity.add(new Column("PRI_NAME", "Name"));

		// init context map
		Map<String, Object> ctxMap = new ConcurrentHashMap<>();
		if (source != null)
			ctxMap.put("SOURCE", source);
		if (target != null)
			ctxMap.put("TARGET", target);

		searchEntity.setRealm(userToken.getProductCode());
		searchEntity = searchUtils.mergeFilterValues(searchEntity, ctxMap);

		// Perform search and evaluate columns
		List<BaseEntity> results = searchUtils.searchBaseEntitys(searchEntity);

		if (results == null)
			throw new DebugException("Dropdown search returned null");

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(results);
		log.info("DROPDOWN :Loaded " + msg.getItems().size() + " baseentitys");

		for (BaseEntity item : msg.getItems()) {
			String name = item.getName();
			String itemCode = item.getCode();
			String logStr = String.format("DROPDOWN : item: %s ===== %s", itemCode, name);
			if (StringUtils.isBlank(name)) {
				log.warn("No Name Set for Item: " + item.getCode());
			} else {
				log.info(logStr);
			}
		}

		// Set all required message fields and return msg
		msg.setQuestionCode(questionCode);
		msg.setToken(userToken.getToken());
		msg.setParentCode(parentCode);
		msg.setLinkCode("LNK_CORE");
		msg.setLinkValue("ITEMS");
		msg.setReplace(true);
		msg.setShouldDeleteLinkedBaseEntities(false);
		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);

		// log duration
		scope.destroy();
		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
	}

}
