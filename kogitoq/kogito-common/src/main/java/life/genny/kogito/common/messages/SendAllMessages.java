package life.genny.kogito.common.messages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.Filter;
import life.genny.qwandaq.utils.SearchUtils;

public class SendAllMessages extends MessageSendingStrategy {

    @Inject
    SearchUtils searchUtils;

    private final String productCode;
    private final String milestoneCode;
    private final BaseEntity coreBE;

    private final Map<String, String> ctxMap = new HashMap<>();
    private String recipientBECode = null;

    public static final String SBE_MILESTONE_MESSAGES = "SBE_MILESTONE_MESSAGES";
    public static final String NAME = "Fetch All Messages associated with milestone Code";
    public static final String PRI_MILESTONE = "PRI_MILESTONE";
    public static final String PRI_RECIPIENT_LNK = "PRI_RECIPIENT_LNK";
    public static final String PRI_SENDER_LNK = "PRI_SENDER_LNK";
    public static final String RECIPIENT = "RECIPIENT";
    public static final String SENDER = "SENDER";
    public static final String SELF = "SELF";
    public static final String USER = "USER";
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
                .addFilter(Attribute.PRI_CODE, Filter.LIKE, "MSG_%")
                .addFilter(PRI_MILESTONE, Filter.LIKE, "%\"" + milestoneCode.toUpperCase() + "\"%")
                .setPageStart(0)
                .setPageSize(100);

        searchEntity.setRealm(productCode);

        List<String> messageCodes = searchUtils.searchBaseEntityCodes(searchEntity);

        if (messageCodes != null) {
            log.info("messages : " + messageCodes.size());
            messageCodes.parallelStream().forEach((messageCode) -> {
                log.info("messageCode : " + messageCode);
                BaseEntity message = beUtils.getBaseEntityByCode(messageCode);

                // Determine the recipientBECode
                String recipientLnkValue = message.getValueAsString(PRI_RECIPIENT_LNK);
                if(recipientLnkValue != null) {
                    determineRecipientLnkValueAndUpdateMap(recipientLnkValue);
                } else {
                    log.error("NO " + PRI_RECIPIENT_LNK + " present");
                }

                // Extract all the contexts from the core baseEntity LNKs
                List<EntityAttribute> lnkEAs = coreBE.findPrefixEntityAttributes("LNK");
                StringBuilder contextMapStr = new StringBuilder();

                lnkEAs.parallelStream().forEach((ea) -> {
                    String aliasCode = ea.getAttributeCode().substring("LNK_".length());
                    String aliasValue = ea.getAsString();
                    aliasValue = aliasValue.replace("\"", "").replace("[", "").replace("]", "");
                    contextMapStr.append(aliasCode).append("=").append(aliasValue).append(",");
                    ctxMap.put(aliasCode, aliasValue);
                });

                // Determine the sender
                String senderLnkValue = message.getValueAsString(PRI_SENDER_LNK);
                if (senderLnkValue != null) {
                    determineSenderAndUpdateMap(senderLnkValue);
                } else {
                    log.error("NO " + PRI_SENDER_LNK + " present");
                }

                log.info("Sending Message " + message.getCode() + " to " + recipientBECode + " with ctx=" + contextMapStr);

                new SendMessage(message.getCode(), recipientBECode, ctxMap).sendMessage();
            });
        } else {
            log.warn("No messages found for milestoneCode -> " + milestoneCode);
        }

        ctxMap.clear();
    }

    private void determineRecipientLnkValueAndUpdateMap(String recipientLnkValue) {
        if (recipientLnkValue != null) {
            // check the various formats to get the recipientBECode
            if (recipientLnkValue.startsWith(SELF)) { // The coreBE is the recipient
                ctxMap.put(RECIPIENT, coreBE.getCode());
                recipientBECode = coreBE.getCode();
            } else if (recipientLnkValue.startsWith("PER_")) {
                ctxMap.put(RECIPIENT, recipientLnkValue);
                recipientBECode = recipientLnkValue;
            } else {
                // check if it is a LNK path (of the coreBE)
                // "LNK_INTERN"
                if (recipientLnkValue.startsWith("LNK_")) {
                    String[] splitStr = recipientLnkValue.split(":");
                    BaseEntity lnkBe = coreBE; // seed
                    for (String s : splitStr) {
                        lnkBe = beUtils.getBaseEntityFromLinkAttribute(lnkBe, s);
                    }
                    ctxMap.put(RECIPIENT, lnkBe.getCode());
                    recipientBECode = lnkBe.getCode();
                }
            }
        }
    }

    private void determineSenderAndUpdateMap(String senderLnkValue) {
        if (senderLnkValue != null) {
            // check the various formats to get the senderBECode
            if (senderLnkValue.startsWith(USER)) { // The user is the sender
                ctxMap.put(SENDER, userToken.getUserCode());
            } else if (senderLnkValue.startsWith("PER_")) {
                ctxMap.put(SENDER, senderLnkValue);
            } else {
                // check if it is a LNK path (of the coreBE)
                // "LNK_INTERN"
                if (senderLnkValue.startsWith("LNK_")) {
                    String[] splitStr = senderLnkValue.split(":");
                    BaseEntity lnkBe = coreBE; // seed
                    for (String s : splitStr) {
                        lnkBe = beUtils.getBaseEntityFromLinkAttribute(lnkBe, s);
                    }
                    ctxMap.put(SENDER, lnkBe.getCode());
                }
            }
        }
    }
}
