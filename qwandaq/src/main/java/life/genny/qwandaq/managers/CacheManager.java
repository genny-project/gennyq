package life.genny.qwandaq.managers;

import life.genny.qwandaq.CoreEntity;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.ECacheRef;
import life.genny.qwandaq.data.GennyCache;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import life.genny.qwandaq.serialization.attribute.AttributeKey;
import life.genny.qwandaq.serialization.attribute.AttributeMessageMarshaller;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.serialization.baseentity.BaseEntityMessageMarshaller;
import life.genny.qwandaq.serialization.common.CoreEntityKey;
import life.genny.qwandaq.serialization.datatype.DataTypeKey;
import life.genny.qwandaq.serialization.datatype.DataTypeMessageMarshaller;
import life.genny.qwandaq.serialization.entityattribute.EntityAttributeKey;
import life.genny.qwandaq.serialization.entityattribute.EntityAttributeMessageMarshaller;
import life.genny.qwandaq.serialization.question.QuestionKey;
import life.genny.qwandaq.serialization.question.QuestionMessageMarshaller;
import life.genny.qwandaq.serialization.questionquestion.QuestionQuestionKey;
import life.genny.qwandaq.serialization.questionquestion.QuestionQuestionMessageMarshaller;
import life.genny.qwandaq.serialization.validation.ValidationMessageMarshaller;
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
		if(ECacheRef.getByCacheName(realm) != null) {
			log.warn("Using string reference to cache: " + realm + ". Are we sure this is a reference to a realm cache?");
		}
		
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
	public CoreEntitySerializable getEntity(ECacheRef cacheRef, CoreEntityKey key) {
		return cache.getEntityFromCache(cacheRef, key);
	}

	/**
	 * Get a CoreEntity object from the cache using a CoreEntityKey.
	 *
	 * @param cacheName The cache to read from
	 * @param key The key they item is saved against
	 * @return The CoreEntity returned
	 */
	public CoreEntityPersistable getPersistableEntity(ECacheRef cacheRef, CoreEntityKey key) {
		return cache.getPersistableEntityFromCache(cacheRef, key);
	}

	/**
	 * Get a CoreEntity object from the cache using a CoreEntityKey.
	 *
	 * @param cacheName The cache to get from
	 * @param key The key to the entity to remove
	 * @return The removed persistable core entity
	 */
	public CoreEntityPersistable removePersistableEntity(ECacheRef cacheRef, CoreEntityKey key) {
		return cache.removeEntityFromCache(cacheRef, key);
	}

	/**
	* Save a {@link CoreEntity} to the cache using a CoreEntityKey.
	*
	* @param cacheName The cache to save to
	* @param key The key to save against
	* @param entity The CoreEntity to save
	* @return The CoreEntity being saved
	 */
	public boolean saveEntity(ECacheRef cacheRef, CoreEntityKey key, CoreEntityPersistable entity) {
		return cache.putEntityIntoCache(cacheRef, key, entity);
	}

	/**
	 * Get a list of {@link CoreEntity}s to from cache by prefix.
	 * @param cacheName - Product Code / Cache to retrieve from
	 * @param prefix - Prefix of the Core Entity code to use
	 * @param keyStruct - {@link CoreEntityKey} for cache retrieval
	 * @return a list of core entities with matching prefixes
	 * 
	 * See Also: {@link CoreEntityKey}
	 */
	public List<CoreEntity> getEntitiesByPrefix(String cacheName, String prefix, CoreEntityKey keyStruct) {
		return cache.getRemoteCache(cacheName)
		.entrySet().stream().map((Map.Entry<String, String> entry) -> {
			String key = entry.getKey();
			CoreEntityKey currentKey = keyStruct.fromKey(key);

			return currentKey.getEntityCode().startsWith(prefix) ? jsonb.fromJson(entry.getValue(), CoreEntity.class) : null;
		})
		.filter(Objects::nonNull).collect(Collectors.toList());
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
		.stream().map(BaseEntity.class::cast).collect(Collectors.toList());
	}

	/**
	 * Get the max attribute id.
	 *
	 * @return the max id
	 */
	public Long getMaxAttributeId() {
		return getMaxId(ECacheRef.ATTRIBUTE, AttributeMessageMarshaller.TYPE_NAME);
	}

	/**
	 * Get the max base entity id.
	 *
	 * @return the max id
	 */
	public Long getMaxBaseEntityId() {
		return getMaxId(ECacheRef.BASEENTITY, BaseEntityMessageMarshaller.TYPE_NAME);
	}

	/**
	 * Get the max question id.
	 *
	 * @return the max id
	 */
	public Long getMaxQuestionId() {
		return getMaxId(ECacheRef.QUESTION, QuestionMessageMarshaller.TYPE_NAME);
	}

	/**
	 * Get the max id for a given core entity.
	 * @param cacheName The cache name for the entity
	 * @param entityName The entity name with classpath (Example: life.genny.qwandaq.persistence.baseentity.BaseEntity)
	 * @return the max id in the cache for the entity
	 */
	public Long getMaxId(ECacheRef cacheRef, String entityName) {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(cacheRef));
		Query<CoreEntity> query = queryFactory.create("from " + entityName + " order by id desc");
		QueryResult<CoreEntity> queryResult = query.maxResults(1).execute();
		List<CoreEntity> coreEntities = queryResult.list();
		if(coreEntities.isEmpty())
			return 0L;
		CoreEntity coreEntity = coreEntities.get(0);
		if(coreEntity != null && coreEntity.getId() != null)
			return coreEntity.getId();
		return 0L;
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
		return (Attribute) getPersistableEntity(ECacheRef.ATTRIBUTE, key);
    }

	/**
	 * Fetch all attributes for a product.
	 *
	 * @return Collection of all attributes in the system across all products
	 */
	public Set<Attribute> getAllAttributes() {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(ECacheRef.ATTRIBUTE));
		Query<Attribute> query = queryFactory
				.create("from " + AttributeMessageMarshaller.TYPE_NAME);
		QueryResult<Attribute> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<Attribute> attributeList = queryResult.list();
		if (attributeList.size() == 1 && attributeList.get(0) == null)
			return Collections.EMPTY_SET;
		return new LinkedHashSet<>(attributeList);
	}

	/**
	 * Fetch all attributes for a product.
	 *
	 * @param productCode
	 * @return
	 */
	public Set<Attribute> getAttributesForProduct(String productCode) {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(ECacheRef.ATTRIBUTE));
		Query<Attribute> query = queryFactory
				.create("from " + AttributeMessageMarshaller.TYPE_NAME + " where realm = '" + productCode + "'");
		QueryResult<Attribute> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<Attribute> attributeList = queryResult.list();
		if (attributeList.isEmpty() || attributeList.size() == 1 && attributeList.get(0) == null)
			return Collections.EMPTY_SET;
		return new LinkedHashSet<>(attributeList);
	}

	/**
	 * Fetch all attributes with a given prefix value in code for a product.
	 *
	 * @param prefix
	 * @return
	 */
	public Set<Attribute> getAttributesWithPrefix(String prefix) {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(ECacheRef.ATTRIBUTE));
		Query<Attribute> query = queryFactory
				.create("from " + AttributeMessageMarshaller.TYPE_NAME + " where code like '" + prefix + "%'");
		QueryResult<Attribute> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<Attribute> attributeList = queryResult.list();
		if (attributeList.size() == 1 && attributeList.get(0) == null)
			return Collections.EMPTY_SET;
		return new LinkedHashSet<>(attributeList);
	}

	/**
	 * Fetch all attributes with a given prefix value in code for a product.
	 *
	 * @param productCode
	 * @param prefix
	 * @return
	 */
	public Set<Attribute> getAttributesWithPrefixForProduct(String productCode, String prefix) {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(ECacheRef.ATTRIBUTE));
		Query<Attribute> query = queryFactory
				.create("from " + AttributeMessageMarshaller.TYPE_NAME + " where realm = '" + productCode
						+ "' and code like '" + prefix + "%'");
		QueryResult<Attribute> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<Attribute> attributeList = queryResult.list();
		if (attributeList.size() == 1 && attributeList.get(0) == null)
			return Collections.EMPTY_SET;
		return new LinkedHashSet<>(attributeList);
	}

	public DataType getDataType(String productCode, String dttCode) {
		DataTypeKey key = new DataTypeKey(productCode, dttCode);
		return (DataType) cache.getPersistableEntityFromCache(ECacheRef.DATATYPE, key);
	}

	public Set<Validation> getValidations(String productCode, String commaSeparatedValidationCodes) {
		String inClauseValue = StringUtils.replace(commaSeparatedValidationCodes, ",", "','");
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(ECacheRef.VALIDATION));
		Query<Validation> query = queryFactory
				.create("from " + ValidationMessageMarshaller.TYPE_NAME + " where realm = '" + productCode +
						"' and code in ('"+inClauseValue+"')");
		QueryResult<Validation> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<Validation> validationList = queryResult.list();
		if (validationList.size() == 1 && validationList.get(0) == null)
			return new HashSet<>(0);
		return new LinkedHashSet<>(validationList);
	}

	public Set<Validation> getValidations(String productCode, List<String> validationCodes) {
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
	public Set<BaseEntity> getBaseEntitiesByPrefixUsingIckle(String productCode, String prefix) {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(ECacheRef.BASEENTITY));
		Query<BaseEntity> query = queryFactory
				.create("from " + BaseEntityMessageMarshaller.TYPE_NAME + " where realm = '" + productCode
						+ "' and code like '" + prefix + "%'");
		QueryResult<BaseEntity> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<BaseEntity> baseEntityList = queryResult.list();
		if (baseEntityList.size() == 1 && baseEntityList.get(0) == null)
			return Collections.EMPTY_SET;
		return new LinkedHashSet<>(baseEntityList);
	}

	/**
	 * @param ickleQuery
	 * @return
	 */
	public Set<life.genny.qwandaq.serialization.baseentity.BaseEntity> getBaseEntitiesUsingIckle(String ickleQuery) {
		QueryFactory queryFactory = Search.getQueryFactory(cache.getRemoteCacheForEntity(ECacheRef.BASEENTITY));
		Query<life.genny.qwandaq.serialization.baseentity.BaseEntity> query = queryFactory.create(ickleQuery);
		QueryResult<life.genny.qwandaq.serialization.baseentity.BaseEntity> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		return new LinkedHashSet<>(queryResult.list());
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
	public Set<EntityAttribute> getAllBaseEntityAttributesForBaseEntity(String productCode, String baseEntityCode) {
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(ECacheRef.BASEENTITY_ATTRIBUTE);
		QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
		Query<EntityAttribute> query = queryFactory
				.create("from " + EntityAttributeMessageMarshaller.TYPE_NAME + " where realm = '" + productCode
						+ "' and baseEntityCode = '" + baseEntityCode + "'");
		QueryResult<EntityAttribute> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<EntityAttribute> entityAttributeList = queryResult.list();
		if (entityAttributeList.size() == 1 && entityAttributeList.get(0) == null)
			return Collections.EMPTY_SET;
		return new LinkedHashSet<>(entityAttributeList);
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
	public Set<EntityAttribute> getBaseEntityAttributesForBaseEntityWithAttributeCodePrefix(String productCode, String baseEntityCode, String attributeCodePrefix) {
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(ECacheRef.BASEENTITY_ATTRIBUTE);
		QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
		Query<EntityAttribute> query = queryFactory
				.create("from " + EntityAttributeMessageMarshaller.TYPE_NAME + " where realm = '" + productCode
						+ "' and baseEntityCode = '" + baseEntityCode + "' and attributeCode like '" + attributeCodePrefix + "%'");
		QueryResult<EntityAttribute> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<EntityAttribute> attributeList = queryResult.list();
		if (attributeList.size() == 1 && attributeList.get(0) == null)
			return Collections.EMPTY_SET;
		return new LinkedHashSet<>(attributeList);
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
	 * <pre>String delQuery = constructDeleteQuery(EntityAttributeMessageMarshaller.TYPE_NAME, "SOME_PRODUCT", "'baseEntityCode' = 'DEF_TEST'" and "attributeCode like 'ATT_%'");
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
	private int removePersistableEntities(ECacheRef cacheRef, String deleteQueryStr) {
		if(!deleteQueryStr.startsWith("delete"))
			throw new IllegalStateException("Not a delete query: " + deleteQueryStr);
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(cacheRef);
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
		String persistenceObject = ValidationMessageMarshaller.TYPE_NAME;
		String conditional = "code = '" + code + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(ECacheRef.VALIDATION, deleteQuery);
	}

	/**
	 * Remove a datatype from cache/persistence
	 * @param productCode - product to remove from
	 * @param code - code of datatype to remove
	 * @return number of entities affected by deletion
	 */
	public int removeDataType(String productCode, String code) {
		String persistenceObject = DataTypeMessageMarshaller.TYPE_NAME;
		String conditional = "dttcode = '" + code + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(ECacheRef.DATATYPE, deleteQuery);
	}

	/**
	 * Remove an Attribute from cache/persistence
	 * @param productCode - product to remove from
	 * @param code - code of attribute to remove
	 * @return number of entities affected by deletion
	 */
	public int removeAttribute(String productCode, String code) {
		String persistenceObject = AttributeMessageMarshaller.TYPE_NAME;
		String conditional = "code = '" + code + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(ECacheRef.ATTRIBUTE, deleteQuery);
	}

	/**
	 * Remove a question from cache/persistence
	 * @param productCode - product to remove from
	 * @param code - code of question to remove
	 * @return number of entities affected by deletion
	 */
	public int removeQuestion(String productCode, String code) {
		String persistenceObject = QuestionMessageMarshaller.TYPE_NAME;
		String conditional = "code = '" + code + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(ECacheRef.QUESTION, deleteQuery);
	}

	/**
	 * Remove all entity attributes from a baseentity from cache/persistence
	 * @param productCode - product to remove from
	 * @param baseEntityCode - baseEntityCode of baseentity to remove all entity attributes for
	 * @return number of entities affected by deletion
	 */
	public int removeAllEntityAttributesOfBaseEntity(String productCode, String baseEntityCode) {
		String persistenceObject = EntityAttributeMessageMarshaller.TYPE_NAME;
		String conditional = "baseEntityCode = '" + baseEntityCode + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(ECacheRef.BASEENTITY_ATTRIBUTE, deleteQuery);
	}

	/**
	 * Remove a single EntityAttribute from cache/persistence
	 * @param productCode - product to remove from
	 * @param baseEntityCode - base entity code of the entity attribute to remove
	 * @param attributeCode - attributeCode of the entity attribute to remove
	 * @return number of entities affected by deletion
	 */
	public int removeEntityAttribute(String productCode, String baseEntityCode, String attributeCode) {
		String persistenceObject = EntityAttributeMessageMarshaller.TYPE_NAME;
		String conditional = "baseEntityCode = '" + baseEntityCode + "' and attributeCode = '" + attributeCode + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(ECacheRef.BASEENTITY_ATTRIBUTE, deleteQuery);
	}

	/**
	 * Remove a QuestionQuestion from cache/persistence
	 * @param productCode - product to remove from
	 * @param sourceCode - sourceCode of QuestionQuestion to remove
	 * @param targetCode - targetCode of QuestionQuestion to remove
	 * @return number of entities affected by deletion
	 */
	public int removeQuestionQuestion(String productCode, String sourceCode, String targetCode) {
		String persistenceObject = QuestionQuestionMessageMarshaller.TYPE_NAME;
		String conditional = "sourceCode = '" + sourceCode + "' and targetCode = '" + targetCode + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(ECacheRef.QUESTIONQUESTION, deleteQuery);
	}

	/**
	 * Remove Base Entity with a given code from cache/persistence
	 * @param productCode - product to remove from
	 * @param code - code of the base entity to remove
	 * @return number of entities affected by deletion
	 */
	public int removeBaseEntity(String productCode, String code) {
		String persistenceObject = BaseEntityMessageMarshaller.TYPE_NAME;
		String conditional = "code = '" + code + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(ECacheRef.BASEENTITY, deleteQuery);
	}

	/**
	 * Remove all QuestionQuestions with the same sourceCode (in a group) from cache/persistence
	 * @param productCode - product to remove from
	 * @param sourceCode - sourceCode of QuestionQuestion to remove
	 * @return number of entities affected by deletion
	 */public int removeAllQuestionQuestionsInGroup(String productCode, String sourceCode) {
		String persistenceObject = QuestionQuestionMessageMarshaller.TYPE_NAME;
		String conditional = "sourceCode = '" + sourceCode + "'";
		String deleteQuery = constructDeleteQuery(persistenceObject, productCode, conditional);
		return removePersistableEntities(ECacheRef.QUESTIONQUESTION, deleteQuery);
	}

	/**
	 * @param questionQuestion
	 * @return The question object to be saved
	 */
	public QuestionQuestion saveQuestionQuestion(QuestionQuestion questionQuestion) {
		QuestionQuestionKey questionKey = new QuestionQuestionKey(questionQuestion.getRealm(), questionQuestion.getParentCode(), questionQuestion.getChildCode());
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(ECacheRef.QUESTIONQUESTION);
		return (QuestionQuestion) remoteCache.put(questionKey, questionQuestion);
	}

	/**
	 * @param question
	 * @return The question object to be saved
	 */
	public Question saveQuestion(Question question) {
		QuestionKey questionKey = new QuestionKey(question.getRealm(), question.getCode());
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(ECacheRef.QUESTION);
		return (Question) remoteCache.put(questionKey, question);
	}

	/**
	 * @param productCode
	 * @param questionCode
	 * @return The question object corresponding to the passed productCode:questionCode
	 */
	public Question getQuestion(String productCode, String questionCode) {
		QuestionKey questionKey = new QuestionKey(productCode, questionCode);
		return (Question) getPersistableEntity(ECacheRef.QUESTION, questionKey);
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

	public Set<QuestionQuestion> getQuestionQuestionsForParentQuestion(Question parent) {
		String productCode = parent.getRealm();
		String parentQuestionCode = parent.getCode();
		return getQuestionQuestionsForParentQuestion(productCode, parentQuestionCode);
	}

	/**
	 * @param productCode The realm for which child questions need to be fetched
	 * @param parentQuestionCode The parent question code for which child questions need to be fetched
	 * @return
	 */
	public Set<QuestionQuestion> getQuestionQuestionsForParentQuestion(String productCode, String parentQuestionCode) {
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(ECacheRef.QUESTIONQUESTION);
		QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
		Query<QuestionQuestion> query = queryFactory
				.create("from " + QuestionQuestionMessageMarshaller.TYPE_NAME + " where sourceCode = '" + parentQuestionCode
						+ "' and realm = '" + productCode + "' order by weight");
		// execute query
		QueryResult<QuestionQuestion> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<QuestionQuestion> questionQuestionList = queryResult.list();
		if (questionQuestionList.size() == 1 && questionQuestionList.get(0) == null)
			return Collections.EMPTY_SET;
		return new LinkedHashSet<>(questionQuestionList);
	}

	/**
	 * @param parent The question for which child questions need to be fetched
	 * @return
	 */
	public Set<QuestionQuestion> getQuestionQuestionsForParentQuestionFromBECache(Question parent) {
		// get bea remote cache for querying
		String productCode = parent.getRealm();
		String parentCode = parent.getCode();
		RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache = cache.getRemoteCacheForEntity(ECacheRef.BASEENTITY_ATTRIBUTE);
		QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
		log.debug("QuestionQuestion -> productCode = " + productCode + ", questionCode = " + parentCode);
		// init query
		Query<EntityAttribute> query = queryFactory
				.create("from " + EntityAttributeMessageMarshaller.TYPE_NAME + " where baseEntityCode like '"+parentCode+"|%'"
					 + " and realm = '"+productCode+"'");
		// execute query
		QueryResult<EntityAttribute> queryResult = query.maxResults(Integer.MAX_VALUE).execute();
		List<EntityAttribute> attributeList = queryResult.list();
		if (attributeList.size() == 1 && attributeList.get(0) == null)
			return Collections.EMPTY_SET;
		// begin building QQ objects
		return new LinkedHashSet<>(questionUtils.createQuestionQuestionsForParentQuestion(parent, attributeList));
	}

	/**
	 * @param question
	 */
	public void saveQuestionAsBE(Question question) {
		life.genny.qwandaq.serialization.baseentity.BaseEntity baseEntity = questionUtils.getSerializableBaseEntityFromQuestion(question);
		BaseEntityKey bek = new BaseEntityKey(baseEntity.getRealm(), baseEntity.getCode());
		cache.putEntityIntoCache(ECacheRef.BASEENTITY, bek, baseEntity);
		questionUtils.getSerializableBaseEntityAttributesFromQuestion(question).parallelStream().forEach(baseEntityAttribute -> {
			EntityAttributeKey beak = new EntityAttributeKey(baseEntityAttribute.getRealm(), baseEntityAttribute.getBaseEntityCode(), baseEntityAttribute.getAttributeCode());
			cache.putEntityIntoCache(ECacheRef.BASEENTITY_ATTRIBUTE, beak, baseEntityAttribute);
		});
		question.getChildQuestions().parallelStream().forEach(questionQuestion -> saveQuestionQuestion(questionQuestion));
	}

	/**
	 * @param questionQuestion
	 */
	public void saveQuestionQuestionAsBE(QuestionQuestion questionQuestion) {
		life.genny.qwandaq.serialization.baseentity.BaseEntity baseEntity = questionUtils.getSerializableBaseEntityFromQuestionQuestion(questionQuestion);
		BaseEntityKey bek = new BaseEntityKey(baseEntity.getRealm(), baseEntity.getCode());
		cache.putEntityIntoCache(ECacheRef.BASEENTITY, bek, baseEntity);
		questionUtils.getSerializableBaseEntityAttributesFromQuestionQuestion(questionQuestion).parallelStream().forEach(baseEntityAttribute -> {
			EntityAttributeKey beak = new EntityAttributeKey(baseEntityAttribute.getRealm(), baseEntityAttribute.getBaseEntityCode(), baseEntityAttribute.getAttributeCode());
			cache.putEntityIntoCache(ECacheRef.BASEENTITY_ATTRIBUTE, beak, baseEntityAttribute);
		});
	}

	public Long getEntityLastUpdatedAt(String entityName, String productCode) {
		return cache.getEntityLastUpdatedAt(entityName, productCode);
	}

	public void updateEntityLastUpdatedAt(String entityName, String productCode, Long updatedTime) {
		cache.updateEntityLastUpdatedAt(entityName, productCode, updatedTime);
	}

	public void reindexCache(ECacheRef cacheRef) {
		cache.reindexCache(cacheRef);
	}
}

