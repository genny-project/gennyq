package life.genny.gadaq.route;

import static life.genny.kogito.common.utils.KogitoUtils.UseService.SELF;

import java.lang.invoke.MethodHandles;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.kogito.common.service.NavigationService;
import life.genny.kogito.common.service.SearchService;
import life.genny.kogito.common.service.TaskService;
import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.exception.checked.GraphQLException;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.MessageData;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.GraphQLUtils;
import life.genny.qwandaq.utils.KafkaUtils;

/**
 * Events
 */
@ApplicationScoped
public class Events {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
	static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	KogitoUtils kogitoUtils;
	@Inject
	GraphQLUtils gqlUtils;

	@Inject
	NavigationService navigation;
	@Inject
	SearchService search;
	@Inject
	TaskService tasks;

	/**
	 * @param msg
	 */
	public void route(QEventMessage msg) {

		MessageData data = msg.getData();

		String code = data.getCode();
		String processId = data.getProcessId();

		String parentCode = data.getParentCode();
		String targetCode = data.getTargetCode();

		// auth init
		if ("AUTH_INIT".equals(code)) {
			kogitoUtils.triggerWorkflow(SELF, "authInit", "userCode", userToken.getUserCode());
			return;
		}

		// submit
		if ("QUE_SUBMIT".equals(code)) {
			kogitoUtils.sendSignal(SELF, "processQuestions", processId, "submit", "");
			return;
		}

		// update
		if ("QUE_UPDATE".equals(code)) {
			kogitoUtils.sendSignal(SELF, "processQuestions", processId, "update", "");
			return;
		}

		// cancel
		if ("QUE_CANCEL".equals(code)) {
			kogitoUtils.sendSignal(SELF, "processQuestions", processId, "cancel", "");
			return;
		}

		// reset
		if ("QUE_CANCEL".equals(code)) {
			kogitoUtils.sendSignal(SELF, "processQuestions", processId, "reset", "");
			return;
		}

		// dashboard
		if ("QUE_DASHBOARD_VIEW".equals(code)) {
			navigation.sendSummary();
			return;
		}

		// detail view
		if ("ACT_VIEW".equals(code)) {
			search.sendDetailView(targetCode);
			return;
		}

		// bucket view
		if ("QUE_TAB_BUCKET_VIEW".equals(code)) {
			search.getBuckets(code);
			return;
		}

		// table view (Default View Mode)
		if (code.startsWith("QUE_") && code.endsWith("_VIEW")) {
			search.sendTable(code);
			return;
		}

		// test question
		if (code.startsWith("TEST_QUE_.*")) {
			JsonObject payload = Json.createObjectBuilder()
					.add("questionCode", code.substring("TEST_".length()))
					.add("userCode", userToken.getUserCode())
					.add("sourceCode", userToken.getUserCode())
					.add("targetCode", targetCode)
					.build();
			kogitoUtils.triggerWorkflow(SELF, "testQuestion", payload);
			return;
		}

		// add item
		if (code.startsWith("QUE_ADD_.*")) {
			code = StringUtils.removeStart(code, "QUE_ADD_");
			String prefix = CacheUtils.getObject(userToken.getProductCode(), "DEF_"+code+":PREFIX", String.class);

			if (!"PER".equals(prefix))
				return;

			JsonObject json = Json.createObjectBuilder()
					.add("definitionCode", "DEF_"+code)
					.add("sourceCode", userToken.getUserCode())
					.build();

			kogitoUtils.triggerWorkflow(SELF, "personLifecycle", json);
			return;
		}

		// edit item
		if ("ACT_EDIT".equals(code) && parentCode.startsWith("SBE_.*") ) {

			if (parentCode.startsWith("SBE_")) {
				JsonObject payload = Json.createObjectBuilder()
						.add("questionCode", "QUE_BASEENTITY_GRP")
						.add("userCode", userToken.getUserCode())
						.add("sourceCode", userToken.getUserCode())
						.add("targetCode", msg.getData().getTargetCode())
						.build();
				kogitoUtils.triggerWorkflow(SELF,"testQuestion", payload);
				return;
			}

			kogitoUtils.triggerWorkflow(SELF, "edit", "eventMessage", msg);
			return;
		}

		/**
		 * If no route exists within gadaq, the message should be
		 * sent to the project specific service.
		 */
		log.info("Forwarding Event Message...");
		KafkaUtils.writeMsg(KafkaTopic.GENNY_EVENTS, msg);
	}
	
}
