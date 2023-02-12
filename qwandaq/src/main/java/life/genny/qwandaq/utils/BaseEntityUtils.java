package life.genny.qwandaq.utils;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.exception.runtime.DefinitionException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.attribute.EntityAttribute;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.Nullable;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static life.genny.qwandaq.attribute.Attribute.*;

/**
 * A non-static utility class used for standard
 * operations involving BaseEntitys.
 * 
 * @author Adam Crow
 * @author Jasper Robison
 */
@ApplicationScoped
@ActivateRequestContext
public class BaseEntityUtils {

	Jsonb jsonb = JsonbBuilder.create();

	private static Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@Inject
	ServiceToken serviceToken;

	@Inject
	UserToken userToken;

	@Inject
	CacheManager cm;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	EntityAttributeUtils beaUtils;

	public BaseEntityUtils() {
	}

	public BaseEntityUtils(ServiceToken serviceToken, UserToken userToken) {
		this.serviceToken = serviceToken;
		this.userToken = userToken;
	}

	public BaseEntityUtils(ServiceToken serviceToken) {
		this.serviceToken = serviceToken;
		this.userToken = new UserToken(serviceToken.getToken());
	}

	public void setUserToken(UserToken userToken) {
		this.userToken = userToken;
	}

	/**
	 * Fetch the user base entity of the {@link UserToken}.
	 * 
	 * @return the user {@link BaseEntity}
	 */
	public BaseEntity getProjectBaseEntity() {
		return getBaseEntity(Prefix.PRJ.concat(userToken.getProductCode().toUpperCase()));
	}

	/**
	 * Fetch the user base entity of the {@link UserToken}
	 * 
	 * @return the user's {@link BaseEntity}
	 */
	public BaseEntity getUserBaseEntity() {
		return getBaseEntity(userToken.getUserCode());
	}

	/**
	 * Get a PCM using a code, but throw an
	 * ItemNotFoundException if the entity does not exist.

	 * @param code
	 * @return
	 */
	public PCM getPCM(String code) {
		return PCM.from(getBaseEntity(code, true));
	}

	/**
	 * Get a Definition using a code, but throw an
	 * ItemNotFoundException if the entity does not exist.
	 * 
	 * @param code
	 * @return
	 */
	public Definition getDefinition(String code) {
		BaseEntity baseEntity = getBaseEntity(code, true);
		Definition definition = Definition.from(baseEntity);
		definition.setBaseEntityAttributes(beaUtils.getAllEntityAttributesForBaseEntity(baseEntity));
		return definition;
	}

	/**
	 * Get a base entity using a code, but throw an
	 * ItemNotFoundException if the entity does not exist.
	 * 
	 * @param code The code of the entity to fetch
	 * @return The BaseEntity
	 */
	public BaseEntity getBaseEntity(String code) {
		return getBaseEntity(userToken.getProductCode(), code); // watch out for no userToken
	}

	/**
	 * Get a base entity using a code, but throw an
	 * ItemNotFoundException if the entity does not exist.
	 *
	 * @param code The code of the entity to fetch
	 * @return The BaseEntity
	 */
	public BaseEntity getBaseEntity(String code, boolean bundleAttributes) {
		return getBaseEntity(userToken.getProductCode(), code, bundleAttributes); // watch out for no userToken
	}

	/**
	 * Get a base entity using a code, but throw an
	 * ItemNotFoundException if the entitiy does not exist.
	 * 
	 * @param productCode The product to search in
	 * @param code        The code of the entity to fetch
	 * @return The BaseEntity
	 */
	public BaseEntity getBaseEntity(String productCode, String code) {
		return getBaseEntity(productCode, code, false);
	}

	/**
	 * Get a base entity using a code, but throw an
	 * ItemNotFoundException if the entitiy does not exist.
	 *
	 * @param productCode
	 * @param code
	 * @param bundleAttributes
	 * @return
	 */
	public BaseEntity getBaseEntity(String productCode, String code, boolean bundleAttributes) {
		// fetch entity
		BaseEntityKey key = new BaseEntityKey(productCode, code);
		BaseEntity baseEntity = (BaseEntity) cm.getPersistableEntity(CacheManager.CACHE_NAME_BASEENTITY, key);
		if (baseEntity == null) {
			throw new ItemNotFoundException(productCode, code);
		}
		// fetch entity attributes
		if (bundleAttributes) {
			List<EntityAttribute> entityAttributesForBaseEntity = beaUtils.getAllEntityAttributesForBaseEntity(productCode, code);
			if(entityAttributesForBaseEntity.isEmpty()) {
				log.infof("No BaseEntityAttributes found for base entity [%s:%s]", productCode, code);
				//throw new ItemNotFoundException();
			} else {
				log.debugf("%s BaseEntityAttributes found for base entity [%s:%s]. Setting them to BE...", entityAttributesForBaseEntity.size(), productCode, code);
			}
			baseEntity.setBaseEntityAttributes(entityAttributesForBaseEntity);
			log.debugf("Added %s BaseEntityAttributes to BE [%s:%s]", baseEntity.getBaseEntityAttributesMap().size(), baseEntity.getRealm(), baseEntity.getCode());
		}
		return baseEntity;
	}

	/**
	 * Update a {@link BaseEntity} in the database and the cache.
	 *
	 * @param baseEntity  The BaseEntity to update
	 * @return the newly cached BaseEntity
	 */
	public BaseEntity updateBaseEntity(BaseEntity baseEntity) {
		return updateBaseEntity(baseEntity, true);
	}

	/**
	 * Update a {@link BaseEntity} in the database and the cache.
	 *
	 * @param baseEntity The BaseEntity to update
	 * @param updateBaseEntityAttributes  Defines whether the BaseEntityAttributes need to be updated
	 * @return the newly cached BaseEntity
	 */
	public BaseEntity updateBaseEntity(BaseEntity baseEntity, boolean updateBaseEntityAttributes) {
		BaseEntityKey key = new BaseEntityKey(baseEntity.getRealm(), baseEntity.getCode());
		boolean savedSuccessfully = cm.saveEntity(CacheManager.CACHE_NAME_BASEENTITY, key, baseEntity);
		if (updateBaseEntityAttributes) {
			beaUtils.getAllEntityAttributesForBaseEntity(baseEntity).forEach(bea -> {
				// ensure for all entityAttribute that baseentity and attribute are not null
				if (bea.getRealm() == null) {
					bea.setRealm(baseEntity.getRealm());
				}
				if (bea.getBaseEntityCode() == null) {
					bea.setBaseEntityCode(baseEntity.getCode());
				}
				if (bea.getAttribute() == null) {
					Attribute attribute = cm.getAttribute(baseEntity.getRealm(), bea.getAttributeCode());
					bea.setAttribute(attribute);
				}
				beaUtils.updateEntityAttribute(bea);
			});
		}
		return savedSuccessfully ? baseEntity : null;
	}

	/**
	 * Get the BaseEntity that is linked with a specific attribute.
	 *
	 * @param baseEntity    The targeted BaseEntity
	 * @param attributeCode The attribute storing the data
	 * @return The BaseEntity with code stored in the attribute
	 */
	public BaseEntity getBaseEntityFromLinkAttribute(BaseEntity baseEntity, String attributeCode) {
		return getBaseEntityFromLinkAttribute(baseEntity, attributeCode, false);
	}

	/**
	 * Get the BaseEntity that is linked with a specific attribute.
	 *
	 * @param baseEntity    The targeted BaseEntity
	 * @param attributeCode The attribute storing the data
	 * @return The BaseEntity with code stored in the attribute
	 */
	public BaseEntity getBaseEntityFromLinkAttribute(BaseEntity baseEntity, String attributeCode, boolean bundleAttributes) {
		String newBaseEntityCode = CommonUtils.cleanUpAttributeValue(getStringValueOfAttribute(baseEntity, attributeCode));
		try {
			return getBaseEntity(newBaseEntityCode, bundleAttributes);
		} catch (ItemNotFoundException e) {
			log.error(ANSIColour.RED + "Could not find entity: " + newBaseEntityCode + ANSIColour.RESET);
			return null;
		}
	}

	/**
	 * Get the code of the BaseEntity that is linked with a specific attribute.
	 *
	 * @param baseEntity    The targeted BaseEntity
	 * @param attributeCode The attribute storing the data
	 * @return The BaseEntity code stored in the attribute
	 */
	public String getBaseEntityCodeFromLinkAttribute(BaseEntity baseEntity, String attributeCode) {
		String attributeValue = getStringValueOfAttribute(baseEntity, attributeCode);
		if (attributeValue == null || !(attributeValue instanceof String))
			return null;
		return CommonUtils.cleanUpAttributeValue(attributeValue);
	}

	public String getStringValueOfAttribute(BaseEntity baseEntity, String attributeCode) {
		return getAttributeValue(baseEntity, attributeCode);
	}

	@Nullable
	private <T> T getAttributeValue(BaseEntity baseEntity, String attributeCode) {
		EntityAttribute entityAttribute = beaUtils.getEntityAttribute(baseEntity.getRealm(), baseEntity.getCode(), attributeCode);
		if (entityAttribute == null) {
			return null;
		}
		return entityAttribute.getValue();
	}

	/**
	 * Get an ArrayList of BaseEntity codes that are linked with a specific
	 * attribute.
	 *
	 * @param baseEntityCode The targeted BaseEntity Code
	 * @param attributeCode  The attribute storing the data
	 * @return An ArrayList of codes stored in the attribute
	 */
	public List<String> getBaseEntityCodeArrayFromLinkAttribute(String baseEntityCode, String attributeCode) {

		BaseEntity be = getBaseEntity(baseEntityCode);
		return getBaseEntityCodeArrayFromLinkAttribute(be, attributeCode);
	}

	/**
	 * Get an ArrayList of BaseEntity codes that are linked with a specific
	 * attribute.
	 *
	 * @param baseEntity    The targeted BaseEntity
	 * @param attributeCode The attribute storing the data
	 * @return An ArrayList of codes stored in the attribute
	 */
	public List<String> getBaseEntityCodeArrayFromLinkAttribute(BaseEntity baseEntity, String attributeCode) {

		String attributeValue = getBaseEntityCodeFromLinkAttribute(baseEntity, attributeCode);
		if (attributeValue == null) {
			return null;
		}

		String[] baseEntityCodeArray = attributeValue.split(",");
		List<String> beCodeList = Arrays.asList(baseEntityCodeArray);
		return beCodeList;
	}

	/**
	 * Get the value of an EntityAttribute as an Object.
	 *
	 * @param baseEntityCode The code of the entity to grab from
	 * @param attributeCode  The code of the attribute to check
	 * @return The value as an Object
	 */
	public Object getBaseEntityValue(final String baseEntityCode, final String attributeCode) {
		BaseEntity be = getBaseEntity(baseEntityCode);
		if (be != null) {
			Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
			if (ea.isPresent())
				return ea.get().getObject();
		}
		return null;
	}

	/**
	 * Get the value of an EntityAttribute as a String.
	 *
	 * @param be            The code of the entity to grab from
	 * @param attributeCode The code of the attribute to check
	 * @return The value as a String
	 */
	public static String getBaseEntityAttrValueAsString(BaseEntity be, String attributeCode) {
		if (be == null)
			return null;
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getObjectAsString();
		}
		return null;
	}

	/**
	 * Get the value of an EntityAttribute as a String.
	 *
	 * @param baseEntityCode The code of the entity to grab from
	 * @param attributeCode  The code of the attribute to check
	 * @return The value as a String
	 */
	public String getBaseEntityValueAsString(final String baseEntityCode, final String attributeCode) {

		BaseEntity be = getBaseEntity(baseEntityCode);
		return getBaseEntityAttrValueAsString(be, attributeCode);
	}

	/**
	 * Get the value of an EntityAttribute as a LocalDateTime.
	 *
	 * @param baseEntityCode The code of the entity to grab from
	 * @param attributeCode  The code of the attribute to check
	 * @return The value as a LocalDateTime
	 */
	public LocalDateTime getBaseEntityValueAsLocalDateTime(final String baseEntityCode, final String attributeCode) {

		BaseEntity be = getBaseEntity(baseEntityCode);
		EntityAttribute ea = (EntityAttribute) beaUtils.getEntityAttribute(be.getRealm(), baseEntityCode, attributeCode).toSerializableCoreEntity();
		if (ea != null) {
			return ea.getValueDateTime();
		}
		return null;
	}

	/**
	 * Get the value of an EntityAttribute as a LocalDate.
	 *
	 * @param baseEntityCode The code of the entity to grab from
	 * @param attributeCode  The code of the attribute to check
	 * @return The value as a LocalDate
	 */
	public LocalDate getBaseEntityValueAsLocalDate(final String baseEntityCode, final String attributeCode) {
		BaseEntity be = getBaseEntity(baseEntityCode);
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getValueDate();
		}
		return null;
	}

	/**
	 * Get the value of an EntityAttribute as a LocalTime.
	 *
	 * @param baseEntityCode The code of the entity to grab from
	 * @param attributeCode  The code of the attribute to check
	 * @return The value as a LocalTime
	 */
	public LocalTime getBaseEntityValueAsLocalTime(final String baseEntityCode, final String attributeCode) {

		BaseEntity be = getBaseEntity(baseEntityCode);
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getValueTime();
		}
		return null;
	}

	/**
	 * Convert a stringified list of BaseEntity codes into a list of BaseEntity
	 * objects.
	 *
	 * @param strArr The stringified array to convert
	 * @return A list of BaseEntitys
	 */
	public List<BaseEntity> convertCodesToBaseEntityArray(String strArr) {

		String[] arr = strArr.replace("\"", "").replace("[", "").replace("]", "").replace(" ", "").split(",");
		List<BaseEntity> entityList = Arrays.stream(arr)
				.filter(item -> !item.isEmpty())
				.map(item -> (BaseEntity) getBaseEntity(item))
				.collect(Collectors.toList());

		return entityList;
	}

	/**
	 * Apply the privacy filter to a BaseEntity.
	 * 
	 * @param entity  The be to apply the filter to
	 * @param allowed The set of allowed attribute codes
	 * @return The filtered BaseEntity
	 */
	public BaseEntity privacyFilter(BaseEntity entity, Set<String> allowed) {
		// Filter out unwanted attributes
		BaseEntity clonedBE = entity.clone(true);
		Iterator<Map.Entry<String, EntityAttribute>> entityAttributesIterator = clonedBE.getBaseEntityAttributesMap().entrySet().iterator();
		while (entityAttributesIterator.hasNext()) {
			Map.Entry<String, EntityAttribute> entry = entityAttributesIterator.next();
			if (!allowed.contains(entry.getValue().getAttributeCode())) {
				entityAttributesIterator.remove();
			}
		}
		return clonedBE;
	}

	// We can parameterise this but there is no point if we aren't going to have any
	// more NonLiteralAttributes than what is already here - Bryn
	/**
	 * Add all non literal attributes to the baseentity.
	 * 
	 * @param entity The entity to update
	 * @return The updated BaseEntity
	 */
	public BaseEntity addNonLiteralAttributes(BaseEntity entity) {

		// Handle Created and Updated attributes
		Attribute createdAttr = new Attribute(PRI_CREATED, "Created", new DataType(LocalDateTime.class));
		EntityAttribute created = new EntityAttribute(entity, createdAttr, 1.0, null);

		if (entity.getCreated() == null) {
			log.error("NPE for PRI_CREATED. Generating created date");
			entity.autocreateCreated();
		}

		// Ensure createdDate is not null
		created.setValueDateTime(entity.getCreated());
		entity.addAttribute(created);

		Attribute createdDateAttr = new Attribute(PRI_CREATED_DATE, "Created", new DataType(LocalDate.class));
		EntityAttribute createdDate = new EntityAttribute(entity, createdDateAttr, 1.0, null);

		// Ensure createdDate is not null
		createdDate.setValueDate(entity.getCreated().toLocalDate());
		entity.addAttribute(createdDate);

		Attribute updatedAttr = new Attribute(PRI_UPDATED, "Updated", new DataType(LocalDateTime.class));
		EntityAttribute updated = new EntityAttribute(entity, updatedAttr, 1.0, null);
		try {
			updated.setValueDateTime(entity.getUpdated());
			entity.addAttribute(updated);

			Attribute updatedDateAttr = new Attribute(PRI_UPDATED_DATE, "Updated", new DataType(LocalDate.class));
			EntityAttribute updatedDate = new EntityAttribute(entity, updatedDateAttr, 1.0, null);
			updatedDate.setValueDate(entity.getUpdated().toLocalDate());
			entity.addAttribute(updatedDate);
		} catch (NullPointerException e) {
			log.error("NPE for PRI_UPDATED");
		}

		Attribute codeAttr = new Attribute(PRI_CODE, "Code", new DataType(String.class));
		EntityAttribute code = new EntityAttribute(entity, codeAttr, 1.0, null);
		code.setValueString(entity.getCode());
		entity.addAttribute(code);

		Attribute nameAttr = new Attribute(PRI_NAME, "Name", new DataType(String.class));
		EntityAttribute name = new EntityAttribute(entity, nameAttr, 1.0, null);
		name.setValueString(entity.getName());
		entity.addAttribute(name);

		return entity;
	}

	/**
	 * Create a new {@link BaseEntity} using a DEF entity.
	 *
	 * @param definition The def entity to use
	 * @return The created BaseEntity
	 */
	public BaseEntity create(final Definition definition) {
		return create(definition, null, null);
	}

	/**
	 * Create a new {@link BaseEntity} using a DEF entity and a name.
	 *
	 * @param definition The def entity to use
	 * @param name  The name of the entity
	 * @return The created BaseEntity
	 */
	public BaseEntity create(final Definition definition, String name) {
		return create(definition, name, null);
	}

	/**
	 * Create a new {@link BaseEntity} using a name and code.
	 *
	 * @param definition The def entity to use
	 * @param name  The name of the entity
	 * @param code  The code of the entity
	 * @return The created BaseEntity
	 */
	public BaseEntity create(final Definition definition, String name, String code) {

		if (definition == null)
			throw new NullParameterException("definition");
		if (code != null && code.charAt(3) != '_')
			throw new DebugException("Code parameter " + code + " is not a valid BE code!");

		BaseEntity item = null;
		String productCode = definition.getRealm();
		String definitionCode = definition.getCode();
		EntityAttribute uuidEA = beaUtils.getEntityAttribute(productCode, definitionCode, Prefix.ATT.concat(Attribute.PRI_UUID));
		if (uuidEA != null) {
			item = createUser(definition);
		}
		else {
			EntityAttribute prefixAttr = beaUtils.getEntityAttribute(productCode, definitionCode, PRI_PREFIX, false);
			if(prefixAttr == null) {
				throw new DefinitionException("No prefix set for the def: " + definitionCode);
			}
			String prefix = prefixAttr.getValueString();
			if (StringUtils.isBlank(prefix))
				throw new DefinitionException("No prefix set for the def: " + definitionCode);

			if (StringUtils.isBlank(code))
				code = prefix + "_" + UUID.randomUUID().toString().substring(0, 32).toUpperCase();
			if (StringUtils.isBlank(name))
				name = definition.getName();

			item = new BaseEntity(code.toUpperCase(), name);
			item.setRealm(productCode);
			//item.addAttribute(new EntityAttribute());
		}

		// save to DB and cache
		updateBaseEntity(item);

		List<EntityAttribute> atts = beaUtils.getBaseEntityAttributesForBaseEntityWithAttributeCodePrefix(productCode, definitionCode, Prefix.ATT);
		for (EntityAttribute ea : atts) {
			String attrCode = ea.getAttributeCode().substring("ATT_".length());
			Attribute attribute = cm.getAttribute(productCode, attrCode);

			if (attribute == null) {
				log.warn("No Attribute found for def attr " + attrCode);
				continue;
			}
			if (item.containsEntityAttribute(attribute.getCode())) {
				log.info(item.getCode() + " already has value for " + attribute.getCode());
				continue;
			}

			// Find any default val for this Attr
			String defaultDefValueAttr = Prefix.DFT.concat(attrCode);
			Object defaultVal = definition.getValue(defaultDefValueAttr, attribute.getDefaultValue());

			// Only process mandatory attributes, or defaults
			Boolean mandatory = ea.getValueBoolean();
			if (mandatory == null) {
				mandatory = false;
				log.warn("**** DEF attribute ATT_" + attrCode + " has no mandatory boolean set in "
						+ definitionCode);
			}
			// Only process mandatory attributes, or defaults
			if (mandatory || defaultVal != null) {
				EntityAttribute newEA = new EntityAttribute(item, attribute, ea.getWeight(), defaultVal);
				newEA.setRealm(userToken.getProductCode());
				log.info("Adding mandatory/default -> " + attribute.getCode());
				item.addAttribute(newEA);
			}
		}

		Attribute linkDef = cm.getAttribute(Attribute.LNK_DEF);
		item.addAnswer(new Answer(item, item, linkDef, "[\"" + definitionCode + "\"]"));

		// author of the BE
		Attribute lnkAuthorAttr = cm.getAttribute(Attribute.LNK_AUTHOR);
		item.addAnswer(new Answer(item, item, lnkAuthorAttr, "[\"" + userToken.getUserCode() + "\"]"));

		updateBaseEntity(item);

		return item;
	}

	/**
	 * Create a new user {@link BaseEntity} using a DEF entity.
	 *
	 * @param definition The def entity to use
	 * @param email The email to use
	 * @return The created BaseEntity
	 */
	public BaseEntity createUser(final Definition definition) {

		String email = "random+" + UUID.randomUUID().toString().substring(0, 20) + "@gada.io";

		Optional<EntityAttribute> opt = definition.findEntityAttribute(Prefix.ATT.concat(Attribute.PRI_UUID));
		if (opt.isEmpty())
			throw new DefinitionException("Definition is not a User definition: " + definition.getCode());

		// generate keycloak id
		String uuid = KeycloakUtils.createDummyUser(serviceToken, userToken.getKeycloakRealm());

		// check definition prefix
		Optional<String> optCode = definition.getValue(Attribute.PRI_PREFIX);
		if (optCode.isEmpty())
			throw new DebugException("Prefix not provided" + definition.getCode());

		String name = definition.getName();
		String code = optCode.get() + "_" + uuid.toUpperCase();
		BaseEntity item = new BaseEntity(code, name);
		item.setRealm(userToken.getProductCode());

		// add email and username
		if (!email.startsWith("random+")) {
			// Check to see if the email exists
			// TODO: check to see if the email exists in the database and keycloak
			Attribute emailAttribute = cm.getAttribute(Attribute.PRI_EMAIL);
			item.addAnswer(new Answer(item, item, emailAttribute, email));
			Attribute usernameAttribute = cm.getAttribute(Attribute.PRI_USERNAME);
			item.addAnswer(new Answer(item, item, usernameAttribute, email));
		}

		// add PRI_UUID
		Attribute uuidAttribute = cm.getAttribute(Attribute.PRI_UUID);
		item.addAnswer(new Answer(item, item, uuidAttribute, uuid.toUpperCase()));

		return item;
	}

	/**
	 * Add a new attributeCode and value to a baseentity.
	 *
	 * @param be            The baseentity to use
	 * @param attributeCode The attributeCode to use
	 * @param value         The value to use
	 *
	 * @return The updated BaseEntity
	 */
	public BaseEntity addValue(final BaseEntity be, final String attributeCode, final String value) {

		if (be == null) {
			throw new NullParameterException("be");
		}
		if (attributeCode == null) {
			throw new NullParameterException("attributeCode");
		}

		Attribute attribute = cm.getAttribute(attributeCode);

		EntityAttribute ea = new EntityAttribute(be, attribute, 1.0, value);
		be.addAttribute(ea);
		return be;
	}

}
