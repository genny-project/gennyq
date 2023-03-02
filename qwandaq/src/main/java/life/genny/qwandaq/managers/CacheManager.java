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
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
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
	 * Get the max validation id.
	 *
	 * @return the max id
	 */
	public Long getMaxValidationId() {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_VALIDATION));
		Query<Validation> query = queryFactory
				.create("from life.genny.qwandaq.persistence.validation.Validation order by id desc");
		QueryResult<Validation> queryResult = query.maxResults(1).execute();

		return queryResult.list().get(0).getId();
	}

	/**
	 * Get the max attribute id.
	 *
	 * @return the max id
	 */
	public Long getMaxAttributeId() {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_ATTRIBUTE));
		Query<Attribute> query = queryFactory
				.create("from life.genny.qwandaq.persistence.attribute.Attribute order by id desc");
		QueryResult<Attribute> queryResult = query.maxResults(1).execute();

		return queryResult.list().get(0).getId();
	}

	/**
	 * Get the max entity id.
	 *
	 * @return the max id
	 */
	public Long getMaxBaseEntityId() {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_BASEENTITY));
		Query<BaseEntity> query = queryFactory
				.create("from life.genny.qwandaq.persistence.baseentity.BaseEntity order by id desc");
		QueryResult<BaseEntity> queryResult = query.maxResults(1).execute();

		return queryResult.list().get(0).getId();
	}

	/**
	 * Get the max entity id.
	 *
	 * @return the max id
	 */
	public Long getMaxQuestionId() {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_QUESTION));
		Query<Question> query = queryFactory
				.create("from life.genny.qwandaq.persistence.question.Question order by id desc");
		QueryResult<Question> queryResult = query.maxResults(1).execute();

		return queryResult.list().get(0).getId();
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
		Attribute attribute = (Attribute) getPersistableEntity(GennyConstants.CACHE_NAME_ATTRIBUTE, key);
		if (attribute == null) {
			throw new ItemNotFoundException(productCode, code);
		}
        return attribute;
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
		return queryResult.list();
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
		return queryResult.list();
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
		return queryResult.list();
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
		return queryResult.list();
	}

	public DataType getDataType(String productCode, String dttCode) {
		DataTypeKey key = new DataTypeKey(productCode, dttCode);
		return (DataType) cache.getPersistableEntityFromCache(GennyConstants.CACHE_NAME_DATATYPE, key);
	}

	public List<Validation> getValidations(String productCode, String commaSeparatedValidationCodes) {
		String inClauseValue = StringUtils.replace(commaSeparatedValidationCodes, ",", "','");
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_VALIDATION));
		Query<Validation> query = queryFactory
				.create("from life.genny.qwandaq.persistence.validation.Validation where realm = '" + productCode +
						"' and code in ('"+inClauseValue+"')");
		QueryResult<Validation> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		return queryResult.list();
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
	public List<life.genny.qwandaq.serialization.baseentity.BaseEntity> getBaseEntitiesByPrefixUsingIckle(String productCode, String prefix) {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_BASEENTITY));
		Query<life.genny.qwandaq.serialization.baseentity.BaseEntity> query = queryFactory
				.create("from life.genny.qwandaq.persistence.baseentity.BaseEntity where realm = '" + productCode
						+ "' and code like '" + prefix + "%'");
		QueryResult<life.genny.qwandaq.serialization.baseentity.BaseEntity> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		return queryResult.list();
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
		return queryResult.list();
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
		return queryResult.list();
	}

	public void removePersistableEntities(String cacheName, String deleteQueryStr) {
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(cacheName);
		QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
		Query<EntityAttribute> query = queryFactory.create(deleteQueryStr);
		query.execute();
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
		return queryResult.list();
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
		// begin building QQ objects
		return questionUtils.createQuestionQuestionsForParentQuestion(parent, queryResult.list());
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
}

