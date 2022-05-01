package life.genny.qwandaq.utils;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.io.Serializable;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Answers;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QSearchBeResult;

/**
 * A non-static utility class used for standard
 * operations involving BaseEntitys.
 * 
 * @author Adam Crow
 * @author Jasper Robison
 */
@RegisterForReflection
public class BaseEntityUtils implements Serializable {

	static final Logger log = Logger.getLogger(BaseEntityUtils.class);
	Jsonb jsonb = JsonbBuilder.create();
	String token;
	String realm;
	GennyToken gennyToken;
	GennyToken serviceToken;

	public static final String BASEENTITY_CACHE = "baseentity";

	public BaseEntityUtils() {
	}

	public BaseEntityUtils(String token, String realm) {
		this(new GennyToken(token));
		this.realm = realm;
	}

	public BaseEntityUtils(GennyToken serviceToken, GennyToken userToken) {
		this(userToken);
		this.serviceToken = serviceToken;
	}

	public BaseEntityUtils(GennyToken gennyToken) {
		this.token = gennyToken.getToken();
		this.realm = gennyToken.getRealm();
		this.gennyToken = gennyToken;
	}

	/**
	 * Get the Token
	 *
	 * @return The token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Set the Token
	 *
	 * @param token The token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * Get the current realm
	 *
	 * @return The realm
	 */
	public String getRealm() {
		return gennyToken.getRealm();
	}

	/**
	 * Get the GennyToken
	 *
	 * @return The gennyToken
	 */
	public GennyToken getGennyToken() {
		return gennyToken;
	}

	/**
	 * Set the GennyToken
	 *
	 * @param gennyToken The genny token to set
	 */
	public void setGennyToken(GennyToken gennyToken) {
		this.gennyToken = gennyToken;
	}

	/**
	 * Get the ServiceToken
	 *
	 * @return The serviceToken
	 */
	public GennyToken getServiceToken() {
		return serviceToken;
	}

	/**
	 * Set the ServiceToken
	 *
	 * @param serviceToken The serviceToken to set
	 */
	public void setServiceToken(GennyToken serviceToken) {
		this.serviceToken = serviceToken;
	}

	/**
	 * Get a string representation of the instance
	 *
	 * @return A string representation of the object
	 */
	@Override
	public String toString() {
		return "BaseEntityUtils [" + (realm != null ? "realm=" + realm : "") + ": "
				+ StringUtils.abbreviateMiddle(token, "...", 30) + "]";
	}

	/**
	 * Fetch the user base entity of the {@link GennyToken} used to initialise the
	 * BaseEntityUtils
	 * 
	 * @return the user {@link BaseEntity}
	 */
	public BaseEntity getProjectBaseEntity() {
		return this.getBaseEntityByCode("PRJ_" + this.getGennyToken().getRealm().toUpperCase());
	}

	/**
	 * Fetch the user base entity of the {@link GennyToken} used to initialise the
	 * BaseEntityUtils
	 * 
	 * @return the user {@link BaseEntity}
	 */
	public BaseEntity getUserBaseEntity() {
		return this.getBaseEntityByCode(this.getGennyToken().getUserCode());
	}

	/**
	 * Update the {@link GennyToken} of this utils instance. Unlike the standard
	 * setter method, this will also update the token and the realm.
	 *
	 * @param gennyToken The genny token to update with
	 */
	public void updateGennyToken(GennyToken gennyToken) {
		this.token = gennyToken.getToken();
		this.realm = gennyToken.getRealm();
		this.gennyToken = gennyToken;
	}

	/**
	 * Fetch A {@link BaseEntity} from cache the using a code.
	 *
	 * @param code The code of the {@link BaseEntity} to fetch
	 * @return The corresponding BaseEntity, or null if not found.
	 */
	public BaseEntity getBaseEntityByCode(String code) {

		return CacheUtils.getObject(this.realm, code, BaseEntity.class);
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
		HttpResponse<String> response = HttpUtils.post(uri, json, this.token);

		if (response != null) {
			String body = response.body();
			log.info("Post " + searchBE.getCode() + " to url " + uri + ", response code:" + response.statusCode());

			if (body != null) {
				try {
					// deserialise and grab entities
					QSearchBeResult results = jsonb.fromJson(body, QSearchBeResult.class);
					return Arrays.asList(results.getEntities());
				} catch (Exception e) {
					log.error(e.getMessage());
					e.printStackTrace();
				}
			}
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
		String uri = GennySettings.fyodorServiceUrl() + "/api/search/fetch";
		String json = jsonb.toJson(searchBE);
		HttpResponse<String> response = HttpUtils.post(uri, json, this.token);
		String body = response.body();

		if (body != null) {
			try {
				// deserialise and grab entities
				QSearchBeResult results = jsonb.fromJson(body, QSearchBeResult.class);
				return results.getTotal();
			} catch (Exception e) {
				log.error(e);
			}
		}

		return null;
	}

	/**
	 * Update a {@link BaseEntity} in the database and the cache.
	 *
	 * @param baseEntity The BaseEntity to update
	 */
	public void updateBaseEntity(BaseEntity baseEntity) {
		/*DatabaseUtils databaseUtils = new DatabaseUtils();
		databaseUtils.saveBaseEntity(baseEntity);*/
		BaseEntityKey key = new BaseEntityKey(baseEntity.getRealm(),baseEntity.getCode());
		CacheUtils.saveEntity(BASEENTITY_CACHE, key, baseEntity);
	}

	/**
	 * Get the BaseEntity that is linked with a specific attribute. Generally this
	 * will be a LNK attribute, although it doesn't have to be.
	 *
	 * @param baseEntityCode The targeted BaseEntity Code
	 * @param attributeCode  The attribute storing the data
	 * @return The BaseEntity with code stored in the attribute
	 */
	public BaseEntity getBaseEntityFromLNKAttr(String baseEntityCode, String attributeCode) {

		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		return getBaseEntityFromLNKAttr(be, attributeCode);
	}

	/**
	 * Get the BaseEntity that is linked with a specific attribute.
	 *
	 * @param baseEntity    The targeted BaseEntity
	 * @param attributeCode The attribute storing the data
	 * @return The BaseEntity with code stored in the attribute
	 */
	public BaseEntity getBaseEntityFromLNKAttr(BaseEntity baseEntity, String attributeCode) {

		String newBaseEntityCode = getBaseEntityCodeFromLNKAttr(baseEntity, attributeCode);
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
	public String getBaseEntityCodeFromLNKAttr(String baseEntityCode, String attributeCode) {

		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		return getBaseEntityCodeFromLNKAttr(be, attributeCode);
	}

	/**
	 * Get the code of the BaseEntity that is linked with a specific attribute.
	 *
	 * @param baseEntity    The targeted BaseEntity
	 * @param attributeCode The attribute storing the data
	 * @return The BaseEntity code stored in the attribute
	 */
	public String getBaseEntityCodeFromLNKAttr(BaseEntity baseEntity, String attributeCode) {

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
	public List<String> getBaseEntityCodeArrayFromLNKAttr(String baseEntityCode, String attributeCode) {

		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		return getBaseEntityCodeArrayFromLNKAttr(be, attributeCode);
	}

	/**
	 * Get an ArrayList of BaseEntity codes that are linked with a specific
	 * attribute.
	 *
	 * @param baseEntity    The targeted BaseEntity
	 * @param attributeCode The attribute storing the data
	 * @return An ArrayList of codes stored in the attribute
	 */
	public List<String> getBaseEntityCodeArrayFromLNKAttr(BaseEntity baseEntity, String attributeCode) {

		String attributeValue = getBaseEntityCodeFromLNKAttr(baseEntity, attributeCode);
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
	 * Save an {@link Answer} object.
	 *
	 * @param answer The answer to save
	 * @return The target BaseEntity
	 */
	public BaseEntity saveAnswer(Answer answer) {

		List<BaseEntity> targets = saveAnswers(Arrays.asList(answer));

		if (targets != null && targets.size() > 0) {
			return targets.get(0);
		}

		return null;
	}

	/**
	 * Save {@link Answers}.
	 * 
	 * @param answers The answers to save
	 * @return The target BaseEntitys
	 */
	public List<BaseEntity> saveAnswers(Answers answers) {

		return saveAnswers(answers.getAnswers());
	}

	/**
	 * Save a List of {@link Answer} objects.
	 *
	 * @param answers The list of answers to save
	 * @return The target BaseEntitys
	 */
	public List<BaseEntity> saveAnswers(List<Answer> answers) {

		List<BaseEntity> targets = new ArrayList<>();

		// sort answers into target BaseEntitys
		Map<String, List<Answer>> answersPerTargetCodeMap = answers.stream()
				.collect(Collectors.groupingBy(Answer::getTargetCode));

		for (String targetCode : answersPerTargetCodeMap.keySet()) {

			// check if target is valid
			BaseEntity target = getBaseEntityByCode(targetCode);
			if (target == null) {
				log.error(targetCode + " does not exist!");
				continue;
			}

			// fetch the DEF for this target
			DefUtils defUtils = new DefUtils();
			BaseEntity defBE = defUtils.getDEF(target);

			// filter Non-valid answers using def
			List<Answer> group = answersPerTargetCodeMap.get(targetCode);
			List<Answer> validAnswers = group.stream()
					.filter(item -> defUtils.answerValidForDEF(defBE, item))
					.collect(Collectors.toList());

			// update target using valid answers
			for (Answer answer : validAnswers) {
				try {
					target.addAnswer(answer);
				} catch (BadDataException e) {
					log.error(e);
				}
			}

			// update target in the cache
			CacheUtils.putObject(realm, target.getCode(), target);

			// update target in the DB
			DatabaseUtils databaseUtils = new DatabaseUtils();
			databaseUtils.saveBaseEntity(target);

			targets.add(target);
		}

		return targets;
	}

	/**
	 * Create a new {@link BaseEntity} using a DEF entity code.
	 *
	 * @param defCode The defCode to use
	 * @return The created BaseEntity
	 * @throws Exception If the entity could not be created
	 */
	public BaseEntity create(final String defCode) throws Exception {

		String realm = this.getGennyToken().getRealm();
		DefUtils defUtils = new DefUtils();
		BaseEntity defBE = defUtils.getDefMap(realm).get(defCode);

		return create(defBE);
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
		QwandaUtils qwandaUtils = new QwandaUtils();

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
			item = new BaseEntity(code.toUpperCase(), name);

			item.setRealm(getRealm());
		}

		if (item != null) {
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
		}

		// update in DB and cache
		updateBaseEntity(item);

		// force the type of baseentity

		Attribute attributeDEF = qwandaUtils.getAttribute("PRI_IS_" + defBE.getCode().substring("DEF_".length()));
		item = saveAnswer(new Answer(item, item, attributeDEF, "TRUE"));

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

		QwandaUtils qwandaUtils = new QwandaUtils();
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
			uuid = KeycloakUtils.createDummyUser(serviceToken.getToken(), serviceToken.getRealm());
			Optional<String> optCode = defBE.getValue("PRI_PREFIX");
			if (optCode.isPresent()) {
				String name = defBE.getName();
				String code = optCode.get() + "_" + uuid.toUpperCase();
				item = new BaseEntity(code, name);
				item.setRealm(getRealm());
				// item = QwandaUtils.createBaseEntityByCode(code, name, qwandaServiceUrl,
				// this.token);
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
					item.addAnswer(
							new Answer(item, item, lnkAuthorAttr, "[\"" + getGennyToken().getUserCode() + "\"]"));
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
