package life.genny.kogito.common.service;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.GraphQLUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.MergeUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;

@ApplicationScoped
public class SummaryService {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	BaseEntityUtils beUtils;
	
	@Inject
	GraphQLUtils gqlUtils;

	@Inject
	NavigationService navigationService;

	@Inject
	SearchUtils searchUtils;

	/**
	 * Send the user's summary based on their lifecycle state.
	 */
	public void sendSummary() {

		// we store the summary code in the persons lifecycle
		JsonArray array = gqlUtils.queryTable("PersonLifecycle", "entityCode", userToken.getUserCode(), "summary");
		if (array == null || array.isEmpty()) {
			log.error("No PersonLifecycle items found");
			return;
		}

		String summaryCode = array.getJsonObject(0).getString("summary");

		// fetch pcm and summary entities
		BaseEntity pcm = beUtils.getBaseEntity("PCM_SUMMARY_"+summaryCode);
		BaseEntity content = beUtils.getBaseEntity("PCM_CONTENT");
		content.setValue("PRI_LOC1", pcm.getCode());

		// package the pcms and send
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(content);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		KafkaUtils.writeMsg("webdata", msg);

		recursivelySendSummaryData(pcm);
	}

	/**
	 * recursively traverse to find and send searches and questions for a pcm.
	 * @param pcm The PCM to traverse
	 */
	public void recursivelySendSummaryData(BaseEntity pcm) {

		List<EntityAttribute> locs = pcm.findPrefixEntityAttributes("PRI_LOC");

		for (EntityAttribute ea : locs) {
			String value = ea.getValueString();

			if (value.startsWith("PCM_")) {
				BaseEntity childPcm = beUtils.getBaseEntity(value);
				recursivelySendSummaryData(childPcm);

			} else if (value.startsWith("SBE_")) {
				SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(), value, SearchEntity.class);
				searchUtils.searchTable(searchEntity);
			}
		}

		Optional<EntityAttribute> questionCodeAttribute = pcm.findEntityAttribute("PRI_QUESTION_CODE");
		if (questionCodeAttribute.isEmpty()) {
			return;
		}

		String questionCode = questionCodeAttribute.get().getValueString();
		BaseEntity user = beUtils.getUserBaseEntity();

		String summaryCode = StringUtils.removeStart(questionCode, "QUE_SUMMARY_");
		summaryCode = StringUtils.removeStart(summaryCode, "QUE_");

		BaseEntity summary = beUtils.getBaseEntity("SUM_"+summaryCode);
		Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, user, summary);

		// build context map for merging
		Map<String, Object> ctxMap = new HashMap<>();
		ctxMap.put("USER", beUtils.getUserBaseEntity());

		// perform merge for any String PRI attributes
		summary.getBaseEntityAttributes().stream()
			.filter(ea -> ea.getAttribute() != null && ea.getAttribute().getCode() != null)
			.filter(ea -> ea.getAttribute().getCode().startsWith("PRI_"))
			.filter(ea -> ea.getAttribute().getDataType().getClassName().contains("String"))
			.forEach(ea -> {
				log.info("Merging EntityAttribute " + ea.getAttribute().getCode());
				String value = ea.getValueString();
				String merge = MergeUtils.merge(value, ctxMap);
				ea.setValueString(merge);
			});

		QDataAskMessage askMsg = new QDataAskMessage(ask);
		askMsg.setToken(userToken.getToken());
		askMsg.setReplace(true);
		KafkaUtils.writeMsg("webcmds", askMsg);

		// package the pcms and send
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(summary);
		msg.add(pcm);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		KafkaUtils.writeMsg("webdata", msg);
	}

}

