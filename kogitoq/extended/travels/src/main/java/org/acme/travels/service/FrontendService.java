
package org.acme.travels.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QCmdMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SecurityUtils;
import life.genny.serviceq.Service;

@ApplicationScoped
public class FrontendService {

    private static final Logger log = Logger.getLogger(FrontendService.class);

    Jsonb jsonb = JsonbBuilder.create();

    @Inject
    QuestionUtils questionUtils;

    @Inject
    DatabaseUtils databaseUtils;

    @Inject
    QwandaUtils qwandaUtils;

    @Inject
    Service service;

	public void sendQDataAskMessageJson(final String askMsgJson) {
		log.info("Entering sendQDataAskMessageJson");
		KafkaUtils.writeMsg("webcmds", askMsgJson);
		log.info("Exiting sendQDataAskMessageJson");
	}

	public void sendQDataAskMessage(final QDataAskMessage askMsg) {
		log.info("Entering sendQDataAskMessage");
		KafkaUtils.writeMsg("webcmds", askMsg);
		log.info("Leaving sendQDataAskMessage");
	}

    public void sendQuestions(final String questionCode, final String sourceCode,
            final String targetCode, final String token) {

		GennyToken userToken = new GennyToken(token);
		String realm = userToken.getRealm();

		log.info("Sending Question using Service! " + userToken.getUsername() + " : " + questionCode + ":" + sourceCode + ":" + targetCode);
        log.info("ServiceToken = " + service.getServiceToken().getToken());

        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken(), userToken);

        BaseEntity source = databaseUtils.findBaseEntityByCode(realm, sourceCode);
        BaseEntity target = databaseUtils.findBaseEntityByCode(realm, targetCode);

        log.info("usercode = " + userToken.getUserCode() + " usernamer=[" + userToken.getUsername() + "]");

        Question rootQuestion = questionUtils.getQuestion(questionCode, beUtils);
		QDataAskMessage msg = questionUtils.findAsks(rootQuestion, source, target, beUtils);
        msg.setToken(beUtils.getGennyToken().getToken());

        questionUtils.sendQuestions(msg, target, userToken);
        log.info("Leaving sendQuestions");
    }

    public String setupProcessBEJson(final String targetCode, BaseEntity processBE, final String qDataAskMessageJson) {

        log.info("Updating processBEJson with latest Target");

        processBE = setupProcessBE(targetCode, processBE, qDataAskMessageJson);
        String processBEJson = jsonb.toJson(processBE);
        return processBEJson;
    }

    public BaseEntity setupProcessBE(final String targetCode, final BaseEntity processBE, final String qDataAskMessageJson) {

        log.info("Updating processBE with latest Target");

        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken());

        // Force the realm
        processBE.setRealm(service.getServiceToken().getRealm());

        // only copy the entityAttributes used in the Asks
        BaseEntity target = beUtils.getBaseEntityByCode(targetCode);
        Set<String> allowedAttributeCodes = new HashSet<>();
        QDataAskMessage qDataAskMessage = jsonb.fromJson(qDataAskMessageJson, QDataAskMessage.class);
        // FIX TODO

        for (Ask ask : qDataAskMessage.getItems()) {
            allowedAttributeCodes.add(ask.getAttributeCode());
            if ((ask.getChildAsks() != null) && (ask.getChildAsks().length > 0)) {
                // dumb single level
                for (Ask childAsk : ask.getChildAsks()) {
                    if ((childAsk.getChildAsks() != null) && (childAsk.getChildAsks().length > 0)) {
                        for (Ask grandChildAsk : childAsk.getChildAsks()) {
                            allowedAttributeCodes.add(grandChildAsk.getAttributeCode());
                        }
                    } else {
                        allowedAttributeCodes.add(childAsk.getAttributeCode());
                    }
                }
            }
        }
        log.info("Number of Asks = " + allowedAttributeCodes.size());
        for (String attributeCode : allowedAttributeCodes) {
            EntityAttribute ea = target.findEntityAttribute(attributeCode).orElse(null);
            if (ea == null) {
                Attribute attribute = qwandaUtils.getAttribute(attributeCode);
                ea = new EntityAttribute(processBE, attribute, 1.0, null);
            }
            processBE.getBaseEntityAttributes().add(ea);
        }
        log.info("Number of BE eas = " + processBE.getBaseEntityAttributes().size());

        log.info("Leaving updateProcessBE");
        return processBE;
    }

    public void sendBaseEntityJson(final String beCode, final String qDataAskMessageJson) {
        QDataAskMessage askMsg = jsonb.fromJson(qDataAskMessageJson, QDataAskMessage.class);
        sendBaseEntity(beCode, askMsg);
    }

    public void sendBaseEntity(final String beCode, final QDataAskMessage askMsg) {

        log.info("Entering sendBaseEntity");

        GennyToken userToken = new GennyToken(askMsg.getToken());
        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken(), userToken);

        // only send the attribute values that are in the questions
        BaseEntity be = beUtils.getBaseEntityByCode(beCode);
        List<String> allowedAttributeCodes = recursivelyFindAllowedAttributes(askMsg.getItems()[0]);

		be = SecurityUtils.privacyFilter(be, allowedAttributeCodes);
		be = qwandaUtils.handleSpecialAttributes(be, allowedAttributeCodes);

        // // Send to front end
        QDataBaseEntityMessage msg = new QDataBaseEntityMessage(be);
        msg.setToken(askMsg.getToken());
        msg.setReplace(true);
        KafkaUtils.writeMsg("webcmds", msg);
        log.info("Leaving sendBaseEntity");
    }

	public List<String> recursivelyFindAllowedAttributes(Ask ask) {

		if (ask == null) {
			log.error("ask cannot be null!");
			return null;
		}

		List<String> allowed = new ArrayList<String>();

		allowed.add(ask.getQuestion().getAttribute().getCode());

		// check if child asks exist
		if (ask.getChildAsks() == null || ask.getChildAsks().length == 0) {

			// call recursively for each child ask
			for (Ask child : ask.getChildAsks()) {
				allowed.addAll(recursivelyFindAllowedAttributes(child));
			}
		}

		return allowed;
	}

    public String getAsksJson(final String questionGroupCode, final String sourceCode, final String targetCode,
            final String userTokenStr, final String processId) {

        log.info("Entering getAsksJson");
        QDataAskMessage askMsg = getAsks(questionGroupCode, sourceCode, targetCode, userTokenStr, processId);
        log.info("About to json QDataAskMessage");

        String askMsgJson = jsonb.toJson(askMsg);
        askMsg = jsonb.fromJson(askMsgJson, QDataAskMessage.class);
        askMsg.setToken(userTokenStr);
        log.info("Leaving getAsksJson");

        return askMsgJson;
    }

    public QDataAskMessage getAsks(final String questionGroupCode, final String sourceCode, final String targetCode,
            final String token, final String processId) {

        log.info("Entering getAsks with ");
        log.info("questionGroupCode :" + questionGroupCode);
        log.info("sourceCode :" + sourceCode);
        log.info("targetCode :" + targetCode);
        log.info("token :" + token);
        log.info("processId :" + processId);

        GennyToken userToken = new GennyToken(token);
        log.info("Getting Asks using Service! " + userToken.getUsername() + " : " + questionGroupCode + ":" + sourceCode + ":" + targetCode);

		String realm = userToken.getRealm();
        BaseEntity source = databaseUtils.findBaseEntityByCode(realm, sourceCode);
        BaseEntity target = databaseUtils.findBaseEntityByCode(realm, targetCode);

        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken(), userToken);

        Question rootQuestion = questionUtils.getQuestion(questionGroupCode, beUtils);
		QDataAskMessage askMsg = questionUtils.findAsks(rootQuestion, source, target, beUtils);
        askMsg.setToken(beUtils.getGennyToken().getToken());

        for (Ask ask : askMsg.getItems()) {
            ask.setProcessId(processId);
        }

        log.info("Leaving getAsks");
        return askMsg;
    }

    public void sendPCM(final String pcmCode, final String token) {

        log.info("Entering sendPCM  PCM -> " + pcmCode);
		QCmdMessage msg = null;

        if (pcmCode.startsWith("PCM")) {
            msg = new QCmdMessage("DISPLAY", "FORM");

		// Old school
        } else {
            String[] displayParms = pcmCode.split(":");
            msg = new QCmdMessage(displayParms[0], displayParms[1]);
        }

		msg.setToken(token);
		KafkaUtils.writeMsg("webcmds", msg);
        log.info("Leaving sendPCM");
    }

    public void sendQuestions(QDataAskMessage askMsg, BaseEntity target, GennyToken userToken) {

        log.info("Entering sendQuestions - AskMsg=" + askMsg);

        QCmdMessage msg = new QCmdMessage("DISPLAY", "FORM");
        msg.setToken(userToken.getToken());

        KafkaUtils.writeMsg("webcmds", msg);

        QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(target);
        beMsg.setToken(userToken.getToken());

        KafkaUtils.writeMsg("webcmds", beMsg); // should be webdata

        askMsg.setToken(userToken.getToken());
        KafkaUtils.writeMsg("webcmds", askMsg);

        QCmdMessage msgend = new QCmdMessage("END_PROCESS", "END_PROCESS");
        msgend.setToken(userToken.getToken());
        msgend.setSend(true);
        KafkaUtils.writeMsg("webcmds", msgend);
        log.info("LEaving sendQuestions ");
    }
}
