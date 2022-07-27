package life.genny.kogito.common.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.message.QCmdMessage;
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
		String codeTrimmed = StringUtils.removeStart(eventCode, "QUE_TREE_ITEM_");
		String searchCode = "SBE_"+StringUtils.removeStart(codeTrimmed, "QUE_");
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

		// update content
		BaseEntity content = beUtils.getBaseEntity("PCM_CONTENT");
		Attribute attribute = qwandaUtils.getAttribute("PRI_LOC1");
		EntityAttribute ea = new EntityAttribute(content, attribute, 1.0, pcmCode);
		content.addAttribute(ea);

		// update target pcm
		BaseEntity pcm = beUtils.getBaseEntity(pcmCode);
		ea = new EntityAttribute(pcm, attribute, 1.0, searchCode);
		pcm.addAttribute(ea);

		// send to alyson
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(content);
		msg.add(pcm);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		KafkaUtils.writeMsg("webcmds", msg);
	}

	/**
	 * Get bucket data with bucket event
	 * @param eventCode Bucket event code
	 */
	public void getBuckets(String eventCode) {
		try {
			String searchCode = "SBE_" + StringUtils.removeStart(eventCode, "QUE_");

			List<String> originBucketCodes = CacheUtils.getObject(userToken.getRealm(), searchCode, List.class);
			List<String>  bucketCodes = getBucketCodesBySearchEntity(originBucketCodes);
			sendBucketCodes(bucketCodes);

			originBucketCodes.stream().forEach(e -> {
				searchUtils.searchTable(e);
				sendSearchPCM("PCM_PROCESS", e);
			});
		}catch (Exception ex){
			log.error(ex);
		}
	}

	/**
	 * Send the list of bucket codes to frond-end
	 * @param bucketCodes The list of bucket codes
	 */
	public void sendBucketCodes(List<String> bucketCodes) {
		QCmdMessage msgProcess = new QCmdMessage("DISPLAY","PROCESS");
		msgProcess.setToken(userToken.getToken());
		KafkaUtils.writeMsg("webcmds", msgProcess);

		QCmdMessage msgCodes = new QCmdMessage("BUCKET_CODES","BUCKET_CODES");
		msgCodes.setToken(userToken.getToken());
		msgCodes.setSourceCode("BUCKET_CODES");
		msgCodes.setTargetCodes(bucketCodes);
		KafkaUtils.writeMsg("webcmds", msgCodes);
	}

	/**
	 * Get the list of bucket codes with session id
	 * @param originBucketCodes List of bucket codes
	 * @return The list of bucket code with session id
	 */
	public List<String> getBucketCodesBySearchEntity(List<String> originBucketCodes){
		List<String> bucketCodes = new ArrayList<>();
		originBucketCodes.stream().forEach(e -> {
			SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(),e, SearchEntity.class);
			String searchCode = searchEntity.getCode() + "_" + userToken.getJTI().toUpperCase();
			bucketCodes.add(searchCode);
		});

		return bucketCodes;
	}
}
