package life.genny.qwandaq.utils;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.CoreEntity;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.data.GennyCache;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.serialization.baseentityattribute.BaseEntityAttribute;
import life.genny.qwandaq.serialization.baseentityattribute.BaseEntityAttributeKey;
import life.genny.qwandaq.serialization.common.CoreEntityKey;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.dsl.QueryResult;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/*
 * A static utility class used for standard read and write
 * operations to the cache.
 *
 * @author Jasper Robison
 */
@RegisterForReflection
@ApplicationScoped
public class CacheUtils {

	static final Logger log = Logger.getLogger(CacheUtils.class);

	static Jsonb jsonb = JsonbBuilder.create();

	private static GennyCache cache = null;

	@Inject
	private QuestionUtils questionUtils;

	@Inject
	private BaseEntityUtils baseEntityUtils;

	@Inject
	private BaseEntityAttributeUtils baseEntityAttributeUtils;

	/**
	 * @param gennyCache the gennyCache to set
	 */
	public static void init(GennyCache gennyCache) {
		cache = gennyCache;
	}

	/**
	 * Clear a remote realm cache
	 *
	 * @param realm The realm of the cache to clear
	 */
	public static void clear(String realm) {

		cache.getRemoteCache(realm).clear();
	}

	/**
	 * Read a stringified item from a realm cache.
	 *
	 * @param realm the realm to read from
	 * @param key the key to read
	 * @return Object
	 */
	public static Object readCache(String realm, String key) {

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
	public static String writeCache(String realm, String key, String value) {

		log.infof("realm: %s, key: %s", realm, key);
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
	public static void removeEntry(String realm, String key) {

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
	public static <T> T getObject(String realm, String key, Class<T> c) {

		log.debugf("realm: %s, key: %s", realm, key);

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
	public static <T> T getObject(String realm, String key, Type t) {

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
	public static void putObject(String realm, String key, Object obj) {

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
	public static CoreEntitySerializable getEntity(String cacheName, CoreEntityKey key) {
		return cache.getEntityFromCache(cacheName, key);
	}

	/**
	 * Save a {@link CoreEntity} to the cache using a CoreEntityKey.
	 *
	 * @param cacheName The cache to save to
	 * @param key The key to save against
	 * @param entity The CoreEntity to save
	 * @return The CoreEntity being saved
	 */
	public static boolean saveEntity(String cacheName, CoreEntityKey key, CoreEntityPersistable entity) {
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
	static List<CoreEntity> getEntitiesByPrefix(String cacheName, String prefix, CoreEntityKey keyStruct) {
		List<CoreEntity> entities = cache.getRemoteCache(cacheName)
				.entrySet().stream().map((Map.Entry<String, String> entry) -> {
					String key = entry.getKey();
					CoreEntityKey currentKey = keyStruct.fromKey(key);

					return currentKey.getEntityCode().startsWith(prefix) ? jsonb.fromJson(entry.getValue(), CoreEntity.class) : null;
				})
				.filter((CoreEntity entity) -> {
					return entity != null;
				}).collect(Collectors.toList());

		return entities;
	}

	/**
	 * Get a list of {@link BaseEntity}s to from cache by prefix.
	 * @param cacheName - Product Code / Cache to retrieve from
	 * @param prefix - Prefix of the Core Entity code to use
	 * @return a list of base entities with matching prefixes
	 *
	 * See Also: {@link BaseEntityKey}, {@link CoreEntityKey#fromKey}, {@link CacheUtils#getEntitiesByPrefix}
	 */
	public static List<BaseEntity> getBaseEntitiesByPrefix(String cacheName, String prefix) {
		return getEntitiesByPrefix(cacheName, prefix, new BaseEntityKey())
				.stream().map((CoreEntity entity) -> (BaseEntity)entity).collect(Collectors.toList());
	}

	/**
	 * Get a list of {@link BaseEntity}s to from cache by prefix.
	 * @param productCode - Product Code to retrieve from
	 * @param prefix - Prefix of the Core Entity code to use
	 * @return a list of base entities with matching prefixes
	 *
	 * See Also: {@link BaseEntityKey}, {@link CoreEntityKey#fromKey}, {@link CacheUtils#getEntitiesByPrefix}
	 */
	public static List<life.genny.qwandaq.serialization.baseentity.BaseEntity> getBaseEntitiesByPrefixUsingIckle(String productCode, String prefix) {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCache(GennyConstants.CACHE_NAME_BASEENTITY));
		Query<life.genny.qwandaq.serialization.baseentity.BaseEntity> query = queryFactory
				.create("from life.genny.qwandaq.persistence.baseentity.BaseEntity where realm : '" + productCode
						+ "' and code like '" + prefix + "%'");
		QueryResult<life.genny.qwandaq.serialization.baseentity.BaseEntity> queryResult = query.execute();
		return queryResult.list();
	}

	public static List<life.genny.qwandaq.serialization.baseentity.BaseEntity> getBaseEntitiesUsingIckle(String ickleQuery) {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCache(GennyConstants.CACHE_NAME_BASEENTITY));
		log.info("##################### got here......");
		Query<life.genny.qwandaq.serialization.baseentity.BaseEntity> query = queryFactory.create(ickleQuery);
		QueryResult<life.genny.qwandaq.serialization.baseentity.BaseEntity> queryResult = query.execute();
		return queryResult.list();
	}

	/**
	 * Get a list of {@link BaseEntityAttribute}s to from cache for a BaseEntity.
	 * @param productCode - Product Code / Cache to retrieve from
	 * @param baseEntityCode - Prefix of the Core Entity code to use
	 * @return a list of base entities with matching prefixes
	 *
	 * See Also: {@link BaseEntityKey}, {@link CoreEntityKey#fromKey}, {@link CacheUtils#getEntitiesByPrefix}
	 */
	public static List<BaseEntityAttribute> getBaseEntityAttributesForBaseEntityUsingIckle(String productCode, String baseEntityCode) {
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_BASEENTITY_ATTRIBUTE);
		QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
		Query<BaseEntityAttribute> query = queryFactory
				.create("from life.genny.qwandaq.persistence.baseentityattribute.BaseEntityAttribute where realm : '" + productCode
						+ "' and baseEntityCode : '" + baseEntityCode + "'");
		QueryResult<BaseEntityAttribute> queryResult = query.execute();
		return queryResult.list();
	}

	public void saveQuestion(Question question) {
		life.genny.qwandaq.serialization.baseentity.BaseEntity baseEntity = questionUtils.getBaseEntityFromQuestion(question);
		BaseEntityKey bek = new BaseEntityKey(baseEntity.getRealm(), baseEntity.getCode());
		cache.putEntityIntoCache(GennyConstants.CACHE_NAME_BASEENTITY, bek, baseEntity);
		questionUtils.getBaseEntityAttributesFromQuestion(question).parallelStream().forEach(baseEntityAttribute -> {
			BaseEntityAttributeKey beak = new BaseEntityAttributeKey(baseEntityAttribute.getRealm(), baseEntityAttribute.getBaseEntityCode(), baseEntityAttribute.getAttributeCode());
			cache.putEntityIntoCache(GennyConstants.CACHE_NAME_BASEENTITY_ATTRIBUTE, beak, baseEntityAttribute);
		});
		question.getChildQuestions().parallelStream().forEach(questionQuestion -> saveQuestionQuestion(questionQuestion));
	}

	public void saveQuestionQuestion(QuestionQuestion questionQuestion) {
		life.genny.qwandaq.serialization.baseentity.BaseEntity baseEntity = questionUtils.getBaseEntityFromQuestionQuestion(questionQuestion);
		BaseEntityKey bek = new BaseEntityKey(baseEntity.getRealm(), baseEntity.getCode());
		cache.putEntityIntoCache(GennyConstants.CACHE_NAME_BASEENTITY, bek, baseEntity);
		questionUtils.getBaseEntityAttributesFromQuestionQuestion(questionQuestion).parallelStream().forEach(baseEntityAttribute -> {
			BaseEntityAttributeKey beak = new BaseEntityAttributeKey(baseEntityAttribute.getRealm(), baseEntityAttribute.getBaseEntityCode(), baseEntityAttribute.getAttributeCode());
			cache.putEntityIntoCache(GennyConstants.CACHE_NAME_BASEENTITY_ATTRIBUTE, beak, baseEntityAttribute);
		});
	}

	public Question getQuestion(String productCode, String questionCode) {
		return getQuestion(productCode, questionCode, false);
	}

	public Question getQuestion(String productCode, String questionCode, boolean fetchChildQuestions) {
		life.genny.qwandaq.serialization.baseentity.BaseEntity baseEntity = baseEntityUtils.getSerializableBaseEntity(productCode, questionCode);
		Set<BaseEntityAttribute> attributes = new HashSet<>();
		attributes.addAll(baseEntityAttributeUtils.getAllBaseEntityAttributesForBaseEntity(productCode, questionCode));
		Question question = questionUtils.getQuestionFromSerializableBaseEntity(baseEntity, attributes);
		if(fetchChildQuestions) {
			question.getChildQuestionCodesAsStrings().parallelStream().forEach(code -> {
				question.getChildQuestions().add(getQuestionQuestionRecursively(productCode, code, true));
			});
		}
		return question;
	}

	public List<QuestionQuestion> getQuestionQuestionByQuestionCode(String productCode, String questionCode) {
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(GennyConstants.CACHE_NAME_BASEENTITY_ATTRIBUTE);
		QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
		Query<BaseEntityAttribute> query = queryFactory
				.create("from life.genny.qwandaq.persistence.baseentityattribute.BaseEntityAttribute where realm : '" + productCode
						+ "' and valueString : '" + questionCode + "'  and attributeCode : 'parentCode' order by baseEntityCode");
		List<QuestionQuestion> questionQuestions = new LinkedList<>();
		QueryResult<BaseEntityAttribute> queryResult = query.execute();
		String prevBaseEntityCode = null;
		List<BaseEntityAttribute> allAttributes = queryResult.list();
		Set<BaseEntityAttribute> attributesForBaseEntity = new HashSet<>();
		for (BaseEntityAttribute baseEntityAttribute : allAttributes) {
			String curBaseEntityCode = baseEntityAttribute.getBaseEntityCode();
			if (prevBaseEntityCode != null && !prevBaseEntityCode.equals(curBaseEntityCode)) {
				life.genny.qwandaq.serialization.baseentity.BaseEntity baseEntity = baseEntityUtils.getSerializableBaseEntity(productCode, curBaseEntityCode);
				questionQuestions.add(questionUtils.getQuestionQuestionFromBaseEntityBaseEntityAttributes(baseEntity, attributesForBaseEntity));
				attributesForBaseEntity.clear();
			} else {
				attributesForBaseEntity.add(baseEntityAttribute);
			}
			prevBaseEntityCode = curBaseEntityCode;
		}
		return questionQuestions;
	}

	public QuestionQuestion getQuestionQuestionRecursively(String productCode, String baseEntityCode, boolean fetchChildQuestions) {
		life.genny.qwandaq.serialization.baseentity.BaseEntity baseEntity = baseEntityUtils.getSerializableBaseEntity(productCode, baseEntityCode);
		Set<BaseEntityAttribute> attributes = new HashSet<>();
		attributes.addAll(baseEntityAttributeUtils.getAllBaseEntityAttributesForBaseEntity(productCode, baseEntityCode));
		QuestionQuestion questionQuestion = questionUtils.getQuestionQuestionFromBaseEntityBaseEntityAttributes(baseEntity, attributes);
		if (fetchChildQuestions) {
			questionQuestion.getChildQuestionCodes().parallelStream().forEach(code -> {
				questionQuestion.getChildQuestionQuestions().add(getQuestionQuestionRecursively(productCode, code, true));
			});
		}
		return questionQuestion;
	}
}

