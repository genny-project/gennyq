package life.genny.kogito.common.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NavigationService {

	private static final Logger log = Logger.getLogger(FrontendService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	DatabaseUtils databaseUtils;

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
}
