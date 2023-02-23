package life.genny.messages.managers;

import life.genny.messages.managers.SMTP.SendGrid.SendEmailWithSendGridAPI;
import life.genny.messages.util.MsgUtils;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.utils.EntityAttributeUtils;
import life.genny.qwandaq.utils.TimeUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public final class QEmailMessageManager extends QMessageProvider {

	@Inject
	EntityAttributeUtils beaUtils;

	private static final Logger log = Logger.getLogger(QEmailMessageManager.class);

    @Override
    public void sendMessage(BaseEntity templateBe, Map<String, Object> contextMap) {
        try {
            BaseEntity recipientBe = (BaseEntity) contextMap.get("RECIPIENT");
            BaseEntity projectBe = (BaseEntity) contextMap.get("PROJECT");

            recipientBe = beUtils.getBaseEntity(recipientBe.getCode());

            if (templateBe == null) {
                throw new NullParameterException("templateBe");
            }

            if (recipientBe == null) {
                log.error(ANSIColour.RED + "Target is NULL" + ANSIColour.RESET);
                throw new NullParameterException("recipientBe");
            }

            String timezone = recipientBe.getValue("PRI_TIMEZONE_ID", "UTC");

            log.info("Timezone returned from recipient BE " + recipientBe.getCode() + " is:: " + timezone);

            // test data
            log.info("Showing what is in recipient BE, code=" + recipientBe.getCode());
            for (EntityAttribute ea : recipientBe.getBaseEntityAttributes()) {
                log.info("attributeCode=" + ea.getAttributeCode() + ", value=" + ea.getObjectAsString());
            }

            String recipient = recipientBe.getValue("PRI_EMAIL", null);

		// test data
		log.debug("Showing what is in recipient BE, code=" + recipientBe.getCode());
		for (EntityAttribute ea : beaUtils.getAllEntityAttributesForBaseEntity(recipientBe)) {
			log.debug("attributeCode=" + ea.getAttributeCode() + ", value=" + ea.getObjectAsString());
		}
            if (recipient != null) {
                recipient = recipient.trim();
            }
            if (timezone == null || timezone.replaceAll(" ", "").isEmpty()) {
                timezone = "UTC";
            }
            log.info("Recipient BeCode: " + recipientBe.getCode() + " Recipient Email: " + recipient + ", Timezone: " + timezone);
            if (recipient == null) {
                log.error(ANSIColour.RED + "Target " + recipientBe.getCode() + ", PRI_EMAIL is NULL" + ANSIColour.RESET);
                throw new NullParameterException("recipient");
            }

            String subject = templateBe.getValue("PRI_SUBJECT", null);
            String body = templateBe.getValue("PRI_BODY", null);

            String emailSender = projectBe.getValueAsString("ENV_SENDGRID_EMAIL_SENDER");
            String emailNameSender = projectBe.getValueAsString("ENV_SENDGRID_EMAIL_NAME_SENDER");
            String emailApiKey = projectBe.getValueAsString("ENV_SENDGRID_API_KEY");
            String apiPath = projectBe.getValueAsString("ENV_SENDGRID_API_PATH");

		String subject = templateBe.getValue("PRI_SUBJECT", null);
		String body = templateBe.getValue("PRI_BODY", null);
		String productCode = projectBe.getRealm();
		String projectBeCode = projectBe.getCode();
		String emailSender = beaUtils.getEntityAttribute(productCode, projectBeCode, "ENV_SENDGRID_EMAIL_SENDER", false).getValueString();
		String emailNameSender = beaUtils.getEntityAttribute(productCode, projectBeCode, "ENV_SENDGRID_EMAIL_NAME_SENDER", false).getValueString();
		String emailApiKey = beaUtils.getEntityAttribute(productCode, projectBeCode, "ENV_SENDGRID_API_KEY", false).getValueString();
		String apiPath = beaUtils.getEntityAttribute(productCode, projectBeCode, "ENV_SENDGRID_API_PATH", false).getValueString();
            log.info("The name for email sender -> " + emailSender);
            log.info("The apiPath for API -> " + apiPath);
            log.info("The emailNameSender -> " + emailNameSender);
            log.info("The emailApiKey -> " + emailApiKey);

            // Build a general data map from context BEs
            HashMap<String, Object> templateData = new HashMap<>();

            for (Map.Entry<String, Object> entry : contextMap.entrySet()) {

                Object value = entry.getValue(); //contextMap.get(entry.getKey());

                if (value.getClass().equals(BaseEntity.class)) {
                    log.info("Processing key as BASEENTITY: " + entry.getKey());
                    BaseEntity be = (BaseEntity) value;
                    HashMap<String, String> deepReplacementMap = new HashMap<>();
                    for (EntityAttribute ea : be.getBaseEntityAttributes()) {

                        String attrCode = ea.getAttributeCode();
                        if (attrCode.startsWith("LNK") || attrCode.startsWith("PRI")) {
                            Object attrVal = ea.getValue();
                            if (attrVal != null) {

			if (value.getClass().equals(BaseEntity.class)) {
				log.info("Processing key as BASEENTITY: " + entry.getKey());
				BaseEntity be = (BaseEntity) value;
				HashMap<String, String> deepReplacementMap = new HashMap<>();
				for (EntityAttribute ea : beaUtils.getAllEntityAttributesForBaseEntity(be)) {
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
            // Base Wrapper
            JsonObjectBuilder mailJsonObjectBuilder = Json.createObjectBuilder();

            JsonObject fromJsonObject = Json
                    .createObjectBuilder()
                    .add("name", emailNameSender)
                    .add("email", emailSender)
                    .build();

            System.out.println("fromJsonObject: " + fromJsonObject);

            JsonObject toJsonObject = Json
                    .createObjectBuilder()
                    .add("email", recipient)
                    .build();

            System.out.println("toJsonObject" + toJsonObject);

            JsonArray tosJsonArray = Json.createArrayBuilder()
                    .add(toJsonObject)
                    .build();

			System.out.println("tosJsonArray" + tosJsonArray);

            JsonArrayBuilder personalizationArrayBuilder = Json.createArrayBuilder();
            JsonObjectBuilder personalizationInnerObjectWrapper = Json.createObjectBuilder();
            personalizationInnerObjectWrapper.add("to", tosJsonArray);
            personalizationInnerObjectWrapper.add("subject", subject);

            // Handle CC and BCC
            Object ccVal = contextMap.get("CC");
            Object bccVal = contextMap.get("BCC");

            if (ccVal != null) {
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

                    if (email != null && !email.equals(recipient)) {
                        ccJsonArrayBuilder.add(
                                Json
                                        .createObjectBuilder()
                                        .add("email", email)
                                        .build()
                        );
                        log.info(ANSIColour.BLUE + "Found CC Email: " + email + ANSIColour.RESET);
                    }
                }
                personalizationInnerObjectWrapper.add("cc", ccJsonArrayBuilder.build());
            }

            if (bccVal != null) {
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

                    if (email != null && !email.equals(recipient)) {
                        bccJsonArrayBuilder.add(
                                Json
                                        .createObjectBuilder()
                                        .add("email", email)
                                        .build()
                        );
                        log.info(ANSIColour.BLUE + "Found BCC Email: " + email + ANSIColour.RESET);
                    }
                }
                personalizationInnerObjectWrapper.add("bcc", bccJsonArrayBuilder.build());
            }

            Map<String, Object> finalData = new HashMap<>(templateData);
            personalizationArrayBuilder.add(personalizationInnerObjectWrapper.build());

            mailJsonObjectBuilder.add("personalizations", personalizationArrayBuilder.build());

            if (subject != null) {
                mailJsonObjectBuilder.add("subject", subject);
            } else {
                log.warn("Subject is null -> " + subject);
            }

            mailJsonObjectBuilder.add("from", fromJsonObject);

            JsonArrayBuilder contentArray = Json.createArrayBuilder();
            JsonObjectBuilder contentJson = Json.createObjectBuilder();

            body = StringEscapeUtils.unescapeHtml4(body);
//		System.out.println("body unescaped: " + body);
            body = MsgUtils.parseToTemplate(body, finalData);

            contentJson.add("type", "text/html");
            contentJson.add("value", body);
            contentArray.add(contentJson.build());
            System.out.println("contentJson.build():"+ contentJson.build());
            System.out.println("contentArray.build(): "+contentArray.build());
            mailJsonObjectBuilder.add("content", contentArray.build());

//		sendRequest(mailJsonObjectBuilder.build(), emailApiKey);

            log.info("ENV_SENDGRID_API_PATH: " + apiPath);
            System.out.println("mailJsonObjectBuilder.build(): "+mailJsonObjectBuilder.build());
            SendEmailWithSendGridAPI sendEmailWithSendGridAPI = new SendEmailWithSendGridAPI(mailJsonObjectBuilder.build(), emailApiKey, apiPath);
            sendEmailWithSendGridAPI.sendRequest();
        } catch (Exception e) {
            log.error("Email sending failed! Error -> " + e);
        }
    }

}
