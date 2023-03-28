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

import static life.genny.messages.util.FailureHandler.required;
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
        String projectCode = (String) contextMap.get("PROJECT");
        String realm = userToken.getRealm();
        String recipientCode = (String) contextMap.get("RECIPIENT");

        if (messageCode == null) {
            log.error(ANSIColour.RED + "message code is NULL" + ANSIColour.RESET);
            return;
        }

        if (recipientCode == null) {
            log.error(ANSIColour.RED + "recipient code is NULL" + ANSIColour.RESET);
            return;
        }

        if (projectCode == null) {
            log.error(ANSIColour.RED + "project code is NULL" + ANSIColour.RESET);
            return;
        }

        if (realm == null) {
            log.error(ANSIColour.RED + "realm is NULL" + ANSIColour.RESET);
            return;
        }

        String recipientPhoneNumber = required(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_MOBILE").getValueString());

        String body = null;
        if (contextMap.containsKey("BODY")) {
            body = (String) contextMap.get("BODY");
        } else {
            body = required(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_SHORT_BODY").getValueString());
        }

        body = mergeUtils.merge(body, contextMap);

        String accountSid = required(() -> beaUtils.getEntityAttribute(realm, projectCode, "ENV_TWILIO_ACCOUNT_SID").getValueString());
        String senderPhoneNumber = required(() -> beaUtils.getEntityAttribute(realm, projectCode, "ENV_TWILIO_SOURCE_PHONE").getValueString());
        String twilioAuthToken = required(() -> beaUtils.getEntityAttribute(realm, projectCode, "ENV_TWILIO_AUTH_TOKEN").getValueString());

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
            log.info(ANSIColour.GREEN + " SMS Sent to " + recipientPhoneNumber + ANSIColour.RESET);

        } catch (Exception e) {
            log.error(ANSIColour.RED + "Could Not Send SMS!!! Exception:" + e + ANSIColour.RESET);
        }
    }
}
