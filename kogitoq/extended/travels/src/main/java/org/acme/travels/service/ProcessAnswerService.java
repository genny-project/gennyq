
package org.acme.travels.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.message.QDataAskMessage;
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
    QuestionUtils questionUtils;

    @Inject
    DatabaseUtils databaseUtils;

    @Inject
    QwandaUtils qwandaUtils;

    @Inject
    EntityManager entityManager;

    @Inject
    Service service;

    public String processBaseEntityJson(
            String qDataAnswerMessageJson,
            final String qDataAskMessageJson,
            String processBEJson) {
        BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);
        BaseEntity retBE = processBaseEntity(qDataAnswerMessageJson, qDataAskMessageJson, processBE);
        String beJson = jsonb.toJson(retBE);
        // log.info("beJson=" + beJson);
        return beJson;
    }

    public BaseEntity processBaseEntity(final String qDataAnswerMessageJson, final String qDataAskMessageJson,
            BaseEntity processBE) {
        Boolean allMandatoryAttributesAnswered = false;
        log.info("In processBaseEntity :");
        // log.info("AnswerMsg " + qDataAnswerMessageJson);
        QDataAnswerMessage answerMessage = jsonb.fromJson(qDataAnswerMessageJson, QDataAnswerMessage.class);
        Answer answer = null;
        if (answerMessage.getItems().length > 0) {
            answer = answerMessage.getItems()[0];
            answer.setAttribute(qwandaUtils.getAttribute(answer.getAttributeCode()));
            String originalValue = processBE.getValueAsString(answer.getAttributeCode());
            log.info("Original Value for " + answer.getAttributeCode() + " into processBE as " + originalValue);
            try {
                processBE.addAnswer(answer);
            } catch (BadDataException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Check value saved
        String savedValue = processBE.getValueAsString(answer.getAttributeCode());
        log.info("Saved Value into processBE " + answer.getAttributeCode() + " as " + savedValue);

        return processBE;
    }

    public Boolean checkMandatory(String qDataAskMessageJson, String processBEJson) {

        QDataAskMessage qDataAskMessage = jsonb.fromJson(qDataAskMessageJson, QDataAskMessage.class);
        BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);
        Boolean mandatoryUnanswered = false;

        for (Ask ask : qDataAskMessage.getItems()) {
            Object val = processBE.getValue(ask.getAttributeCode(), null);
            if (val == null) {
                if (ask.getMandatory().equals(Boolean.TRUE)) {
                    mandatoryUnanswered = true;
                    return false;
                }
            }
        }

        return !mandatoryUnanswered;
    }

    public void saveAllAnswers(String sourceCode, String targetCode, String processBEJson) {
        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken());

        BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);

        BaseEntity source = beUtils.getBaseEntityByCode(sourceCode);
        BaseEntity target = beUtils.getBaseEntityByCode(targetCode);
        // Now to go through all the fields and override them in the target BE
        for (EntityAttribute ea : processBE.getBaseEntityAttributes()) {
            // try {
            Answer ans = new Answer(source, target, ea.getAttribute(), ea.getValueString());
            ans.setWeight(ea.getWeight());
            beUtils.saveAnswer(ans);
            // target.setValue(ea.getAttributeCode(), ea.getValue(), ea.getValueDouble());
            // } catch (BadDataException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }
        }

    }

}
