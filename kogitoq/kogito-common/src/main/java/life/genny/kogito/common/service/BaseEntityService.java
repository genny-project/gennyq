package life.genny.kogito.common.service;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.exception.DebugException;
import life.genny.qwandaq.exception.NullParameterException;
import life.genny.qwandaq.exception.ItemNotFoundException;
import life.genny.qwandaq.exception.GennyRuntimeException;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.qwandaq.utils.QwandaUtils;

@ApplicationScoped
public class BaseEntityService {

	private static final Logger log = Logger.getLogger(BaseEntityService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	ServiceToken serviceToken;

	@Inject
	UserToken userToken;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	DefUtils defUtils;

	public String commission(String definitionCode) {

		if (definitionCode == null)
			throw new NullParameterException("definitionCode");
		if (!definitionCode.startsWith("DEF_"))
			throw new DebugException("Invalid definitionCode: " + definitionCode);

		// fetch the def baseentity
		BaseEntity def = beUtils.getBaseEntity(definitionCode);

		// use entity create function and save to db
		BaseEntity entity = beUtils.create(def);
		log.info("BaseEntity Created: " + entity.getCode());

		return entity.getCode();
	}

	public void decommission(String code) {

		if (code == null)
			throw new NullParameterException("code");

		BaseEntity baseEntity = beUtils.getBaseEntity(code);
		log.info("Decommissioning entity " + baseEntity.getCode());

		// archive the entity
		baseEntity.setStatus(EEntityStatus.ARCHIVED);
		beUtils.updateBaseEntity(baseEntity);
	}

	public String getDEFPrefix(String definitionCode) {

		BaseEntity definition = beUtils.getBaseEntity(definitionCode);

		Optional<String> prefix = definition.getValue("PRI_PREFIX");
		if (prefix.isEmpty()) {
			throw new GennyRuntimeException("definition " + definition.getCode() + " has no prefix attribute!");
		}

		return prefix.get();
	}

	public String getBaseEntityQuestionGroup(String targetCode) {

		BaseEntity target = beUtils.getBaseEntity(targetCode);
		BaseEntity definition = defUtils.getDEF(target);

		if (definition == null) {
			throw new GennyRuntimeException("No definition for target " + target);
		}

		return CommonUtils.replacePrefix(definition.getCode(), "QUE");
	}

	/**
	 * Update the email, firstname and lastname in keycloak
	 */
	public void updateKeycloak(String userCode) {

		BaseEntity user = beUtils.getBaseEntity(userCode);
		String email = user.getValue("PRI_EMAIL", null);
		String firstName = user.getValue("PRI_FIRSTNAME", null);
		String lastName = user.getValue("PRI_LASTNAME", null);

		// update user fields
		// NOTE: this could be turned into a single http request
		// Could make it a builder pattern to make it a single http request?
		KeycloakUtils.updateUserEmail(serviceToken, user, email);
		KeycloakUtils.updateUserField(serviceToken, user, "username", email);
		KeycloakUtils.updateUserField(serviceToken, user, "firstName", firstName);
		KeycloakUtils.updateUserField(serviceToken, user, "lastName", lastName);
	}

	/**
	 * Merge a process entity into another entity
	 */
	public void mergeFromProcessEntity(String entityCode, String processBEJson) {

		BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);
		BaseEntity entity = beUtils.getBaseEntity(entityCode);

		// iterate our stored process updates and create an answer
		for (EntityAttribute ea : processBE.getBaseEntityAttributes()) {

			if (ea.getAttribute() == null) {
				log.debug("Attribute is null, fetching " + ea.getAttributeCode());
				Attribute attribute = qwandaUtils.getAttribute(ea.getAttributeCode());
				ea.setAttribute(attribute);
			}
			ea.setBaseEntity(entity);
			entity.addAttribute(ea);
		}

		// save these answrs to db and cache
		beUtils.updateBaseEntity(entity);
		log.info("Saved answers for entity " + entityCode);
	}
}
