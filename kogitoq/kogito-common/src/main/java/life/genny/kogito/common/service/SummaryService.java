package life.genny.kogito.common.service;

import java.lang.invoke.MethodHandles;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.GraphQLUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;

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

	public void sendSummary() {

		// we store the summary code in the persons lifecycle
		String lifecycle = "PersonLifecycle";
		String body = gqlUtils.queryTable(lifecycle, "entityCode", userToken.getUserCode(), "summary");

		// unpack json
		JsonObject bodyObj = jsonb.fromJson(body, JsonObject.class);
		JsonObject dataObj = bodyObj.getJsonObject("data");
		if (dataObj == null) {
			log.error("No data field found");
			return;
		}
		JsonArray tenantLifecycle = dataObj.getJsonArray(lifecycle);
		if (tenantLifecycle == null || tenantLifecycle.isEmpty()) {
			log.error("No " + lifecycle + " field found");
			return;
		}

		String summaryCode = tenantLifecycle.getJsonObject(0).getString("summary");

		// fetch pcm and summary entities
		BaseEntity summary = beUtils.getBaseEntityByCode("SUM_"+summaryCode);
		BaseEntity pcm = beUtils.getBaseEntityByCode("PCM_SUMMARY_"+summaryCode);
		BaseEntity content = beUtils.getBaseEntityByCode("PCM_CONTENT");
		try {
            content.setValue("PRI_LOC1", pcm.getCode());
        } catch (BadDataException e) {
            e.printStackTrace();
        }

		// package the pcms and send
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(summary);
		msg.add(pcm);
		msg.add(content);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		KafkaUtils.writeMsg("webcmds", msg);

		// fetch the asks for the summary
		BaseEntity user = beUtils.getUserBaseEntity();
		Ask ask = qwandaUtils.generateAskFromQuestionCode("QUE_SUMMARY_"+summaryCode, user, summary);

		QDataAskMessage askMsg = new QDataAskMessage(ask);
		askMsg.setToken(userToken.getToken());
		askMsg.setReplace(true);
		KafkaUtils.writeMsg("webcmds", askMsg);
	}

}
