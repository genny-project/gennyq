package life.genny.qwandaq.utils;

import static life.genny.qwandaq.attribute.Attribute.PRI_CODE;
import static life.genny.qwandaq.attribute.Attribute.PRI_CREATED;
import static life.genny.qwandaq.attribute.Attribute.PRI_CREATED_DATE;
import static life.genny.qwandaq.attribute.Attribute.PRI_NAME;
import static life.genny.qwandaq.attribute.Attribute.PRI_UPDATED;
import static life.genny.qwandaq.attribute.Attribute.PRI_UPDATED_DATE;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
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
	BaseEntityAttributeUtils beaUtils;

	public BaseEntityUtils() {
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
		return PCM.from(getBaseEntity(code));
	}

	/**
	 * Get a Definition using a code, but throw an
	 * ItemNotFoundException if the entity does not exist.
	 * 
	 * @param code
	 * @return
	 */
	public Definition getDefinition(String code) {
		return Definition.from(getBaseEntity(code));
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
	 * ItemNotFoundException if the entitiy does not exist.
	 * 
	 * @param productCode The product to search in
	 * @param code        The code of the entity to fetch
	 * @return The BaseEntity
	 */
	public BaseEntity getBaseEntity(String productCode, String code) {
		return getBaseEntity(productCode, code, true);
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
			beaUtils.getAllEntityAttributesForBaseEntity(productCode, code).parallelStream() .forEach(bea -> {
				baseEntity.getBaseEntityAttributes().add(bea);
			});
			log.debugf("Added %s BaseEntityAttributes to BE.", baseEntity.getBaseEntityAttributes().size());
		}
		return baseEntity;
	}

	/**
	 * Update a {@link BaseEntity} in the database and the cache.
	 *
	 * @param baseEntity The BaseEntity to update
	 * @return the newly cached BaseEntity
	 */
	public BaseEntity updateBaseEntity(BaseEntity baseEntity) {
		return updateBaseEntity(baseEntity.getRealm(), baseEntity);
	}

	/**
	 * Update a {@link BaseEntity} in the database and the cache.
	 *
	 * @param productCode The productCode to cache into
	 * @param baseEntity  The BaseEntity to update
	 * @return the newly cached BaseEntity
	 */
	public BaseEntity updateBaseEntity(String productCode, BaseEntity baseEntity) {
		// ensure for all entityAttribute that baseentity and attribute are not null
		for (EntityAttribute ea : baseEntity.getBaseEntityAttributes()) {
			if (ea.getRealm() == null) {
				ea.setRealm(baseEntity.getRealm());
			}
			if (ea.getBaseEntityCode() == null) {
				ea.setBaseEntityCode(baseEntity.getCode());
			}
			if (ea.getAttribute() == null) {
				Attribute attribute = cm.getAttribute(baseEntity.getRealm(), ea.getAttributeCode());
				ea.setAttribute(attribute);
			}
		}

		// databaseUtils.saveBaseEntity(baseEntity);
		// cache.putObject(userToken.getProductCode(), baseEntity.getCode(), baseEntity);

		BaseEntityKey key = new BaseEntityKey(baseEntity.getRealm(), baseEntity.getCode());
		boolean savedSuccssfully = cm.saveEntity(CacheManager.CACHE_NAME_BASEENTITY, key, baseEntity);
		return savedSuccssfully ? baseEntity : null;
	}

	/**
	 * Get the BaseEntity that is linked with a specific attribute. Generally this
	 * will be a LNK attribute, although it doesn't have to be.
	 *
	 * @param baseEntityCode The targeted BaseEntity Code
	 * @param attributeCode  The attribute storing the data
	 * @return The BaseEntity with code stored in the attribute
	 */
	public BaseEntity getBaseEntityFromLinkAttribute(String baseEntityCode, String attributeCode) {
		BaseEntity be = getBaseEntity(baseEntityCode);
		return getBaseEntityFromLinkAttribute(be, attributeCode);
	}

	/**
	 * Get the BaseEntity that is linked with a specific attribute.
	 *
	 * @param baseEntity    The targeted BaseEntity
	 * @param attributeCode The attribute storing the data
	 * @return The BaseEntity with code stored in the attribute
	 */
	public BaseEntity getBaseEntityFromLinkAttribute(BaseEntity baseEntity, String attributeCode) {
		String newBaseEntityCode = getBaseEntityCodeFromLinkAttribute(baseEntity, attributeCode);
		// return null if attributeCode valueString is null or empty
		if (StringUtils.isEmpty(newBaseEntityCode))
			return null;
		try {
			BaseEntity newBe = getBaseEntity(newBaseEntityCode);
			return newBe;
		} catch (ItemNotFoundException e) {
			log.error(ANSIColour.RED + "Could not find entity: " + newBaseEntityCode + ANSIColour.RESET);
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

		Optional<String> attributeValue = baseEntity.getValue(attributeCode);
		if (attributeValue.isPresent()) {

			Object value = attributeValue.get();
			if (value == null)
				return null;
			if (!(value instanceof String))
				return null;

			return CommonUtils.cleanUpAttributeValue((String) value);
		}

		return null;
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
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getValueDateTime();
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
		entity.setBaseEntityAttributes(
				entity.getBaseEntityAttributes()
						.stream()
						.filter(x -> allowed.contains(x.getAttributeCode()))
						.collect(Collectors.toSet()));

		return entity;
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
		Optional<EntityAttribute> uuidEA = definition.findEntityAttribute(Prefix.ATT.concat(Attribute.PRI_UUID));

		if (uuidEA.isPresent())
			item = createUser(definition);
		else {
			String prefix = definition.getValueAsString(Attribute.PRI_PREFIX);
			if (StringUtils.isBlank(prefix))
				throw new DefinitionException("No prefix set for the def: " + definition.getCode());

			if (StringUtils.isBlank(code))
				code = prefix + "_" + UUID.randomUUID().toString().substring(0, 32).toUpperCase();
			if (StringUtils.isBlank(name))
				name = definition.getName();

			item = new BaseEntity(code.toUpperCase(), name);
			item.setRealm(definition.getRealm());
		}

		// save to DB and cache
		updateBaseEntity(item);

		List<EntityAttribute> atts = definition.findPrefixEntityAttributes(Prefix.ATT);
		for (EntityAttribute ea : atts) {
			String attrCode = ea.getAttributeCode().substring("ATT_".length());
			Attribute attribute = cm.getAttribute(definition.getRealm(), attrCode);

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
						+ definition.getCode());
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
		item.addAnswer(new Answer(item, item, linkDef, "[\"" + definition.getCode() + "\"]"));

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
