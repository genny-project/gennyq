package life.genny.kogito.common.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		if(userToken.getRealm().equalsIgnoreCase("internmatch")){
			getInternSummary();
		}

		// we store the summary code in the persons lifecycle
		JsonArray array = gqlUtils.queryTable("PersonLifecycle", "entityCode", userToken.getUserCode(), "summary");
		if (array == null || array.isEmpty()) {
			log.error("No PersonLifecycle items found");
			return;
		}

		String summaryCode = array.getJsonObject(0).getString("summary");
		// navigationService.navigateContent("PCM_SUMMARY_"+summaryCode, "QUE_SUMMARY_"+summaryCode);

		// fetch pcm and summary entities
		BaseEntity summary = beUtils.getBaseEntityByCode("SUM_"+summaryCode);
		BaseEntity pcm = beUtils.getBaseEntityByCode("PCM_SUMMARY_"+summaryCode);
		BaseEntity content = beUtils.getBaseEntityByCode("PCM_CONTENT");
		try {
            content.setValue("PRI_LOC1", pcm.getCode());
        } catch (BadDataException e) {
            e.printStackTrace();
        }

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

		// package the pcms and send
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(summary);
		msg.add(pcm);
		msg.add(content);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		KafkaUtils.writeMsg("webdata", msg);

		// fetch and send the asks for the summary
		BaseEntity user = beUtils.getUserBaseEntity();
		Ask ask = qwandaUtils.generateAskFromQuestionCode("QUE_SUMMARY_"+summaryCode, user, summary);

		QDataAskMessage askMsg = new QDataAskMessage(ask);
		askMsg.setToken(userToken.getToken());
		askMsg.setReplace(true);
		KafkaUtils.writeMsg("webcmds", askMsg);

		recursivelySendSummaryData(pcm);
	}

	public void recursivelySendSummaryData(BaseEntity pcm) {

		for (EntityAttribute ea : pcm.getBaseEntityAttributes()) {
			String code = ea.getAttributeCode();
			String value = ea.getValueString();

			if (code.startsWith("PRI_LOC")) {

				if (value.startsWith("PCM_")) {
					BaseEntity childPcm = beUtils.getBaseEntity(value);
					recursivelySendSummaryData(childPcm);

				} else if (value.startsWith("SBE_")) {
					SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(), value, SearchEntity.class);
					searchUtils.searchTable(searchEntity);
				}

			} else if (code.equals("PRI_QUESTION_CODE")) {

				BaseEntity user = beUtils.getUserBaseEntity();

				String summaryCode = StringUtils.removeStart(value, "QUE_SUMMARY_");
				summaryCode = StringUtils.removeStart(summaryCode, "QUE_");

				BaseEntity summary = beUtils.getBaseEntity("SUM_"+summaryCode);
				Ask ask = qwandaUtils.generateAskFromQuestionCode(value, user, summary);

				QDataAskMessage askMsg = new QDataAskMessage(ask);
				askMsg.setToken(userToken.getToken());
				askMsg.setReplace(true);
				KafkaUtils.writeMsg("webcmds", askMsg);
			}
		}
	}

	public void getInternSummary() {
		try {
			String summaryCodes = "SUMMARY_CODES";
			List<String> bucketCodes = CacheUtils.getObject(userToken.getRealm(), summaryCodes, List.class);

			sendInternSummaryCodes(summaryCodes, bucketCodes);

			bucketCodes.stream().forEach(e -> {
				searchUtils.searchTable(e);
			});
		} catch (Exception ex){
			log.error(ex);
		}
	}

	public void sendInternSummaryCodes(String source, List<String> bucketCodes) {
		List<BaseEntity> listBase = new ArrayList<>();
		for(String str: bucketCodes){
			BaseEntity base = new BaseEntity(str);
			base.setRealm(userToken.getRealm());
			listBase.add(new BaseEntity(str));
		}

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(listBase);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		msg.setParentCode(source);
		KafkaUtils.writeMsg("webcmds", msg);
	}
}

