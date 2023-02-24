package life.genny.kogito.common.service;

import java.util.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import life.genny.qwandaq.datatype.DataType;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataAttributeMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.*;

/**
 * A Service class used for Auth Init operations.
 *
 * @auther Bryn Meachem
 * @author Jasper Robison
 */
@ApplicationScoped
public class InitService extends KogitoService {

	@Inject
	private BaseEntityUtils beUtils;

	@Inject
	private UserToken userToken;

	@Inject
	private QwandaUtils qwandaUtils;

	@Inject
	CacheManager cacheManager;

	@Inject
	Logger log;

	/**
	 * Send the Project BaseEntity.
	 */
	public void sendProject() {

		BaseEntity project = beUtils.getProjectBaseEntity();
		log.info("Sending Project " + project.getCode());

		// configure msg and send
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(project);
		msg.setToken(userToken.getToken());
		msg.setAliasCode("PROJECT");
		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
	}

	/**
	 * Send the User.
	 */
	public void sendUser() {

		// fetch the users baseentity
		BaseEntity user = beUtils.getUserBaseEntity();
		log.info("Sending User " + user.getCode());

		// configure msg and send
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(user);
		msg.setToken(userToken.getToken());
		msg.setAliasCode("USER");

		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
	}

	/**
	 * Send All attributes for the productCode.
	 */
	public void sendAllAttributes() {
		log.info("Sending Attributes for " + userToken.getProductCode());
		String productCode = userToken.getProductCode();

		Collection<Attribute> allAttributes = attributeUtils.getAttributesForProduct(productCode);

		int BATCH_SIZE = 500;
		int count = 0;
		int batchNum = 1;
		int totalAttributesCount = allAttributes.size();
		int totalBatches = totalAttributesCount / BATCH_SIZE;
		if (totalAttributesCount % BATCH_SIZE != 0) {
			totalBatches++;
		}
		log.infof("%s Attribute(s) to be sent in %s batch(es).", totalAttributesCount, totalBatches);
		List<Attribute> attributesBatch = new LinkedList<>();
		for(Attribute attribute : allAttributes) {
			String attributeCode = attribute.getCode();
			if (attributeCode.startsWith(Prefix.CAP_)) {
				continue;
			}
			// see if dtt exists
			try {
				DataType dataType = attributeUtils.getDataType(attribute, true);
				attribute.setDataType(dataType);
			} catch (ItemNotFoundException e) {
				e.printStackTrace();
				continue;
			}
			attributesBatch.add(attribute);
			count++;
			if (count == BATCH_SIZE) {
				dispatchAttributesToKafka(attributesBatch, batchNum, totalBatches);
				attributesBatch.clear();
				count = 0;
				batchNum++;
			}
		}
		// Dispatch the last batch, if any
		if (!attributesBatch.isEmpty()) {
			dispatchAttributesToKafka(attributesBatch, batchNum, totalBatches);
		}
	}

	private void dispatchAttributesToKafka(List<Attribute> attributesBatch, int batchNum, int totalBatches) {
		QDataAttributeMessage msg = new QDataAttributeMessage();
		msg.add(attributesBatch);
		msg.setItems(attributesBatch);
		// set token and send
		msg.setToken(userToken.getToken());
		msg.setAliasCode("ATTRIBUTE_MESSAGE_BATCH_" + batchNum + "_OF_" + totalBatches);
		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
	}

	public void sendDrafts() {

		BaseEntity user = beUtils.getUserBaseEntity();
		Ask ask = qwandaUtils.generateAskFromQuestionCode("QUE_DRAFTS_GRP", user, user, new CapabilitySet(user), new ReqConfig());

		// configure msg and send
		QDataAskMessage msg = new QDataAskMessage(ask);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
	}

}
