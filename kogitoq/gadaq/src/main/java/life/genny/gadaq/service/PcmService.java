package life.genny.gadaq.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.Service;

@ApplicationScoped
public class PcmService {

    Jsonb jsonb = JsonbBuilder.create();

    @Inject
    Service service;

    @Inject
    DatabaseUtils databaseUtils;

    @Inject
    BaseEntityUtils beUtils;

    @Inject
    UserToken userToken;

    @Inject
    QwandaUtils qwandaUtils;

    private static final Logger log = Logger.getLogger(PcmService.class);

    private EntityAttribute makePcmAttribute(BaseEntity be, String attrCode, String value, double weight) {
        EntityAttribute out = new EntityAttribute();

        Attribute att = databaseUtils.findAttributeByCode(userToken.getProductCode(), attrCode);
        if (att != null) {
            out.setAttribute(att);
        }
        out.setBaseEntity(be);
        out.setAttributeCode(attrCode);
        out.setValueString(value);
        out.setWeight(weight);

        return out;
    }

    public void sendApprovalOnePcm(String pcmCode, String loc) {

        final String approvalPcmCode = "PCM_APPROVAL_FORM_ONE";

        // Text PCM
        BaseEntity textPcm = new BaseEntity(approvalPcmCode + "_TEXT", "Text Pcm for header");

        Set<EntityAttribute> textAttributes = new HashSet<>();

        EntityAttribute textTpl = makePcmAttribute(textPcm, "PRI_TEMPLATE_CODE", "TPL_LOJING_HEADER", 0);
        EntityAttribute textQuestionCode = makePcmAttribute(textPcm, "PRI_QUESTION_CODE", "QUE_FORM_HEADER_GRP", 1);
        EntityAttribute textPriLoc1 = makePcmAttribute(textPcm, "PRI_LOC1", "PRI_FIRSTNAME", 2);

        textAttributes.add(textTpl);
        textAttributes.add(textQuestionCode);
        textAttributes.add(textPriLoc1);

        textPcm.setBaseEntityAttributes(textAttributes);

        // Progress Bar PCM
        BaseEntity progressPcm = new BaseEntity(approvalPcmCode + "_PROGRESS", "Progress bar pcm for header");

        Set<EntityAttribute> progressAttributes = new HashSet<>();

        EntityAttribute progressTpl = makePcmAttribute(progressPcm, "PRI_TEMPLATE_CODE", "TPL_PROGRESS_BAR", 0);
        EntityAttribute progressQuestionCode = makePcmAttribute(progressPcm, "PRI_QUESTION_CODE",
                "QUE_FORM_HEADER_GRP", 1);
        EntityAttribute progressPriLoc1 = makePcmAttribute(progressPcm, "PRI_LOC1", "PRI_COMPLETION", 2);

        progressAttributes.add(progressPriLoc1);
        progressAttributes.add(progressQuestionCode);
        progressAttributes.add(progressTpl);

        progressPcm.setBaseEntityAttributes(progressAttributes);

        // Header PCM
        BaseEntity headerPcm = new BaseEntity(approvalPcmCode + "_HEADER", "HEADER_PCM");

        Set<EntityAttribute> headerAttributes = new HashSet<>();

        EntityAttribute headerTpl = makePcmAttribute(headerPcm, "PRI_TEMPLATE_CODE", "TPL_VERT", 0);
        EntityAttribute headerPriLoc1 = makePcmAttribute(headerPcm, "PRI_LOC1", approvalPcmCode + "_TEXT", 1);
        EntityAttribute headerPriLoc2 = makePcmAttribute(headerPcm, "PRI_LOC2", approvalPcmCode + "_PROGRESS", 2);

        headerAttributes.add(headerTpl);
        headerAttributes.add(headerPriLoc1);
        headerAttributes.add(headerPriLoc2);

        headerPcm.setBaseEntityAttributes(headerAttributes);

        // Form PCM
        BaseEntity formPcm = new BaseEntity(approvalPcmCode + "_FORM", "Form PCM ");

        Set<EntityAttribute> formAttributes = new HashSet<>();

        EntityAttribute formTpl = makePcmAttribute(formPcm, "PRI_TEMPLATE_CODE", "TPL_FORM", 0);
        EntityAttribute formQuestionCode = makePcmAttribute(formPcm, "PRI_QUESTION_CODE",
                "QUE_TENANT_APPROVAL_START_GRP", 1);

        formAttributes.add(formTpl);
        formAttributes.add(formQuestionCode);

        formPcm.setBaseEntityAttributes(formAttributes);

        // Main PCM
        BaseEntity mainPcm = new BaseEntity(approvalPcmCode, approvalPcmCode);

        EntityAttribute tpl = makePcmAttribute(mainPcm, "PRI_TEMPLATE_CODE", "TPL_VERT", 0);
        EntityAttribute loc1 = makePcmAttribute(mainPcm, "PRI_LOC1", headerPcm.getCode(), 1);
        EntityAttribute loc2 = makePcmAttribute(mainPcm, "PRI_LOC2", formPcm.getCode(), 2);

        Set<EntityAttribute> mainAttributes = new HashSet<>();
        mainAttributes.add(tpl);
        mainAttributes.add(loc1);
        mainAttributes.add(loc2);

        mainPcm.setBaseEntityAttributes(mainAttributes);

        BaseEntity[] pcms = new BaseEntity[] {
                progressPcm,
                headerPcm,
                textPcm,
                formPcm,
                mainPcm
        };

        QDataBaseEntityMessage msg = new QDataBaseEntityMessage(pcms);
        msg.setTotal((long) pcms.length);

        List<Ask> asks = new ArrayList<>();

        BaseEntity user = beUtils.getBaseEntityByCode(userToken.getUserCode());

        log.info(userToken.getUserCode());

        for (BaseEntity pcm : pcms) {

            Optional<EntityAttribute> questionAttribute = pcm.findEntityAttribute("PRI_QUESTION_CODE");
            if (questionAttribute.isPresent()) {
                asks.add(qwandaUtils.generateAskFromQuestionCode(questionAttribute.get().getValueString(),
                        user, user));
            }

        }

        log.info(asks.get(0).getTargetCode());

        QDataAskMessage askMsg = new QDataAskMessage(asks.toArray(new Ask[0]));

        askMsg.setToken(userToken.getToken());
        askMsg.setReplace(true);

        msg.setToken(userToken.getToken());
        msg.setReplace(true);

        KafkaUtils.writeMsg("webdata", askMsg);

        KafkaUtils.writeMsg("webdata", msg);

        updatePcm(pcmCode, loc, approvalPcmCode);
    }

    public void updatePcm(String pcmCode, String loc, String newValue) {
        log.info("Got update PCM command for " + pcmCode);
        log.info("Replacing " + loc + " with " + newValue);

        BaseEntity pcm = databaseUtils.findBaseEntityByCode(userToken.getProductCode(), pcmCode);

        if (pcm == null) {
            log.error("Couldn't find PCM with code " + pcmCode);
            throw new NullPointerException("Couldn't find PCM with code " + pcmCode);
        }

        log.info("Found PCM " + pcm);

        Optional<EntityAttribute> locOptional = pcm.findEntityAttribute(loc);
        if (locOptional.isPresent()) {
            log.info("Found loc " + loc);

        } else {
            log.error("Couldn't find base entity attribute " + loc);
            throw new NullPointerException("Couldn't find base entity attribute " + loc);
        }

        EntityAttribute locAttribute = locOptional.get();

        log.info(locAttribute.getAttributeCode() + " has valueString " + locAttribute.getValueString());

        locAttribute.setValueString(newValue);

        Set<EntityAttribute> attributes = pcm.getBaseEntityAttributes();

        attributes.removeIf(att -> att.getAttributeCode().equals(loc));

        attributes.add(locAttribute);

        pcm.setBaseEntityAttributes(attributes);

        QDataBaseEntityMessage msg = new QDataBaseEntityMessage(pcm);
        msg.setToken(userToken.getToken());
        msg.setReplace(true);

        KafkaUtils.writeMsg("webdata", msg);
    }

}
