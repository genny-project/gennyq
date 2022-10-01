package life.genny.kogito.common.service;

import static life.genny.kogito.common.utils.KogitoUtils.UseService.GADAQ;

import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.checked.GraphQLException;
import life.genny.qwandaq.exception.checked.RoleException;
import life.genny.qwandaq.exception.runtime.response.GennyResponseException;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.capabilities.role.RoleManager;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;

@ApplicationScoped
public class NavigationService {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	SearchService searchService;

	@Inject
	KogitoUtils kogitoUtils;

	@Inject
	SearchUtils searchUtils;

	@Inject
	RoleManager roleManager;

	@Inject
	TaskService tasks;

	public static final String PRI_IS_PREFIX = "PRI_IS_";

	/**
	 * Trigger the default redirection for the user.
	 */
	public void redirect() {
		redirect(null);
	}

	/**
	 * Trigger a redirect based on a code.
	 * @param code
	 */
	public void redirect(String code) {
		// route using code if specified
		if (code != null) {
			log.infof("Performing redirect with code %s", code);
			kogitoUtils.triggerWorkflow(GADAQ, "view", 
				Json.createObjectBuilder()
				.add("code", code)
				.add("targetCode", userToken.getUserCode())
				.build()
			);
		}

		// check for outstanding tasks
		try {
			String processId = kogitoUtils.getOutstandingTaskProcessId();
			kogitoUtils.sendSignal(GADAQ, "processQuestions", processId, "requestion");
			log.info("Outstanding task triggered");
			return;
		} catch (GraphQLException e) {
			log.debug(e.getMessage());
		} catch (GennyResponseException re) {
			log.debug(re.getMessage());
		}

		// otherwise check for default role redirect
		try {
			String redirectCode = roleManager.getUserRoleRedirectCode();
			log.infof("Role Redirect found: %s", redirectCode);

			// send event to gadaq
			QEventMessage msg = new QEventMessage("EVT_MSG", redirectCode);
			msg.getData().setTargetCode(userToken.getUserCode());
			msg.setToken(userToken.getToken());
			KafkaUtils.writeMsg(KafkaTopic.EVENTS, msg);
			return;
		} catch (RoleException e) {
			log.warn(e.getMessage());
		}

		// default to summary
		sendSummary();
	}

	/**
	 * Send a user's dashboard summary
	 */
	public void sendSummary() {

		BaseEntity user = beUtils.getUserBaseEntity();
		BaseEntity pcm = beUtils.getBaseEntityFromLinkAttribute(user, Attribute.LNK_SUMMARY);

		log.infof("Dispatching Summary %s for user %s", user.getCode(), pcm.getCode());
		tasks.dispatch(user.getCode(), user.getCode(), pcm.getCode(), "PCM_CONTENT", "PRI_LOC1");
	}

	/**
	 * Send a view event.
	 *
	 * @param code       The code of the view event.
	 * @param targetCode The targetCode of the view event.
	 */
	public void sendViewEvent(final String code, final String targetCode) {

		JsonObject json = Json.createObjectBuilder()
				.add("event_type", "VIEW")
				.add("msg_type", "EVT_MSG")
				.add("token", userToken.getToken())
				.add("data", Json.createObjectBuilder()
						.add("code", code)
						.add("targetCode", targetCode))
				.build();

		log.info("Sending View Event -> " + code + " : " + targetCode);

		KafkaUtils.writeMsg(KafkaTopic.EVENTS, json.toString());
	}

	public void showProcessPage(final String targetCode) {
		String sourceCode = userToken.getUserCode();
		String eventJson = "{\"data\":{\"targetCode\":\"" + targetCode + "\",\"sourceCode\":\"" + sourceCode
				+ "\",\"parentCode\":\"QUE_SIDEBAR_GRP\",\"code\":\"QUE_TAB_BUCKET_VIEW\",\"attributeCode\":\"QQQ_QUESTION_GROUP\",\"processId\":\"no-idq\"},\"msg_type\":\"EVT_MSG\",\"event_type\":\"BTN_CLICK\",\"redirect\":true,\"token\":\""
				+ userToken.getToken() + "\"}";

		KafkaUtils.writeMsg(KafkaTopic.EVENTS, eventJson);
	}

	/**
	 * Redirect by question code
	 * @param questionCode Question code
	 */
	public void redirectByQuestionCode(String questionCode) {
		String redirectCode = getRedirectCodeByQuestionCode(questionCode);

		JsonObjectBuilder builder = Json.createObjectBuilder()
				.add("code", CommonUtils.removePrefix(redirectCode));

		if (userToken.getUserCode() != null)
			builder.add("targetCode", userToken.getUserCode());

		kogitoUtils.triggerWorkflow(GADAQ, "view", builder.build());
	}

	/**
	 * Return definition code by question code
	 * @param questionCode question code
	 * @return Definition code
	 */
	public String getDefCodeByQuestionCode(String questionCode){
		String defCode = "DEF_" + questionCode.replaceFirst("QUE_QA_","")
				.replaceFirst("QUE_ADD_","")
				.replaceFirst("QUE_","")
				.replace("_GRP","");

		return defCode;
	}

	/**
	 * return redirect question code
	 * @param questionCode Question code
	 * @return redirect question code
	 */
	public String getRedirectCodeByQuestionCode(String questionCode){
		String defaultRedirectCode = "";
		String defCode =  getDefCodeByQuestionCode(questionCode);
		//firstly, check question code
		try {
			BaseEntity target = beUtils.getBaseEntity(defCode);

			defaultRedirectCode = target.getValueAsString("DFT_PRI_DEFAULT_REDIRECT");
			log.info("Actioning redirect for question: " + target.getCode() + " : " + defaultRedirectCode);
		}catch (Exception ex){
			log.error(ex);
		}

		//Secondly, check user to get redirect code
		if (defaultRedirectCode == null || defaultRedirectCode.isEmpty()) {
			defaultRedirectCode = getRedirectCodeByUser();
		}

		if (defaultRedirectCode == null || defaultRedirectCode.isEmpty()) {
			log.error("Question and user has no default redirect!");
			return "";
		}

		return defaultRedirectCode;
	}

	/**
	 * return redirect code by user
	 * @return redirect code
	 */
	public String getRedirectCodeByUser(){
		String redirectCode = "";
		String defCode = "";
		try {
			BaseEntity user = beUtils.getUserBaseEntity();
			List<EntityAttribute> priIsAttributes = user.findPrefixEntityAttributes(PRI_IS_PREFIX);
			if (priIsAttributes.size() > 0) {
				EntityAttribute attr = priIsAttributes.get(0);
				defCode = "DEF_" + attr.getAttributeCode().replaceFirst(PRI_IS_PREFIX, "");
			}

			BaseEntity target = beUtils.getBaseEntity(defCode);
			redirectCode = target.getValueAsString("DFT_PRI_DEFAULT_REDIRECT");
		} catch (Exception ex){
			log.error(ex);
		}

		return redirectCode;
	}

	public static Logger getLog() {
		return log;
	}

	public Jsonb getJsonb() {
		return jsonb;
	}

	public UserToken getUserToken() {
		return userToken;
	}

	public QwandaUtils getQwandaUtils() {
		return qwandaUtils;
	}

	public BaseEntityUtils getBeUtils() {
		return beUtils;
	}

	public SearchService getSearchService() {
		return searchService;
	}

	public KogitoUtils getKogitoUtils() {
		return kogitoUtils;
	}

	public SearchUtils getSearchUtils() {
		return searchUtils;
	}

	public RoleManager getRoleManager() {
		return roleManager;
	}

	public TaskService getTasks() {
		return tasks;
	}

	public static String getPriIsPrefix() {
		return PRI_IS_PREFIX;
	}

}
