package life.genny.messages.managers.impl;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import life.genny.messages.live.qualifer.MessageType;
import life.genny.messages.managers.QMessageProvider;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.utils.MergeUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

import static life.genny.qwandaq.utils.FailureHandler.required;
import static life.genny.qwandaq.message.QBaseMSGMessageType.SMS;

@ApplicationScoped
@MessageType(type = SMS)
public final class QSMSMessageManager extends QMessageProvider {

    @Inject
    Logger log;

    @Inject
    MergeUtils mergeUtils;

    @Override
    public void sendMessage(Map<String, Object> contextMap) {

        log.info(ANSIColour.doColour(">>>>>>>>>>> Triggering SMS <<<<<<<<<<<<<<", ANSIColour.GREEN));

        String messageCode = (String) contextMap.get("MESSAGE");
        log.info(ANSIColour.doColour("messageCode: "+ messageCode, ANSIColour.GREEN));
        String projectCode = (String) contextMap.get("PROJECT");
        log.info(ANSIColour.doColour("projectCode: "+ projectCode, ANSIColour.GREEN));
        String realm = userToken.getRealm();
        log.info(ANSIColour.doColour("realm: "+ realm, ANSIColour.GREEN));
        String recipientCode = (String) contextMap.get("RECIPIENT");
        log.info(ANSIColour.doColour("recipientCode: "+ recipientCode, ANSIColour.GREEN));

        String recipientPhoneNumber = required(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_MOBILE").getValueString());
        log.info(ANSIColour.doColour("recipientPhoneNumber: "+ recipientPhoneNumber, ANSIColour.GREEN));

        String body = null;
        if (contextMap.containsKey("BODY")) {
            body = (String) contextMap.get("BODY");
        } else {
            body = required(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_SHORT_BODY").getValueString());
            log.info(ANSIColour.doColour("body: "+ body, ANSIColour.GREEN));
        }

        body = mergeUtils.merge(body, contextMap);
        log.info(ANSIColour.doColour("merge body: "+ body, ANSIColour.GREEN));

        String accountSid = required(() -> beaUtils.getEntityAttribute(realm, projectCode, "ENV_TWILIO_ACCOUNT_SID").getValueString());
        log.info(ANSIColour.doColour("accountSid: "+ accountSid, ANSIColour.GREEN));
        String senderPhoneNumber = required(() -> beaUtils.getEntityAttribute(realm, projectCode, "ENV_TWILIO_SOURCE_PHONE").getValueString());
        log.info(ANSIColour.doColour("senderPhoneNumber: "+ senderPhoneNumber, ANSIColour.GREEN));
        String twilioAuthToken = required(() -> beaUtils.getEntityAttribute(realm, projectCode, "ENV_TWILIO_AUTH_TOKEN").getValueString());
        log.info(ANSIColour.doColour("twilioAuthToken: "+ twilioAuthToken, ANSIColour.GREEN));

        try {
            // Init and Send SMS
            Twilio.init(accountSid, twilioAuthToken);

            PhoneNumber targetPhoneNumber = new PhoneNumber(recipientPhoneNumber);
            PhoneNumber sourcePhoneNumber = new PhoneNumber(senderPhoneNumber);

            Message msg = Message
                    .creator(targetPhoneNumber, sourcePhoneNumber, body)
                    .create();

            // Log response
            log.info("message status:" + msg.getStatus());
            log.info(ANSIColour.doColour("SMS Sent to " + recipientPhoneNumber, ANSIColour.GREEN));

        } catch (Exception e) {
            log.error(ANSIColour.doColour("Could Not Send SMS!!! Exception:" + e, ANSIColour.RED));
        }
    }
}
