package life.genny.kogito.common.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.NoResultException;

import org.jboss.logging.Logger;

import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.Ask;

import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.datatype.capability.Capability;
import life.genny.qwandaq.datatype.capability.CapabilityMode;
import life.genny.qwandaq.datatype.capability.CapabilityNode;
import life.genny.qwandaq.datatype.capability.PermissionMode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;

import life.genny.qwandaq.exception.runtime.ItemNotFoundException;

import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;

import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.managers.capabilities.role.RoleManager;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataAttributeMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.DebugTimer;
import life.genny.qwandaq.utils.GraphQLUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;
import life.genny.serviceq.Service;

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
	private Service service;

	@Inject
	private DatabaseUtils databaseUtils;

	@Inject
	private BaseEntityUtils beUtils;

	@Inject
	private UserToken userToken;

	@Inject
	private QwandaUtils qwandaUtils;

	@Inject
	private KogitoUtils kogitoUtils;

	@Inject
	private GraphQLUtils gqlUtils;

	@Inject
	private SearchUtils searchUtils;

	@Inject
	private RoleManager roleMan;

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

		Integer TOTAL_PAGES = CacheUtils.getObject(productCode, "ATTRIBUTE_PAGES", Integer.class);

		for (int currentPage = 0; currentPage < TOTAL_PAGES + 1; currentPage++) {

			QDataAttributeMessage msg = CacheUtils.getObject(productCode,
					"ATTRIBUTES_P" + currentPage, QDataAttributeMessage.class);

			// set token and send
			msg.setToken(userToken.getToken());
			msg.setAliasCode("ATTRIBUTE_MESSAGE_" + (currentPage + 1) + "_OF_" + (TOTAL_PAGES + 1));
			KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
		}
	}

	private BaseEntity configureSidebar(BaseEntity sidebarPCM) {
		
		return sidebarPCM;
	}

	/**
	 * Send PCM BaseEntities.
	 */
	public void sendPCMs() {

		log.info("Sending PCMs for " + userToken.getProductCode());

		String productCode = userToken.getProductCode();
		BaseEntity user = beUtils.getUserBaseEntity();

		// get pcms using search
		SearchEntity searchEntity = new SearchEntity("SBE_PCMS", "PCM Search")
				.add(new Filter(Attribute.PRI_CODE, Operator.LIKE, "PCM_%"))
				.setAllColumns(true)
				.setPageSize(1000)
				.setRealm(productCode);

		log.info(jsonb.toJson(searchEntity));

		List<BaseEntity> pcms = searchUtils.searchBaseEntitys(searchEntity);
		if (pcms == null) {
			log.info("No PCMs found for " + productCode);
			return;
		}

		// TODO: Find a better way of doing this. This does not feel elegant
		// Perhaps we have a flag on whether or not to check capabilities on a question / BaseEntity?
		Optional<BaseEntity> sidebarPCMOpt = pcms.stream().filter((BaseEntity pcm) -> {
			return "PCM_SIDEBAR".equals(pcm.getCode());
		}).findFirst();

		if(!sidebarPCMOpt.isPresent())
			throw new ItemNotFoundException("PCM_SIDEBAR");
		BaseEntity sidebarPcm = sidebarPCMOpt.get();
		// Replace sidebar pcm
		pcms.remove(sidebarPcm);
		pcms.add(configureSidebar(sidebarPcm));

		log.info("Sending "+pcms.size()+" PCMs");

		// configure ask msg
		QDataAskMessage askMsg = new QDataAskMessage();
		askMsg.setToken(userToken.getToken());
		askMsg.setReplace(true);
		askMsg.setAliasCode("PCM_INIT_ASK_MESSAGE");

		for (BaseEntity pcm : pcms) {
			log.info("Processing " + pcm.getCode());
			String questionCode = pcm.getValue("PRI_QUESTION_CODE", null);
			if (questionCode == null) {
				log.warn("(" + pcm.getCode() + " :: " + pcm.getName() + ") null PRI_QUESTION_CODE");
				continue;
			}

			Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, user, user);
			if (ask == null) {
				log.warn("(" + pcm.getCode() + " :: " + pcm.getName() + ") No asks found for " + questionCode);
				continue;
			}
			askMsg.add(ask);
		}

		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, askMsg);

		// configure msg and send
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(pcms);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		msg.setAliasCode("PCM_INIT_MESSAGE");

		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
	}

	private Ask generateAddItemsAsk(String productCode, BaseEntity user) {
		// find the question in the database
		Question groupQuestion;
		try {
			groupQuestion = databaseUtils.findQuestionByCode(productCode, "QUE_ADD_ITEMS_GRP");
		} catch (NoResultException e) {
			throw new ItemNotFoundException("QUE_ADD_ITEMS_GRP", e);
		}

		Ask parentAsk = new Ask(groupQuestion);
		parentAsk.setSourceCode(user.getCode());
		parentAsk.setTargetCode(user.getCode());
		parentAsk.setRealm(productCode);

		Set<Capability> capabilities = capMan.getUserCapabilities();
		
		// Generate the Add Items asks from the capabilities
		// Check if there is a def first
		for(Capability capability : capabilities) {
			// If they don't have the capability then don't bother finding the def
			if(!capability.checkPerms(false, new CapabilityNode(CapabilityMode.ADD, PermissionMode.ALL)))
				continue;

			String defCode = CommonUtils.substitutePrefix(capability.code, "DEF");
			try {
				// Check for a def
				beUtils.getBaseEntity(productCode, defCode);
			} catch(ItemNotFoundException e) {
				// We don't need to handle this. We don't care if there isn't always a def
				continue;
			}
			// Create the ask (there is a def and we have the capability)
			String baseCode = CommonUtils.safeStripPrefix(capability.code);

			String eventCode = "EVT_ADD".concat(baseCode);
			String name = "Add ".concat(CommonUtils.normalizeString(baseCode));
			Attribute event = qwandaUtils.createEvent(eventCode, name);

			Question question = new Question("QUE_ADD_".concat(baseCode), name, event);

			Ask addAsk = new Ask(question);
			addAsk.setSourceCode(user.getCode());
			addAsk.setTargetCode(user.getCode());
			addAsk.setRealm(productCode);

			parentAsk.addChildAsk(addAsk);
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

		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
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
