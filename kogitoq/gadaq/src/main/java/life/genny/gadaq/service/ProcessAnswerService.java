package life.genny.gadaq.service;

import java.util.HashMap;
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

    public Boolean checkMandatory(String qDataAskMessageJson, String processBEJson, String userTokenStr) {

        QDataAskMessage qDataAskMessage = jsonb.fromJson(qDataAskMessageJson, QDataAskMessage.class);
        BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);

        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken());
        Boolean mandatoryUnanswered = false;
        Map<String, Boolean> mandatoryAttributeMap = new HashMap<>();

        // Show the Current Answer List
        log.info("Current ProcessQuestion Results for: " + processBE.getCode());

        Ask submitAsk = null; // identify the submit ask

        for (Ask ask : qDataAskMessage.getItems()) {
            if ((ask.getChildAsks() != null) && (ask.getChildAsks().length > 0)) {
                // dumb single level
                for (Ask childAsk : ask.getChildAsks()) {
                    if ((childAsk.getChildAsks() != null) && (childAsk.getChildAsks().length > 0)) {
                        for (Ask grandChildAsk : childAsk.getChildAsks()) {
                            mandatoryAttributeMap.put(grandChildAsk.getAttributeCode(), grandChildAsk.getMandatory());
                            if (grandChildAsk.getAttributeCode().equals("PRI_SUBMIT")) {
                                submitAsk = grandChildAsk;
                            }
                        }
                    } else {
                        mandatoryAttributeMap.put(childAsk.getAttributeCode(), childAsk.getMandatory());
                        if (childAsk.getAttributeCode().equals("PRI_SUBMIT")) {
                            submitAsk = childAsk;
                        }
                    }
                }
            } else {
                mandatoryAttributeMap.put(ask.getAttributeCode(), ask.getMandatory());
                if (ask.getAttributeCode().equals("PRI_SUBMIT")) {
                    submitAsk = ask;
                }
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
                    + (changed ? "X"
                            : ".")
                    + ":"
                    + ea.getAttributeCode() + ":"
                    + ":" + value;
            log.info("===>" + resultLine);

            if ((StringUtils.isBlank(value)) && (mandatory)) {
                // log.info("Mandatory Unaswered! " + ea.getAttributeCode());
                mandatoryUnanswered = true;
            }
        }

        // for (Ask ask : qDataAskMessage.getItems()) {
        // Object val = processBE.getValue(ask.getAttributeCode(), null);
        // if (val == null) {
        // if (ask.getMandatory().equals(Boolean.TRUE)) {
        // mandatoryUnanswered = true;
        // return false;
        // }
        // }
        // }
        log.info("Mandatory fields are " + (mandatoryUnanswered ? "not" : "ALL") + " filled in ");
        if (submitAsk != null) {
            taskService.enableTaskQuestion(submitAsk, !mandatoryUnanswered, userTokenStr);
        }
        return !mandatoryUnanswered;
    }

    @Transactional
    public void saveAllAnswers(String sourceCode, String targetCode, String processBEJson) {
        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken(), service.getServiceToken());

        BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);

        BaseEntity source = beUtils.getBaseEntityByCode(sourceCode);
        BaseEntity target = databaseUtils.findBaseEntityByCode(processBE.getRealm(), targetCode); // .getBaseEntityByCode(targetCode);
        // Now to go through all the fields and override them in the target BE
        for (EntityAttribute ea : processBE.getBaseEntityAttributes()) {
            Attribute attribute = qwandaUtils.getAttribute(ea.getAttributeCode());

            if (target.containsEntityAttribute(ea.getAttributeCode())) {
                try {
                    target.setValue(attribute, ea.getValue());
                } catch (BadDataException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                // try {
                EntityAttribute newEA = new EntityAttribute(target, attribute, ea.getWeight(), ea.getValue());
                newEA.setRealm(target.getRealm());
                entityManager.persist(newEA);
                target.getBaseEntityAttributes().add(newEA);
                // Answer ans = new Answer(source, target, attribute, ea.getValueString());
                // ans.setWeight(ea.getWeight());
                // ans.setRealm(target.getRealm());
                // ans.setAttribute(attribute);
                // try {
                // target.addAnswer(ans);
                // } catch (BadDataException e) {
                // // TODO Auto-generated catch block
                // e.printStackTrace();
                // }
            }
        }
        CacheUtils.putObject(target.getRealm(), target.getCode(), target);

        // update target in the DB
        if (target.getId() == null) {
            entityManager.persist(target);
        } else {
            entityManager.merge(target);
        }
        log.info("Saved target " + target.getCode());
        // databaseUtils.saveBaseEntity(target); // TODO, should not be needed with
        // infinispan persist

    }

}
