package life.genny.messages.process;

import life.genny.messages.live.factory.QMessageFactory;
import life.genny.messages.managers.QMessageProvider;
import life.genny.messages.util.MsgUtils;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.message.QBaseMSGMessageType;
import life.genny.qwandaq.message.QMessageGennyMSG;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.AttributeUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.EntityAttributeUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.qwandaq.utils.MergeUtils;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static life.genny.messages.util.FailureHandler.optional;

@ApplicationScoped
public class MessageProcessor {

    private static final Logger log = Logger.getLogger(MessageProcessor.class);

    private static final ManagedExecutor executor = ManagedExecutor.builder().build();

    @Inject
    KeycloakUtils keycloakUtils;

    @Inject
    BaseEntityUtils beUtils;

    @Inject
    EntityAttributeUtils beaUtils;

    @Inject
    QMessageFactory messageFactory;

    @Inject
    UserToken userToken;

    @Inject
    ServiceToken serviceToken;

    @Inject
    CacheManager cm;

    @Inject
    MergeUtils mergeUtils;

    @Inject
    AttributeUtils attributeUtils;

    public void processGenericMessage(QMessageGennyMSG message) {
        executor.supplyAsync(() -> {
            boolean success = false;
            try {
                processMessageHelper(message);
                success = true;
            } catch (Exception e) {
                success = false;
            }
            return success;
        });
    }

    /**
     * Generic Message Handling method.
     *
     * @param message
     * @return status of sending message
     */
    private void processMessageHelper(QMessageGennyMSG message) {
        if (message == null) {
            log.error(ANSIColour.doColour("GENNY COM MESSAGE IS NULL", ANSIColour.RED));
            return;
        }
        // Begin recording duration
        long start = System.currentTimeMillis();

        String realm = userToken.getProductCode();
        String messageCode = message.getTemplateCode();
        log.info("messageCode " + messageCode);
        log.info("msgTypes: " + Arrays.toString(message.getMessageTypeArr()));

        List<QBaseMSGMessageType> messageTypeList = Arrays.asList(message.getMessageTypeArr());

        String cc = optional(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_CC").getValueString());
        String bcc = optional(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_BCC").getValueString());

        if (cc != null) {
            log.info(ANSIColour.doColour("CC found", ANSIColour.GREEN));
            cc = CommonUtils.cleanUpAttributeValue(cc);
            message.getMessageContextMap().put("CC", cc);
        }
        if (bcc != null) {
            log.info(ANSIColour.doColour("BCC found", ANSIColour.GREEN));
            bcc = CommonUtils.cleanUpAttributeValue(bcc);
            message.getMessageContextMap().put("BCC", bcc);
        }

        // Create context map with BaseEntities
        Map<String, Object> baseEntityContextMap;
        baseEntityContextMap = createBaseEntityContextMap(message);
        baseEntityContextMap.put("PROJECT", "PRJ_" + realm.toUpperCase());
        baseEntityContextMap.put("MESSAGE", messageCode);

        String contextAssociations = optional(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_CONTEXT_ASSOCIATIONS").getValueString());

        if (contextAssociations != null) {
            mergeUtils.addAssociatedContexts(beUtils, baseEntityContextMap, contextAssociations, false);
        }

        String[] recipientArr = message.getRecipientArr();
        for (String recipient : recipientArr) {

            // Set our current recipient
            baseEntityContextMap.put("RECIPIENT", recipient);

            // Process any URL contexts for this recipient
            if (baseEntityContextMap.containsKey("URL:ENCODE")) {
                // Fetch form contexts
                String[] componentArray = ((String) baseEntityContextMap.get("URL:ENCODE")).split("/");
                // Init and grab url structure
                String parentCode = null;
                String code = null;
                String targetCode = null;
                if (componentArray.length > 0) {
                    parentCode = componentArray[0];
                }
                if (componentArray.length > 1) {
                    code = componentArray[1];
                }
                if (componentArray.length > 2) {
                    targetCode = componentArray[2];
                }

                log.info("Fetching Token from " + serviceToken.getKeycloakUrl() + " for user " + recipient + " with realm " + serviceToken.getRealm());

                // Fetch access token
                String accessToken = keycloakUtils.getImpersonatedToken(recipient, serviceToken);

                // Encode URL and put back in the map
                String url = MsgUtils.encodedUrlBuilder(GennySettings.projectUrl() + "/home", parentCode, code, targetCode, accessToken);

                log.info("URL: " + url);
                baseEntityContextMap.put("URL", url);
            }

            sendToProvider(baseEntityContextMap, messageTypeList);

        }

        long duration = System.currentTimeMillis() - start;
        log.info("FINISHED PROCESSING MESSAGE :: time taken = " + duration + "ms");
    }

    private BaseEntity createRecipient(String recipient) {

        BaseEntity recipientBe = null;

        if (recipient.contains("[\"") && recipient.contains("\"]")) {
            // This is a BE Code
            String code = CommonUtils.cleanUpAttributeValue(recipient);
            recipientBe = beUtils.getBaseEntity(code);
        }

        return recipientBe;
    }

    private void sendToProvider(Map<String, Object> baseEntityContextMap, List<QBaseMSGMessageType> messageTypeList) {
        if (messageTypeList == null) {
            log.error("messageTypeList is null");
            return;
        }
        // Iterate our array of send types
        for (QBaseMSGMessageType msgType : messageTypeList) {
            log.info("msgType:  " + msgType.toString());
            QMessageProvider provider = messageFactory.getMessageProvider(msgType);
            provider.sendMessage(baseEntityContextMap);
        }
    }

    private HashMap<String, Object> createBaseEntityContextMap(QMessageGennyMSG message) {

        HashMap<String, Object> baseEntityContextMap = new HashMap<>();

        for (Map.Entry<String, String> entry : message.getMessageContextMap().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            String logStr = "key: " + key + ", value: " + (key.equalsIgnoreCase("PASSWORD") ? "REDACTED" : value);
            log.info(logStr);

            if ((value != null) && (value.length() > 4)) {

                // MUST CONTAIN A BE CODE
                if (value.matches("[A-Z]{3}\\_.*") && !key.startsWith("URL")) {
                    // Create Array of Codes
                    String[] codeArr = CommonUtils.cleanUpAttributeValue(value).split(",");
                    log.info("Fetching contextCodeArray :: " + Arrays.toString(codeArr));
                    // Convert to BEs
                    BaseEntity[] beArray = Arrays.stream(codeArr).map(itemCode -> beUtils.getBaseEntity(itemCode, true)).toArray(BaseEntity[]::new);

                    if (beArray.length == 1) {
                        baseEntityContextMap.put(entry.getKey().toUpperCase(), beArray[0]);
                    } else {
                        baseEntityContextMap.put(entry.getKey().toUpperCase(), beArray);
                    }

                    continue;

                }
            }

            // By Default, add it as is
            baseEntityContextMap.put(entry.getKey().toUpperCase(), value);
        }

        return baseEntityContextMap;
    }

}
