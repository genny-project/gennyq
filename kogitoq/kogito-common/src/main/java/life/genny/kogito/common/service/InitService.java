package life.genny.kogito.common.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataAttributeMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.GraphQLUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.Service;

/**
 * A Service class used for Auth Init operations.
 *
 * @author Jasper Robison
 */
@ApplicationScoped
public class InitService {

	private static final Logger log = Logger.getLogger(InitService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	Service service;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	UserToken userToken;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	KogitoUtils kogitoUtils;

	@Inject
	GraphQLUtils gqlUtils;

	/**
	 * Send the Project BaseEntity.
	 */
	public void sendProject() {

		BaseEntity project = beUtils.getProjectBaseEntity();
		log.info("Sending Project " + project.getCode());

		// configure msg and send
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(project);
		msg.setToken(userToken.getToken());
		msg.setAliasCode("PROJECT");
		KafkaUtils.writeMsg("webdata", msg);
	}

	/**
	 * Send the User.
	 */
	public void sendUser() {

		// fetch the users baseentity
		BaseEntity user = beUtils.getUserBaseEntity();
		log.info("Sending User " + user.getCode());

		// configure msg and send
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(user);
		msg.setToken(userToken.getToken());
		msg.setAliasCode("USER");

		KafkaUtils.writeMsg("webdata", msg);
	}

	/**
	 * Send All attributes for the productCode.
	 */
	public void sendAllAttributes() {

		log.info("Sending Attributes for " + userToken.getProductCode());
		String productCode = userToken.getProductCode();

		Integer TOTAL_PAGES = CacheUtils.getObject(productCode, "ATTRIBUTE_PAGES", Integer.class);

		for (int currentPage = 0; currentPage < TOTAL_PAGES + 1; currentPage++) {

			QDataAttributeMessage msg = CacheUtils.getObject(productCode,
					"ATTRIBUTES_P" + currentPage, QDataAttributeMessage.class);

			// set token and send
			msg.setToken(userToken.getToken());
			msg.setAliasCode("ATTRIBUTE_MESSAGE_" + (currentPage + 1) + "_OF_" + (TOTAL_PAGES + 1));
			KafkaUtils.writeMsg("webdata", msg);
		}
	}

	/**
	 * Send PCM BaseEntities.
	 */
	public void sendPCMs() {

		log.info("Sending PCMs for " + userToken.getProductCode());

		String productCode = userToken.getProductCode();
		BaseEntity user = beUtils.getUserBaseEntity();

		// get pcms using search
		SearchEntity searchBE = new SearchEntity("SBE_PCMS", "PCM Search")
				.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "PCM_%")
				.addColumn("*", "All Columns");

		searchBE.setRealm(productCode);
		searchBE.setPageSize(1000);
		List<BaseEntity> pcms = beUtils.getBaseEntitys(searchBE);
		if (pcms == null) {
			log.info("No PCMs found for " + productCode);
			return;
		}
		log.info("Sending "+pcms.size()+" PCMs");

		// configure ask msg
		QDataAskMessage askMsg = new QDataAskMessage();
		askMsg.setToken(userToken.getToken());
		askMsg.setReplace(true);
		askMsg.setAliasCode("PCM_INIT_ASK_MESSAGE");

		for (BaseEntity pcm : pcms) {
			log.info("Processing " + pcm.getCode());
			String questionCode = pcm.getValue("PRI_QUESTION_CODE", null);
			if (questionCode == null) {
				log.warn("(" + pcm.getCode() + " :: " + pcm.getName() + ") null PRI_QUESTION_CODE");
				continue;
			}
			Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, user, user);
			if (ask == null) {
				log.warn("(" + pcm.getCode() + " :: " + pcm.getName() + ") No asks found for " + questionCode);
				continue;
			}
			askMsg.add(ask);
		}

		KafkaUtils.writeMsg("webdata", askMsg);

		// configure msg and send
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(pcms);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		msg.setAliasCode("PCM_INIT_MESSAGE");

		KafkaUtils.writeMsg("webdata", msg);
	}

	/**
	 * Send Add Items Menu
	 */
	public void sendAddItems() {

		BaseEntity user = beUtils.getUserBaseEntity();
		Ask ask = qwandaUtils.generateAskFromQuestionCode("QUE_ADD_ITEMS_GRP", user, user);

		// configure msg and send
		QDataAskMessage msg = new QDataAskMessage(ask);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		KafkaUtils.writeMsg("webdata", msg);
	}

	/**
	 * Send Outstanding Tasks
	 */
	public void sendOutstandingTasks() {

		// we store the summary code in the persons lifecycle
		JsonArray array = gqlUtils.queryTable("ReceiveQuestionRequest", "sourceCode", userToken.getUserCode(), "id");
		if (array == null || array.isEmpty()) {
			log.error("No ReceiveQuestionRequest items found");
			return;
		}

		// grab ProcessInstances with the parentId equal to this calling id
		String callProcessId = array.getJsonObject(0).getString("id");
		array = gqlUtils.queryTable("ProcessInstances", "parentProcessInstanceId", callProcessId, "id");
		if (array == null || array.isEmpty()) {
			log.error("No ProcessInstances items found");
			return;
		}

		// force this workflow to re-ask the questions
		String processId = array.getJsonObject(0).getString("id");
		kogitoUtils.sendSignal("processQuestions", processId, "requestion", "");
	}

}
