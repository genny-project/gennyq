package life.genny.kogito.common.service;

import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
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

    public void updatePcm(String pcmCode, String loc, String newValue) {
        log.info("Got update PCM command for " + pcmCode);
        log.info("Replacing " + loc + " with " + newValue);

        String cachedCode = userToken.getJTI() + ":" + pcmCode;

        BaseEntity pcm = CacheUtils.getObject(userToken.getProductCode(), cachedCode, BaseEntity.class);

        if (pcm == null) {
            log.info("Couldn't find " + cachedCode + " in cache, grabbing from db!");
            pcm = beUtils.getBaseEntityByCode(userToken.getProductCode(), pcmCode);
        }

       
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

        CacheUtils.putObject(userToken.getProductCode(), cachedCode, pcm);
    }

}
