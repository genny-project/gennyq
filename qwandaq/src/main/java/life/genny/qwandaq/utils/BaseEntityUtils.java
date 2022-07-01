package life.genny.qwandaq.utils;

import java.net.http.HttpResponse;
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
import javax.ws.rs.core.Response;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QSearchBeResult;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

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
		return this.getBaseEntityByCode("PRJ_" + userToken.getProductCode().toUpperCase());
	}

	/**
	 * Fetch the user base entity of the {@link UserToken}
	 * 
	 * @return the user's {@link BaseEntity}
	 */
	public BaseEntity getUserBaseEntity() {
		return this.getBaseEntityByCode(userToken.getUserCode());
	}

	/**
	 * Fetch A {@link BaseEntity} from the cache using a code.
	 *
	 * @param code The code of the BaseEntity to fetch
	 * @return The corresponding BaseEntity, or null if not found.
	 */
	public BaseEntity getBaseEntityByCode(String code) {
		if ("backend".equals(userToken.getProductCode())) {
			userToken.setProductCode("internmatch");
		}
		log.info("TOKEN PRODUCT CODEE: " + userToken.getProductCode());
		return getBaseEntityByCode(userToken.getProductCode(), code);
	}

	/**
	 * Fetch A {@link BaseEntity} from the cache using a code.
	 *
	 * @param productCode The productCode to use
	 * @param code The code of the BaseEntity to fetch
	 * @return The corresponding BaseEntity, or null if not found.
	 */
	public BaseEntity getBaseEntityByCode(String productCode, String code) {

		// check for entity in the cache
		// BaseEntityKey key = new BaseEntityKey(productCode, code);
		// BaseEntity entity = (BaseEntity) CacheUtils.getEntity(GennyConstants.CACHE_NAME_BASEENTITY, key);
		BaseEntity entity = null;

		// check in database if not in cache
		if (entity == null) {
			log.debug("BaseEntity " + code + " not in cache, checking in database...");
			entity = databaseUtils.findBaseEntityByCode(productCode, code);
		}

		return entity;
	}

	/**
	 * Call the Fyodor API to fetch a list of {@link BaseEntity}
	 * objects using a {@link SearchEntity} object.
	 *
	 * @param searchBE A {@link SearchEntity} object used to determine the results
	 * @return A list of {@link BaseEntity} objects
	 */
	public List<BaseEntity> getBaseEntitys(SearchEntity searchBE) {

		// build uri, serialize payload and fetch data from fyodor
		String uri = GennySettings.fyodorServiceUrl() + "/api/search/fetch";
		String json = jsonb.toJson(searchBE);
		HttpResponse<String> response = HttpUtils.post(uri, json, userToken.getToken());

		if (response == null) {
			log.error("Null response from " + uri);
			return null;
		}

		Integer status = response.statusCode();

		if (Response.Status.Family.familyOf(status) != Response.Status.Family.SUCCESSFUL) {
			log.error("Bad response status " + status + " from " + uri);
		}

		try {
			// deserialise and grab entities
			QSearchBeResult results = jsonb.fromJson(response.body(), QSearchBeResult.class);
			return Arrays.asList(results.getEntities());
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Call the Fyodor API to fetch a list of codes 
	 * associated with result entities.
	 *
	 * @param searchBE A {@link SearchEntity} object used to determine the results
	 * @return A list of code strings
	 */
	public List<String> getBaseEntityCodes(SearchEntity searchBE) {

		// build uri, serialize payload and fetch data from fyodor
		String uri = GennySettings.fyodorServiceUrl() + "/api/search";
		String json = jsonb.toJson(searchBE);
		HttpResponse<String> response = HttpUtils.post(uri, json, userToken);

		if (response == null) {
			log.error("Null response from " + uri);
			return null;
		}

		Integer status = response.statusCode();

		if (Response.Status.Family.familyOf(status) != Response.Status.Family.SUCCESSFUL) {
			log.error("Bad response status " + status + " from " + uri);
		}

		try {
			// deserialise and grab entities
			QSearchBeResult results = jsonb.fromJson(response.body(), QSearchBeResult.class);
			return Arrays.asList(results.getCodes());
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Call the Fyodor API to fetch a count of {@link BaseEntity}
	 * objects using a {@link SearchEntity} object.
	 *
	 * @param searchBE A {@link SearchEntity} object used to determine the results
	 * @return A count of items
	 */
	public Long getBaseEntityCount(SearchEntity searchBE) {

		// build uri, serialize payload and fetch data from fyodor
		String uri = GennySettings.fyodorServiceUrl() + "/api/search/count";
		String json = jsonb.toJson(searchBE);
		HttpResponse<String> response = HttpUtils.post(uri, json, userToken.getToken());

		if (response == null) {
			log.error("Null response from " + uri);
			return null;
		}

		Integer status = response.statusCode();

		if (Response.Status.Family.familyOf(status) != Response.Status.Family.SUCCESSFUL) {
			log.error("Bad response status " + status + " from " + uri);
		}

		try {
			// deserialise and grab entities
			QSearchBeResult results = jsonb.fromJson(response.body(), QSearchBeResult.class);
			return results.getTotal();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		return null;
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

		BaseEntityKey key = new BaseEntityKey(baseEntity.getRealm(), baseEntity.getCode());
		return (BaseEntity) CacheUtils.saveEntity(GennyConstants.CACHE_NAME_BASEENTITY, key, baseEntity);
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

		BaseEntity be = getBaseEntityByCode(baseEntityCode);
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
		BaseEntity newBe = getBaseEntityByCode(newBaseEntityCode);
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

		BaseEntity be = getBaseEntityByCode(baseEntityCode);
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

		String attributeValue = baseEntity.getValue(attributeCode, null);
		if (attributeValue == null) {
			return null;
		}
		String newBaseEntityCode = cleanUpAttributeValue(attributeValue);
		return newBaseEntityCode;
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

		BaseEntity be = getBaseEntityByCode(baseEntityCode);
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
		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getObject();
		} else {
			return null;
		}
	}

	/**
	 * Get the value of an EntityAttribute as a String.
	 *
	 * @param be            The code of the entity to grab from
	 * @param attributeCode The code of the attribute to check
	 * @return The value as a String
	 */
	public static String getBaseEntityAttrValueAsString(BaseEntity be, String attributeCode) {

		String attributeVal = null;
		for (EntityAttribute ea : be.getBaseEntityAttributes()) {
			try {
				if (ea.getAttributeCode().equals(attributeCode)) {
					attributeVal = ea.getObjectAsString();
				}
			} catch (Exception e) {
			}
		}

		return attributeVal;
	}

	/**
	 * Get the value of an EntityAttribute as a String.
	 *
	 * @param baseEntityCode The code of the entity to grab from
	 * @param attributeCode  The code of the attribute to check
	 * @return The value as a String
	 */
	public String getBaseEntityValueAsString(final String baseEntityCode, final String attributeCode) {

		String attrValue = null;

		if (baseEntityCode != null) {

			BaseEntity be = getBaseEntityByCode(baseEntityCode);
			attrValue = getBaseEntityAttrValueAsString(be, attributeCode);
		}

		return attrValue;
	}

	/**
	 * Get the value of an EntityAttribute as a LocalDateTime.
	 *
	 * @param baseEntityCode The code of the entity to grab from
	 * @param attributeCode  The code of the attribute to check
	 * @return The value as a LocalDateTime
	 */
	public LocalDateTime getBaseEntityValueAsLocalDateTime(final String baseEntityCode, final String attributeCode) {
		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getValueDateTime();
		} else {
			return null;
		}
	}

	/**
	 * Get the value of an EntityAttribute as a LocalDate.
	 *
	 * @param baseEntityCode The code of the entity to grab from
	 * @param attributeCode  The code of the attribute to check
	 * @return The value as a LocalDate
	 */
	public LocalDate getBaseEntityValueAsLocalDate(final String baseEntityCode, final String attributeCode) {
		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getValueDate();
		} else {
			return null;
		}
	}

	/**
	 * Get the value of an EntityAttribute as a LocalTime.
	 *
	 * @param baseEntityCode The code of the entity to grab from
	 * @param attributeCode  The code of the attribute to check
	 * @return The value as a LocalTime
	 */
	public LocalTime getBaseEntityValueAsLocalTime(final String baseEntityCode, final String attributeCode) {

		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getValueTime();
		} else {
			return null;
		}
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
				.map(item -> (BaseEntity) getBaseEntityByCode(item))
				.collect(Collectors.toList());

		return entityList;
	}

	/**
	 * Create a new {@link BaseEntity} using a DEF entity.
	 *
	 * @param defBE The def entity to use
	 * @return The created BaseEntity
	 * @throws Exception If the entity could not be created
	 */
	public BaseEntity create(final BaseEntity defBE) throws Exception {
		return create(defBE, null, null);
	}

	/**
	 * Create a new {@link BaseEntity} using a DEF entity and a name.
	 *
	 * @param defBE The def entity to use
	 * @param name  The name of the entity
	 * @return The created BaseEntity
	 * @throws Exception If the entity could not be created
	 */
	public BaseEntity create(final BaseEntity defBE, String name) throws Exception {
		return create(defBE, name, null);
	}

	/**
	 * Create a new {@link BaseEntity} using a name and code.
	 *
	 * @param defBE The def entity to use
	 * @param name  The name of the entity
	 * @param code  The code of the entity
	 * @return The created BaseEntity
	 * @throws Exception If the entity could not be created
	 */
	public BaseEntity create(final BaseEntity defBE, String name, String code) throws Exception {

		if (defBE == null) {
			String errorMsg = "defBE is NULL";
			log.error(errorMsg);
			throw new Exception(errorMsg);
		}

		if (code != null && code.charAt(3) != '_') {
			String errorMsg = "Code parameter " + code + " is not a valid BE code!";
			log.error(errorMsg);
			throw new Exception(errorMsg);
		}

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
			if (StringUtils.isBlank(prefix)) {
				log.error("No prefix set for the def: " + defBE.getCode());
				throw new Exception("No prefix set for the def: " + defBE.getCode());
			}
			if (StringUtils.isBlank(code)) {
				code = prefix + "_" + UUID.randomUUID().toString().substring(0, 32).toUpperCase();
			}

			if (StringUtils.isBlank(name)) {
				name = defBE.getName();
			}

			// create entity and set realm
			item = new BaseEntity(code.toUpperCase(), name);
			item.setRealm(userToken.getProductCode());
		}

		// save to DB and cache
		databaseUtils.saveBaseEntity(item);
		try {
			updateBaseEntity(item);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Establish all mandatory base entity attributes
		for (EntityAttribute ea : defBE.getBaseEntityAttributes()) {
			if (ea.getAttributeCode().startsWith("ATT_")) {

				String attrCode = ea.getAttributeCode().substring("ATT_".length());
				Attribute attribute = qwandaUtils.getAttribute(attrCode);

				if (attribute != null) {

					// if not already filled in
					if (!item.containsEntityAttribute(attribute.getCode())) {
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
							EntityAttribute newEA = new EntityAttribute(item, attribute, ea.getWeight(),
									defaultVal);

							log.info("Adding mandatory/default -> " + attribute.getCode());
							item.addAttribute(newEA);
						}
					} else {
						log.info(item.getCode() + " already has value for " + attribute.getCode());
					}

				} else {
					log.warn("No Attribute found for def attr " + attrCode);
				}
			}
		}

		try {
			updateBaseEntity(item);
			CacheUtils.putObject(userToken.getProductCode(), item.getCode(), item);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return item;
	}

	/**
	 * Create a new user {@link BaseEntity} using a DEF entity.
	 *
	 * @param defBE The def entity to use
	 * @param email The email to use
	 * @return The created BaseEntity
	 * @throws Exception If the user could not be created
	 */
	public BaseEntity createUser(final BaseEntity defBE, final String email) throws Exception {

		BaseEntity item = null;
		String uuid = null;
		Optional<EntityAttribute> uuidEA = defBE.findEntityAttribute("ATT_PRI_UUID");

		if (uuidEA.isPresent()) {

			if (!StringUtils.isBlank(email)) {
				// TODO: run a regexp check to see if the email is valid

				if (!email.startsWith("random+")) {
					// TODO: check to see if the email exists in the database and keycloak
				}
			}
			// this is a user, generate keycloak id
			uuid = KeycloakUtils.createDummyUser(serviceToken.getToken(), userToken.getKeycloakRealm());
			Optional<String> optCode = defBE.getValue("PRI_PREFIX");

			if (optCode.isPresent()) {

				String name = defBE.getName();
				String code = optCode.get() + "_" + uuid.toUpperCase();
				item = new BaseEntity(code, name);
				item.setRealm(userToken.getProductCode());

				if (item != null) {
					// Add PRI_EMAIL
					if (!email.startsWith("random+")) {
						// Check to see if the email exists
						// TODO: check to see if the email exists in the database and keycloak
						Attribute emailAttribute = qwandaUtils.getAttribute("PRI_EMAIL");
						item.addAnswer(new Answer(item, item, emailAttribute, email));
						Attribute usernameAttribute = qwandaUtils.getAttribute("PRI_USERNAME");
						item.addAnswer(new Answer(item, item, usernameAttribute, email));
					}

					// Add PRI_UUID
					Attribute uuidAttribute = qwandaUtils.getAttribute("PRI_UUID");
					item.addAnswer(new Answer(item, item, uuidAttribute, uuid.toUpperCase()));

					// Keycloak UUID
					Attribute keycloakAttribute = qwandaUtils.getAttribute("PRI_KEYCLOAK_UUID");
					item.addAnswer(new Answer(item, item, keycloakAttribute, uuid.toUpperCase()));

					// Author of the BE
					// NOTE: Maybe should be moved to run for all BEs
					Attribute lnkAuthorAttr = qwandaUtils.getAttribute("LNK_AUTHOR");
					item.addAnswer(new Answer(item, item, lnkAuthorAttr, "[\""+userToken.getUserCode()+"\"]"));
				} else {
					log.error("create BE returned NULL for " + code);
				}

			} else {
				log.error("Prefix not provided");
				throw new Exception("Prefix not provided" + defBE.getCode());
			}
		} else {
			log.error("Passed defBE is not a user def!");
			throw new Exception("Passed defBE is not a user def!" + defBE.getCode());
		}

		return item;
	}
}
