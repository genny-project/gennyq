package life.genny.qwandaq.utils;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.ECacheRef;
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
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static life.genny.qwandaq.attribute.Attribute.*;

/**
 * A non-static utility class used for standard
 * operations involving BaseEntitys.
 * 
 * @author Adam Crow
 * @author Jasper Robison
 */
@ApplicationScoped
public class BaseEntityUtils {

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	Logger log;

	@Inject
	ServiceToken serviceToken;

	@Inject
	UserToken userToken;

	@Inject
	CacheManager cm;

	@Inject
	EntityAttributeUtils beaUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	AttributeUtils attributeUtils;

	@Inject
	DefUtils defUtils;

	public BaseEntityUtils() { 
	}

	/**
	 * Fetch the user base entity of the {@link UserToken}.
	 * 
	 * @return the user {@link BaseEntity}
	 */
	public BaseEntity getProjectBaseEntity() {
		return getBaseEntity(Prefix.PRJ_.concat(userToken.getProductCode().toUpperCase()), true);
	}

	/**
	 * Fetch the user base entity of the {@link UserToken}
	 * 
	 * @return the user's {@link BaseEntity}
	 */
	public BaseEntity getUserBaseEntity() {
		return getBaseEntity(userToken.getProductCode(), userToken.getUserCode(), true);
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
	 * @param code the code of the {@link Definition} BaseEntity
	 * @return a Definition with all entity attributes based on the base entity found with the code
	 * 
	 * @throws {@link ItemNotFoundException} if entity linked to code is not persisted/cannot be found
	 */
	public Definition getDefinition(String code) {
		return getDefinition(code, true);
	}

	/**
	 * Get a {@link Definition} using a code and optionally include all EntityAttributes
	 * 
	 * @param code - the code of the Definition BaseEntity
	 * @param bundleAttributes - whether or not to include all the BaseEntity's {@link EntityAttribute EntityAttributes}
	 * @return a Definition with all entity attributes based on the base entity found with the code
	 * 
	 * @throws {@link ItemNotFoundException} if entity linked to the code is not persisted/cannot be found
	 */
	public Definition getDefinition(String code, boolean bundleAttributes) {
		return Definition.from(getBaseEntity(code, bundleAttributes));
	}

	/**
	 * Get a base entity using a code
	 * 
	 * @param code The code of the entity to fetch
	 * @return The BaseEntity without its {@link EntityAttribute EntityAttributes}
	 * 
	 * @throws {@link ItemNotFoundException} if entity linked to the code is not persisted/cannot be found
	 */
	public BaseEntity getBaseEntity(String code) {
		return getBaseEntity(userToken.getProductCode(), code); // watch out for no userToken
	}

	
	/**
	 * Get a base entity using a code
	 *
	 * @param code The code of the entity to fetch
	 * @param bundleAttributes whether or not to bundle the BaseEntity's {@link EntityAttribute EntityAttributes}
	 * @return The BaseEntity with its EntityAttributes if bundleAttributes is <b>true</b>
	 * 
	 * @throws {@link ItemNotFoundException} if entity linked to the code is not persisted/cannot be found
	 */
	public BaseEntity getBaseEntity(String code, boolean bundleAttributes) {
		return getBaseEntity(userToken.getProductCode(), code, bundleAttributes); // watch out for no userToken
	}

	/**
	 * Get a base entity using a code,
	 * 
	 * @param productCode The product to search in
	 * @param code        The code of the entity to fetch
	 * @return The BaseEntity without its {@link EntityAttribute EntityAttributes}
	 * 
	 * @throws {@link ItemNotFoundException} if entity linked to the code is not persisted/cannot be found
	 */
	public BaseEntity getBaseEntity(String productCode, String code) {
		return getBaseEntity(productCode, code, false);
	}

	/**
	 * Get a base entity using a code from a specific product and optionally bundling its {@link EntityAttribute EntityAttributes}
	 *
	 * @param productCode - the product the BaseEntity belongs to
	 * @param code - The code of the base entity
	 * @param bundleAttributes - whether or not to bundle the EntityAttributes
	 * @return The BaseEntity with its EntityAttributes if bundleAttributes is <b>true</b>
	 * 
	 * @throws {@link ItemNotFoundException} if entity linked to the code is not persisted/cannot be found
	 */
	public BaseEntity getBaseEntity(String productCode, String code, boolean bundleAttributes) {
		// fetch entity
		BaseEntityKey key = new BaseEntityKey(productCode, code);
		BaseEntity baseEntity = (BaseEntity) cm.getPersistableEntity(ECacheRef.BASEENTITY, key);
		if (baseEntity == null) {
			throw new ItemNotFoundException(productCode, "baseentity", code);
		}
		// fetch entity attributes
		if (bundleAttributes) {
			Set<EntityAttribute> entityAttributesForBaseEntity = beaUtils.getAllEntityAttributesForBaseEntity(productCode, code);
			if(entityAttributesForBaseEntity.isEmpty()) {
				log.infof("No BaseEntityAttributes found for base entity [%s:%s]", productCode, code);
				//throw new ItemNotFoundException();
			} else {
				log.tracef("%s BaseEntityAttributes found for base entity [%s:%s]. Setting them to BE...", entityAttributesForBaseEntity.size(), productCode, code);
			}
			baseEntity.setBaseEntityAttributes(entityAttributesForBaseEntity);
			log.tracef("Added %s BaseEntityAttributes to BE [%s:%s]", baseEntity.getBaseEntityAttributesMap().size(), baseEntity.getRealm(), baseEntity.getCode());
		}
		return baseEntity;
	}

	/**
	 * Update a {@link BaseEntity} and all of its {@link EntityAttribute EntityAttributes} in the database and the cache.
	 *
	 * @param baseEntity  The BaseEntity to update
	 * @return the newly cached BaseEntity and any linked EntityAttributes
	 */
	public BaseEntity updateBaseEntity(BaseEntity baseEntity) {
		return updateBaseEntity(baseEntity, true);
	}

	/**
	 * Update a {@link BaseEntity} and (optionally) its {@link EntityAttribute EntityAttributes} in the database and the cache.
	 *
	 * @param baseEntity The BaseEntity to update
	 * @param updateBaseEntityAttributes  Defines whether the BaseEntityAttributes need to be updated
	 * @return the newly cached BaseEntity and any linked EntityAttributes
	 */
	public BaseEntity updateBaseEntity(BaseEntity baseEntity, boolean updateBaseEntityAttributes) {
		BaseEntityKey key = new BaseEntityKey(baseEntity.getRealm(), baseEntity.getCode());
		if (baseEntity.getId() == null) {
			Long id = cm.getMaxBaseEntityId() + 1;
			baseEntity.setId(id);
		}
		boolean savedSuccessfully = cm.saveEntity(ECacheRef.BASEENTITY, key, baseEntity);
		if (updateBaseEntityAttributes) {
			baseEntity.getBaseEntityAttributes().forEach(bea -> {
				// ensure for all entityAttribute that baseentity and attribute are not null
				bea.setBaseEntityId(baseEntity.getId());
				if (bea.getRealm() == null) {
					bea.setRealm(baseEntity.getRealm());
				}
				if (bea.getBaseEntityCode() == null) {
					bea.setBaseEntityCode(baseEntity.getCode());
					bea.setBaseEntityId(baseEntity.getId());
				}
				if (bea.getAttribute() == null) {
					Attribute attribute = attributeUtils.getAttribute(baseEntity.getRealm(), bea.getAttributeCode());
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
		String value = getStringValueOfAttribute(baseEntity, attributeCode);
		if (StringUtils.isBlank(value)) {
			log.error("Value contained within " + baseEntity.getCode() + ":" + attributeCode + " is null. Cannot retrieve base entity from lnk attribute");
			return null;
		}
		String newBaseEntityCode = CommonUtils.cleanUpAttributeValue(value);
		try {
			return getBaseEntity(newBaseEntityCode, bundleAttributes);
		} catch (ItemNotFoundException e) {
			log.error(ANSIColour.doColour("Could not find entity: " + newBaseEntityCode, ANSIColour.RED));
			return null;
		}
	}

	/**
	 * Get the code of the BaseEntity that is linked with a specific attribute.
	 *
	 * @param baseEntityCode The targeted BaseEntity Code
	 * @param attributeCode  The attribute storing the data
	 * @return The BaseEntity code stored in the attribute
	 */
	public String getBaseEntityCodeFromLinkAttribute(String baseEntityCode, String attributeCode) {
		BaseEntity be = getBaseEntity(baseEntityCode);
		return getBaseEntityCodeFromLinkAttribute(be, attributeCode);
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
		return CommonUtils.cleanUpAttributeValue(attributeValue);
	}

	public String getStringValueOfAttribute(BaseEntity baseEntity, String attributeCode) {
		return getAttributeValue(baseEntity, attributeCode);
	}

	@Nullable
	private <T> T getAttributeValue(BaseEntity baseEntity, String attributeCode) {
		try {
			EntityAttribute entityAttribute = beaUtils.getEntityAttribute(baseEntity.getRealm(), baseEntity.getCode(), attributeCode, true);
			return entityAttribute.getValue();
		} catch(ItemNotFoundException e) {
			throw ItemNotFoundException.general("EntityAttribute not found when fetching value: " + baseEntity.getCode() + ":" + attributeCode, e);
		}
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

		String attributeValue;
		try {
			attributeValue = getBaseEntityCodeFromLinkAttribute(baseEntity, attributeCode);
		} catch(ItemNotFoundException e) {
			return new ArrayList<>(0);
		}

		String[] baseEntityCodeArray = StringUtils.split(attributeValue,",");
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
		return ea.map(EntityAttribute::getObjectAsString).orElse(null);
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
	 * Convert a stringified list of BaseEntity codes into a list of BaseEntity
	 * objects.
	 *
	 * @param strArr The stringified array to convert
	 * @return A list of BaseEntitys
	 */
	public List<BaseEntity> convertCodesToBaseEntityArray(String strArr) {
		String[] arr = strArr.replace("\"", "").replace("[", "").replace("]", "").replace(" ", "").split(",");
		return Arrays.stream(arr)
				.filter(item -> !item.isEmpty())
				.map(item -> getBaseEntity(item))
				.toList();
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

		created.setValueDateTime(entity.getCreated());
		entity.addAttribute(created);

		Attribute createdDateAttr = new Attribute(PRI_CREATED_DATE, "Created", new DataType(LocalDate.class));
		EntityAttribute createdDate = new EntityAttribute(entity, createdDateAttr, 1.0, null);

		createdDate.setValueDate(entity.getCreated().toLocalDate());
		entity.addAttribute(createdDate);

		// last updated
		Attribute updatedAttr = new Attribute(PRI_UPDATED, "Updated", new DataType(LocalDateTime.class));
		EntityAttribute updated = new EntityAttribute(entity, updatedAttr, 1.0, null);
		updated.setValueDateTime(entity.getUpdated());
		entity.addAttribute(updated);
		try {
			updated.setValueDateTime(entity.getUpdated());
			entity.addAttribute(updated);

			// last updated date
			Attribute updatedDateAttr = new Attribute(PRI_UPDATED_DATE, "Updated", new DataType(LocalDate.class));
			EntityAttribute updatedDate = new EntityAttribute(entity, updatedDateAttr, 1.0, null);
			updatedDate.setValueDate(entity.getUpdated().toLocalDate());
			entity.addAttribute(updatedDate);
		} catch (NullPointerException e) {
			log.error("NPE for PRI_UPDATED");
		}

		// code
		Attribute codeAttr = new Attribute(PRI_CODE, "Code", new DataType(String.class));
		EntityAttribute code = new EntityAttribute(entity, codeAttr, 1.0, null);
		code.setValueString(entity.getCode());
		entity.addAttribute(code);

		// name
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
		return create(definition, null, null);
	}

	/**
	 * Create a new {@link BaseEntity} using a name and code.
	 *
	 * @param definition The def entity to use
	 * @param name  The name of the entity
	 * @param code  The code of the entity
	 * @return The created BaseEntity
	 *
	 * @throws DefinitionException if a prefix could not be found for the supplied definition
	 * @throws DebugException if a user base entity is missing a prefix
	 */
	public BaseEntity create(final Definition definition, String name, String code)
			throws DefinitionException {
		return create(definition, name, code, true);
	}

	/**
	 * Create a new {@link BaseEntity} using a name and code.
	 *
	 * @param definition The def entity to use
	 * @param name  The name of the entity
	 * @param code  The code of the entity
	 * @param saveBaseEntity  Defines whether the created entity should be saved
	 * @return The created BaseEntity
	 * 
	 * @throws DefinitionException if a prefix could not be found for the supplied definition
	 * @throws DebugException if a user base entity is missing a prefix
	 */
	public BaseEntity create(final Definition definition, String name, String code, boolean saveBaseEntity)
		throws DefinitionException {

		if (definition == null)
			throw new NullParameterException("definition");
		if (code != null && code.charAt(3) != '_')
			throw new DebugException("Code parameter " + code + " is not a valid BE code!");

		BaseEntity item;
		String productCode = definition.getRealm();
		String definitionCode = definition.getCode();
		try {
			beaUtils.getEntityAttribute(productCode, definitionCode, Prefix.ATT_.concat(Attribute.PRI_UUID));
			log.debug("Creating user base entity");
			item = createUser(definition);
		} catch(ItemNotFoundException e) {
			String prefix = defUtils.getDefinitionPrefix(productCode, definitionCode);
			
			// TODO: Standardize this
			if (StringUtils.isBlank(prefix)) {
				throw new DefinitionException("No prefix set for the def: " + definitionCode);
			}

			if (StringUtils.isBlank(code)) {
				code = (prefix + "_" + UUID.randomUUID().toString().substring(0, 32)).toUpperCase();
			}

			if (StringUtils.isBlank(name))
				name = definition.getName();

			item = new BaseEntity(code.toUpperCase(), name);
			item.setRealm(productCode);
			//item.addAttribute(new EntityAttribute());
		}

		if (saveBaseEntity) {
			// saveBaseEntity to DB and cache
			updateBaseEntity(item);
		}

		Set<EntityAttribute> atts = beaUtils.getBaseEntityAttributesForBaseEntityWithAttributeCodePrefix(productCode, definitionCode, Prefix.ATT_);
		for (EntityAttribute ea : atts) {
			String attrCode = ea.getAttributeCode().substring(Prefix.ATT_.length());
			Attribute attribute;
			try {
				attribute = attributeUtils.getAttribute(attrCode, true);
			} catch(ItemNotFoundException e) {
				log.warn("No Attribute found for def attr " + attrCode);
				continue;
			}

			if (item.containsEntityAttribute(attribute.getCode())) {
				log.info(item.getCode() + " already has value for " + attribute.getCode());
				continue;
			}

			// Find any default val for this Attr
			String defaultDefValueAttr = Prefix.DFT_.concat(attrCode);
			Object defaultVal = definition.getValue(defaultDefValueAttr, attribute.getDefaultValue());
			if(defaultVal != null && defaultVal == attribute.getDefaultValue()) {
				log.debug("Same memory reference for: " + defaultVal + ". Likely not DFT associated with: " + attrCode);
			} else if(defaultVal != null && defaultVal.equals(attribute.getDefaultValue())) {
				log.debug("Not same memory reference but defaultVal and attribute.getDefaultValue are identical for: " + attrCode + ". Value: " + defaultVal);
			}

			// Only process mandatory attributes, or defaults
			Boolean mandatory = ea.getValueBoolean();
			if (mandatory == null) {
				mandatory = false;
				log.warn("**** DEF attribute ATT_" + attrCode + " has no mandatory boolean set in "
						+ definitionCode);
			}
			// Only process mandatory attributes, or defaults
			if (mandatory || defaultVal != null) {
				log.info("Adding mandatory/default -> " + attribute.getCode() + ". Default: " + (defaultVal != null ? defaultVal : "null") + ", mand: " + mandatory);
				EntityAttribute newEA = new EntityAttribute(item, attribute, ea.getWeight(), defaultVal);
				newEA.setRealm(userToken.getProductCode());
				item.addAttribute(newEA);
				if (saveBaseEntity) {
					beaUtils.updateEntityAttribute(newEA);
				}
			}
		}

		Attribute linkDef = attributeUtils.getAttribute(Attribute.LNK_DEF, true);
		EntityAttribute linkDefEA = new EntityAttribute(item, linkDef, 1.0, "[\"" + definitionCode + "\"]");
		item.addAttribute(linkDefEA);

		// author of the BE
		Attribute lnkAuthorAttr = attributeUtils.getAttribute(Attribute.LNK_AUTHOR, true);
		EntityAttribute linkAuthorEA = new EntityAttribute(item, lnkAuthorAttr, 1.0, "[\"" + userToken.getUserCode() + "\"]");
		item.addAttribute(linkAuthorEA);

		if (saveBaseEntity) {
			beaUtils.updateEntityAttribute(linkDefEA);
			beaUtils.updateEntityAttribute(linkAuthorEA);
			updateBaseEntity(item, false);
		}

		return item;
	}

	/**
	 * Create a new user {@link BaseEntity} using a DEF entity.
	 *
	 * @param definition The def entity to use
	 * @return The created BaseEntity
	 */
	public BaseEntity createUser(final Definition definition) {

		Optional<EntityAttribute> opt = definition.findEntityAttribute(Prefix.ATT_.concat(Attribute.PRI_UUID));
		if (opt.isEmpty())
			throw new DefinitionException("Definition is not a User definition: " + definition.getCode());

		String email = "random+" + UUID.randomUUID().toString().substring(0, 20) + "@gada.io";

		// generate keycloak id
		String uuid = KeycloakUtils.createDummyUser(serviceToken, userToken.getKeycloakRealm());

		// check definition prefix
		Optional<String> optCode = definition.getValue(Attribute.PRI_PREFIX);
		if (optCode.isEmpty())
			throw new DebugException("Prefix not provided" + definition.getCode());

		String code = optCode.get() + "_" + uuid.toUpperCase();
		BaseEntity item = new BaseEntity(code, null);
		item.setRealm(userToken.getProductCode());

		// add email and username
		if (!email.startsWith("random+")) {
			// Check to see if the email exists
			// TODO: check to see if the email exists in the database and keycloak
			Attribute emailAttribute = attributeUtils.getAttribute(Attribute.PRI_EMAIL, true);
			item.addAttribute(new EntityAttribute(item, emailAttribute, 1.0, email));
			Attribute usernameAttribute = attributeUtils.getAttribute(Attribute.PRI_USERNAME, true);
			item.addAttribute(new EntityAttribute(item, usernameAttribute, 1.0, email));
		}

		// add PRI_UUID
		Attribute uuidAttribute = attributeUtils.getAttribute(Attribute.PRI_UUID, true);
		item.addAttribute(new EntityAttribute(item, uuidAttribute, 1.0, uuid.toUpperCase()));

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

		Attribute attribute = attributeUtils.getAttribute(attributeCode, true);

		EntityAttribute ea = new EntityAttribute(be, attribute, 1.0, value);
		be.addAttribute(ea);
		return be;
	}

	/**
	 * Remove a {@link BaseEntity} and all of its {@link EntityAttribute EntityAttributes} from the ISPN Cache
	 * @param baseEntity base entity to remove
	 * @return the number of affected entities (including the number of affected entity attributes)
	 */
	public int removeBaseEntity(BaseEntity baseEntity) {
		int numAffected = beaUtils.removeBaseEntityAttributesForBaseEntity(baseEntity);
		return numAffected + removeBaseEntity(baseEntity.getRealm(), baseEntity.getCode());
	}

	/**
	 * Remove a {@link BaseEntity} and all of its {@link EntityAttribute EntityAttributes} from the ISPN Cache
	 * @param productCode product of the BaseEntity
	 * @param beCode code of the BaseEntity
	 * @return the number of affected entities (including the number of affected entity attributes)
	 */
	public int removeBaseEntity(String productCode, String beCode) {
		int numAffected = beaUtils.removeBaseEntityAttributesForBaseEntity(productCode, beCode);
		return numAffected + cm.removeBaseEntity(productCode, beCode);
	}
}
