package life.genny.kogito.common.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import life.genny.qwandaq.datatype.DataType;
import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QDataAttributeMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
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
	Logger log;

	private static Map<String, List<Set<Attribute>>> batchedAttributesListPerProduct = new ConcurrentHashMap<>();

	private static Map<String, Long> attributesLastFetchedAtPerProduct = new ConcurrentHashMap<>();

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
		EntityAttribute firstName = beaUtils.getEntityAttribute(productCode, userCode, Attribute.PRI_FIRSTNAME);
		user.addAttribute(firstName);
		EntityAttribute lastName = beaUtils.getEntityAttribute(productCode, userCode, Attribute.PRI_LASTNAME);
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
		Long attributesUpdatedAt = attributeUtils.getAttributesLastUpdatedAt(productCode);
		Long attributesLastFetchedAt = attributesLastFetchedAtPerProduct.get(productCode);
		List<Set<Attribute>> batchedAttributesList = batchedAttributesListPerProduct.get(productCode);
		if (batchedAttributesList == null || batchedAttributesList.isEmpty() ||
			attributesUpdatedAt == null || attributesLastFetchedAt == null ||
				attributesLastFetchedAt.compareTo(attributesUpdatedAt) < 0
				) {
			cacheAttributesLocallyAndDispatch(productCode);
			attributesLastFetchedAtPerProduct.put(productCode, attributesUpdatedAt);
		} else {
			log.debugf("No change to attributes since previous read. Sending out the locally cached batch of attributes");
			dispatchLocallyCachedAttributes(batchedAttributesList);
		}
	}

	private void cacheAttributesLocallyAndDispatch(String productCode) {
		long start = System.nanoTime();
		Collection<Attribute> allAttributes = attributeUtils.getAttributesForProduct(productCode);
		long end = System.nanoTime();
		log.debugf("Time taken to read all attributes: %s (nanos)", end-start);

		int BATCH_SIZE = 500;
		int count = 0;
		int batchNum = 1;
		int totalAttributesCount = allAttributes.size();
		int totalBatches = totalAttributesCount / BATCH_SIZE;
		if (totalAttributesCount % BATCH_SIZE != 0) {
			totalBatches++;
		}

		log.debug("Discovered " + totalBatches + " batches for a total attribute count: " + totalAttributesCount);
		List<Set<Attribute>> batchedAttributesList = new CopyOnWriteArrayList<>();
		Set<Attribute> attributesBatch = new CopyOnWriteArraySet<>();
		for(Attribute attribute : allAttributes) {
			log.info("loading batch: " + batchNum);
			if (attribute == null || attribute.getCode().startsWith(Prefix.CAP_)) {
				if(attribute == null) {
					log.info("\tfound null attribute");
				}
				totalAttributesCount--;
				continue;
			}
			// see if dtt exists
			try {
				log.info("\tAttempting to find datatype for: " + attribute.getCode());
				DataType dataType = attributeUtils.getDataType(attribute, true);
				attribute.setDataType(dataType);
			} catch (ItemNotFoundException e) {
				log.errorf("Error fetching data type [dttcode:%s] for attribute [%s:%s]", attribute.getDttCode(), attribute.getRealm(), attribute.getCode());
				totalAttributesCount--;
				continue;
			}
			log.info("\tAdding " + attribute.getCode() + " to batch. Current count: " + ++count);
			attributesBatch.add(attribute);
			if (count == BATCH_SIZE) {
				log.info("\tDispatching batch " + batchNum + " call!");
				dispatchAttributesToKafka(attributesBatch, "ATTRIBUTE_MESSAGE_BATCH_" + batchNum + "_OF_" + totalBatches);
				batchedAttributesList.add(attributesBatch);
				count = 0;
				batchNum++;
				attributesBatch = new HashSet<>();
			}
		}

		// Dispatch the last batch, if any
		if (!attributesBatch.isEmpty()) {
			dispatchAttributesToKafka(attributesBatch, "ATTRIBUTE_MESSAGE_BATCH_" + batchNum + "_OF_" + totalBatches);
		}
		end = System.nanoTime();
		log.infof("%s Attribute(s) dispatched in %s batch(es).", totalAttributesCount, totalBatches);
		log.debugf("Total time taken to send out the attributes (for the first time): %s (nanos)", end-start);
		batchedAttributesList.add(attributesBatch);
		batchedAttributesListPerProduct.put(productCode, batchedAttributesList);
	}

	private void dispatchLocallyCachedAttributes(List<Set<Attribute>> batchedAttributesList) {
		long start = System.nanoTime();
		int batchNum = 1;
		for (Set<Attribute> attributesBatch : batchedAttributesList) {
			dispatchAttributesToKafka(attributesBatch, "ATTRIBUTE_MESSAGE_BATCH_" + batchNum + "_OF_" + batchedAttributesList.size());
			batchNum++;
		}
		long end = System.nanoTime();
		log.debugf("Time taken to send out the locally cached batch of attributes: %s (nanos)", end-start);
	}

	/**
	 * Dispatch a batch of attributes.
	 *
	 * @param attributesBatch
	 * @param aliasCode
	 */
	private void dispatchAttributesToKafka(Set<Attribute> attributesBatch, String aliasCode) {
		QDataAttributeMessage msg = new QDataAttributeMessage();
		msg.add(attributesBatch);
		msg.setItems(attributesBatch);
		// set token and send
		msg.setToken(userToken.getToken());
		msg.setAliasCode(aliasCode);
		log.debug("Sending attribute batch: " + aliasCode + " to webdata");
		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
	}

}
