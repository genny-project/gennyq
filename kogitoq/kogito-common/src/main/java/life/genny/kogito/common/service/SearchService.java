package life.genny.kogito.common.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;

@ApplicationScoped
public class SearchService {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	SearchUtils searchUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	DefUtils defUtils;

	@Inject
	QwandaUtils qwandaUtils;

	/**
	 * Perform a Detail View search.
	 *
	 * @param targetCode The code of the target to display
	 */
	public void sendTable(String eventCode) {

		String searchCode = "SBE_"+StringUtils.removeStart(eventCode, "QUE_");
		log.info("Sending Table :: " + searchCode);

		searchUtils.searchTable(searchCode);
		sendSearchPCM("PCM_TABLE", searchCode);
	}

	/**
	 * Perform a Detail View search.
	 *
	 * @param targetCode The code of the target to display
	 */
	public void sendDetailView(String targetCode) {

		// fetch target and find it's definition
		BaseEntity target = beUtils.getBaseEntityByCode(targetCode);
		BaseEntity definition = defUtils.getDEF(target);

		// grab the corresponding detail view SBE
		String searchCode = "SBE_" + StringUtils.removeStart(definition.getCode(), "DEF_");
		log.info("Sending Detail View :: " + searchCode);

		SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(), searchCode, SearchEntity.class);
		searchEntity.addFilter("PRI_CODE", SearchEntity.StringFilter.EQUAL, targetCode);

		// perform the search
		searchUtils.searchTable(searchEntity);
		sendSearchPCM("PCM_DETAIL_VIEW", searchEntity.getCode());
	}

	/**
	 * Perform a Bucket search.
	 */
	public void sendBuckets() {
	}

	/**
	 * Send a search PCM with the correct search code.
	 *
	 * @param pcmCode The code of pcm to send
	 * @param searchCode The code of the searhc to send
	 */
	public void sendSearchPCM(String pcmCode, String searchCode) {

		BaseEntity content = beUtils.getBaseEntityByCode("PCM_CONTENT");
		Attribute attribute = qwandaUtils.getAttribute("PRI_LOC1");
		EntityAttribute ea = new EntityAttribute(content, attribute, 1.0, pcmCode);

		try {
			content.addAttribute(ea);
		} catch (BadDataException e) {
			e.printStackTrace();
		}

		BaseEntity pcm = beUtils.getBaseEntityByCode(pcmCode);
		ea = new EntityAttribute(pcm, attribute, 1.0, searchCode);

		try {
			pcm.addAttribute(ea);
		} catch (BadDataException e) {
			e.printStackTrace();
		}

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(content);
		msg.add(pcm);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		KafkaUtils.writeMsg("webcmds", msg);
	}

	public void getBuckets(String eventCode) throws  Exception {
		String searchCode = "SBE_"+StringUtils.removeStart(eventCode, "QUE_");

		List<String> bucketCodes = CacheUtils.getObject(userToken.getRealm(), searchCode, List.class);

		sendBucketData(searchCode,bucketCodes);

		bucketCodes.stream().forEach(e -> {
			searchUtils.searchTable(e);
		});

	}

	public void sendBucketData(String source, List<String> bucketCodes) {
		List<BaseEntity> listBase = new ArrayList<>();
		for(String str: bucketCodes){
			BaseEntity base = new BaseEntity(str);
			base.setRealm(userToken.getRealm());
			listBase.add(new BaseEntity(str));
		}

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(listBase);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		msg.setParentCode(source);
		KafkaUtils.writeMsg("webcmds", msg);
	}
}
