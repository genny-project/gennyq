package life.genny.messages.process;

import life.genny.messages.managers.QMessageFactory;
import life.genny.messages.managers.QMessageProvider;
import life.genny.messages.util.MsgUtils;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QBaseMSGMessageType;
import life.genny.qwandaq.message.QMessageGennyMSG;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.*;
import life.genny.qwandaq.managers.CacheManager;
import org.eclipse.microprofile.context.ManagedExecutor;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;

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
                success =  true;
            } catch(Exception e) {
                success = false;
            }
            return success;
        });
    }

    /**
     * Generic Message Handling method.
     *
     * @param message
     * 
     * @return status of sending message
     */
    private void processMessageHelper(QMessageGennyMSG message) {
        // Begin recording duration
        long start = System.currentTimeMillis();

        String realm = userToken.getProductCode();

        if (message == null) {
            log.error(ANSIColour.doColour("GENNY COM MESSAGE IS NULL", ANSIColour.RED));
            return;
        }

        log.debug("Realm is " + realm + " - Incoming Message :: " + message.toString());

        BaseEntity projectBe = beUtils.getBaseEntity("PRJ_" + realm.toUpperCase());

        List<QBaseMSGMessageType> messageTypeList = Arrays.asList(message.getMessageTypeArr());

        BaseEntity templateBe = null;
        if (message.getTemplateCode() != null) {
            templateBe = beUtils.getBaseEntity(message.getTemplateCode(),true);
            if(templateBe == null){
                return;
            }
        }else{
            return;
        }

        String templateBeCode = templateBe.getCode();
        String cc = null;
        String bcc = null;
        try{
            EntityAttribute ccAttribute = beaUtils.getEntityAttribute(realm, templateBeCode, "PRI_CC");
            cc = ccAttribute != null ? ccAttribute.getValueString() : null;
            EntityAttribute bccAttribute = beaUtils.getEntityAttribute(realm, templateBeCode, "PRI_BCC");
            bcc = bccAttribute != null ? bccAttribute.getValueString() : null;
        }catch(ItemNotFoundException ex){
            log.error("Error fetching PRI_CC or PRI_BC");
            log.error("Exception: "+ ex.getMessage());
        }

        if (cc != null) {
            log.debug("Using CC from template BaseEntity");
            cc = CommonUtils.cleanUpAttributeValue(cc);
            message.getMessageContextMap().put("CC", cc); 
        }
        if (bcc != null) {
            log.debug("Using BCC from template BaseEntity");
            bcc = CommonUtils.cleanUpAttributeValue(bcc);
            message.getMessageContextMap().put("BCC", bcc);
        }

        // Create context map with BaseEntities
        Map<String, Object> baseEntityContextMap;
        baseEntityContextMap = createBaseEntityContextMap(message);
        baseEntityContextMap.put("PROJECT", projectBe);

        log.info("Using TemplateBE " + templateBe.getCode());
        EntityAttribute contextAssociationsAttribute = null;
        try{
            // Handle any default context associations
            contextAssociationsAttribute = beaUtils.getEntityAttribute(templateBe.getRealm(), templateBe.getCode(), "PRI_CONTEXT_ASSOCIATIONS");
        }catch(ItemNotFoundException ex){
            log.error("Error fetching PRI_CONTEXT_ASSOCIATIONS");
            log.error("Exception: "+ ex.getMessage());
        }

        String contextAssociations = contextAssociationsAttribute != null ? contextAssociationsAttribute.getValueString() : null;
        if (contextAssociations != null) {
            mergeUtils.addAssociatedContexts(beUtils, baseEntityContextMap, contextAssociations, false);
        }

        log.info("msgType: "+ Arrays.toString(message.getMessageTypeArr()));
        
        // Check for default msg
        if (Arrays.stream(message.getMessageTypeArr()).anyMatch(item -> item == QBaseMSGMessageType.DEFAULT)) {
            log.debug("Selecting default message type");

            // Use default if told to do so
            List<String> typeList = null;
            try{
                typeList = beUtils.getBaseEntityCodeArrayFromLinkAttribute(templateBe, "PRI_DEFAULT_MSG_TYPE");
                log.debug("typeList: "+ typeList);
            }catch(ItemNotFoundException ex){
                log.error("Error fetching PRI_DEFAULT_MSG_TYPE");
                log.error("Exception: "+ ex.getMessage());
                return;
            }

            if(typeList != null){
                try {
                    messageTypeList = typeList.stream().map(QBaseMSGMessageType::valueOf).toList();
                } catch (IllegalArgumentException e) {
                    log.error("Cannot be converted to QBaseMSGMessageType enum");
                    log.error("Exception: "+ e.getMessage());
                    return;
                }
            }
        }

		String[] recipientArr = message.getRecipientArr();
        for (String recipient : recipientArr) {
            BaseEntity recipientBe = createRecipient(recipient);
            
            if (recipientBe == null) {
                log.error(ANSIColour.doColour("Could not process recipient " + recipient, ANSIColour.RED));
                return;
            }

            // Set our current recipient
            baseEntityContextMap.put("RECIPIENT", recipientBe);

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

				log.info("Fetching Token from " + serviceToken.getKeycloakUrl() + " for user "
						+ recipientBe.getCode() + " with realm " + serviceToken.getRealm());

                // Fetch access token
				String accessToken = keycloakUtils.getImpersonatedToken(recipientBe, serviceToken, projectBe);

                // Encode URL and put back in the map
                String url = MsgUtils.encodedUrlBuilder(GennySettings.projectUrl() + "/home", parentCode, code, targetCode, accessToken);
                log.info("Access URL: " + url);
                baseEntityContextMap.put("URL", url);
            }

            sendToProvider(baseEntityContextMap, templateBe, messageTypeList);

        }

        long duration = System.currentTimeMillis() - start;
        log.info("FINISHED PROCESSING MESSAGE :: time taken = " + duration + "ms");
    }

    private BaseEntity createRecipient(String recipient){
        Attribute emailAttr = attributeUtils.getAttribute(Attribute.PRI_EMAIL, true);
        Attribute mobileAttr = attributeUtils.getAttribute(Attribute.PRI_MOBILE, true);
        BaseEntity recipientBe = null;

        if (recipient.contains("[\"") && recipient.contains("\"]")) {
            // This is a BE Code
            String code = CommonUtils.cleanUpAttributeValue(recipient);
            recipientBe = beUtils.getBaseEntity(code);
        } else {
            // Probably an actual email
            String code = "RCP_" + UUID.randomUUID().toString().toUpperCase();
            recipientBe = new BaseEntity(code, recipient);

            try {
                EntityAttribute email = new EntityAttribute(recipientBe, emailAttr, 1.0, recipient);
                recipientBe.addAttribute(email);
                EntityAttribute mobile = new EntityAttribute(recipientBe, mobileAttr, 1.0, recipient);
                recipientBe.addAttribute(mobile);
            } catch (Exception e) {
                log.error("Exception when passing recipient BE: " + recipient);
                e.printStackTrace();
            }
        }

        return recipientBe;
    }

    private void sendToProvider(Map<String, Object> baseEntityContextMap, BaseEntity templateBe, List<QBaseMSGMessageType> messageTypeList) {
        if(messageTypeList == null){
            log.error("messageTypeList is null");
            return;
        }
        // Iterate our array of send types
        for (QBaseMSGMessageType msgType : messageTypeList) {
            log.info("Sending:  "+ msgType);
            QMessageProvider provider = messageFactory.getMessageProvider(msgType);
            provider.sendMessage(templateBe, baseEntityContextMap);
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
                    BaseEntity[] beArray = Arrays.stream(codeArr)
                            .map(itemCode -> beUtils.getBaseEntity(itemCode, true))
                            .toArray(BaseEntity[]::new);

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
