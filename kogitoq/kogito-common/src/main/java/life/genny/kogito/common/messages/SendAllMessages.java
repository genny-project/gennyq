package life.genny.kogito.common.messages;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.SearchUtils;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendAllMessages extends MessageSendingStrategy {

    @Inject
    SearchUtils searchUtils;

    private String productCode;
    private String milestoneCode;
    private BaseEntity coreBE;

    private static final String SBE_MILESTONE_MESSAGES = "SBE_MILESTONE_MESSAGES";
    private static final String NAME = "Fetch All Messages associated with milestone Code";
    private static final String PRI_CODE = "SBE_MILESTONE_MESSAGES";
    private static final String PRI_MILESTONE = "SBE_MILESTONE_MESSAGES";
    private static final String PRI_RECIPIENT_LNK = "PRI_RECIPIENT_LNK";
    private static final String PRI_SENDER_LNK = "PRI_SENDER_LNK";
    public SendAllMessages(String productCode, String milestoneCode, BaseEntity coreBE) {
        this.productCode = productCode;
        this.milestoneCode = milestoneCode;
        this.coreBE = coreBE;
    }

    public SendAllMessages(String milestoneCode, String coreBeCode) {
        this.milestoneCode = milestoneCode;
        this.coreBE = beUtils.getBaseEntity(userToken.getProductCode(), coreBeCode);
        this.productCode = this.coreBE.getRealm();
    }

    @Override
    public void sendMessage() {
        SearchEntity searchEntity = new SearchEntity(SBE_MILESTONE_MESSAGES, NAME)
                .addFilter(PRI_CODE, SearchEntity.StringFilter.LIKE, "MSG_%")
                .addFilter(PRI_MILESTONE, SearchEntity.StringFilter.LIKE, "%\"" + milestoneCode.toUpperCase() + "\"%")
                .setPageStart(0)
                .setPageSize(100);

        searchEntity.setRealm(productCode);

        List<String> messageCodes = searchUtils.searchBaseEntityCodes(searchEntity);

        if (messageCodes != null) {
            log.info("messages : " + messageCodes.size());
            for (String messageCode : messageCodes) {
                log.info("messageCode : " + messageCode);
                BaseEntity message = beUtils.getBaseEntityByCode(messageCode);
                // Construct a contextMap
                Map<String, String> ctxMap = new HashMap<>();
                String recipientBECode;

                // Determine the recipientBECode
                String recipientLnkValue = message.getValueAsString(PRI_RECIPIENT_LNK);
                if(recipientLnkValue != null) {
                    recipientBECode = determineRecipientLnkValue(recipientLnkValue, ctxMap);
                } else {
                    log.error("NO " + PRI_RECIPIENT_LNK + " present");
                    continue;
                }

                // Determine the sender
                String senderLnkValue = message.getValueAsString(PRI_SENDER_LNK);
                if (senderLnkValue != null) {
                    ctxMap = determineSender(senderLnkValue, ctxMap);
                } else {
                    log.error("NO " + PRI_SENDER_LNK + " present");
                    continue;
                }

                // Extract all the contexts from the core baseEntity LNKs
                List<EntityAttribute> lnkEAs = coreBE.findPrefixEntityAttributes("LNK");
                StringBuilder contextMapStr = new StringBuilder();
                for (EntityAttribute ea : lnkEAs) {
                    String aliasCode = ea.getAttributeCode().substring("LNK_".length());
                    String aliasValue = ea.getAsString();
                    aliasValue = aliasValue.replace("\"", "").replace("[", "").replace("]", "");
                    contextMapStr.append(aliasCode).append("=").append(aliasValue).append(",");
                    ctxMap.put(aliasCode, aliasValue);
                }

                log.info("Sending Message " + message.getCode() + " to " + recipientBECode + " with ctx=" + contextMapStr);

                new SendMessage(message.getCode(), recipientBECode, ctxMap).sendMessage();
            }
        } else {
            log.warn("No messages found for milestoneCode -> " + milestoneCode);
        }
    }

    private String determineRecipientLnkValue(String recipientLnkValue, Map<String, String> ctxMap) {
        String recipientBECode = null;

        if (recipientLnkValue != null) {
            // check the various formats to get the recipientBECode
            if (recipientLnkValue.startsWith("SELF")) { // The coreBE is the recipient
                ctxMap.put("RECIPIENT", coreBE.getCode());
                recipientBECode = coreBE.getCode();
            } else if (recipientLnkValue.startsWith("PER_")) {
                ctxMap.put("RECIPIENT", recipientLnkValue);
                recipientBECode = recipientLnkValue;
            } else {
                // check if it is a LNK path (of the coreBE)
                // "LNK_INTERN"
                if (recipientLnkValue.startsWith("LNK_")) {
                    String[] splitStr = recipientLnkValue.split(":");
                    Integer numPathItems = splitStr.length;
                    BaseEntity lnkBe = coreBE; // seed
                    for (int index = 0; index < numPathItems; index++) {
                        lnkBe = beUtils.getBaseEntityFromLinkAttribute(lnkBe, splitStr[index]);
                    }
                    ctxMap.put("RECIPIENT", lnkBe.getCode());
                    recipientBECode = lnkBe.getCode();
                }
            }
        }

        return recipientBECode;
    }

    private Map<String, String> determineSender(String senderLnkValue, Map<String, String> ctxMap) {
        if (senderLnkValue != null) {
            // check the various formats to get the senderBECode
            if (senderLnkValue.startsWith("USER")) { // The user is the sender
                ctxMap.put("SENDER", userToken.getUserCode());
            } else if (senderLnkValue.startsWith("PER_")) {
                ctxMap.put("SENDER", senderLnkValue);
            } else {
                // check if it is a LNK path (of the coreBE)
                // "LNK_INTERN"
                if (senderLnkValue.startsWith("LNK_")) {
                    String[] splitStr = senderLnkValue.split(":");
                    BaseEntity lnkBe = coreBE; // seed
                    for (String s : splitStr) {
                        lnkBe = beUtils.getBaseEntityFromLinkAttribute(lnkBe, s);
                    }
                    ctxMap.put("SENDER", lnkBe.getCode());
                }
            }
        }

        return ctxMap;
    }
}
