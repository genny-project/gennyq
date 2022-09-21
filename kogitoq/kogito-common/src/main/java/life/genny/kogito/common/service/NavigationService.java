package life.genny.kogito.common.service;

import static life.genny.kogito.common.utils.KogitoUtils.UseService.GADAQ;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;
import life.genny.qwandaq.utils.capabilities.CapabilityUtils;
import life.genny.qwandaq.utils.CommonUtils;
import org.jboss.logging.Logger;

import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.exception.checked.GraphQLException;
import life.genny.qwandaq.exception.checked.RoleException;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.exception.runtime.response.GennyResponseException;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;

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
	SummaryService summaryService;

	@Inject
	SearchService searchService;

	@Inject
	KogitoUtils kogitoUtils;

	@Inject
	SearchUtils searchUtils;

	@Inject
	CapabilityUtils capabilityUtils;

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
			String redirectCode = capabilityUtils.getUserRoleRedirectCode();
			log.infof("Role Redirect found: %s", redirectCode);
			kogitoUtils.triggerWorkflow(GADAQ, "view", 
				Json.createObjectBuilder()
				.add("code", redirectCode)
				.build()
			);
			log.info("Role Redirect sent");
			return;
		} catch (RoleException e) {
			log.warn(e.getMessage());
		}

		// default to dashboard
		kogitoUtils.triggerWorkflow(GADAQ, "view", 
			Json.createObjectBuilder()
			.add("code", "DASHBOARD_VIEW")
			.build()
		);
		log.info("Dashboard View triggered");
	}

	/**
	 * Control main content navigation using a pcm and a question
	 *
	 * @param pcmCode      The code of the PCM baseentity
	 * @param questionCode The code of the question
	 */
	public void navigateContent(final String pcmCode, final String questionCode) {

		// fetch and update content pcm
		BaseEntity content = beUtils.getBaseEntityByCode("PCM_CONTENT");
		try {
			content.setValue("PRI_LOC1", pcmCode);
		} catch (BadDataException e) {
			e.printStackTrace();
		}

		// fetch and update desired pcm
		BaseEntity pcm = beUtils.getBaseEntityByCode(pcmCode);
		Attribute attribute = qwandaUtils.getAttribute("PRI_QUESTION_CODE");
		EntityAttribute ea = new EntityAttribute(1.0, questionCode);
		ea.setBaseEntityCode(pcm.getCode());
		ea.setAttribute(attribute);
		try {
			pcm.addAttribute(ea);
		} catch (BadDataException e) {
			e.printStackTrace();
		}

		// package all and send
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage();
		msg.add(content);
		msg.add(pcm);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		log.info("Sending PCMs for " + questionCode);
		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);

		recursivelyPerformPcmSearches(pcm);
	}

	/**
	 * Recuresively traverse a pcm tree and send out any nested searches.
	 * 
	 * @param pcm The pcm to begin raversing
	 */
	public void recursivelyPerformPcmSearches(BaseEntity pcm) {

		log.info("(0) running recursive function for " + pcm.getCode());

		// filter location attributes
		Set<EntityAttribute> locs = pcm.getBaseEntityAttributes()
				.stream()
				.filter(ea -> ea.getAttributeCode().startsWith("PRI_LOC"))
				.collect(Collectors.toSet());

		// perform searches
		locs.stream()
				.filter(ea -> ea.getValueString().startsWith("SBE"))
				.map(ea -> CacheUtils.getObject(userToken.getProductCode(), ea.getValueString(), SearchEntity.class))
				.peek(sbe -> log.info("Sending Search " + sbe.getCode()))
				.forEach(sbe -> searchUtils.searchBaseEntitys(sbe));

		// run function for nested pcms
		locs.stream()
				.filter(ea -> ea.getValueString().startsWith("PCM"))
				.map(ea -> beUtils.getBaseEntity(ea.getValueString()))
				.peek(ent -> recursivelyPerformPcmSearches(ent));

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

	/**
	 * Function to update a pcm location
	 */
	public void updatePcm(String pcmCode, String loc, String newValue) {

		log.info("Replacing " + pcmCode + ":" + loc + " with " + newValue);

		String cachedCode = userToken.getJTI() + ":" + pcmCode;
		BaseEntity pcm = CacheUtils.getObject(userToken.getProductCode(), cachedCode, BaseEntity.class);

		if (pcm == null) {
			log.info("Couldn't find " + cachedCode + " in cache, grabbing from db!");
			pcm = beUtils.getBaseEntity(pcmCode);
		}

		log.info("Found PCM " + pcm);

		Optional<EntityAttribute> locOptional = pcm.findEntityAttribute(loc);
		if (!locOptional.isPresent()) {
			log.error("Couldn't find base entity attribute " + loc);
			throw new NullPointerException("Couldn't find base entity attribute " + loc);
		}

		EntityAttribute locAttribute = locOptional.get();
		log.info(locAttribute.getAttributeCode() + " has valueString " + locAttribute.getValueString());
		locAttribute.setValueString(newValue);

		Set<EntityAttribute> attributes = pcm.getBaseEntityAttributes();
		attributes.removeIf(att -> att.getAttributeCode().equals(loc));
		attributes.add(locAttribute);
		pcm.setBaseEntityAttributes(attributes);

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(pcm);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);

		CacheUtils.putObject(userToken.getProductCode(), cachedCode, pcm);
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

}
