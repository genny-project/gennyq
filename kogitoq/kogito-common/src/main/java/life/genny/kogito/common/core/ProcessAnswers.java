package life.genny.kogito.common.core;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.utils.*;
import org.jboss.logging.Logger;

import life.genny.kogito.common.service.TaskService;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.entity.PCM;

/**
 * ProcessAnswers
 */
@ApplicationScoped
public class ProcessAnswers {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	CacheManager cm;

	@Inject
	TaskService taskService;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	DefUtils defUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	FilterUtils filter;

	@Inject
	EntityAttributeUtils beaUtils;

	@Inject
	AttributeUtils attributeUtils;

	/**
	 * @param answer
	 * @param processData
	 * @return
	 */
	public Boolean isValid(Answer answer, ProcessData processData) {
		// filter valid
		if(filter.validFilter(answer.getAttributeCode())) {
			log.info("Filter Attribute !!!");
			return false;
		}

		// ensure targetCode is correct
		if (!answer.getTargetCode().equals(processData.getProcessEntityCode())) {
			log.warn("Bad targetCode in answer!");
			return false;
		}

		// check if the answer is valid for the target
		for (String defCode : processData.getDefCodes()) {
			Definition definition = beUtils.getDefinition(defCode);
			if (!defUtils.answerValidForDEF(definition, answer)) {
				log.error("Bad incoming answer... Not saving!");
				return false;
			}
		}

		return true;
	}

	/**
	 * Check that uniqueness of BE (if required) is satisifed .
	 *
	 * @param processData        The target BE containing the answer data
	 * @return Boolean representing whether uniqueness is satisifed
	 */
	public Boolean checkUniqueness(ProcessData processData) {

		List<Definition> definitions = new ArrayList<>();
		List<String> defCodes = processData.getDefCodes();
		for (String defCode : defCodes) {
			Definition definition = beUtils.getDefinition(defCode);
			definitions.add(definition);
		}

		List<Answer> answers = processData.getAnswers();

		BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
		BaseEntity originalTarget = beUtils.getBaseEntity(processData.getTargetCode());

		// send error for last answer in the list
		// NOTE: This should be reconsidered
		if (answers.isEmpty())
			return true;

		// TODO: Might review below in the future
		if (qwandaUtils.isDuplicate(definitions, null, processEntity, originalTarget)) {
			String feedback = "Error: This value already exists and must be unique.";

			for(Definition definition : definitions){
				for(Answer answer : answers) {
					String attributeCode = answer.getAttributeCode();

					if (definition.findEntityAttribute("UNQ_" + attributeCode).isPresent()) {
						String questionCode = answer.getCode();
						PCM mainPcm = beUtils.getPCM(processData.getPcmCode());
						EntityAttribute subPcmLocationEA = beaUtils.getEntityAttribute(mainPcm.getRealm(), mainPcm.getCode(), Attribute.PRI_LOC + 1, true, true);
						String subPcmLocation = subPcmLocationEA.getAsString();
						PCM subPcm = beUtils.getPCM(subPcmLocation);

						EntityAttribute subPcmQuestionCodeEA = beaUtils.getEntityAttribute(subPcm.getRealm(), subPcm.getCode(), Attribute.PRI_QUESTION_CODE, true, true);
						String subPcmQuestionCode = subPcmQuestionCodeEA.getAsString();
						qwandaUtils.sendAttributeErrorMessage(subPcmQuestionCode, questionCode, attributeCode, feedback);
						return false;
					}
				}
			}
		}

		return true;
	}

	/**
	 * Save all answers gathered in the processBE.
	 * 
	 * @param processData The process entity that is storing the answer data
	 */
	public void saveAllAnswers(ProcessData processData) {
		// save answers
		String targetCode = processData.getTargetCode();
		
		BaseEntity target = beUtils.getBaseEntity(targetCode);

		// iterate our stored process updates and create an answer
		for (Answer answer : processData.getAnswers()) {
			answer.setTargetCode(targetCode);
			// find the attribute
			String attributeCode = answer.getAttributeCode();
			Attribute attribute = attributeUtils.getAttribute(attributeCode, true);

			// check if name needs updating
			if (Attribute.PRI_NAME.equals(attributeCode)) {
				String name = answer.getValue();
				log.debug("Updating BaseEntity Name Value -> " + name);
				target.setName(name);
				continue;
			}

			// update the baseentity
			EntityAttribute entityAttribute = new EntityAttribute(target, attribute, 1.0, answer.getValue());
			target.addAttribute(entityAttribute);
			beaUtils.updateEntityAttribute(entityAttribute);
		}

		// save these answrs to db and cache
		beUtils.updateBaseEntity(target, false);
		log.info("Saved answers for target " + targetCode);

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(target);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
	}

	/**
	 * Clear completed or canceled process Cache Entries.
	 *
	 * @param productCode
	 * @param processBEcode
	 * @return Boolean existed
	 */
	public Boolean clearProcessCacheEntries(String processId, String targetCode) {
		qwandaUtils.clearProcessData(processId);
		log.infof("Cleared caches for %s", processId);
		return true;
	}

	/**
	 * @param processData
	 * @return
	 */
	public ProcessData deleteStoredAnswers(ProcessData processData) {
		processData.setAnswers(new ArrayList<>());
		return processData;
	}
}
