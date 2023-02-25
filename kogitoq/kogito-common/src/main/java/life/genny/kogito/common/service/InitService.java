package life.genny.kogito.common.service;

import java.util.*;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.converter.ValidationListConverter;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.validation.Validation;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataAttributeMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.*;
import org.jboss.logging.Logger;
import org.jbpm.process.core.datatype.DataTypeUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A Service class used for Auth Init operations.
 *
 * @auther Bryn Meachem
 * @author Jasper Robison
 */
@ApplicationScoped
public class InitService extends KogitoService {

	@Inject
	private CacheManager cm;

	@Inject
	private BaseEntityUtils beUtils;

	@Inject
	private UserToken userToken;

	@Inject
	private QwandaUtils qwandaUtils;

	@Inject
	private SearchUtils searchUtils;

	@Inject
	private CapabilitiesManager capMan;

	@Inject
	CacheManager cacheManager;

	@Inject
	Logger log;

	/**
	 * Send the Project BaseEntity.
	 */
	public void sendProject() {

		BaseEntity project = beUtils.getBaseEntity(Prefix.PRJ_.concat(userToken.getProductCode().toUpperCase()));
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

		String userCode = userToken.getUserCode();
		String productCode = userToken.getProductCode();

		// fetch the users baseentity, and names
		BaseEntity user = beUtils.getBaseEntity(userCode);
		EntityAttribute name = new EntityAttribute(user, attributeUtils.getAttribute(Attribute.PRI_NAME, true), 1.0, user.getName());
		user.addAttribute(name);
		EntityAttribute firstName = beaUtils.getEntityAttribute(productCode, userCode, Attribute.PRI_FIRSTNAME, true, true);
		user.addAttribute(firstName);
		EntityAttribute lastName = beaUtils.getEntityAttribute(productCode, userCode, Attribute.PRI_LASTNAME, true, true);
		user.addAttribute(lastName);

		// configure msg and send
		log.info("Sending User " + user.getCode());
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
			DataType dataType = attributeUtils.getDataType(attribute, true);
			if (dataType != null) {
				attribute.setDataType(dataType);
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

	/**
	 * Dispatch a batch of attributes.
	 *
	 * @param attributesBatch
	 * @param batchNum
	 * @param totalBatches
	 */
	private void dispatchAttributesToKafka(List<Attribute> attributesBatch, int batchNum, int totalBatches) {
		QDataAttributeMessage msg = new QDataAttributeMessage();
		msg.add(attributesBatch);
		msg.setItems(attributesBatch);
		// set token and send
		msg.setToken(userToken.getToken());
		msg.setAliasCode("ATTRIBUTE_MESSAGE_BATCH_" + batchNum + "_OF_" + totalBatches);
		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
	}

}
