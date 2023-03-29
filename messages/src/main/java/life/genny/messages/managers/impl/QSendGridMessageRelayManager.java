package life.genny.messages.managers.impl;

import life.genny.messages.live.qualifer.MessageType;
import life.genny.messages.managers.QMessageProvider;
import life.genny.messages.util.MsgUtils;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.utils.HttpUtils;
import life.genny.qwandaq.utils.TimeUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static life.genny.qwandaq.utils.FailureHandler.optional;
import static life.genny.qwandaq.utils.FailureHandler.required;
import static life.genny.qwandaq.message.QBaseMSGMessageType.SENDGRID_RELAY;

/**
 * Sends email using sendgrid as a relay.
 */
@ApplicationScoped
@MessageType(type = SENDGRID_RELAY)
public final class QSendGridMessageRelayManager extends QMessageProvider {

    @Inject
    Logger log;

    @Override
    public void sendMessage(Map<String, Object> contextMap) {
        log.info(ANSIColour.doColour(">>>>>>>>>>> Triggering Sendgrid relay email <<<<<<<<<<<<<<", ANSIColour.GREEN));

        String recipientCode = (String) required(() -> contextMap.get("RECIPIENT"));
        log.info(ANSIColour.doColour("recipientCode: " + recipientCode, ANSIColour.GREEN));
        String messageCode = (String) required(() -> contextMap.get("MESSAGE"));
        log.info(ANSIColour.doColour("messageCode: " + messageCode, ANSIColour.GREEN));
        String projectCode = (String) required(() -> contextMap.get("PROJECT"));
        log.info(ANSIColour.doColour("projectCode: " + projectCode, ANSIColour.GREEN));
        String realm = userToken.getRealm();
        log.info(ANSIColour.doColour("realm: " + realm, ANSIColour.GREEN));

        String recipientEmail = required(() -> beaUtils.getEntityAttribute(realm, recipientCode, "PRI_EMAIL").getValueString()).trim();
        log.info(ANSIColour.doColour("recipientEmail: " + recipientEmail, ANSIColour.GREEN));
        String subject = required(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_SUBJECT").getValueString());
        log.info(ANSIColour.doColour("subject: " + subject, ANSIColour.GREEN));
        String body = required(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_BODY").getValueString());
        log.info(ANSIColour.doColour("body: " + body, ANSIColour.GREEN));
        String emailSender = required(() -> beaUtils.getEntityAttribute(realm, projectCode, "ENV_SENDGRID_EMAIL_SENDER").getValueString());
        log.info(ANSIColour.doColour("emailSender: " + emailSender, ANSIColour.GREEN));
        String emailNameSender = required(() -> beaUtils.getEntityAttribute(realm, projectCode, "ENV_SENDGRID_EMAIL_NAME_SENDER").getValueString());
        log.info(ANSIColour.doColour("emailNameSender: " + emailNameSender, ANSIColour.GREEN));
        String emailApiKey = required(() -> beaUtils.getEntityAttribute(realm, projectCode, "ENV_SENDGRID_API_KEY").getValueString());
        log.info(ANSIColour.doColour("emailApiKey: " + emailApiKey, ANSIColour.GREEN));
        String apiPath = required(() -> beaUtils.getEntityAttribute(realm, projectCode, "ENV_SENDGRID_API_PATH").getValueString());
        log.info(ANSIColour.doColour("apiPath: " + apiPath, ANSIColour.GREEN));

        HashMap<String, Object> templateData = formatValue(contextMap, recipientCode);

        // Base Wrapper
        JsonObjectBuilder mailJsonObjectBuilder = Json.createObjectBuilder();

        JsonObject fromJsonObject = Json.createObjectBuilder().add("name", emailNameSender).add("email", emailSender).build();

        JsonObject toJsonObject = Json.createObjectBuilder().add("email", recipientEmail).build();

        JsonArray tosJsonArray = Json.createArrayBuilder().add(toJsonObject).build();

        JsonArrayBuilder personalizationArrayBuilder = Json.createArrayBuilder();
        JsonObjectBuilder personalizationInnerObjectWrapper = Json.createObjectBuilder();

        personalizationInnerObjectWrapper.add("to", tosJsonArray);
        personalizationInnerObjectWrapper.add("subject", subject);

        // Handle CC and BCC
        Object ccVal = contextMap.get("CC");
        Object bccVal = contextMap.get("BCC");

        if (ccVal != null) {
            buildCc(recipientEmail, personalizationInnerObjectWrapper, ccVal);
        }

        if (bccVal != null) {
            buildBcc(recipientEmail, personalizationInnerObjectWrapper, bccVal);
        }

        Map<String, Object> finalData = new HashMap<>(templateData);
        personalizationArrayBuilder.add(personalizationInnerObjectWrapper.build());

        mailJsonObjectBuilder.add("subject", subject);
        mailJsonObjectBuilder.add("from", fromJsonObject);

        mailJsonObjectBuilder.add("personalizations", personalizationArrayBuilder.build());

        JsonArrayBuilder contentArray = Json.createArrayBuilder();
        JsonObjectBuilder contentJson = Json.createObjectBuilder();

        body = StringEscapeUtils.unescapeHtml4(body);
        body = MsgUtils.parseToTemplate(body, finalData);

        contentJson.add("type", "text/html");
        contentJson.add("value", body);
        contentArray.add(contentJson.build());
        mailJsonObjectBuilder.add("content", contentArray.build());

        HttpResponse<String> httpResponse = HttpUtils.post(apiPath, mailJsonObjectBuilder.build().toString(), emailApiKey);
        log.info("####### response: " + httpResponse.body());
        log.info("####### statusCode: " + httpResponse.statusCode());
    }

    private HashMap<String, Object> formatValue(Map<String, Object> contextMap, String recipientCode) {
        // Build a general data map from context BEs
        HashMap<String, Object> templateData = new HashMap<>();

        for (Map.Entry<String, Object> entry : contextMap.entrySet()) {
            String value = (String) entry.getValue();
            log.debug("Key: " + entry.getKey() + " value: " + value);
            boolean isBaseEntity = Pattern.matches("^[A-Z]{0,3}_.*", value);
            log.info("Key: "+ entry.getKey()+ " is BASEENTITY: "+ isBaseEntity);
            if (isBaseEntity) {
                BaseEntity be = beUtils.getBaseEntity(value);
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
                                    valueString = TimeUtils.formatDateTime((LocalDateTime) attrVal, format);
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
                                    LocalDateTime convertedToLDT = converted.toLocalDateTime();

                                    valueString = TimeUtils.formatDateTime(convertedToLDT, format);
                                    log.info("date format");
                                    log.info("formatted date: " + valueString);

                                } else {
                                    log.info("No DATETIMEFORMAT key present in context map, defaulting to stringified dateTime");
                                }
                            }
                            deepReplacementMap.put(attrCode, valueString);
                        }
                    }
                }
                templateData.put(entry.getKey(), deepReplacementMap);
            } else {
                templateData.put(entry.getKey(), value);
            }
        }
        return templateData;
    }

    private void buildBcc(String recipientEmail, JsonObjectBuilder personalizationInnerObjectWrapper, Object bccVal) {
        BaseEntity[] bccArray = new BaseEntity[1];

        JsonArrayBuilder bccJsonArrayBuilder = Json.createArrayBuilder();

        if (bccVal.getClass().equals(BaseEntity.class)) {
            bccArray[0] = (BaseEntity) bccVal;
        } else {
            bccArray = (BaseEntity[]) bccVal;
        }
        for (BaseEntity item : bccArray) {

            String email = item.getValue("PRI_EMAIL", null);
            if (email != null) {
                email = email.trim();
            }

            if (email != null && !email.equals(recipientEmail)) {
                bccJsonArrayBuilder.add(Json.createObjectBuilder().add("email", email).build());
                log.info(ANSIColour.doColour("Found BCC Email: " + email, ANSIColour.BLUE));
            }
        }
        personalizationInnerObjectWrapper.add("bcc", bccJsonArrayBuilder.build());
    }

    private void buildCc(String recipientEmail, JsonObjectBuilder personalizationInnerObjectWrapper, Object ccVal) {
        BaseEntity[] ccArray = new BaseEntity[1];

        if (ccVal.getClass().equals(BaseEntity.class)) {
            ccArray[0] = (BaseEntity) ccVal;
        } else {
            ccArray = (BaseEntity[]) ccVal;
        }

        JsonArrayBuilder ccJsonArrayBuilder = Json.createArrayBuilder();

        for (BaseEntity item : ccArray) {

            String email = item.getValue("PRI_EMAIL", null);
            if (email != null) {
                email = email.trim();
            }

            if (email != null && !email.equals(recipientEmail)) {
                ccJsonArrayBuilder.add(Json.createObjectBuilder().add("email", email).build());
                log.info(ANSIColour.doColour("Found CC Email: " + email, ANSIColour.BLUE));
            }
        }
        personalizationInnerObjectWrapper.add("cc", ccJsonArrayBuilder.build());
    }

}
