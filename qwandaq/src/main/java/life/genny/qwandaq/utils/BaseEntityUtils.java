package life.genny.qwandaq.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;

/**
 * A non-static utility class used for standard
 * operations involving BaseEntitys.
 * 
 * @author Adam Crow
 * @author Jasper Robison
 */
@ApplicationScoped
public class BaseEntityUtils {

	static final Logger log = Logger.getLogger(BaseEntityUtils.class);
	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	ServiceToken serviceToken;

	@Inject
	UserToken userToken;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	QwandaUtils qwandaUtils;

	public BaseEntityUtils() { }

	/**
	 * Fetch the user base entity of the {@link UserToken}.
	 * 
	 * @return the user {@link BaseEntity}
	 */
	public BaseEntity getProjectBaseEntity() {

		return getBaseEntity("PRJ_" + userToken.getProductCode().toUpperCase());
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
	 * Get a base entity using a code, but throw an 
	 * ItemNotFoundException if the entitiy does not exist.
	 * @param code The code of the entity to fetch
	 * @return The BaseEntity
	 */
	public BaseEntity getBaseEntity(String code) {

		return getBaseEntity(userToken.getProductCode(), code);
	}

	/**
	 * Get a base entity using a code, but throw an 
	 * ItemNotFoundException if the entitiy does not exist.
	 * @param productCode The product to search in
	 * @param code The code of the entity to fetch
	 * @return The BaseEntity
	 */
	public BaseEntity getBaseEntity(String productCode, String code) {

		BaseEntity baseEntity = getBaseEntityOrNull(productCode, code);
		if (baseEntity == null)
			throw new ItemNotFoundException(code);

		return baseEntity;
	}

	/**
	 * Get a base entity using the code, or return null if not found.
	 * @param code The code of the entity to fetch
	 * @return The BaseEntity, or null if not found
	 */
	public BaseEntity getBaseEntityOrNull(String code) {

		return getBaseEntityOrNull(userToken.getProductCode(), code);
	}

	/**
	 * Get a base entity using the code, or return null if not found.
	 * @param productCode The product to search in
	 * @param code The code of the entity to fetch
	 * @return The BaseEntity, or null if not found
	 */
	public BaseEntity getBaseEntityOrNull(String productCode, String code) {

		return getBaseEntityByCode(productCode, code);
	}

	/**
	 * Fetch A {@link BaseEntity} from the cache using a code.
	 *
	 * @param code The code of the BaseEntity to fetch
	 * @return The corresponding BaseEntity, or null if not found.
	 */
	public BaseEntity getBaseEntityByCode(String code) {
		if (userToken == null) {
			throw new NullParameterException("User Token");
		}
		return getBaseEntityByCode(userToken.getProductCode(), code);
	}
  
	/**
	 * Fetch A {@link BaseEntity} from the cache using a code.
	 *
	 * @param productCode The productCode to use
	 * @param code The code of the BaseEntity to fetch
	 * @return The corresponding BaseEntity, or null if not found.
	 */
	@Deprecated
	public BaseEntity getBaseEntityByCode(String productCode, String code) {

		if (productCode == null) 
			throw new NullParameterException("productCode");
		if (code == null) 
			throw new NullParameterException("code");
		if (StringUtils.isBlank(productCode))
			throw new DebugException("productCode is empty");
		if (StringUtils.isBlank(code))
			throw new DebugException("code is empty");

		// check for entity in the cache
		//  BaseEntityKey key = new BaseEntityKey(productCode, code);
		//  BaseEntity entity = (BaseEntity) CacheUtils.getEntity(GennyConstants.CACHE_NAME_BASEENTITY, key);

		// NOTE: No more hacks, keep it simple and reliable until infinispan auto updates are working.
		BaseEntity entity = null;
		entity = CacheUtils.getObject(productCode, code, BaseEntity.class);
			
		// check in database if not in cache
		if (entity == null) {			
			try {
				entity = databaseUtils.findBaseEntityByCode(productCode, code);
				log.debug(code + " not in cache for product " + productCode+" but "+(entity==null?"not found in db":"found in db"));
			} catch (NoResultException e) {
				log.error(new ItemNotFoundException(productCode, code).getLocalizedMessage());
			}
		}

		return entity;
	}

	/**
	 * Update a {@link BaseEntity} in the database and the cache.
	 *
	 * @param baseEntity The BaseEntity to update
	 * @return the newly cached BaseEntity
	 */
	public BaseEntity updateBaseEntity(BaseEntity baseEntity) {

		// ensure for all entityAttribute that baseentity and attribute are not null
		for (EntityAttribute ea : baseEntity.getBaseEntityAttributes()) {

			if (ea.getPk().getBaseEntity() == null) {
				ea.getPk().setBaseEntity(baseEntity);
			}

			if (ea.getPk().getAttribute() == null) {
				Attribute attribute = qwandaUtils.getAttribute(ea.getAttributeCode());
				ea.getPk().setAttribute(attribute);
			}
		}

		databaseUtils.saveBaseEntity(baseEntity);
		CacheUtils.putObject(userToken.getProductCode(), baseEntity.getCode(), baseEntity);

		// BaseEntityKey key = new BaseEntityKey(baseEntity.getRealm(), baseEntity.getCode());
		// return (BaseEntity) CacheUtils.saveEntity(GennyConstants.CACHE_NAME_BASEENTITY, key, baseEntity);
		return baseEntity;
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

		BaseEntity be = getBaseEntityOrNull(baseEntityCode);
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
		if (StringUtils.isEmpty(newBaseEntityCode)) {
			return null;
		}
		BaseEntity newBe = getBaseEntityOrNull(newBaseEntityCode);
		return newBe;
	}

	/**
	 * Get the code of the BaseEntity that is linked with a specific attribute.
	 *
	 * @param baseEntityCode The targeted BaseEntity Code
	 * @param attributeCode  The attribute storing the data
	 * @return The BaseEntity code stored in the attribute
	 */
	public String getBaseEntityCodeFromLinkAttribute(String baseEntityCode, String attributeCode) {

		BaseEntity be = getBaseEntityOrNull(baseEntityCode);
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

			return cleanUpAttributeValue((String) value);
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

		BaseEntity be = getBaseEntityOrNull(baseEntityCode);
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
	 * Classic Genny style string clean up. This will remove any double quotes,
	 * whitespaces and square brackets from the string.
	 * <p>
	 * Hope this makes our code look a little
	 * nicer :)
	 * <p>
	 *
	 * @param value The value to clean
	 * @return A clean string
	 */
	public static String cleanUpAttributeValue(String value) {
		String cleanCode = value.replace("\"", "").replace("[", "").replace("]", "").replace(" ", "");
		return cleanCode;
	}

	/**
	 * Get the value of an EntityAttribute as an Object.
	 *
	 * @param baseEntityCode The code of the entity to grab from
	 * @param attributeCode  The code of the attribute to check
	 * @return The value as an Object
	 */
	public Object getBaseEntityValue(final String baseEntityCode, final String attributeCode) {

		BaseEntity be = getBaseEntityOrNull(baseEntityCode);
		if (be != null) {
			Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
			if (ea.isPresent()) {
				return ea.get().getObject();
			}
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

		if (be == null) {
			return null;
		}

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

		BaseEntity be = getBaseEntityOrNull(baseEntityCode);
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

		BaseEntity be = getBaseEntityOrNull(baseEntityCode);
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
		BaseEntity be = getBaseEntityOrNull(baseEntityCode);
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

		BaseEntity be = getBaseEntityOrNull(baseEntityCode);
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
				.map(item -> (BaseEntity) getBaseEntityOrNull(item))
				.collect(Collectors.toList());

		return entityList;
	}

	/**
	 * Apply the privacy filter to a BaseEntity.
	 * @param entity The be to apply the filter to
	 * @param allowed The list of allowed attribute codes
	 * @return The filtered BaseEntity
	 */
	public BaseEntity privacyFilter(BaseEntity entity, List<String> allowed) {

		// Filter out unwanted attributes
		entity.setBaseEntityAttributes(
				entity.getBaseEntityAttributes()
						.stream()
						.filter(x -> allowed.contains(x.getAttributeCode()))
						.collect(Collectors.toSet()));

		return entity;
	}

	/**
	 * Add all non literal attributes to the baseentity.
	 * @param entity The entity to update
	 * @return The updated BaseEntity
	 */
	public BaseEntity addNonLiteralAttributes(BaseEntity entity) {

		// Handle Created and Updated attributes
		Attribute createdAttr = new Attribute("PRI_CREATED", "Created", new DataType(LocalDateTime.class));
		EntityAttribute created = new EntityAttribute(entity, createdAttr, 1.0);
		// Ensure createdDate is not null
		try {
			created.setValueDateTime(entity.getCreated());
		} catch(NullPointerException e) {
			log.error("NPE for PRI_CREATED. Generating created date");
			entity.autocreateCreated();
			created.setValueDateTime(entity.getCreated());
		}
		entity.addAttribute(created);

		Attribute createdDateAttr = new Attribute("PRI_CREATED_DATE", "Created", new DataType(LocalDate.class));
		EntityAttribute createdDate = new EntityAttribute(entity, createdDateAttr, 1.0);
		// Ensure createdDate is not null
		try {
			createdDate.setValueDate(entity.getCreated().toLocalDate());
		} catch(NullPointerException e) {
			log.error("NPE for PRI_CREATED_DATE. Generating created date");
			entity.autocreateCreated();
			createdDate.setValueDate(entity.getCreated().toLocalDate());
		}
		entity.addAttribute(createdDate);

		Attribute updatedAttr = new Attribute("PRI_UPDATED", "Updated", new DataType(LocalDateTime.class));
		EntityAttribute updated = new EntityAttribute(entity, updatedAttr, 1.0);
		try {
			updated.setValueDateTime(entity.getUpdated());
			entity.addAttribute(updated);
		} catch(NullPointerException e) {
			log.error("NPE for PRI_UPDATED");
		}

		try {
			Attribute updatedDateAttr = new Attribute("PRI_UPDATED_DATE", "Updated", new DataType(LocalDate.class));
			EntityAttribute updatedDate = new EntityAttribute(entity, updatedDateAttr, 1.0);
			updatedDate.setValueDate(entity.getUpdated().toLocalDate());
			entity.addAttribute(updatedDate);
		} catch(NullPointerException e) {
			log.error("NPE for PRI_UPDATED_DATE");
		}

		try {
			Attribute codeAttr = new Attribute("PRI_CODE", "Code", new DataType(String.class));
			EntityAttribute code = new EntityAttribute(entity, codeAttr, 1.0);
			code.setValueString(entity.getCode());
			entity.addAttribute(code);
		} catch(NullPointerException e) {
			log.error("NPE for PRI_CODE");
		}

		try {
			Attribute nameAttr = new Attribute("PRI_NAME", "Name", new DataType(String.class));
			EntityAttribute name = new EntityAttribute(entity, nameAttr, 1.0);
			name.setValueString(entity.getName());
			entity.addAttribute(name);
		} catch(NullPointerException e) {
			log.error("NPE for PRI_NAME");
		}

		return entity;
	}

	/**
	 * Create a new {@link BaseEntity} using a DEF entity.
	 *
	 * @param defBE The def entity to use
	 * @return The created BaseEntity
	 */
	public BaseEntity create(final BaseEntity defBE) {
		return create(defBE, null, null);
	}

	/**
	 * Create a new {@link BaseEntity} using a DEF entity and a name.
	 *
	 * @param defBE The def entity to use
	 * @param name  The name of the entity
	 * @return The created BaseEntity
	 */
	public BaseEntity create(final BaseEntity defBE, String name) {
		return create(defBE, name, null);
	}

	/**
	 * Create a new {@link BaseEntity} using a name and code.
	 *
	 * @param defBE The def entity to use
	 * @param name  The name of the entity
	 * @param code  The code of the entity
	 * @return The created BaseEntity
	 */
	public BaseEntity create(final BaseEntity defBE, String name, String code) {

		if (defBE == null)
			throw new NullParameterException("defBE");

		if (code != null && code.charAt(3) != '_')
			throw new DebugException("Code parameter " + code + " is not a valid BE code!");

		BaseEntity item = null;
		Optional<EntityAttribute> uuidEA = defBE.findEntityAttribute("ATT_PRI_UUID");

		if (uuidEA.isPresent()) {
			// if the defBE is a user without an email provided, create a keycloak acc using
			// a unique random uuid
			String randomEmail = "random+" + UUID.randomUUID().toString().substring(0, 20) + "@gada.io";
			item = createUser(defBE, randomEmail);
		}

		if (item == null) {
			String prefix = defBE.getValueAsString("PRI_PREFIX");

			if (StringUtils.isBlank(prefix))
				throw new DebugException("No prefix set for the def: " + defBE.getCode());

			if (StringUtils.isBlank(code))
				code = prefix + "_" + UUID.randomUUID().toString().substring(0, 32).toUpperCase();

			if (StringUtils.isBlank(name))
				name = defBE.getName();

			// create entity and set realm
			item = new BaseEntity(code.toUpperCase(), name);
			item.setRealm(userToken.getProductCode());
		}

		// save to DB and cache
		updateBaseEntity(item);

		List<EntityAttribute> atts = defBE.findPrefixEntityAttributes("ATT_");
		for (EntityAttribute ea : atts) {
			String attrCode = ea.getAttributeCode().substring("ATT_".length());
			Attribute attribute = qwandaUtils.getAttribute(attrCode);

			if (attribute == null) {
				log.warn("No Attribute found for def attr " + attrCode);
				continue;
			}
			if (item.containsEntityAttribute(attribute.getCode())) {
				log.info(item.getCode() + " already has value for " + attribute.getCode());
				continue;
			}

			// Find any default val for this Attr
			String defaultDefValueAttr = "DFT_" + attrCode;
			Object defaultVal = defBE.getValue(defaultDefValueAttr, attribute.getDefaultValue());

			// Only process mandatory attributes, or defaults
			Boolean mandatory = ea.getValueBoolean();
			if (mandatory == null) {
				mandatory = false;
				log.warn("**** DEF attribute ATT_" + attrCode + " has no mandatory boolean set in "
						+ defBE.getCode());
			}
			// Only process mandatory attributes, or defaults
			if (mandatory || defaultVal != null) {
				EntityAttribute newEA = new EntityAttribute(item, attribute, ea.getWeight(), defaultVal);
				log.info("Adding mandatory/default -> " + attribute.getCode());
				item.addAttribute(newEA);
			}
		}

		Attribute linkDef = qwandaUtils.getAttribute("LNK_DEF");
		item.addAnswer(new Answer(item, item, linkDef, "[\""+defBE.getCode()+"\"]"));

		// author of the BE
		Attribute lnkAuthorAttr = qwandaUtils.getAttribute("LNK_AUTHOR");
		item.addAnswer(new Answer(item, item, lnkAuthorAttr, "[\""+userToken.getUserCode()+"\"]"));

		updateBaseEntity(item);

		return item;
	}

	/**
	 * Create a new user {@link BaseEntity} using a DEF entity.
	 *
	 * @param defBE The def entity to use
	 * @param email The email to use
	 * @return The created BaseEntity
	 */
	public BaseEntity createUser(final BaseEntity defBE, final String email) {

		BaseEntity item = null;
		String uuid = null;
		Optional<EntityAttribute> uuidEA = defBE.findEntityAttribute("ATT_PRI_UUID");

		if (uuidEA.isEmpty()) {
			throw new DebugException("Passed defBE is not a user def!" + defBE.getCode());
		}

		if (!StringUtils.isBlank(email)) {
			// TODO: run a regexp check to see if the email is valid
		}

		// this is a user, generate keycloak id
		uuid = KeycloakUtils.createDummyUser(serviceToken, userToken.getKeycloakRealm());
		Optional<String> optCode = defBE.getValue("PRI_PREFIX");

		if (optCode.isEmpty()) {
			throw new DebugException("Prefix not provided" + defBE.getCode());
		}

		String name = defBE.getName();
		String code = optCode.get() + "_" + uuid.toUpperCase();
		item = new BaseEntity(code, name);
		item.setRealm(userToken.getProductCode());

		// add email and username
		if (!email.startsWith("random+")) {
			// Check to see if the email exists
			// TODO: check to see if the email exists in the database and keycloak
			Attribute emailAttribute = qwandaUtils.getAttribute("PRI_EMAIL");
			item.addAnswer(new Answer(item, item, emailAttribute, email));
			Attribute usernameAttribute = qwandaUtils.getAttribute("PRI_USERNAME");
			item.addAnswer(new Answer(item, item, usernameAttribute, email));
		}

		// add PRI_UUID
		Attribute uuidAttribute = qwandaUtils.getAttribute("PRI_UUID");
		item.addAnswer(new Answer(item, item, uuidAttribute, uuid.toUpperCase()));

		// keycloak UUID
		Attribute keycloakAttribute = qwandaUtils.getAttribute("PRI_KEYCLOAK_UUID");
		item.addAnswer(new Answer(item, item, keycloakAttribute, uuid.toUpperCase()));

		return item;
	}
}
