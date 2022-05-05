package life.genny.gadaq.service;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.Service;

@ApplicationScoped
public class ProcessAnswerService {

	private static final Logger log = Logger.getLogger(ProcessAnswerService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	QuestionUtils questionUtils;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	TaskService taskService;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	Service service;

	@Inject
	EntityManager entityManager;

	/**
	 * Save incoming answers to the process baseentity.
	 *
	 * @param answerMessage The incoming answers
	 * @param askMessage The ask message representing the questions
	 * @param processBE The process entity to store the answer data
	 * @return The updated process baseentity
	 */
	public BaseEntity storeIncomingAnswers(QDataAnswerMessage answerMessage, QDataAskMessage askMessage, BaseEntity processBE) {

		// iterate incoming answers
		for (Answer answer : answerMessage.getItems()) {

			// ensure the target code matches
			if (!answer.getTargetCode().equals(processBE.getCode())) {
				log.warn("Found an Answer with a bad target code : " + answer.getTargetCode());
				continue;
			}

			// find the attribute
			String attributeCode = answer.getAttributeCode();
			Attribute attribute = qwandaUtils.getAttribute(attributeCode);
			answer.setAttribute(attribute);

			// debug log the value before saving
			String currentValue = processBE.getValueAsString(attributeCode);
			log.debug("Overwriting Value -> " + answer.getAttributeCode() + " = " + currentValue);

			// update the baseentity
			try {
				processBE.addAnswer(answer);
			} catch (BadDataException e) {
				e.printStackTrace();
			}

			// log the new value
			String savedValue = processBE.getValueAsString(answer.getAttributeCode());
			log.info("Value Saved -> " + answer.getAttributeCode() + " = " + savedValue);
		}

		return processBE;
	}

	/**
	 * Check if all mandatory questions have been answered.
	 *
	 * @param askMessage The ask message representing the questions
	 * @param processBE The process entity storing the answer data
	 * @return Boolean representing whether all mandatory questions have been answered
	 */
	public Boolean checkMandatory(QDataAskMessage askMessage, BaseEntity processBE) {

		log.info("Checking mandatorys against " + processBE.getCode());

		Boolean mandatoryUnanswered = false;

		// find the submit ask
		Ask submit = null;
		for (Ask ask : askMessage.getItems()) {
			submit = recursivelyCheckForSubmit(ask);
			if (submit != null) {
				break;
			}
		}

		// find all the mandatory booleans
		Map<String, Boolean> map = new HashMap<>();
		for (Ask ask : askMessage.getItems()) {
			map = recursivelyFillMandatoryMap(map, ask);
		}

		// TODO: Fix this!
		// this assumes all asks are targeting the same entity
		BaseEntity target = beUtils.getBaseEntityByCode(askMessage.getItems()[0].getTargetCode());
		if (target == null) {
			log.error("Target is null");
			return null;
		}

		Boolean answered = true;

		// iterate entity attributes to check which have been answered
		for (EntityAttribute ea : processBE.getBaseEntityAttributes()) {

			String attributeCode = ea.getAttributeCode();
			Boolean mandatory = map.get(attributeCode);

			String value = ea.getAsString();

			// if any are both blank and mandatory, then task is not complete
			if ((StringUtils.isBlank(value)) && (mandatory)) {
				answered = false;
			}

			String resultLine = (mandatory?"M":"O")+ " : " + ea.getAttributeCode() + " : " + value; 
			log.info("===>" + resultLine);
		}

		log.info("Mandatory fields are " + (mandatoryUnanswered ? "not" : "ALL") + " complete");

		// enable/disable the submit according to answered var
		if (submit != null) {
			taskService.enableTaskQuestion(submit, answered);
		}

		return answered;
	}

	/**
	 * Find the submit ask using recursion.
	 *
	 * @param ask The ask to traverse
	 * @return The submit ask
	 */
	public Ask recursivelyCheckForSubmit(Ask ask) {

		if (ask.getAttributeCode().equals("PRI_SUBMIT")) {
			return ask;
		}

		for (Ask child : ask.getChildAsks()) {

			Ask childSubmit = recursivelyCheckForSubmit(child);

			if (childSubmit != null) {
				return childSubmit;
			}
		}

		return null;
	}

	/**
	 * Fill the mandatory map using recursion.
	 *
	 * @param map The map to fill
	 * @param ask The ask to traverse
	 * @return The filled map
	 */
	public Map<String, Boolean> recursivelyFillMandatoryMap(Map<String, Boolean> map, Ask ask) {

		map.put(ask.getAttributeCode(), ask.getMandatory());

		for (Ask child : ask.getChildAsks()) {
			map = recursivelyFillMandatoryMap(map, child);
		}

		return map;
	}

	/**
	 * Save all answers gathered in the processBE.
	 *
	 * @param sourceCode The source of the answers
	 * @param targetCode The target of the answers
	 * @param processBE The process entity that is storing the answer data
	 */
	@Transactional
	public void saveAllAnswers(String sourceCode, String targetCode, BaseEntity processBE) {

		List<Answer> answers = new ArrayList<>();

		// iterate our stored process updates and create an answer
		for (EntityAttribute ea : processBE.getBaseEntityAttributes()) {
			// TODO: Check if this needs greater dataType precision
			Answer answer = new Answer(sourceCode, targetCode, ea.getAttributeCode(), ea.getValueString());
			answers.add(answer);
		}

		// save these answrs to db and cache
		beUtils.saveAnswers(answers);
		log.info("Saved answers for target " + targetCode);
	}

}
