package life.genny.gadaq.live.data;

import static life.genny.kogito.common.utils.KogitoUtils.UseService.SELF;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import life.genny.kogito.common.service.SearchService;
import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.exception.checked.GraphQLException;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.MessageData;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.GraphQLUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.SecurityUtils;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;

@ApplicationScoped
public class InternalConsumer {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	Service service;

	@Inject
	GennyScopeInit scope;

	@Inject
	UserToken userToken;

	@Inject
	KogitoUtils kogitoUtils;

	@Inject
	SearchService search;

	@Inject
	GraphQLUtils gqlUtils;

	/**
	 * Execute on start up.
	 *
	 * @param ev The startup event
	 */
	void onStart(@Observes StartupEvent ev) {
		service.fullServiceInit();
	}

	/**
	 * Consume incoming answers for inference
	 * 
	 * @param data The incoming data
	 */
	@Incoming("valid_data")
	@Blocking
	public void getData(String data) {

		Instant start = Instant.now();
		log.info("Received Data : " + SecurityUtils.obfuscate(data));

		// init scope and process msg
		scope.init(data);
		List<Answer> answers = kogitoUtils.runDataInference(data);
		if (answers.isEmpty())
			log.warn("[!] No answers after inference");
		// else
		// kogitoUtils.funnelAnswers(answers);

		Optional<Answer> searchText = answers.stream()
				.filter(ans -> ans.getAttributeCode().equals("PRI_SEARCH_TEXT"))
				.findFirst();

		if (searchText.isPresent()) {
			Answer ans = searchText.get();
			search.sendNameSearch(ans.getTargetCode(), ans.getValue());
		}

		// pass it on to the next stage of inference pipeline
		QDataAnswerMessage msg = new QDataAnswerMessage(answers);
		msg.setToken(userToken.getToken());
		KafkaUtils.writeMsg(KafkaTopic.GENNY_DATA, msg);

		scope.destroy();
		// log duration
		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
	}

	/**
	 * Consume from the genny_events topic.
	 * 
	 * @param event The incoming event
	 */
	@Incoming("events")
	@Blocking
	public void getEvent(String event) {

		// init scope and process msg
		Instant start = Instant.now();
		scope.init(event);

		// check if event is a valid event
		QEventMessage msg = null;
		try {
			msg = jsonb.fromJson(event, QEventMessage.class);
		} catch (Exception e) {
			log.error("Cannot parse this event! " + event);
			e.printStackTrace();
			return;
		}

		log.info("Received Event : " + SecurityUtils.obfuscate(event));

		// If the event is a Dropdown then leave it for DropKick
		if ("DD".equals(msg.getEvent_type())) {
			return;
		}
		routeEvent(msg);
		scope.destroy();
		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
	}

	/**
	 * @param msg
	 */
	public void routeEvent(QEventMessage msg) {

		MessageData data = msg.getData();

		String code = data.getCode();
		String processId = data.getProcessId();

		String parentCode = data.getParentCode();
		String targetCode = data.getTargetCode();

		// auth init
		if ("AUTH_INIT".equals(code)) {
			kogitoUtils.triggerWorkflow(SELF, "authInit", "eventMessage", msg);
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

		// route view
		if ((code.startsWith("QUE_") && code.endsWith("_VIEW")) || code == "ACT_VIEW" || code.startsWith("QUE_TREE_ITEM_.*")) {
			JsonObjectBuilder builder = Json.createObjectBuilder()
				.add("code", CommonUtils.removePrefix(code));

			if (targetCode != null)
				builder.add("targetCode", targetCode);

			kogitoUtils.triggerWorkflow(SELF, "view", builder.build());
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

		// update summary code
		if ("UPDATE_SUMMARY".equals(code)) {
			try {
				processId = gqlUtils.fetchProcessId("PersonLifecycle", "entityCode", msg.getData().getTargetCode());
			} catch (GraphQLException e) {
				e.printStackTrace();
				return;
			}
			kogitoUtils.sendSignal(SELF, "personLifecycle", processId, "update_summary", jsonb.toJson(msg));
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
