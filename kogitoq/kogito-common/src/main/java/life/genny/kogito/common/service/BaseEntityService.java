package life.genny.kogito.common.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QwandaUtils;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QCmdMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.qwandaq.utils.QwandaUtils;

@ApplicationScoped
public class BaseEntityService {

	private static final Logger log = Logger.getLogger(BaseEntityService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	ServiceToken serviceToken;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	BaseEntityUtils beUtils;
	
	public String commission(String definitionCode) {

		if (definitionCode == null || !definitionCode.startsWith("DEF_")) {
			log.error("Invalid definitionCode: " + definitionCode);
			return null;
		}

		// fetch the def baseentity
		BaseEntity def = beUtils.getBaseEntityByCode(definitionCode);
		if(def == null) {
			log.error("Could not find DEF BaseEntity with code: " + definitionCode);
		}

		// use entity create function and save to db
		try {
			BaseEntity entity = beUtils.create(def);
			log.info("BaseEntity Created: " + entity.getCode());

			return entity.getCode();

		} catch (Exception e) {
			log.error("Error creating BaseEntity! DEF Code: " + definitionCode);
			e.printStackTrace();
		}

		return null;
	}

	public void decommission(String code) {

		BaseEntity baseEntity = beUtils.getBaseEntityByCode(code);

		if (baseEntity == null) {
			log.error("BaseEntity " + code + " is null!");
			return;
		}

		log.info("Decommissioning entity " + baseEntity.getCode());

		// archive the entity
		baseEntity.setStatus(EEntityStatus.ARCHIVED);
		beUtils.updateBaseEntity(baseEntity);
	}

	public String getDEFPrefix(String definitionCode) {

		BaseEntity definition = beUtils.getBaseEntityByCode(definitionCode);

		if (definition == null) {
			log.error("No definition exists with code " + definitionCode);
			return null;
		}

		String prefix = definition.getValue("PRI_PREFIX", null);

		if (prefix == null) {
			log.error("definition " + definition.getCode() + " has no prefix attribute!");
		}

		return prefix;
	}

	public void updateKeycloak(String userCode) {

		BaseEntity user = beUtils.getBaseEntityByCode(userCode);
		String email = user.getValue("PRI_EMAIL", null);
		String firstName = user.getValue("PRI_FIRSTNAME", null);
		String lastName = user.getValue("PRI_LASTNAME", null);

		// update user fields
		// NOTE: this could be turned into a single http request
		KeycloakUtils.updateUserEmail(serviceToken, user, email);
		KeycloakUtils.updateUserField(serviceToken, user, "username", email);
		KeycloakUtils.updateUserField(serviceToken, user, "firstName", firstName);
		KeycloakUtils.updateUserField(serviceToken, user, "lastName", lastName);
	}
}
