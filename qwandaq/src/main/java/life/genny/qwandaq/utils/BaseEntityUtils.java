package life.genny.qwandaq.utils;

import io.quarkus.arc.Arc;
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
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


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

	@Inject
	EntityManagerFactory emf;

	@Inject
	// @PersistenceUnit("genny")
	EntityManager entityManager;

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
	 * 
	 * @param code The code of the entity to fetch
	 * @return The BaseEntity
	 */
	public BaseEntity getBaseEntity(String code) {

		return getBaseEntity(userToken.getProductCode(), code);
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

		BaseEntity baseEntity = getBaseEntityOrNull(productCode, code);
		if (baseEntity == null)
			throw new ItemNotFoundException(code);

		return baseEntity;
	}

	/**
	 * Get a base entity using the code, or return null if not found.
	 * 
	 * @param code The code of the entity to fetch
	 * @return The BaseEntity, or null if not found
	 */
	public BaseEntity getBaseEntityOrNull(String code) {

		return getBaseEntityOrNull(userToken.getProductCode(), code);
	}

	/**
	 * Get a base entity using the code, or return null if not found.
	 * 
	 * @param productCode The product to search in
	 * @param code        The code of the entity to fetch
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
	 * @param code        The code of the BaseEntity to fetch
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
		// BaseEntityKey key = new BaseEntityKey(productCode, code);
		// BaseEntity entity = (BaseEntity)
		// CacheUtils.getEntity(GennyConstants.CACHE_NAME_BASEENTITY, key);

		// NOTE: No more hacks, keep it simple and reliable until infinispan auto
		// updates are working.
		BaseEntity entity = null;
		entity = CacheUtils.getObject(productCode, code, BaseEntity.class);

		// check in database if not in cache
		if (entity == null) {
			try {
				if (databaseUtils == null) {
					log.error("databaseUtils is null");
					// Arc.container().requestContext().activate();
					Arc.container().instance(DatabaseUtils.class);
					// databaseUtils = new DatabaseUtils();
					EntityManagerFactory factory = Persistence.createEntityManagerFactory("genny");
					entityManager = factory.createEntityManager();
					// entityManager =
					// Persistence.createEntityManagerFactory("genny").createEntityManager();
					if (entityManager == null) {
						log.error("entityManager is null");
					}
					if (databaseUtils == null) {
						log.error("databaseUtils is still null");
						databaseUtils = new DatabaseUtils();

						databaseUtils.setEntityManager(entityManager);
					} else {
						databaseUtils.setEntityManager(entityManager);
					}
				}
				entity = databaseUtils.findBaseEntityByCode(productCode, code);
				log.debug(code + " not in cache for product " + productCode + " but "
						+ (entity == null ? "not found in db" : "found in db"));
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
		return updateBaseEntity(userToken.getProductCode(), baseEntity);
	}

	/**
	 * Update a {@link BaseEntity} in the database and the cache.
	 *
	 * @param productCode The productCode to cache into
	 * @param baseEntity The BaseEntity to update
	 * @return the newly cached BaseEntity
	 */
	public BaseEntity updateBaseEntity(String productCode, BaseEntity baseEntity) {

		// ensure for all entityAttribute that baseentity and attribute are not null
		for (EntityAttribute ea : baseEntity.getBaseEntityAttributes()) {
			ea.setRealm(productCode);
			if (ea.getPk().getBaseEntity() == null) {
				ea.getPk().setBaseEntity(baseEntity);
			}

			if (ea.getPk().getAttribute() == null) {
				Attribute attribute = qwandaUtils.getAttribute(ea.getAttributeCode());
				ea.getPk().setAttribute(attribute);
			}
		}

		baseEntity.setRealm(productCode);
		databaseUtils.saveBaseEntity(baseEntity);
		CacheUtils.putObject(productCode, baseEntity.getCode(), baseEntity);

		// BaseEntityKey key = new BaseEntityKey(baseEntity.getRealm(),
		// baseEntity.getCode());
		// return (BaseEntity)
		// CacheUtils.saveEntity(GennyConstants.CACHE_NAME_BASEENTITY, key, baseEntity);
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
			if (value instanceof String sValue)
				return cleanUpAttributeValue(sValue);
			else
				return null;
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
	 * TODO: Consider moving this to CommonUtils
	 *
	 * @param value The value to clean
	 * @return A clean string
	 */
	public String cleanUpAttributeValue(String value) throws NullParameterException {
		if(value == null) throw new NullParameterException("value");
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
		return Arrays.stream(arr)
				.filter(item -> !item.isEmpty())
				.map(item -> getBaseEntityOrNull(item))
				.toList();
	}

	/**
	 * Apply the privacy filter to a BaseEntity.
	 * 
	 * @param entity  The be to apply the filter to
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
		Attribute createdAttr = new Attribute("PRI_CREATED", "Created", new DataType(LocalDateTime.class));
		EntityAttribute created = new EntityAttribute(entity, createdAttr, 1.0);

		if(entity.getCreated() == null) {
			log.error("NPE for PRI_CREATED. Generating created date");
			entity.autocreateCreated();
		}
		
		// Ensure createdDate is not null
		created.setValueDateTime(entity.getCreated());
		entity.addAttribute(created);

		Attribute createdDateAttr = new Attribute("PRI_CREATED_DATE", "Created", new DataType(LocalDate.class));
		EntityAttribute createdDate = new EntityAttribute(entity, createdDateAttr, 1.0);

		// Ensure createdDate is not null
		createdDate.setValueDate(entity.getCreated().toLocalDate());
		entity.addAttribute(createdDate);

		Attribute updatedAttr = new Attribute("PRI_UPDATED", "Updated", new DataType(LocalDateTime.class));
		EntityAttribute updated = new EntityAttribute(entity, updatedAttr, 1.0);
		try {
			updated.setValueDateTime(entity.getUpdated());
			entity.addAttribute(updated);

			Attribute updatedDateAttr = new Attribute("PRI_UPDATED_DATE", "Updated", new DataType(LocalDate.class));
			EntityAttribute updatedDate = new EntityAttribute(entity, updatedDateAttr, 1.0);
			updatedDate.setValueDate(entity.getUpdated().toLocalDate());
			entity.addAttribute(updatedDate);
		} catch (NullPointerException e) {
			log.error("NPE for PRI_UPDATED");
		}

		Attribute codeAttr = new Attribute("PRI_CODE", "Code", new DataType(String.class));
		EntityAttribute code = new EntityAttribute(entity, codeAttr, 1.0);
		code.setValueString(entity.getCode());
		entity.addAttribute(code);

		Attribute nameAttr = new Attribute("PRI_NAME", "Name", new DataType(String.class));
		EntityAttribute name = new EntityAttribute(entity, nameAttr, 1.0);
		name.setValueString(entity.getName());
		entity.addAttribute(name);
		
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

		// TODO: Surely we don't have to fetch attribute from attribute code if the attribute
		// is already stored in the entity attribute?
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
		item.addAnswer(new Answer(item, item, linkDef, "[\"" + defBE.getCode() + "\"]"));

		// author of the BE
		Attribute lnkAuthorAttr = qwandaUtils.getAttribute("LNK_AUTHOR");
		item.addAnswer(new Answer(item, item, lnkAuthorAttr, "[\"" + userToken.getUserCode() + "\"]"));

		updateBaseEntity(item);

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

		if (be == null)
			throw new NullParameterException("be");
		if (attributeCode == null)
			throw new NullParameterException("attributeCode");
		// if (value == null)
		// throw new NullParameterException("value");

		Attribute attribute = qwandaUtils.getAttribute(attributeCode);

		EntityAttribute ea = new EntityAttribute(be, attribute, 1.0, value);
		be.addAttribute(ea);
		return be;
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
