package life.genny.kogito.common.service;

import static life.genny.kogito.common.utils.KogitoUtils.UseService.GADAQ;
import static life.genny.qwandaq.entity.PCM.PCM_CONTENT;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.exception.checked.GraphQLException;
import life.genny.qwandaq.exception.checked.RoleException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.response.GennyResponseException;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.KafkaUtils;

@ApplicationScoped
public class NavigationService extends KogitoService {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	Jsonb jsonb = JsonbBuilder.create();

	public static final String PRI_IS_PREFIX = "PRI_IS_";

	@Inject
	FilterService filterService;

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
		log.info("^^^^^ Running redirect");
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
		// fetch user's linked summary
		String userCode = userToken.getUserCode();
		String productCode = userToken.getProductCode();
    		EntityAttribute summary;
    		try {
			summary = beaUtils.getEntityAttribute(productCode, userCode, Attribute.LNK_SUMMARY);
    		} catch (ItemNotFoundException e) {
			log.warn("Could not find user summary/dashboard for: " + userCode);
      			e.printStackTrace();
      			return;
    		}
		
		String summaryCode = CommonUtils.cleanUpAttributeValue(summary.getValueString());
		log.debug("Found summary PCM: " + summaryCode);

		log.infof("Dispatching Summary %s for user %s", userCode, summaryCode);
		JsonObject payload = Json.createObjectBuilder()
				.add("sourceCode", userCode)
				.add("targetCode", userCode)
				.add("pcmCode", summaryCode)
				.add("parent", PCM_CONTENT)
				.add("location", PCM.location(1))
				.build();

		kogitoUtils.triggerWorkflow(GADAQ, "processQuestions", payload);
	}

	/**
	 * Redirect by question code
	 * 
	 * @param questionCode Question code
	 */
	@Deprecated
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
	@Deprecated
	public String getRedirectCodeByQuestionCode(String questionCode) {
		String defaultRedirectCode = "";
		String defCode = getDefCodeByQuestionCode(questionCode);
		// firstly, check question code
		try {
			BaseEntity target = beUtils.getBaseEntity(defCode);
			String targetCode = target.getCode();
			EntityAttribute priDefaultRedirect = beaUtils.getEntityAttribute(target.getRealm(), targetCode, "DFT_PRI_DEFAULT_REDIRECT", false);
			if (priDefaultRedirect != null) {
				defaultRedirectCode = priDefaultRedirect.getValueString();
			}
			log.info("Actioning redirect for question: " + targetCode + " : " + defaultRedirectCode);
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
	@Deprecated
	public String getRedirectCodeByUser() {
		String redirectCode = "";
		String defCode = "";
		try {
			BaseEntity user = beUtils.getUserBaseEntity();
			Set<EntityAttribute> priIsAttributes = beaUtils.getBaseEntityAttributesForBaseEntityWithAttributeCodePrefix(user.getRealm(), user.getCode(), PRI_IS_PREFIX);
			if (priIsAttributes.size() > 0) {
				EntityAttribute attr = priIsAttributes.iterator().next();
				defCode = "DEF_" + attr.getAttributeCode().replaceFirst(PRI_IS_PREFIX, "");
			}

			BaseEntity target = beUtils.getBaseEntity(defCode);
			redirectCode = beaUtils.getEntityAttribute(target.getRealm(), defCode, "DFT_PRI_DEFAULT_REDIRECT", false).getValueString();
		} catch (Exception ex) {
			log.error(ex);
		}

		return redirectCode;
	}

	/**
	 * Redirect by question code
	 *
	 * @param code Question code
	 */
	public void redirectByTable(String code) {
		filterService.init(code);
		searchService.sendTable(code);
	}

}
