package life.genny.qwandaq.managers;

import life.genny.qwandaq.CoreEntity;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.data.GennyCache;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import life.genny.qwandaq.serialization.attribute.AttributeKey;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.serialization.common.CoreEntityKey;
import life.genny.qwandaq.serialization.datatype.DataTypeKey;
import life.genny.qwandaq.serialization.entityattribute.EntityAttributeKey;
import life.genny.qwandaq.serialization.question.QuestionKey;
import life.genny.qwandaq.serialization.questionquestion.QuestionQuestionKey;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.qwandaq.validation.Validation;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.dsl.QueryResult;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/*
 * A utility class used for standard read and write 
 * operations to the cache.
 * 
 * @author Jasper Robison
 */
@ApplicationScoped
public class CacheManager {

	Jsonb jsonb = JsonbBuilder.create();

	private GennyCache cache;

	@Inject
	Logger log;

	@Inject
	UserToken userToken;

	@Inject
	QuestionUtils questionUtils;

	@Inject
	BaseEntityUtils baseEntityUtils;

	/**
	 * @param gennyCache the gennyCache to set
	 */
	public void init(GennyCache gennyCache) {
		cache = gennyCache;
	}

	/**
	* Clear a remote realm cache
	*
	* @param realm The realm of the cache to clear
	 */
	public void clear(String realm) {
		cache.getRemoteCache(realm).clear();
	}

	/**
	 * Read a stringified item from a realm cache.
	 *
	 * @param realm the realm to read from
	 * @param key the key to read
	 * @return Object
	 */
	public Object readCache(String realm, String key) {
		Object ret = cache.getRemoteCache(realm).get(key);
		return ret;
	}

	/**
	 * Write a stringified item to a realm cache.
	 *
	 * @param realm The realm cache to use.
	 * @param key   The key to save under.
	 * @param value The value to save.
	 * 
	 * @return returns the newly written value
	 */
	public String writeCache(String realm, String key, String value) {
		log.debugf("realm: %s, key: %s", realm, key);
		RemoteCache<String, String> remoteCache = cache.getRemoteCache(realm);
		remoteCache.put(key, value);
		return remoteCache.get(key);
	}

	/**
	* Remove an entry from a realm cache.
	*
	* @param realm The realm cache to remove from.
	* @param key The key of the entry to remove.
	 */
	public void removeEntry(String realm, String key) {
		cache.getRemoteCache(realm).remove(key);
	}

	/**
	 * Get an object from a realm cache using a {@link Class}.
	 *
	 * @param <T> the Type to cast as
	 * @param realm the realm to get from
	 * @param key the key to get
	 * @param c the Class to get as
	 * @return T
	 */
	public <T> T getObject(String realm, String key, Class<T> c) {
		String data = (String) readCache(realm, key);
		log.tracef("key: %s, value: %s", key, data);
		if (StringUtils.isEmpty(data)) {
			return null;
		}
		T object = jsonb.fromJson(data, c);
		return object;
	}

	/**
	 * Get an object from a realm cache using a {@link Type}.
	 *
	 * @param <T> the Type to cast as
	 * @param realm the realm to get from
	 * @param key the key to get
	 * @param t the Type to get as
	 * @return T
	 */
	public <T> T getObject(String realm, String key, Type t) {
		String data = (String) readCache(realm, key);
		if (data == null) {
			return null;
		}
		T object = jsonb.fromJson(data, t);
		return object;
	}

	/**
	 * Put an object into the cache.
	 *
	 * @param realm the realm to put object into
	 * @param key the key to put object under
	 * @param obj the obj to put
	 */
	public void putObject(String realm, String key, Object obj) {
		String json = jsonb.toJson(obj);
		cache.getRemoteCache(realm).put(key, json);
		log.tracef("Caching: [%s:%s]=%s", realm , key, obj);
	}

	/**
	* Get a CoreEntity object from the cache using a CoreEntityKey.
	* 
	* @param cacheName The cache to read from
	* @param key The key they item is saved against
	* @return The CoreEntity returned
	 */
	public CoreEntitySerializable getEntity(String cacheName, CoreEntityKey key) {
		return cache.getEntityFromCache(cacheName, key);
	}

	/**
	 * Get a CoreEntity object from the cache using a CoreEntityKey.
	 *
	 * @param cacheName The cache to read from
	 * @param key The key they item is saved against
	 * @return The CoreEntity returned
	 */
	public CoreEntityPersistable getPersistableEntity(String cacheName, CoreEntityKey key) {
		return cache.getPersistableEntityFromCache(cacheName, key);
	}

	/**
	 * Get a CoreEntity object from the cache using a CoreEntityKey.
	 *
	 * @param cacheName The cache to get from
	 * @param key The key to the entity to remove
	 * @return The removed persistable core entity
	 */
	public CoreEntityPersistable removePersistableEntity(String cacheName, CoreEntityKey key) {
		return cache.removeEntityFromCache(cacheName, key);
	}

	/**
	* Save a {@link CoreEntity} to the cache using a CoreEntityKey.
	*
	* @param cacheName The cache to save to
	* @param key The key to save against
	* @param entity The CoreEntity to save
	* @return The CoreEntity being saved
	 */
	public boolean saveEntity(String cacheName, CoreEntityKey key, CoreEntityPersistable entity) {
		return cache.putEntityIntoCache(cacheName, key, entity);
	}

	/**
	 * Get a list of {@link CoreEntity}s to from cache by prefix.
	 * @param cacheName - Product Code / Cache to retrieve from
	 * @param prefix - Prefix of the Core Entity code to use
	 * @param callback - Callback to construct a {@link CoreEntityKey} for cache retrieval
	 * @return a list of core entities with matching prefixes
	 * 
	 * See Also: {@link CoreEntityKey}, {@link FICacheKeyCallback}
	 */
	public List<CoreEntity> getEntitiesByPrefix(String cacheName, String prefix, CoreEntityKey keyStruct) {
		List<CoreEntity> entities = cache.getRemoteCache(cacheName)
		.entrySet().stream().map((Map.Entry<String, String> entry) -> {
			String key = entry.getKey();
			CoreEntityKey currentKey = keyStruct.fromKey(key);

			return currentKey.getEntityCode().startsWith(prefix) ? jsonb.fromJson(entry.getValue(), CoreEntity.class) : null;
		})
		.filter(Objects::nonNull).collect(Collectors.toList());

		return entities;
	}

	/**
	 * Get a list of {@link BaseEntity}s to from cache by prefix.
	 * @param cacheName - Product Code / Cache to retrieve from
	 * @param prefix - Prefix of the Core Entity code to use
	 * @return a list of base entities with matching prefixes
	 * 
	 * See Also: {@link BaseEntityKey}, {@link CoreEntityKey#fromKey}, {@link CacheManager#getEntitiesByPrefix}
	 */
	public List<BaseEntity> getBaseEntitiesByPrefix(String cacheName, String prefix) {
		return getEntitiesByPrefix(cacheName, prefix, new BaseEntityKey())
		.stream().map((CoreEntity entity) -> (BaseEntity)entity).collect(Collectors.toList());
	}

    /**
     * @param code
     * @return
     */
    public Attribute getAttribute(String code) {
		return getAttribute(userToken.getProductCode(), code);
    }

    /**
     * @param productCode
     * @param code
     * @return
     */
    public Attribute getAttribute(String productCode, String code) {
        AttributeKey key = new AttributeKey(productCode, code);
		return (Attribute) getPersistableEntity(GennyConstants.CACHE_NAME_ATTRIBUTE, key);
    }

	/**
	 * Fetch all attributes for a product.
	 *
	 * @return Collection of all attributes in the system across all products
	 */
	public List<Attribute> getAllAttributes() {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_ATTRIBUTE));
		Query<Attribute> query = queryFactory
				.create("from life.genny.qwandaq.persistence.attribute.Attribute");
		QueryResult<Attribute> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<Attribute> attributeList = queryResult.list();
		if (attributeList.size() == 1 && attributeList.get(0) == null)
			return new ArrayList<>(0);
		return attributeList;
	}

	/**
	 * Fetch all attributes for a product.
	 *
	 * @param productCode
	 * @return
	 */
	public List<Attribute> getAttributesForProduct(String productCode) {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_ATTRIBUTE));
		Query<Attribute> query = queryFactory
				.create("from life.genny.qwandaq.persistence.attribute.Attribute where realm = '" + productCode + "'");
		QueryResult<Attribute> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<Attribute> attributeList = queryResult.list();
		if (attributeList.size() == 1 && attributeList.get(0) == null)
			return new ArrayList<>(0);
		return attributeList;
	}

	/**
	 * Fetch all attributes with a given prefix value in code for a product.
	 *
	 * @param prefix
	 * @return
	 */
	public List<Attribute> getAttributesWithPrefix(String prefix) {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_ATTRIBUTE));
		Query<Attribute> query = queryFactory
				.create("from life.genny.qwandaq.persistence.attribute.Attribute where code like '" + prefix + "%'");
		QueryResult<Attribute> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<Attribute> attributeList = queryResult.list();
		if (attributeList.size() == 1 && attributeList.get(0) == null)
			return new ArrayList<>(0);
		return attributeList;
	}

	/**
	 * Fetch all attributes with a given prefix value in code for a product.
	 *
	 * @param productCode
	 * @param prefix
	 * @return
	 */
	public List<Attribute> getAttributesWithPrefixForProduct(String productCode, String prefix) {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_ATTRIBUTE));
		Query<Attribute> query = queryFactory
				.create("from life.genny.qwandaq.persistence.attribute.Attribute where realm = '" + productCode
						+ "' and code like '" + prefix + "%'");
		QueryResult<Attribute> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<Attribute> attributeList = queryResult.list();
		if (attributeList.size() == 1 && attributeList.get(0) == null)
			return new ArrayList<>(0);
		return attributeList;
	}

	public DataType getDataType(String productCode, String code) {
		DataTypeKey key = new DataTypeKey(productCode, code);
		return (DataType) cache.getPersistableEntityFromCache(GennyConstants.CACHE_NAME_DATATYPE, key);
	}

	public List<Validation> getValidations(String productCode, String commaSeparatedValidationCodes) {
		String inClauseValue = StringUtils.replace(commaSeparatedValidationCodes, ",", "','");
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_VALIDATION));
		Query<Validation> query = queryFactory
				.create("from life.genny.qwandaq.persistence.validation.Validation where realm = '" + productCode +
						"' and code in ('"+inClauseValue+"')");
		QueryResult<Validation> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<Validation> validationList = queryResult.list();
		if (validationList.size() == 1 && validationList.get(0) == null)
			return new ArrayList<>(0);
		return validationList;
	}

	public List<Validation> getValidations(String productCode, List<String> validationCodes) {
		return getValidations(productCode, new CopyOnWriteArrayList<>(validationCodes).toString());
	}

	/**
	 * Get a list of {@link BaseEntity}s to from cache by prefix.
	 *
	 * @param productCode - Product Code to retrieve from
	 * @param prefix - Prefix of the Core Entity code to use
	 * @return a list of base entities with matching prefixes
	 *
	 * See Also: {@link BaseEntityKey}, {@link CoreEntityKey#fromKey}, {@link CacheManager#getEntitiesByPrefix}
	 */
	public List<BaseEntity> getBaseEntitiesByPrefixUsingIckle(String productCode, String prefix) {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_BASEENTITY));
		Query<BaseEntity> query = queryFactory
				.create("from life.genny.qwandaq.persistence.baseentity.BaseEntity where realm = '" + productCode
						+ "' and code like '" + prefix + "%'");
		QueryResult<BaseEntity> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<BaseEntity> baseEntityList = queryResult.list();
		if (baseEntityList.size() == 1 && baseEntityList.get(0) == null)
			return new ArrayList<>(0);
		return baseEntityList;
	}

	/**
	 * @param ickleQuery
	 * @return
	 */
	public List<life.genny.qwandaq.serialization.baseentity.BaseEntity> getBaseEntitiesUsingIckle(String ickleQuery) {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_BASEENTITY));
		Query<life.genny.qwandaq.serialization.baseentity.BaseEntity> query = queryFactory.create(ickleQuery);
		QueryResult<life.genny.qwandaq.serialization.baseentity.BaseEntity> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		return queryResult.list();
	}

	/**
	 * Get a list of {@link EntityAttribute}s to from cache for a BaseEntity.
	 *
	 * @param productCode - Product Code / Cache to retrieve from
	 * @param baseEntityCode - Prefix of the Core Entity code to use
	 * @return a list of base entities with matching prefixes
	 *
	 * See Also: {@link BaseEntityKey}, {@link CoreEntityKey#fromKey}, {@link CacheManager#getEntitiesByPrefix}
	 */
	public List<EntityAttribute> getAllBaseEntityAttributesForBaseEntity(String productCode, String baseEntityCode) {
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_BASEENTITY_ATTRIBUTE);
		QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
		Query<EntityAttribute> query = queryFactory
				.create("from life.genny.qwandaq.persistence.entityattribute.EntityAttribute where realm = '" + productCode
						+ "' and baseEntityCode = '" + baseEntityCode + "'");
		QueryResult<EntityAttribute> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<EntityAttribute> attributeList = queryResult.list();
		if (attributeList.size() == 1 && attributeList.get(0) == null)
			return new ArrayList<>(0);
		return attributeList;
	}

	/**
	 * Get a list of {@link EntityAttribute}s to from cache for a BaseEntity.
	 *
	 * @param productCode - Product Code / Cache to retrieve from
	 * @param baseEntityCode - Base Entity code to use
	 * @param attributeCodePrefix - Attribute Code Prefix to use
	 * @return a list of base entities with matching prefixes
	 *
	 * See Also: {@link BaseEntityKey}, {@link CoreEntityKey#fromKey}, {@link CacheManager#getEntitiesByPrefix}
	 */
	public List<EntityAttribute> getBaseEntityAttributesForBaseEntityWithAttributeCodePrefix(String productCode, String baseEntityCode, String attributeCodePrefix) {
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_BASEENTITY_ATTRIBUTE);
		QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
		Query<EntityAttribute> query = queryFactory
				.create("from life.genny.qwandaq.persistence.entityattribute.EntityAttribute where realm = '" + productCode
						+ "' and baseEntityCode = '" + baseEntityCode + "' and attributeCode like '" + attributeCodePrefix + "%'");
		QueryResult<EntityAttribute> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<EntityAttribute> attributeList = queryResult.list();
		if (attributeList.size() == 1 && attributeList.get(0) == null)
			return new ArrayList<>(0);
		return attributeList;
	}

	/**
	 * Create an Ickle delete query using a persistence name, product code and a few conditionals
	 * @param persistenceName - the fully qualified name of the Java POJO that is used as a template for persistence of the entity(s) needing to be deleted
	 * @param productCode - the product the entity is in
	 * @param conditionalCheck - the component of the where clause of the ickle query used to find the entity
	 * 
	 * @return a correctly formatted ickle delete query with the above parameters
	 * @throws {@link IllegalStateException} if the persistenceName does not start with life.genny (as all of persistenceNames in the ecosystem do)
	 * @throws {@link IllegalStateException} if the conditionalCheck is not well formatted (not empty and  contains either single or double quotes)
	 * <p>e.g
	 * <pre>String delQuery = constructDeleteQuery("life.genny.qwandaq.persistence.entityattribute.EntityAttribute", "SOME_PRODUCT", "'baseEntityCode' = 'DEF_TEST'" and "attributeCode like 'ATT_%'");
	 * </pre>
	 * Will generate a deleting query for deleting all ATT entity attributes of base entity: DEF_TEST within product: SOME_PRODUCT</p> 
	 * 
	 * <p><b>NOTE:</b> this assumes that conditionalCheck does not start with where. Do not add where to the start of conditionalCheck</p>
	 */
	private static String constructDeleteQuery(String persistenceName, String productCode, String conditionalCheck) {
		if(!persistenceName.startsWith("life.genny") || !persistenceName.contains("persistence"))
			throw new IllegalStateException("PersistenceName likely not valid persistence object (does not start with life.genny or not in persistence folder): " + persistenceName);

		if(StringUtils.isBlank(conditionalCheck))
			throw new IllegalStateException("Blank conditional check detected. This would delete all data for " + persistenceName + " in product: " + productCode + ". Stopping");
		// TODO: Going to revisit this conditional check (not deleting the commented code)
		// if(!(conditionalCheck.contains("\'") && conditionalCheck.contains("\""))) {
		// 	throw new IllegalStateException("Conditional Check does not have quotes. Likely malformed Conditional Check: " + conditionalCheck);
		// }

		StringBuilder sb = new StringBuilder("delete from ")
						.append(persistenceName)
						.append(" where realm = '")
						.append(productCode)
						// Conditional check never blank so its okay to add and
						.append("' and ");
		
		conditionalCheck = conditionalCheck.strip();
		sb.append(conditionalCheck);
		return sb.toString();			
	}

	/**
	 * Delete an Entity from a specified cache using an ickle query
	 * @param cacheName - the cache to execute on
	 * @param deleteQueryStr - the ickle to execute
	 * @return the number of affected entries (if any)
	 * 
	 * @throws {@link IllegalStateException} if the deleteQueryStr requested is not a delete statement
	 */
	private int removePersistableEntities(String cacheName, String deleteQueryStr) {
		if(!deleteQueryStr.startsWith("delete"))
			throw new IllegalStateException("Not a delete query: " + deleteQueryStr);
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(cacheName);
		QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
		Query<EntityAttribute> query = queryFactory.create(deleteQueryStr);
		return query.executeStatement();
	}

	/**
	 * Remove a validation from cache/persistence
	 * @param productCode - product to remove from
	 * @param code - code of validation to remove
	 * @return number of entities affected by deletion
	 */
	public int removeValidation(String productCode, String code) {
		String persistenceObject = "life.genny.qwandaq.persistence.validation.Validation";
		String conditional = "code = '" + code + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(GennyConstants.CACHE_NAME_VALIDATION, deleteQuery);
	}

	/**
	 * Remove a datatype from cache/persistence
	 * @param productCode - product to remove from
	 * @param code - code of datatype to remove
	 * @return number of entities affected by deletion
	 */
	public int removeDataType(String productCode, String code) {
		String persistenceObject = "life.genny.qwandaq.persistence.datatype.DataType";
		String conditional = "code = '" + code + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(GennyConstants.CACHE_NAME_DATATYPE, deleteQuery);
	}

	/**
	 * Remove an Attribute from cache/persistence
	 * @param productCode - product to remove from
	 * @param code - code of attribute to remove
	 * @return number of entities affected by deletion
	 */
	public int removeAttribute(String productCode, String code) {
		String persistenceObject = "life.genny.qwandaq.persistence.attribute.Attribute";
		String conditional = "code = '" + code + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(GennyConstants.CACHE_NAME_ATTRIBUTE, deleteQuery);
	}

	/**
	 * Remove a question from cache/persistence
	 * @param productCode - product to remove from
	 * @param code - code of question to remove
	 * @return number of entities affected by deletion
	 */
	public int removeQuestion(String productCode, String code) {
		String persistenceObject = "life.genny.qwandaq.persistence.question.Question";
		String conditional = "code = '" + code + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(GennyConstants.CACHE_NAME_QUESTION, deleteQuery);
	}

	/**
	 * Remove all entity attributes from a baseentity from cache/persistence
	 * @param productCode - product to remove from
	 * @param baseEntityCode - baseEntityCode of baseentity to remove all entity attributes for
	 * @return number of entities affected by deletion
	 */
	public int removeAllEntityAttributesOfBaseEntity(String productCode, String baseEntityCode) {
		String persistenceObject = "life.genny.qwandaq.persistence.entityattribute.EntityAttribute";
		String conditional = "baseEntityCode = '" + baseEntityCode + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(GennyConstants.CACHE_NAME_BASEENTITY_ATTRIBUTE, deleteQuery);
	}

	/**
	 * Remove a single EntityAttribute from cache/persistence
	 * @param productCode - product to remove from
	 * @param baseEntityCode - base entity code of the entity attribute to remove
	 * @param attributeCode - attributeCode of the entity attribute to remove
	 * @return number of entities affected by deletion
	 */
	public int removeEntityAttribute(String productCode, String baseEntityCode, String attributeCode) {
		String persistenceObject = "life.genny.qwandaq.persistence.entityattribute.EntityAttribute";
		String conditional = "baseEntityCode = '" + baseEntityCode + "' and attributeCode = '" + attributeCode + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(GennyConstants.CACHE_NAME_BASEENTITY_ATTRIBUTE, deleteQuery);
	}

	/**
	 * Remove a QuestionQuestion from cache/persistence
	 * @param productCode - product to remove from
	 * @param sourceCode - sourceCode of QuestionQuestion to remove
	 * @param targetCode - targetCode of QuestionQuestion to remove
	 * @return number of entities affected by deletion
	 */
	public int removeQuestionQuestion(String productCode, String sourceCode, String targetCode) {
		String persistenceObject = "life.genny.qwandaq.persistence.questionquestion.QuestionQuestion";
		String conditional = "sourceCode = '" + sourceCode + "' and targetCode = '" + targetCode + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(GennyConstants.CACHE_NAME_QUESTIONQUESTION, deleteQuery);
	}

	/**
	 * Remove all QuestionQuestions with the same sourceCode (in a group) from cache/persistence
	 * @param productCode - product to remove from
	 * @param sourceCode - sourceCode of QuestionQuestion to remove
	 * @return number of entities affected by deletion
	 */
	public int removeAllQuestionQuestionsInGroup(String productCode, String sourceCode) {
		String persistenceObject = "life.genny.qwandaq.persistence.questionquestion.QuestionQuestion";
		String conditional = "sourceCode = '" + sourceCode + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(GennyConstants.CACHE_NAME_QUESTIONQUESTION, deleteQuery);
	}

	/**
	 * @param questionQuestion
	 * @return The question object to be saved
	 */
	public QuestionQuestion saveQuestionQuestion(QuestionQuestion questionQuestion) {
		QuestionQuestionKey questionKey = new QuestionQuestionKey(questionQuestion.getRealm(), questionQuestion.getParentCode(), questionQuestion.getChildCode());
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_QUESTIONQUESTION);
		return (QuestionQuestion) remoteCache.put(questionKey, questionQuestion);
	}

	/**
	 * @param question
	 * @return The question object to be saved
	 */
	public Question saveQuestion(Question question) {
		QuestionKey questionKey = new QuestionKey(question.getRealm(), question.getCode());
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_QUESTION);
		return (Question) remoteCache.put(questionKey, question);
	}

	/**
	 * @param productCode
	 * @param questionCode
	 * @return The question object corresponding to the passed productCode:questionCode
	 */
	public Question getQuestion(String productCode, String questionCode) {
		QuestionKey questionKey = new QuestionKey(productCode, questionCode);
		return (Question) getPersistableEntity(GennyConstants.CACHE_NAME_QUESTION, questionKey);
	}

	/**
	 * @param productCode
	 * @param questionCode
	 * @return
	 */
	public Question getQuestionFromBECache(String productCode, String questionCode) {
		log.info("Question BaseEntity Code = " + questionCode);
		Question question = questionUtils.getQuestionFromBaseEntityCode(productCode, questionCode);
		log.info("question = " + jsonb.toJson(question));
		return question;
	}

	/**
	 * @param parent The question for which child questions need to be fetched
	 * @return
	 */
	public List<QuestionQuestion> getQuestionQuestionsForParentQuestion(Question parent) {
		String productCode = parent.getRealm();
		String parentQuestionCode = parent.getCode();
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_QUESTIONQUESTION);
		QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
		Query<QuestionQuestion> query = queryFactory
				.create("from life.genny.qwandaq.persistence.questionquestion.QuestionQuestion where sourceCode = '" + parentQuestionCode
						+ "' and realm = '" + productCode + "' order by weight");
		// execute query
		QueryResult<QuestionQuestion> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<QuestionQuestion> questionQuestionList = queryResult.list();
		if (questionQuestionList.size() == 1 && questionQuestionList.get(0) == null)
			return new ArrayList<>(0);
		return questionQuestionList;
	}

	/**
	 * @param parent The question for which child questions need to be fetched
	 * @return
	 */
	public List<QuestionQuestion> getQuestionQuestionsForParentQuestionFromBECache(Question parent) {
		// get bea remote cache for querying
		String productCode = parent.getRealm();
		String parentCode = parent.getCode();
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_BASEENTITY_ATTRIBUTE);
		QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
		log.debug("QuestionQuestion -> productCode = " + productCode + ", questionCode = " + parentCode);
		// init query
		Query<EntityAttribute> query = queryFactory
				.create("from life.genny.qwandaq.persistence.entityattribute.EntityAttribute where baseEntityCode like '"+parentCode+"|%'"
					 + " and realm = '"+productCode+"'");
		// execute query
		QueryResult<EntityAttribute> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<EntityAttribute> attributeList = queryResult.list();
		if (attributeList.size() == 1 && attributeList.get(0) == null)
			return new ArrayList<>(0);
		// begin building QQ objects
		return questionUtils.createQuestionQuestionsForParentQuestion(parent, attributeList);
	}

	/**
	 * @param question
	 */
	public void saveQuestionAsBE(Question question) {
		life.genny.qwandaq.serialization.baseentity.BaseEntity baseEntity = questionUtils.getSerializableBaseEntityFromQuestion(question);
		BaseEntityKey bek = new BaseEntityKey(baseEntity.getRealm(), baseEntity.getCode());
		cache.putEntityIntoCache(GennyConstants.CACHE_NAME_BASEENTITY, bek, baseEntity);
		questionUtils.getSerializableBaseEntityAttributesFromQuestion(question).parallelStream().forEach(baseEntityAttribute -> {
			EntityAttributeKey beak = new EntityAttributeKey(baseEntityAttribute.getRealm(), baseEntityAttribute.getBaseEntityCode(), baseEntityAttribute.getAttributeCode());
			cache.putEntityIntoCache(GennyConstants.CACHE_NAME_BASEENTITY_ATTRIBUTE, beak, baseEntityAttribute);
		});
		question.getChildQuestions().parallelStream().forEach(questionQuestion -> saveQuestionQuestion(questionQuestion));
	}

	/**
	 * @param questionQuestion
	 */
	public void saveQuestionQuestionAsBE(QuestionQuestion questionQuestion) {
		life.genny.qwandaq.serialization.baseentity.BaseEntity baseEntity = questionUtils.getSerializableBaseEntityFromQuestionQuestion(questionQuestion);
		BaseEntityKey bek = new BaseEntityKey(baseEntity.getRealm(), baseEntity.getCode());
		cache.putEntityIntoCache(GennyConstants.CACHE_NAME_BASEENTITY, bek, baseEntity);
		questionUtils.getSerializableBaseEntityAttributesFromQuestionQuestion(questionQuestion).parallelStream().forEach(baseEntityAttribute -> {
			EntityAttributeKey beak = new EntityAttributeKey(baseEntityAttribute.getRealm(), baseEntityAttribute.getBaseEntityCode(), baseEntityAttribute.getAttributeCode());
			cache.putEntityIntoCache(GennyConstants.CACHE_NAME_BASEENTITY_ATTRIBUTE, beak, baseEntityAttribute);
		});
	}

	public Long getEntityLastUpdatedAt(String entityName, String productCode) {
		return cache.getEntityLastUpdatedAt(entityName, productCode);
	}

	public void updateEntityLastUpdatedAt(String entityName, String productCode, Long updatedTime) {
		cache.updateEntityLastUpdatedAt(entityName, productCode, updatedTime);
	}
}

