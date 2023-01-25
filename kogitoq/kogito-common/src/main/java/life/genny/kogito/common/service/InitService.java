package life.genny.kogito.common.service;

import java.util.Arrays;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import life.genny.qwandaq.Ask;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.entity.BaseEntity;

import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataAttributeMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.utils.CacheUtils;

import life.genny.qwandaq.utils.KafkaUtils;

/**
 * A Service class used for Auth Init operations.
 *
 * @auther Bryn Meachem
 * @author Jasper Robison
 */
@ApplicationScoped
public class InitService extends KogitoService {

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

		Integer TOTAL_PAGES = CacheUtils.getObject(productCode, "ATTRIBUTE_PAGES", Integer.class);

		for (int currentPage = 0; currentPage < TOTAL_PAGES + 1; currentPage++) {

			QDataAttributeMessage msg = CacheUtils.getObject(productCode,
					"ATTRIBUTES_P" + currentPage, QDataAttributeMessage.class);

			Attribute[] attributes = Arrays.asList(msg.getItems()).stream()
				// Filter capability attributes
				.filter((attribute) -> !attribute.getCode().startsWith(Prefix.CAP_))
				.collect(Collectors.toList())
				.toArray(new Attribute[0]);

			msg.setItems(attributes);
			// set token and send
			msg.setToken(userToken.getToken());
			msg.setAliasCode("ATTRIBUTE_MESSAGE_" + (currentPage + 1) + "_OF_" + (TOTAL_PAGES + 1));
			KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
		}
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
