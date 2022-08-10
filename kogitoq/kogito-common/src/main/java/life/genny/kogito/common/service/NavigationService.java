package life.genny.kogito.common.service;

import static life.genny.kogito.common.utils.KogitoUtils.UseService.GADAQ;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.utils.*;
import org.jboss.logging.Logger;

import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;

@ApplicationScoped
public class NavigationService {

	private static final Logger log = Logger.getLogger(SearchService.class);

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
	DefUtils defUtils;

	public static final String PRI_IS_PREFIX = "PRI_IS_";

	/**
	 * Trigger the default redirection for the user.
	 */
	public void defaultRedirect() {

		// grab default redirect from user be
		BaseEntity user = beUtils.getUserBaseEntity();
		String defaultRedirectCode = user.getValueAsString("PRI_DEFAULT_REDIRECT");
		log.info("Actioning redirect for user " + user.getCode() + " : " + defaultRedirectCode);

		if (defaultRedirectCode == null) {
			log.error("User has no default redirect!");
			return;
		}

		// build json and trigger view workflow
		JsonObject json = Json.createObjectBuilder()
			.add("eventMessage", Json.createObjectBuilder()
				.add("data", Json.createObjectBuilder()
					.add("code", defaultRedirectCode)
					.add("targetCode", userToken.getUserCode())))
			.build();

		kogitoUtils.triggerWorkflow(GADAQ, "view", json);
	}

	/**
	 * Control main content navigation using a pcm and a question
	 *
	 * @param pcmCode The code of the PCM baseentity
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
		EntityAttribute ea = new EntityAttribute(pcm, attribute, 1.0, questionCode);
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
		KafkaUtils.writeMsg("webdata", msg);

		recursivelyPerformPcmSearches(pcm);
	}

	/**
	 * Recuresively traverse a pcm tree and send out any nested searches.
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
	 * @param code The code of the view event.
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

		KafkaUtils.writeMsg("events", json.toString());
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
            pcm = beUtils.getBaseEntityByCode(userToken.getProductCode(), pcmCode);
        }

        if (pcm == null) {
            log.error("Couldn't find PCM with code " + pcmCode);
            throw new NullPointerException("Couldn't find PCM with code " + pcmCode);
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
        KafkaUtils.writeMsg("webdata", msg);

        CacheUtils.putObject(userToken.getProductCode(), cachedCode, pcm);
    }

	/**
	 * Redirect by question code
	 * @param questionCode Question code
	 */
	public void redirectByQuestionCode(String questionCode) {
		String redirectCode = getRedirectCodeByQuestionCode(questionCode);

		// build json and trigger view workflow
		JsonObject json = Json.createObjectBuilder()
				.add("eventMessage", Json.createObjectBuilder()
						.add("data", Json.createObjectBuilder()
								.add("code", redirectCode)
								.add("targetCode", userToken.getUserCode())))
				.build();

		kogitoUtils.triggerWorkflow(GADAQ, "view", json);
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
		String defCode =  getDefCodeByQuestionCode(questionCode);
		BaseEntity target = beUtils.getBaseEntity(defCode);

		String defaultRedirectCode = target.getValueAsString("DFT_PRI_DEFAULT_REDIRECT");
		log.info("Actioning redirect for question: " + target.getCode() + " : " + defaultRedirectCode);

		//Secondly, check user to get redirect code
		if (defaultRedirectCode == null) {
			defaultRedirectCode = getRedirectCodeByUser();
		}

		if (defaultRedirectCode == null) {
			log.error("Question and user has no default redirect!");
			return "";
		}

		return defaultRedirectCode;
	}

	public String getRedirectCodeByUser(){
		String defCode = "";
		BaseEntity user = beUtils.getUserBaseEntity();
		List<EntityAttribute> priIsAttributes = user.findPrefixEntityAttributes(PRI_IS_PREFIX);
		if(priIsAttributes.size() > 0){
			EntityAttribute attr = priIsAttributes.get(0);
			defCode = "DEF_" + attr.getAttributeCode().replaceFirst(PRI_IS_PREFIX, "");
		}

		BaseEntity target = beUtils.getBaseEntity(defCode);
		String redirectCode = target.getValueAsString("DFT_PRI_DEFAULT_REDIRECT");

		return redirectCode;
	}
}
