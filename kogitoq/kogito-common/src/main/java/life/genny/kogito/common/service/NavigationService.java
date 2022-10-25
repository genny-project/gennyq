package life.genny.kogito.common.service;

import static life.genny.kogito.common.utils.KogitoUtils.UseService.GADAQ;
import static life.genny.qwandaq.entity.PCM.PCM_CONTENT;

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
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.exception.checked.GraphQLException;
import life.genny.qwandaq.exception.checked.RoleException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
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
	 * 
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
							.build());
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
			log.info("Role Redirect sent");
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
		BaseEntity summary = beUtils.getBaseEntityFromLinkAttribute(user, Attribute.LNK_SUMMARY);
		if (summary == null)
			throw new ItemNotFoundException("LNK_SUMMARY for " + user.getCode());

		PCM pcm = PCM.from(summary);

		log.infof("Dispatching Summary %s for user %s", user.getCode(), pcm.getCode());
		tasks.dispatch(user.getCode(), user.getCode(), pcm, PCM_CONTENT, "PRI_LOC1");
	}

	/**
	 * Redirect by question code
	 * 
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
	 * 
	 * @param questionCode question code
	 * @return Definition code
	 */
	public String getDefCodeByQuestionCode(String questionCode) {
		String defCode = "DEF_" + questionCode.replaceFirst("QUE_QA_", "")
				.replaceFirst("QUE_ADD_", "")
				.replaceFirst("QUE_", "")
				.replace("_GRP", "");

		return defCode;
	}

	/**
	 * return redirect question code
	 * 
	 * @param questionCode Question code
	 * @return redirect question code
	 */
	public String getRedirectCodeByQuestionCode(String questionCode) {
		String defaultRedirectCode = "";
		String defCode = getDefCodeByQuestionCode(questionCode);
		// firstly, check question code
		try {
			BaseEntity target = beUtils.getBaseEntity(defCode);

			defaultRedirectCode = target.getValueAsString("DFT_PRI_DEFAULT_REDIRECT");
			log.info("Actioning redirect for question: " + target.getCode() + " : " + defaultRedirectCode);
		} catch (Exception ex) {
			log.error(ex);
		}

		// Secondly, check user to get redirect code
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
	 * 
	 * @return redirect code
	 */
	public String getRedirectCodeByUser() {
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
		} catch (Exception ex) {
			log.error(ex);
		}

		return redirectCode;
	}

}
