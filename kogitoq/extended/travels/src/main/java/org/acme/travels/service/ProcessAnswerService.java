
package org.acme.travels.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.Service;

@ApplicationScoped
public class ProcessAnswerService {

    private static final Logger log = Logger.getLogger(ProcessAnswerService.class);

    Jsonb jsonb = JsonbBuilder.create();
    @Inject
    QuestionUtils questionUtils;

    @Inject
    DatabaseUtils databaseUtils;

    @Inject
    QwandaUtils qwandaUtils;

    @Inject
    TaskService taskService;

    @Inject
    Service service;

    public String processBaseEntityJson(String qDataAnswerMessageJson, String processBEJson) {

        BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);
        QDataAnswerMessage answerMsg = jsonb.fromJson(qDataAnswerMessageJson, QDataAnswerMessage.class);

        BaseEntity retBE = saveAnswersToProcessBaseEntity(answerMsg, processBE);
        String beJson = jsonb.toJson(retBE);

        return beJson;
    }

    public BaseEntity saveAnswersToProcessBaseEntity(QDataAnswerMessage answerMsg, BaseEntity processBE) {

        if (answerMsg.getItems().length > 0) {

			for (Answer answer : answerMsg.getItems()) {
				Attribute attribute = qwandaUtils.getAttribute(answer.getAttributeCode());
				answer.setAttribute(attribute);

				try {
					processBE.addAnswer(answer);
				} catch (BadDataException e) {
					e.printStackTrace();
				}

				String originalValue = processBE.getValueAsString(answer.getAttributeCode());
			}
        }

        return processBE;
    }

    public Boolean checkMandatory(String qDataAskMessageJson, String processBEJson, String userTokenStr) {

        QDataAskMessage qDataAskMessage = jsonb.fromJson(qDataAskMessageJson, QDataAskMessage.class);
        BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);

        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken());
        Boolean mandatoryUnanswered = false;

        // Show the Current Answer List
        log.info("Current ProcessQuestion Results for: " + processBE.getCode());

		Map<String, Boolean> mandatoryAttributeMap = new HashMap<>();
		Ask submitAsk = null;

        for (Ask ask : qDataAskMessage.getItems()) {

			mandatoryAttributeMap.putAll(recursivelyFindMandatoryMap(ask));

			Ask possibleSubmit = recursivelyLookForSubmit(ask);
			if (possibleSubmit != null) {
				submitAsk = possibleSubmit;
			}
		}

        String targetCode = qDataAskMessage.getItems()[0].getTargetCode();
        BaseEntity target = beUtils.getBaseEntityByCode(targetCode);

        if (target == null) {
            log.error("Target is null : targetCode=" + targetCode);
        }

        for (EntityAttribute ea : processBE.getBaseEntityAttributes()) {

            Boolean mandatory = mandatoryAttributeMap.get(ea.getAttributeCode());
            if (mandatory == null) {
                mandatory = false;
            }

            String oldValue = target.getValueAsString(ea.getAttributeCode());
            String value = ea.getAsString();
            if (oldValue == null) {
                oldValue = "";
            }
            if (value == null) {
                value = "";
            }
            Boolean changed = !oldValue.equals(value);

            String resultLine = (mandatory ? "M" : "o") + ":"
                    + (changed ? "X" : ".")
                    + ":"
                    + ea.getAttributeCode() + ":"
                    + ":" + value;

            log.info("===>" + resultLine);

            if ((StringUtils.isBlank(value)) && (mandatory)) {
                mandatoryUnanswered = true;
            }
        }

        log.info("Mandatory fields are " + (mandatoryUnanswered ? "not" : "ALL") + " filled in ");
        if (submitAsk != null) {
            taskService.enableTaskQuestion(submitAsk, !mandatoryUnanswered, userTokenStr);
        }

        return !mandatoryUnanswered;
    }

	public Ask recursivelyLookForSubmit(Ask ask) {

		if (ask == null) {
			log.error("ask must not be null!");
			return null;
		}

		// check if children exist
		if (ask.getChildAsks() != null && ask.getChildAsks().length > 0) {

			// recuresively check children
			for (Ask child : ask.getChildAsks()) {
				Ask possibleSubmit = recursivelyLookForSubmit(child);

				if (possibleSubmit != null) {
					return possibleSubmit;
				}
			}
		}

		// check if this is our submit
		if ("PRI_SUBMIT".equals(ask.getAttributeCode())) {
			return ask;
		}

		return null;
	}

	public Map<String, Boolean> recursivelyFindMandatoryMap(Ask ask) {

        Map<String, Boolean> mandatoryAttributeMap = new HashMap<>();

		if (ask == null) {
			log.error("ask must not be null!");
			return null;
		}

		// check if children exist
		if (ask.getChildAsks() != null && ask.getChildAsks().length > 0) {

			// recuresively check children
			for (Ask child : ask.getChildAsks()) {
				mandatoryAttributeMap.putAll(recursivelyFindMandatoryMap(child));
			}
		}

		// add our mandatory value to map
		mandatoryAttributeMap.put(ask.getAttributeCode(), ask.getMandatory());

		return mandatoryAttributeMap;
	}

    @Transactional
    public void saveAllAnswers(String sourceCode, String targetCode, String processBEJson) {

        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken(), service.getServiceToken());

        BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);

        BaseEntity source = beUtils.getBaseEntityByCode(sourceCode);
        BaseEntity target = databaseUtils.findBaseEntityByCode(processBE.getRealm(), targetCode);

        // Now to go through all the fields and override them in the target BE
        for (EntityAttribute ea : processBE.getBaseEntityAttributes()) {

            Attribute attribute = qwandaUtils.getAttribute(ea.getAttributeCode());

            if (target.containsEntityAttribute(ea.getAttributeCode())) {
                try {
                    target.setValue(attribute, ea.getValue());
                } catch (BadDataException e) {
                    e.printStackTrace();
                }
            } else {
                // try {
                EntityAttribute newEA = new EntityAttribute(target, attribute, ea.getWeight(), ea.getValue());
                newEA.setRealm(target.getRealm());
                databaseUtils.saveEntityAttribute(newEA);
                target.getBaseEntityAttributes().add(newEA);
            }
        }

        // update target in the cache and DB
		CacheUtils.putObject(target.getRealm(), target.getCode(), target);
        databaseUtils.saveBaseEntity(target);
		log.info("Saved target " + target.getCode());
    }

}
