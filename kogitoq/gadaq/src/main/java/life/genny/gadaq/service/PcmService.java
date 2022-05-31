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
import life.genny.qwandaq.attribute.EntityAttribute;
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
        out.setBaseEntity(be);
        out.setAttributeCode(attrCode);
        out.setValueString(value);
        out.setWeight(weight);

        return out;
    }

    public void sendApprovalOnePcm() {

        final String pcmCode = "PCM_APPROVAL_FORM_ONE";

        // Text PCM
        BaseEntity textPcm = new BaseEntity(pcmCode + "_TEXT", "Text Pcm for header");

        Set<EntityAttribute> textAttributes = new HashSet<>();

        EntityAttribute textTpl = makePcmAttribute(textPcm, "PRI_TEMPLATE_CODE", "TPL_LOJING_HEADER", 0);
        EntityAttribute textQuestionCode = makePcmAttribute(textPcm, "PRI_QUESTION_CODE", "QUE_FORM_HEADER_GRP", 1);
        EntityAttribute textPriLoc1 = makePcmAttribute(textPcm, "PRI_LOC1", "PRI_FIRSTNAME", 2);

        textAttributes.add(textTpl);
        textAttributes.add(textQuestionCode);
        textAttributes.add(textPriLoc1);

        // Progress Bar PCM
        BaseEntity progressPcm = new BaseEntity(pcmCode + "_PROGRESS", "Progress bar pcm for header");

        Set<EntityAttribute> progressAttributes = new HashSet<>();

        EntityAttribute progressTpl = makePcmAttribute(progressPcm, "PRI_TEMPLATE_CODE", "TPL_PROGRESS_CODE", 0);
        EntityAttribute progressQuestionCode = makePcmAttribute(progressPcm, "PRI_QUESTION_CODE",
                "QUE_FORM_HEADER_GRP", 1);
        EntityAttribute progressPriLoc1 = makePcmAttribute(progressPcm, "PRI_LOC1", "PRI_COMPLETION", 2);

        progressAttributes.add(progressPriLoc1);
        progressAttributes.add(progressQuestionCode);
        progressAttributes.add(progressTpl);

        progressPcm.setBaseEntityAttributes(progressAttributes);

        // Header PCM
        BaseEntity headerPcm = new BaseEntity(pcmCode + "_HEADER", "HEADER_PCM");

        Set<EntityAttribute> headerAttributes = new HashSet<>();

        EntityAttribute headerTpl = makePcmAttribute(headerPcm, "PRI_TEMPLATE_CODE", "TPL_VERT", 0);
        EntityAttribute headerPriLoc1 = makePcmAttribute(headerPcm, "PRI_LOC1", pcmCode + "_TEXT", 1);
        EntityAttribute headerPriLoc2 = makePcmAttribute(headerPcm, "PRI_LOC2", pcmCode + "_PROGRESS", 2);

        headerAttributes.add(headerTpl);
        headerAttributes.add(headerPriLoc1);
        headerAttributes.add(headerPriLoc2);

        headerPcm.setBaseEntityAttributes(headerAttributes);

        // Main PCM
        BaseEntity mainPcm = new BaseEntity(pcmCode, pcmCode);

        EntityAttribute tpl = makePcmAttribute(mainPcm, "PRI_TEMPLATE_CODE", "TPL_VERT", 0);
        EntityAttribute loc1 = makePcmAttribute(mainPcm, "PRI_LOC1", pcmCode + "_HEADER", 1);

        Set<EntityAttribute> mainAttributes = new HashSet<>();
        mainAttributes.add(tpl);
        mainAttributes.add(loc1);

        mainPcm.setBaseEntityAttributes(mainAttributes);

        BaseEntity[] pcms = new BaseEntity[] { textPcm,
                progressPcm,
                headerPcm,
                textPcm,
                mainPcm
        };

        QDataBaseEntityMessage msg = new QDataBaseEntityMessage(pcms);

        List<Ask> asks = new ArrayList<>();

        for (BaseEntity pcm : pcms) {

            Optional<EntityAttribute> questionAttribute = pcm.findEntityAttribute("PRI_QUESTION_CODE");
            if (questionAttribute.isPresent()) {
                asks.add(qwandaUtils.generateAskFromQuestionCode(questionAttribute.get().getValueString(), pcm, pcm));
            }

        }

        QDataAskMessage askMsg = new QDataAskMessage(asks.toArray(new Ask[0]));

        askMsg.setToken(userToken.getToken());
        askMsg.setReplace(true);

        msg.setToken(userToken.getToken());
        msg.setReplace(true);

        KafkaUtils.writeMsg("webdata", askMsg);

        KafkaUtils.writeMsg("webdata", msg);
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
