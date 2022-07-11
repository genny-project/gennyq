package life.genny.kogito.common.service;

import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.constants.CacheName;
import life.genny.qwandaq.serialization.key.baseentity.BaseEntityKey;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;

@ApplicationScoped
public class NavigationService {

	private static final Logger log = Logger.getLogger(SearchService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	/**
	 * Control main content navigation using a pcm and a question
	 *
	 * @param pcmCode The code of the PCM baseentity
	 * @param questionCode The code of the question
	 */
	public void navigateContent(final String pcmCode, final String questionCode) {

		BaseEntity content = beUtils.getBaseEntityByCode("PCM_CONTENT");

		try {
			content.setValue("PRI_LOC1", pcmCode);
		} catch (BadDataException e) {
			e.printStackTrace();
		}

		BaseEntity pcm = beUtils.getBaseEntityByCode(pcmCode);
		Attribute attribute = qwandaUtils.getAttribute("PRI_QUESTION_CODE");
		EntityAttribute ea = new EntityAttribute(pcm, attribute, 1.0, questionCode);

		try {
			pcm.addAttribute(ea);
		} catch (BadDataException e) {
			e.printStackTrace();
		}

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage();
		msg.add(content);
		msg.add(pcm);

		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		KafkaUtils.writeMsg("webdata", msg);
	}

    public void updatePcm(String pcmCode, String loc, String newValue) {

        log.info("Replacing " + pcmCode + ":" + loc + " with " + newValue);

        String cachedCode = userToken.getJTI() + ":" + pcmCode;
		BaseEntityKey pcmKey = new BaseEntityKey(userToken.getProductCode(), cachedCode);
        BaseEntity pcm = CacheUtils.getObject(CacheName.BASEENTITY, pcmKey, BaseEntity.class);

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
        if (!locOptional.isPresent()) {
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

        CacheUtils.putObject(CacheName.BASEENTITY, pcmKey, pcm);
    }
}
