package life.genny.gadaq.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QDataAttributeMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.serviceq.Service;

/**
 * A Service class used for Auth Init operations.
 *
 * @author Jasper Robison
 */
@ApplicationScoped
public class InitService {

    private static final Logger log = Logger.getLogger(InitService.class);

    @Inject
    Service service;

    @Inject
    DatabaseUtils databaseUtils;

	/**
	* Send the Project BaseEntity.
	*
	* @param userToken The GennyToken of the user
	 */
	public void sendProject(String token) {

		log.info("Sending Project");

		GennyToken userToken = new GennyToken(token);
		BaseEntity projectBE = service.getBeUtils().getProjectBaseEntity();

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(projectBE);
		msg.setToken(userToken.getToken());
		msg.setAliasCode("PROJECT");

		log.info("Sending Project down kafka");

		KafkaUtils.writeMsg("webdata", msg);
	}

	/**
	* Send the User.
	*
	* @param userToken The GennyToken of the user
	 */
	public void sendUser(String token) {

		log.info("Sending User");

		GennyToken userToken = new GennyToken(token);
		BaseEntity userBE = service.getBeUtils().getBaseEntityByCode(userToken.getCode());

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(userBE);
		msg.setToken(userToken.getToken());
		msg.setAliasCode("USER");

		KafkaUtils.writeMsg("webdata", msg);
	}

	/**
	* Send All attributes for the realm.
	*
	* @param userToken The GennyToken of the user
	 */
	public void sendAllAttributes(String token) {

		log.info("Sending Attributes");

		GennyToken userToken = new GennyToken(token);
		String realm = userToken.getRealm();

		QDataAttributeMessage msg = CacheUtils.getObject(realm, "ALL_ATTRIBUTES", QDataAttributeMessage.class);
		msg.setToken(userToken.getToken());

		KafkaUtils.writeMsg("webdata", msg);
	}

	/**
	* Send PCM BaseEntities.
	*
	* @param userToken The GennyToken of the user
	 */
	public void sendPCMs(String token) {

		log.info("Sending PCMs");

		GennyToken userToken = new GennyToken(token);

		// get pcms using search
		SearchEntity searchBE = new SearchEntity("SBE_PCMS", "PCM Search")
			.addSort("PRI_CREATED", "Created", SearchEntity.Sort.ASC)
			.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "PCM_%")
			.addColumn("*", "All Columns");

		searchBE.setRealm(userToken.getRealm());
		List<BaseEntity> pcms = service.getBeUtils().getBaseEntitys(searchBE);

		// send to frontend
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(pcms);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		KafkaUtils.writeMsg("webdata", msg);
	}

}
