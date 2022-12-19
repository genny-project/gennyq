package life.genny.kogito.common.service;

import java.util.Arrays;
import java.util.List;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.NoResultException;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;

import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityMode;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.core.node.PermissionMode;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;

import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataAttributeMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.DatabaseUtils;

import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;

/**
 * A Service class used for Auth Init operations.
 *
 * @auther Bryn Meachem
 * @author Jasper Robison
 */
@ApplicationScoped
public class InitService {

	private static final Logger log = Logger.getLogger(InitService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	private CacheUtils cacheUtils;

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

		QDataAttributeMessage msg = new QDataAttributeMessage();
		List<Attribute> allAttributes = cacheUtils.getAllAttributes(productCode);
		Attribute[] attributes = allAttributes.stream()
			// Filter capability attributes
			.filter((attribute) -> !attribute.getCode().startsWith(Prefix.CAP))
			.collect(Collectors.toList())
			.toArray(new Attribute[0]);

		msg.setItems(attributes);
		// set token and send
		msg.setToken(userToken.getToken());
		msg.setAliasCode("ATTRIBUTES");
		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
	}

	private Ask generateAddItemsAsk(String productCode, BaseEntity user) {
		// find the question in the database
		Question groupQuestion;
		try {
			groupQuestion = cacheUtils.getQuestion(productCode, "QUE_ADD_ITEMS_GRP");
		} catch (NoResultException e) {
			throw new ItemNotFoundException("QUE_ADD_ITEMS_GRP", e);
		}

		Ask parentAsk = new Ask(groupQuestion, user.getCode(), user.getCode());
		parentAsk.setRealm(productCode);

		ReqConfig userConfig = capMan.getUserCapabilities();
		
		// Generate the Add Items asks from the capabilities
		// Check if there is a def first
		for(Capability capability : userConfig.userCapabilities) {
			// If they don't have the capability then don't bother finding the def
			if(!capability.checkPerms(false, CapabilityNode.get(CapabilityMode.ADD, PermissionMode.ALL)))
				continue;

			String defCode = CommonUtils.substitutePrefix(capability.code, "DEF");
			try {
				// Check for a def
				beUtils.getBaseEntity(productCode, defCode);
			} catch (ItemNotFoundException e) {
				// We don't need to handle this. We don't care if there isn't always a def
				continue;
			}
			// Create the ask (there is a def and we have the capability)
			String baseCode = CommonUtils.safeStripPrefix(capability.code);

			String eventCode = "EVT_ADD_".concat(baseCode);
			String name = "Add ".concat(CommonUtils.normalizeString(baseCode));
			Attribute event = qwandaUtils.createButtonEvent(eventCode, name);

			Question question = new Question("QUE_ADD_".concat(baseCode), name, event);

			Ask addAsk = new Ask(question, user.getCode(), user.getCode());
			addAsk.setRealm(productCode);

			parentAsk.add(addAsk);
		}
		return parentAsk;
	}

	/**
	 * Send Add Items Menu
	 */
	public void sendAddItems() {

		BaseEntity user = beUtils.getUserBaseEntity();

		Ask ask = generateAddItemsAsk(userToken.getProductCode(), user);
		// configure msg and send
		QDataAskMessage msg = new QDataAskMessage(ask);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		// KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
	}

	public void sendDrafts() {

		BaseEntity user = beUtils.getUserBaseEntity();
		Ask ask = qwandaUtils.generateAskFromQuestionCode("QUE_DRAFTS_GRP", user, user);

		// configure msg and send
		QDataAskMessage msg = new QDataAskMessage(ask);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
	}

}
