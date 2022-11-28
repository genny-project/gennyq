package life.genny.messages.process;

import javax.inject.Inject;
import java.util.*; // TODO: We should avoid wildcard imports if we can (mainly due to unforeseen class conflicts)
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import org.eclipse.microprofile.context.ManagedExecutor;

import life.genny.qwandaq.message.QBaseMSGMessageType;
import life.genny.qwandaq.message.QMessageGennyMSG;

import life.genny.messages.managers.QMessageFactory;
import life.genny.messages.managers.QMessageProvider;
import life.genny.messages.util.MsgUtils;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;

import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.models.ServiceToken;

// Utils imports
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.qwandaq.utils.MergeUtils;
import life.genny.qwandaq.utils.QwandaUtils;

@ApplicationScoped
public class MessageProcessor {

    private static final Logger log = Logger.getLogger(MessageProcessor.class);

	private static final ManagedExecutor executor = ManagedExecutor.builder().build();

	@Inject
	BaseEntityUtils beUtils;

    @Inject
    QMessageFactory messageFactory;

    @Inject
    UserToken userToken;

    @Inject
    ServiceToken serviceToken;

	@Inject
	QwandaUtils qwandaUtils;

    public void processGenericMessage(QMessageGennyMSG message) {
        executor.supplyAsync(() -> {
            Object result = "";
            try {
                result = processMessageHelper(message);
            } catch(Exception e) {
                result = CommonUtils.logAndReturn(log::error, "Issue with thread");
                e.printStackTrace();
            }

            return result;
        });
    }

    /**
     * Generic Message Handling method.
     *
     * @param message
     * 
     * @return status of sending message
     */
    private Object processMessageHelper(QMessageGennyMSG message) {
        // UserToken userToken = new UserToken(message.getToken());
        // Begin recording duration
        long start = System.currentTimeMillis();

        String realm = userToken.getProductCode();

        if (message == null) {
            log.error(ANSIColour.RED + "GENNY COM MESSAGE IS NULL" + ANSIColour.RESET);
        }

        log.debug("Realm is " + realm + " - Incoming Message :: " + message.toString());

        BaseEntity projectBe = beUtils.getBaseEntityByCode("PRJ_" + realm.toUpperCase());

        // try {
        //     log.warn("*** HORRIBLE ACC HACK TO DELAY FOR 10 SEC TO ALLOW CACHE ITEM TO BE COMPLETE");
        //     Thread.sleep(10000);
        // } catch (InterruptedException e1) {
        //     e1.printStackTrace();
        // } /* TODO: horrible hack by ACC to give the be time to save - should use Shleemy , hopefully updated cache will help */


        List<QBaseMSGMessageType> messageTypeList = Arrays.asList(message.getMessageTypeArr());

        BaseEntity templateBe = null;
        if (message.getTemplateCode() != null) {
            templateBe = beUtils.getBaseEntityByCode(message.getTemplateCode());
        }

        if (templateBe != null) {
            String cc = templateBe.getValue("PRI_CC", null);
            String bcc = templateBe.getValue("PRI_BCC", null);

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
        }

        // Create context map with BaseEntities
        Map<String, Object> baseEntityContextMap = new HashMap<>();
        baseEntityContextMap = createBaseEntityContextMap(message);
        baseEntityContextMap.put("PROJECT", projectBe);

        if (templateBe == null) {
            log.warn(ANSIColour.YELLOW + "No Template found for " + message.getTemplateCode() + ANSIColour.RESET);
        } else {
            log.info("Using TemplateBE " + templateBe.getCode());

            // Handle any default context associations
            String contextAssociations = templateBe.getValue("PRI_CONTEXT_ASSOCIATIONS", null);
            if (contextAssociations != null) {
                MergeUtils.addAssociatedContexts(beUtils, baseEntityContextMap, contextAssociations, false);
            }

            // Check for Default Message
            if (Arrays.stream(message.getMessageTypeArr()).anyMatch(item -> item == QBaseMSGMessageType.DEFAULT)) {
                // Use default if told to do so
                List<String> typeList = beUtils.getBaseEntityCodeArrayFromLinkAttribute(templateBe, "PRI_DEFAULT_MSG_TYPE");
                try {
					messageTypeList = typeList.stream().map(item -> QBaseMSGMessageType.valueOf(item)).collect(Collectors.toList());
				} catch (Exception e) {
					log.error(e.getLocalizedMessage());
                    return CommonUtils.logAndReturn(log::error, e);
				}
            }
        }

		String[] recipientArr = message.getRecipientArr();
        List<BaseEntity> recipientBeList = getRecipientBeList(recipientArr);
        // TODO: Why not merge these two loops into one? That would save on processing time as they are essentially the same and the results
        // of the recipientBeList are all independent

        /* Iterating and triggering email to each recipient individually */
        for (BaseEntity recipientBe : recipientBeList) {

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
				String accessToken = KeycloakUtils.getImpersonatedToken(recipientBe, serviceToken, projectBe);

                // Encode URL and put back in the map
                String url = MsgUtils.encodedUrlBuilder(GennySettings.projectUrl() + "/home", parentCode, code, targetCode, accessToken);
                log.info("Access URL: " + url);
                baseEntityContextMap.put("URL", url);
            }

            sendToProvider(message, baseEntityContextMap, templateBe, recipientBe, messageTypeList);
        }

        long duration = System.currentTimeMillis() - start;
        return CommonUtils.logAndReturn(log::info, "FINISHED PROCESSING MESSAGE :: time taken = " + String.valueOf(duration) + "ms");
    }

    private List<BaseEntity> getRecipientBeList(String[] recipientArr) {
        Attribute emailAttr = qwandaUtils.getAttribute("PRI_EMAIL");
        Attribute mobileAttr = qwandaUtils.getAttribute("PRI_MOBILE");
        List<BaseEntity> recipientBeList = new ArrayList<>();

        for (String recipient : recipientArr) {

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

            if (recipientBe != null) {
                recipientBeList.add(recipientBe);
            } else {
                log.error(ANSIColour.RED + "Could not process recipient " + recipient + ANSIColour.RESET);
            }
        }

        return recipientBeList;
    }

    // TODO: Make this nicer
    // Ideally we have all our code broken out into different functions in this class, and this is the beginning of that
    // This class should make more use of attributes
    private void sendToProvider(QMessageGennyMSG message, Map<String, Object> baseEntityContextMap, 
                    BaseEntity templateBe, BaseEntity recipientBe, List<QBaseMSGMessageType> messageTypeList) {
        final String templateCode = message.getTemplateCode() + "_UNSUBSCRIBE";
        final BaseEntity unsubscriptionBe = beUtils.getBaseEntityByCode("COM_EMAIL_UNSUBSCRIPTION");
        if(unsubscriptionBe == null) {
            log.warn("Unsubscription Base Entity is null! All users will be treated at subscribed");
        }

        log.debug("unsubscribe be :: " + unsubscriptionBe);

        // Iterate our array of send types
        for (QBaseMSGMessageType msgType : messageTypeList) {
            /* Get Message Provider */
            final QMessageProvider provider = messageFactory.getMessageProvider(msgType);
            Boolean isUserUnsubscribed = false;

            if (unsubscriptionBe != null) {
                /* check if unsubscription list for the template code has the userCode */
                String templateAssociation = unsubscriptionBe.getValue(templateCode, "");
                isUserUnsubscribed = templateAssociation.contains(recipientBe.getCode());
            }

            /*
                * if user is unsubscribed, then dont send emails. But toast and sms are still
                * applicable
                */

            if (isUserUnsubscribed && !QBaseMSGMessageType.EMAIL.equals(msgType)) {
                log.info("unsubscribed");
                provider.sendMessage(templateBe, baseEntityContextMap);
            }

            /* if subscribed, allow messages */
            if (!isUserUnsubscribed) {
                log.info("subscribed");
                provider.sendMessage(templateBe, baseEntityContextMap);
            }
        }
    }

    private HashMap<String, Object> createBaseEntityContextMap(QMessageGennyMSG message) {

        HashMap<String, Object> baseEntityContextMap = new HashMap<>();

        for (Map.Entry<String, String> entry : message.getMessageContextMap().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            String logStr = "key: " + key + ", value: " + (key.toUpperCase().equals("PASSWORD") ? "REDACTED" : value);
            log.info(logStr);

            if ((value != null) && (value.length() > 4)) {

                // MUST CONTAIN A BE CODE
                if (value.matches("[A-Z]{3}\\_.*") && !key.startsWith("URL")) {
                    // Create Array of Codes
                    String[] codeArr = CommonUtils.cleanUpAttributeValue(value).split(",");
                    log.info("Fetching contextCodeArray :: " + Arrays.toString(codeArr));
                    // Convert to BEs
                    BaseEntity[] beArray = Arrays.stream(codeArr)
                            .map(itemCode -> (BaseEntity) beUtils.getBaseEntityByCode(itemCode))
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
