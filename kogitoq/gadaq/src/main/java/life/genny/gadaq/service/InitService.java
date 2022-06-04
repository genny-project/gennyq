package life.genny.gadaq.service;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataAttributeMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
// import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.serviceq.Service;
// import life.genny.fyodor.endpoints;

/**
 * A Service class used for Auth Init operations.
 *
 * @author Jasper Robison
 */
@ApplicationScoped
public class InitService {

	private static final Logger log = Logger.getLogger(InitService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	Service service;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	UserToken userToken;

	@Inject
	QwandaUtils qwandaUtils;

	// @Inject
	// QuestionUtils questionUtils;
	/**
	 * Send the Project BaseEntity.
	 */
	public void sendProject() {
		log.info("Running send project...");
		log.info("Attempting to find token " + userToken.getToken());
		log.info("Sending Project PRJ_" + userToken.getProductCode().toUpperCase());

		// grab baseentity for the project
		BaseEntity projectBE = databaseUtils.findBaseEntityByCode(userToken.getProductCode(),
				"PRJ_" + userToken.getProductCode().toUpperCase());
		// BaseEntity projectBE = beUtils.getProjectBaseEntity();

		// configure msg and send
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(projectBE);
		msg.setToken(userToken.getToken());
		msg.setAliasCode("PROJECT");
		log.info("Project BE base " + projectBE);
		log.info("Sending PRJ Message " + msg.getItems());
		KafkaUtils.writeMsg("webdata", msg);
	}

	/**
	 * Send the User.
	 */
	public void sendUser() {

		log.info("Sending User " + userToken.getUserCode());

		// fetch the users baseentity
		BaseEntity userBE = beUtils.getBaseEntityByCode(userToken.getUserCode());

		// configure msg and send
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(userBE);
		msg.setToken(userToken.getToken());
		msg.setAliasCode("USER");

		KafkaUtils.writeMsg("webdata", msg);
	}

	/**
	 * Send All attributes for the productCode.
	 */
	public void sendAllAttributes() {

		log.info("Sending Attributes for " + userToken.getProductCode());
		String productCode = userToken.getProductCode();

		// fetch bulk attribute msg from cache
		QDataAttributeMessage msg = CacheUtils.getObject(productCode, "ALL_ATTRIBUTES", QDataAttributeMessage.class);

		if (msg == null) {
			log.error("No attribute msg cached for " + productCode);
			return;
		}

		// set token and send
		msg.setToken(userToken.getToken());
		KafkaUtils.writeMsg("webdata", msg);
	}

	/**
	 * Send PCM BaseEntities.
	 */
	public void sendPCMs() {
		// sendProject();
		log.info("Sending PCMs for " + userToken.getProductCode());
		String productCode = userToken.getProductCode();

		// get pcms using search
		SearchEntity searchBE = new SearchEntity("SBE_PCMS", "PCM Search")
				.addSort("PRI_CREATED", "Created", SearchEntity.Sort.ASC)
				.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "PCM_%")
				.addColumn("*", "All Columns");

		searchBE.setRealm(productCode);
		List<BaseEntity> pcms = beUtils.getBaseEntitys(searchBE);
		// sendASKs(pcms.get(1));
		sendBulkASKs(pcms);
		// configure msg and send
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(pcms);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		KafkaUtils.writeMsg("webdata", msg);
	}

	/**
	 * Send Add Items Menu
	 */
	public void sendAddItems() {

		// send basic add items
		String sourceCode = userToken.getUserCode();
		String targetCode = userToken.getUserCode();

		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));
		Attribute eventAttribute = new Attribute("PRI_EVENT", "link", new DataType(String.class));

		/* ADD ITEMS group */
		Question addItemsQues = new Question("QUE_ADD_ITEMS_GRP", "Add Items", questionAttribute, true);
		Ask addItemsAsk = new Ask(addItemsQues, sourceCode, targetCode);

		Question customerQues = new Question("QUE_CUSTOMER_MENU", "Add Customer", eventAttribute, true);
		Ask customerAsk = new Ask(customerQues, sourceCode, targetCode);

		Question internQues = new Question("QUE_QA_INTERN_MENU", "Intern", eventAttribute, true);
		Ask internAsk = new Ask(internQues, sourceCode, targetCode);

		Ask[] children = { customerAsk, internAsk };

		addItemsAsk.setChildAsks(children);

		// configure msg and send
		QDataAskMessage msg = new QDataAskMessage(addItemsAsk);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		KafkaUtils.writeMsg("webdata", msg);
	}

	/**
	 * Send asks [rudimentary and terrible]
	 */
	public void sendBulkASKs(List<BaseEntity> entities) {
		entities.forEach(entity -> {
			log.info("Sending entity " + entity);
			sendASKs(entity);
		});
		// sendASKs(entity);
	}

	public void getASKs(EntityAttribute attribute, BaseEntity entity) {
		// log.info("Got attribute " + attribute);
		// log.info("questionCode :" + attribute.getValueString());
		// log.info("sourceCode :" + attribute.getBaseEntityCode());
		// log.info("targetCode :" + entity.getCode());
		// log.info("processId :" + entity.getId());
		// String questionCode = userToken.getUserCode();
		// BaseEntity source =
		// beUtils.getBaseEntityByCode(attribute.getBaseEntityCode());
		// BaseEntity target =
		// beUtils.getBaseEntityByCode(attribute.getBaseEntityCode());

		// log.info("Fetching asks -> " + questionCode + ":" + entity.getCode() + ":" +
		// entity.getCode());
		// if(attribute.getValueString() == null){
		// log.info("Value is null");
		// attribute.setValueString("");
		// }
		// log.info("Attribute " + attribute);
		// if(!attribute.getValueString().startsWith("QUE_") ||
		// attribute.getValue().getClass() != String.class){
		// log.info("Break! " + attribute.getValueString());
		// } else {

		// log.info("Getting question with code " + attribute.getValueString());
		// Question rootQuestion = qwandaUtils.getQuestion(attribute.getValueString());
		// List<Ask> asks =
		// qwandaUtils.generateAskFromQuestionCode(attribute.getValueString(), entity,
		// entity);
		// log.info("Got question " + rootQuestion);
		// log.info("Got asks " + asks);
		// create ask msg from asks11
		// QDataAskMessage msg = new QDataAskMessage(asks.toArray(new
		// Ask[asks.size()]));
		// msg.setToken(userToken.getToken());
		// msg.setReplace(true);

		// // TODO: make this recursive
		// // update the processId
		// for (Ask ask : msg.getItems()) {
		// ask.setProcessId(entity.getId().toString());
		// }

		// KafkaUtils.writeMsg("webdata", msg);}
	}

	public void sendASKs(BaseEntity entity) {
		log.info("Sending asks for " + userToken.getProductCode());
		log.info("Entity is " + entity.getBaseEntityAttributes());

		List<Ask> asks = new ArrayList<>();
		entity.getBaseEntityAttributes().forEach(attribute -> {
			System.out.println("Attribute Value " + attribute.getValueString());
			if (attribute.getValue().getClass() == String.class && attribute.getValueString().startsWith("QUE_")) {
				try {
					Ask ask = qwandaUtils.generateAskFromQuestionCode(attribute.getValueString(), entity, entity);
					asks.add(ask);
				} catch (Exception e) {
					log.info("Could not find ASK " + attribute.getValueString() + e);
				}

			}
		});
		log.info("Asks " + asks);
		QDataAskMessage msg = new QDataAskMessage(asks.toArray(new Ask[asks.size()]));
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		for (Ask ask : msg.getItems()) {
			ask.setProcessId(entity.getId().toString());
		}
		List<EntityAttribute> attributeList = new ArrayList<>(entity.getBaseEntityAttributes());
		// EntityAttribute attribute = attributeList.get(1);
		// attributeList.forEach(attribute -> {
		// getASKs(attribute, entity);
		// });
		KafkaUtils.writeMsg("webdata", msg);
		// return msg;
	}
}
