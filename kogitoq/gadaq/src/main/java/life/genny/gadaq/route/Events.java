package life.genny.gadaq.route;

import static life.genny.kogito.common.utils.KogitoUtils.UseService.SELF;

import java.lang.invoke.MethodHandles;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.message.QDataAnswerMessage;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.kogito.common.service.NavigationService;
import life.genny.kogito.common.service.SearchService;
import life.genny.kogito.common.service.TaskService;
import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.MessageData;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.GraphQLUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.gadaq.search.FilterGroupService;
import life.genny.qwandaq.constants.FilterConst;

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

	@Inject
	FilterGroupService filter;

	/**
	 * @param msg
	 */
	public void route(QEventMessage msg) {

		MessageData data = msg.getData();

		String code = data.getCode();
		String processId = data.getProcessId();

		String parentCode = data.getParentCode();
		String targetCode = data.getTargetCode();

		// Filter
		if(filter.isValidEvent(msg)){
			filter.handleBtnEvents(msg);
			return;
		}

		// If the event is a Dropdown then leave it for DropKick
		if ("DD".equals(msg.getEvent_type()))
			return;

		// auth init
		if ("AUTH_INIT".equals(code)) {
			kogitoUtils.triggerWorkflow(SELF, "authInit", "userCode", userToken.getUserCode());
			return;
		}

		// submit
		if (Question.QUE_SUBMIT.equals(code) || Question.QUE_NEXT.equals(code)) {
			kogitoUtils.sendSignal(SELF, "processQuestions", processId, "submit", "");
			return;
		}

		// update
		if (Question.QUE_UPDATE.equals(code)) {
			kogitoUtils.sendSignal(SELF, "processQuestions", processId, "update", "");
			return;
		}

		// undo
		if (Question.QUE_UNDO.equals(code)) {
			kogitoUtils.sendSignal(SELF, "processQuestions", processId, "undo", "");
			return;
		}

		// redo
		if (Question.QUE_REDO.equals(code)) {
			kogitoUtils.sendSignal(SELF, "processQuestions", processId, "redo", "");
			return;
		}

		// undo
		if (Question.QUE_PREVIOUS.equals(code)) {
			kogitoUtils.sendSignal(SELF, "processQuestions", processId, "previous", "");
			return;
		}

		// cancel
		if (Question.QUE_CANCEL.equals(code)) {
			kogitoUtils.sendSignal(SELF, "processQuestions", processId, "cancel", "");
			return;
		}

		// reset
		if (Question.QUE_RESET.equals(code)) {
			kogitoUtils.sendSignal(SELF, "processQuestions", processId, "reset", "");
			return;
		}

		// dashboard
		if (Question.QUE_DASHBOARD.equals(code)) {
			navigation.sendSummary();
			return;
		}

		// bucket view
		if (Question.QUE_PROCESS.equals(code)) {
			filter.init(code);
			search.sendBuckets();
			return;
		}

		// detail view
		if ("ACT_VIEW".equals(code)) {
			search.sendDetailView(targetCode);
			return;
		}

		// search pagination
		if (GennyConstants.PAGINATION_NEXT.equals(code)) {
			search.handleSearchPagination(targetCode, false);
			return;
		} else if (GennyConstants.PAGINATION_PREV.equals(code)) {
			search.handleSearchPagination(targetCode, true);
			return;
		}

		// bucket pagination
		if (Question.QUE_TABLE_LAZY_LOAD.equals(code)) {
			search.handleSearchPagination(targetCode, false);
			return;
		}

		// table view (Default View Mode)
		code = code.replace("QUE_EXPLORE_", "QUE_TABLE_");
		if (code.startsWith("QUE_TABLE_")) {
			filter.init(code);
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
		if (code.startsWith("QUE_ADD_")) {
			code = StringUtils.removeStart(code, "QUE_ADD_");
			String prefix = CacheUtils.getObject(userToken.getProductCode(), "DEF_" + code + ":PREFIX", String.class);

			log.info("Prefix: " + code);
			if ("PER".equals(prefix)) {
				JsonObject json = Json.createObjectBuilder()
						.add("definitionCode", "DEF_".concat(code))
						.add("sourceCode", userToken.getUserCode())
						.build();

				kogitoUtils.triggerWorkflow(SELF, "personLifecycle", json);
				return;
			}else if ("MSG".equals(prefix)){
				JsonObject json = Json.createObjectBuilder()
						.add("definitionCode", "DEF_".concat(code))
						.add("sourceCode", userToken.getUserCode())
						.build();

				kogitoUtils.triggerWorkflow(SELF, "messageLifecycle", json);
				return;
			}
		}

		// edit item
		if ("ACT_EDIT".equals(code) && parentCode.startsWith("SBE_.*")) {

			if (parentCode.startsWith("SBE_")) {
				JsonObject payload = Json.createObjectBuilder()
						.add("questionCode", "QUE_BASEENTITY_GRP")
						.add("userCode", userToken.getUserCode())
						.add("sourceCode", userToken.getUserCode())
						.add("targetCode", msg.getData().getTargetCode())
						.build();
				kogitoUtils.triggerWorkflow(SELF, "testQuestion", payload);
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

	/**
	 * Event route
	 * @param msg Message
	 */
	public void route(QDataAnswerMessage msg) {
		if(filter.isValidEvent(msg)){
			filter.handleDataEvents(msg);
		}
	}
}
