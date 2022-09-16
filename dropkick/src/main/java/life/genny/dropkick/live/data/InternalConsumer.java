package life.genny.dropkick.live.data;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
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
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.clause.Or;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.CapabilityUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.GraphQLUtils;
import life.genny.qwandaq.utils.KafkaUtils;
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
	CapabilityUtils capabilityUtils;

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
		BaseEntity definition = null;

		if (!StringUtils.isBlank(processId)) {
			ProcessData processData = qwandaUtils.fetchProcessData(processId);
			if (processData == null) {
				log.error("Process data not found for processId: " + processId);
				return;
			}
			target = qwandaUtils.generateProcessEntity(processData);
			definition = beUtils.getBaseEntity(processData.getDefinitionCode());
		} else {
			target = beUtils.getBaseEntity(targetCode);
			definition = defUtils.getDEF(target);
		}

		log.info("Target DEF is " + definition.getCode() + " : " + definition.getName());
		log.info("Attribute is " + attrCode);

		// grab search entity
		String productCode = userToken.getProductCode();
		String searchAttributeCode = new StringBuilder("SBE_SER_").append(attrCode).toString();
		String key = new StringBuilder(definition.getCode()).append(":").append(searchAttributeCode).toString();
		SearchEntity searchEntity = CacheUtils.getObject(productCode, key, SearchEntity.class);

		if (searchEntity == null)
			throw new ItemNotFoundException(key);

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
		searchEntity = defUtils.mergeFilterValueVariables(searchEntity, ctxMap);

		// Perform search and evaluate columns
		List<BaseEntity> results = searchUtils.searchBaseEntitys(searchEntity);

		if (results == null)
			throw new DebugException("Dropdown search returned null");

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(results);
		log.info("DROPDOWN :Loaded " + msg.getItems().size() + " baseentitys");

		for (BaseEntity item : msg.getItems()) {
			String logStr = String.format("DROPDOWN : item: %s ===== %s", item.getCode(),
					item.getValueAsString(Attribute.PRI_NAME));

			if (item.getValueAsString(Attribute.PRI_NAME) == null)
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

		// log duration
		scope.destroy();
		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
	}

}
