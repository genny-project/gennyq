package life.genny.messages.managers.impl;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import life.genny.messages.live.qualifer.MessageType;
import life.genny.messages.managers.QMessageProvider;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.utils.TimeUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static life.genny.qwandaq.utils.FailureHandler.optional;
import static life.genny.qwandaq.utils.FailureHandler.required;
import static life.genny.qwandaq.message.QBaseMSGMessageType.SENDGRID;

/**
 * Sends email using sendgrid along with the email template id and data for the email template
 */
@ApplicationScoped
@MessageType(type = SENDGRID)
public final class QSendGridMessageManager extends QMessageProvider {

    @Inject
    Logger log;

    @Override
    public void sendMessage(Map<String, Object> contextMap) {
        log.info(ANSIColour.doColour(">>>>>>>>>>> Triggering Sendgrid email <<<<<<<<<<<<<<", ANSIColour.GREEN));

        String recipientCode = (String) required(() -> contextMap.get("RECIPIENT"));
        log.info(ANSIColour.doColour("recipientCode: "+ recipientCode, ANSIColour.GREEN));
        String projectCode = (String) required(() -> contextMap.get("PROJECT"));
        log.info(ANSIColour.doColour("projectCode: "+ projectCode, ANSIColour.GREEN));
        String messageCode = (String) required(() -> contextMap.get("MESSAGE"));
        log.info(ANSIColour.doColour("messageCode: "+ messageCode, ANSIColour.GREEN));
        String realm = userToken.getRealm();
        log.info(ANSIColour.doColour("realm: "+ realm, ANSIColour.GREEN));

        String recipientEmail = required(() -> beaUtils.getEntityAttribute(realm, recipientCode, "PRI_EMAIL").getValueString()).trim();
        log.info(ANSIColour.doColour("recipientEmail: "+ recipientEmail, ANSIColour.GREEN));
        String templateId = required(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_SENDGRID_ID").getValueString());
        log.info(ANSIColour.doColour("templateId: "+ templateId, ANSIColour.GREEN));
        String subject = required(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_SUBJECT").getValueString());
        log.info(ANSIColour.doColour("subject: "+ subject, ANSIColour.GREEN));
        String emailSender = required(() -> beaUtils.getEntityAttribute(realm, projectCode, "ENV_SENDGRID_EMAIL_SENDER").getValueString());
        log.info(ANSIColour.doColour("emailSender: "+ emailSender, ANSIColour.GREEN));
        String emailNameSender = required(() -> beaUtils.getEntityAttribute(realm, projectCode, "ENV_SENDGRID_EMAIL_NAME_SENDER").getValueString());
        log.info(ANSIColour.doColour("emailNameSender: "+ emailNameSender, ANSIColour.GREEN));
        String emailApiKey = required(() -> beaUtils.getEntityAttribute(realm, projectCode, "ENV_SENDGRID_API_KEY").getValueString());
        log.info(ANSIColour.doColour("emailApiKey: "+ emailApiKey, ANSIColour.GREEN));

        Email from = new Email(emailSender, emailNameSender);
        Email to = new Email(recipientEmail);

        Personalization personalization = new Personalization();
        personalization.addTo(to);
        personalization.setSubject(subject);

        // Hande CC and BCC
        String ccVal = (String) contextMap.get("CC");
        String bccVal = (String) contextMap.get("BCC");

        if (ccVal != null) {
            buildCc(to, personalization, ccVal);
        }

        if (bccVal != null) {
            buildBcc(to, personalization, bccVal);
        }

        // Build a general data map from context BEs
        Map<String, Object> templateData = formatValue(contextMap, messageCode, recipientCode);

        for (Map.Entry<String, Object> entry : templateData.entrySet()) {
            personalization.addDynamicTemplateData(entry.getKey(), entry.getValue());
        }

        Mail mail = buildMail(templateId, from, personalization);

        sendEmail(mail, emailApiKey);
    }

    private Map<String, Object> formatValue(Map<String, Object> contextMap, String messageCode, String recipientCode) {

        Map<String, Object> templateData = new HashMap<>();

        for (Map.Entry<String, Object> entry : contextMap.entrySet()) {

            Object value = entry.getValue();
            if (value == null) {
                log.error("========================== FATAL ==================");
                log.error("Could not retrieve value for: " + entry.getKey() + " in message " + messageCode);
                log.error("=================================================");
                continue;
            }
            if (BaseEntity.class.equals(value.getClass())) {
                log.info("Processing key as BASEENTITY: " + entry.getKey());
                BaseEntity be = (BaseEntity) value;
                HashMap<String, String> deepReplacementMap = new HashMap<>();
                for (EntityAttribute ea : beaUtils.getAllEntityAttributesForBaseEntity(be)) {

                    String attrCode = ea.getAttributeCode();
                    if (attrCode.startsWith("LNK") || attrCode.startsWith("PRI")) {
                        Attribute attribute = attributeUtils.getAttribute(ea.getAttributeCode(), true, true);
                        ea.setAttribute(attribute);
                        Object attrVal = ea.getValue();
                        if (attrVal != null) {

                            String valueString = attrVal.toString();

                            if (attrVal.getClass().equals(LocalDate.class)) {
                                if (contextMap.containsKey("DATEFORMAT")) {
                                    String format = (String) contextMap.get("DATEFORMAT");
                                    valueString = TimeUtils.formatDate((LocalDate) attrVal, format);
                                } else {
                                    log.info("No DATEFORMAT key present in context map, defaulting to stringified date");
                                }
                            } else if (attrVal.getClass().equals(LocalDateTime.class)) {
                                if (contextMap.containsKey("DATETIMEFORMAT")) {

                                    String timezone = optional(() -> beaUtils.getEntityAttribute(userToken.getRealm(), recipientCode, "PRI_TIMEZONE_ID").getValueString(), "UTC");

                                    String format = (String) contextMap.get("DATETIMEFORMAT");
                                    LocalDateTime dtt = (LocalDateTime) attrVal;

                                    ZonedDateTime zonedDateTime = dtt.atZone(ZoneId.of("UTC"));
                                    ZonedDateTime converted = zonedDateTime.withZoneSameInstant(ZoneId.of(timezone));

                                    valueString = TimeUtils.formatZonedDateTime(converted, format);
                                    log.info("date format");
                                    log.info("formatted date: " + valueString);
                                } else {
                                    log.info("No DATETIMEFORMAT key present in context map, defaulting to stringifies dateTime");
                                }
                            }
                            // templateData.put(key+"."+attrCode, valueString);
                            deepReplacementMap.put(attrCode, valueString);
                        }
                    }
                }
                templateData.put(entry.getKey(), deepReplacementMap);
            } else if (value.getClass().equals(String.class)) {
                log.info("Processing key as STRING: " + entry.getKey());
                templateData.put(entry.getKey(), value);
            }
        }
        return templateData;
    }

    private void buildBcc(Email to, Personalization personalization, String bcc) {
            String email = required(() -> beaUtils.getEntityAttribute(userToken.getRealm(), bcc, "PRI_EMAIL").getValueString()).trim();

            if (!email.equals(to.getEmail())) {
                personalization.addBcc(new Email(email));
                log.info(ANSIColour.doColour("Found BCC Email: " + email, ANSIColour.BLUE));
            }
    }

    private void buildCc(Email to, Personalization personalization, String cc) {
        String email = required(() -> beaUtils.getEntityAttribute(userToken.getRealm(), cc, "PRI_EMAIL").getValueString()).trim();

        if (!email.equals(to.getEmail())) {
            personalization.addCc(new Email(email));
            log.info(ANSIColour.doColour("Found BCC Email: " + email, ANSIColour.BLUE));
        }
    }

    private Mail buildMail(String templateId, Email from, Personalization personalization) {
        Mail mail = new Mail();
        mail.addPersonalization(personalization);
        mail.setTemplateId(templateId);
        mail.setFrom(from);
        return mail;
    }

    private void sendEmail(Mail mail, String emailApiKey) {
        SendGrid sg = new SendGrid(emailApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            if (response.getStatusCode() == javax.ws.rs.core.Response.Status.ACCEPTED.getStatusCode()) {
                log.info(ANSIColour.doColour("SendGrid Message Sent!", ANSIColour.GREEN));
            } else {
                log.error(ANSIColour.doColour("Error sending to SendGrid!", ANSIColour.RED));
                log.error(ANSIColour.doColour(response.getStatusCode(), ANSIColour.RED));
                log.error(ANSIColour.doColour(response.getBody(), ANSIColour.RED));
                log.error(ANSIColour.doColour(response.getHeaders(), ANSIColour.RED));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
