package life.genny.kogito.common.service;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.graphql.ProcessData;
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
		// TODO
		// String randomCode = UUID.randomUUID().toString().substring(0,
		// 32).toUpperCase();
		// return commission(definitionCode, randomCode);

		if (definitionCode == null)
			throw new NullParameterException("definitionCode");

		if (!definitionCode.startsWith("DEF_"))
			throw new DebugException("Invalid definitionCode: " + definitionCode);

		// fetch the def baseentity
		BaseEntity def = beUtils.getBaseEntity(definitionCode);

		// use entity create function and save to db
		BaseEntity entity = beUtils.create(def);
		log.info("BaseEntity Created: " + entity.getCode());

		entity.setStatus(EEntityStatus.PENDING);
		beUtils.updateBaseEntity(entity);

		return entity.getCode();
	}

	public String commission(String definitionCode, String processId) {

		return commission(definitionCode, processId, EEntityStatus.PENDING);
	}

	public String commission(String definitionCode, String processId, EEntityStatus status) {

		if (definitionCode == null)
			throw new NullParameterException("definitionCode");
		if (processId == null)
			throw new NullParameterException("processId");
		if (status == null)
			throw new NullParameterException("status");
		if (!definitionCode.startsWith("DEF_"))
			throw new DebugException("Invalid definitionCode: " + definitionCode);

		// fetch the def baseentity
		BaseEntity def = beUtils.getBaseEntity(definitionCode);

		// use entity create function and save to db
		String defaultName = StringUtils.capitalize(def.getCode().substring(4));
		BaseEntity entity = beUtils.create(def, defaultName,
				def.getValueAsString("PRI_PREFIX") + "_" + processId.toUpperCase());
		log.info("BaseEntity Created: " + entity.getCode());

		entity.setStatus(status);
		beUtils.updateBaseEntity(entity);

		return entity.getCode();
	}

	public void delete(String code) {

		if (code == null)
			throw new NullParameterException("code");

		BaseEntity baseEntity = beUtils.getBaseEntity(code);
		log.info("Deleting entity using EEntityStatus" + baseEntity.getCode());

		// archive the entity
		baseEntity.setStatus(EEntityStatus.DELETED);
		beUtils.updateBaseEntity(baseEntity);
	}

	public void pendingDelete(String code) {

		if (code == null)
			throw new NullParameterException("code");

		BaseEntity baseEntity = beUtils.getBaseEntity(code);
		log.info("Pending Deleting entity using EEntityStatus" + baseEntity.getCode());

		// archive the entity
		baseEntity.setStatus(EEntityStatus.PENDING_DELETE);
		beUtils.updateBaseEntity(baseEntity);
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

	public void setActive(String entityCode) {

		BaseEntity entity = beUtils.getBaseEntity(entityCode);
		entity.setStatus(EEntityStatus.ACTIVE);
		beUtils.updateBaseEntity(entity);
	}

	public void setDisabled(String entityCode) {

		BaseEntity entity = beUtils.getBaseEntity(entityCode);
		entity.setStatus(EEntityStatus.DISABLED);
		beUtils.updateBaseEntity(entity);
	}

	public String getDEFPrefix(String definitionCode) {

		BaseEntity definition = beUtils.getBaseEntity(definitionCode);

		Optional<String> prefix = definition.getValue("PRI_PREFIX");
		if (prefix.isEmpty()) {
			throw new NullParameterException(definition.getCode() + ":PRI_PREFIX");
		}

		return prefix.get();
	}

	public String getBaseEntityQuestionGroup(String targetCode) {

		BaseEntity target = beUtils.getBaseEntity(targetCode);
		BaseEntity definition = defUtils.getDEF(target);

		if (definition == null) {
			throw new NullParameterException("DEF:" + targetCode);
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
	public void mergeFromProcessEntity(String entityCode, ProcessData processData) {

		BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
		BaseEntity entity = beUtils.getBaseEntity(entityCode);

		// iterate our stored process updates and create an answer
		for (EntityAttribute ea : processEntity.getBaseEntityAttributes()) {

			if (ea.getAttribute() == null) {
				log.debug("Attribute is null, fetching " + ea.getAttributeCode());
				Attribute attribute = qwandaUtils.getAttribute(ea.getAttributeCode());
				ea.setAttribute(attribute);
			}
			ea.setBaseEntityCode(entity.getCode());
			entity.addAttribute(ea);
		}

		// save these answrs to db and cache
		beUtils.updateBaseEntity(entity);
		log.info("Saved answers for entity " + entityCode);
	}

	/**
	 * Update entityAttributes
	 */
	public void updateBaseEntity(String baseEntityCode, String attributeCode, String value) {

		BaseEntity be = beUtils.getBaseEntity(baseEntityCode);
		BaseEntity defBe = defUtils.getDEF(be);

		Optional<EntityAttribute> defEAttribute = defBe.findEntityAttribute("ATT_" + attributeCode);
		if (defEAttribute.isPresent()) {

			if (attributeCode.startsWith("LNK_")) {
				// Check if value is in JsonArray format , otherwise wrap it..
				if (value != null) {
					if (!value.startsWith("[")) {
						value = "[\"" + value + "\"]";
					}
				}
			}

			be = beUtils.addValue(be, attributeCode, value);

			beUtils.updateBaseEntity(be);
		} else {
			log.error("This attribute is not defined in " + defBe.getCode() + " for the attribute: " + attributeCode);
		}
	}

	/**
	 * Update baseentity with setup attribute values
	 */
	public void setupBaseEntity(String baseEntityCode, String... attributeCodeValue) {

		BaseEntity be = beUtils.getBaseEntity(baseEntityCode);

		for (int i = 0; i < attributeCodeValue.length; i++) {
			String[] split = attributeCodeValue[i].split("=");
			String attributeCode = split[0];
			String value = split[1];
			if (attributeCode.startsWith("LNK_")) {
				// Check if value is in JsonArray format , otherwise wrap it..
				if (value != null) {
					if (!value.startsWith("[")) {
						value = "[\"" + value + "\"]";
					}
				}
			}
			be = beUtils.addValue(be, attributeCode, value);
		}

		beUtils.updateBaseEntity(be);
	}

}
