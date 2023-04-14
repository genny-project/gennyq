package life.genny.qwandaq.data;

import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.constants.ECacheRef;
import life.genny.qwandaq.exception.GennyRuntimeException;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import life.genny.qwandaq.serialization.attribute.AttributeInitializerImpl;
import life.genny.qwandaq.serialization.attribute.AttributeKeyInitializerImpl;
import life.genny.qwandaq.serialization.baseentity.BaseEntityInitializerImpl;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKeyInitializerImpl;
import life.genny.qwandaq.serialization.common.CoreEntityKey;
import life.genny.qwandaq.serialization.datatype.DataTypeInitializerImpl;
import life.genny.qwandaq.serialization.datatype.DataTypeKeyInitializerImpl;
import life.genny.qwandaq.serialization.entityattribute.EntityAttributeInitializerImpl;
import life.genny.qwandaq.serialization.entityattribute.EntityAttributeKeyInitializerImpl;
import life.genny.qwandaq.serialization.question.QuestionInitializerImpl;
import life.genny.qwandaq.serialization.question.QuestionKeyInitializerImpl;
import life.genny.qwandaq.serialization.questionquestion.QuestionQuestionInitializerImpl;
import life.genny.qwandaq.serialization.questionquestion.QuestionQuestionKeyInitializerImpl;
import life.genny.qwandaq.serialization.userstore.UserStoreInitializerImpl;
import life.genny.qwandaq.serialization.userstore.UserStoreKeyInitializerImpl;
import life.genny.qwandaq.serialization.validation.ValidationInitializerImpl;
import life.genny.qwandaq.serialization.validation.ValidationKeyInitializerImpl;

import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.commons.util.Util;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * A remote cache management class for accessing realm caches.
 * 
 * @author Jasper Robison
 * @author Varun Shastry
 */
@ApplicationScoped
public class GennyCache {

	static final Logger log = Logger.getLogger(GennyCache.class);

    private final Set<String> realms = new HashSet<>();

    private final Map<String, RemoteCache> caches = new HashMap<>();

	private RemoteCacheManager remoteCacheManager;

	public static final String HOTROD_CLIENT_PROPERTIES = "hotrod-client.properties";

	@PostConstruct
	public void init() {
		log.info("Initializing RemoteCacheManager");
		initRemoteCacheManager();
	}

	/**
	 * Initialize the remote cache manager using the
	 * hotrod clcient properties file.
	 **/
	private void initRemoteCacheManager() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		builder.classLoader(cl);

		// load infinispan properties
		InputStream stream = FileLookupFactory.newInstance().lookupFile(HOTROD_CLIENT_PROPERTIES, cl);

		if (stream == null) {
			log.error("Could not find infinispan hotrod client properties file: " + HOTROD_CLIENT_PROPERTIES);
			return;
		}

		try {
			builder.withProperties(loadFromStream(stream));
		} finally {
			Util.close(stream);
		}

		// create cache manager
        getAllSerializationContextInitializers().forEach(builder::addContextInitializer);
		Configuration config = builder.build();
		if (remoteCacheManager == null) {
			remoteCacheManager = new RemoteCacheManager(config);
		}
	}

	/**
	 * Get a list of {@link SerializationContextInitializer} objects
	 * used in configureing the cache.
	 *
	 * @return The list of SerializationContextInitializer objects
	 */
	private List<SerializationContextInitializer> getAllSerializationContextInitializers() {

		List<SerializationContextInitializer> serCtxInitList = new LinkedList<>();
		SerializationContextInitializer baseEntitySCI = new BaseEntityInitializerImpl();
		serCtxInitList.add(baseEntitySCI);
		SerializationContextInitializer baseEntityKeySCI = new BaseEntityKeyInitializerImpl();
		serCtxInitList.add(baseEntityKeySCI);
		SerializationContextInitializer baseEntityAttributeSCI = new EntityAttributeInitializerImpl();
		serCtxInitList.add(baseEntityAttributeSCI);
		SerializationContextInitializer baseEntityAttributeKeySCI = new EntityAttributeKeyInitializerImpl();
		serCtxInitList.add(baseEntityAttributeKeySCI);
		SerializationContextInitializer attributeSCI = new AttributeInitializerImpl();
		serCtxInitList.add(attributeSCI);
		SerializationContextInitializer attributeKeySCI = new AttributeKeyInitializerImpl();
		serCtxInitList.add(attributeKeySCI);
		SerializationContextInitializer questionSCI = new QuestionInitializerImpl();
		serCtxInitList.add(questionSCI);
		SerializationContextInitializer questionKeySCI = new QuestionKeyInitializerImpl();
		serCtxInitList.add(questionKeySCI);
		SerializationContextInitializer questionQuestionSCI = new QuestionQuestionInitializerImpl();
		serCtxInitList.add(questionQuestionSCI);
		SerializationContextInitializer questionQuestionKeySCI = new QuestionQuestionKeyInitializerImpl();
		serCtxInitList.add(questionQuestionKeySCI);
		SerializationContextInitializer userStoreSCI = new UserStoreInitializerImpl();
		serCtxInitList.add(userStoreSCI);
		SerializationContextInitializer userStoreKeySCI = new UserStoreKeyInitializerImpl();
		serCtxInitList.add(userStoreKeySCI);
		SerializationContextInitializer validationSCI = new ValidationInitializerImpl();
		serCtxInitList.add(validationSCI);
		SerializationContextInitializer validationKeySCI = new ValidationKeyInitializerImpl();
		serCtxInitList.add(validationKeySCI);
		SerializationContextInitializer dataTypeSCI = new DataTypeInitializerImpl();
		serCtxInitList.add(dataTypeSCI);
		SerializationContextInitializer dataTypeKeySCI = new DataTypeKeyInitializerImpl();
		serCtxInitList.add(dataTypeKeySCI);

		return serCtxInitList;
	}

	/**
	 * Load properties from an input stream.
	 *
	 * @param stream The stream to load from
	 * @return The Properties object
	 */
	private Properties loadFromStream(InputStream stream) {
		Properties properties = new Properties();
		try {
			properties.load(stream);
		} catch (Exception e) {
			throw new HotRodClientException("Issues configuring from client hotrod-client.properties", e);
		}
		return properties;
	}

	/**
	 * Return a remote cache for the given realm.
	 *
	 * @param realm
	 *              the associated realm of the desired cache
	 * @return RemoteCache&lt;String, String&gt;
	 *         the remote cache associatd with the realm
	 */
	public RemoteCache<String, String> getRemoteCache(final String realm) {

		if (realms.contains(realm)) {
			return caches.get(realm);
		}

		remoteCacheManager.administration().withFlags(CacheContainerAdmin.AdminFlag.VOLATILE).getOrCreateCache(realm,
				DefaultTemplate.DIST_SYNC);
		realms.add(realm);
		caches.put(realm, remoteCacheManager.getCache(realm));

		return caches.get(realm);
	}

	/**
	 * Return a remote cache for the given entity.
	 *
	 * @param cacheRef - the relevant {@link ECacheRef} to the cache
	 * 		the name of the associated entity the desired cache
	 * @return RemoteCache&lt;String, String&gt;
	 * 		the remote cache associated with the entity
	 */
	public RemoteCache<CoreEntityKey, CoreEntityPersistable> getRemoteCacheForEntity(ECacheRef cacheRef) {
		if (remoteCacheManager == null) {
			initRemoteCacheManager();
		}
		return remoteCacheManager.getCache(cacheRef.cacheName);
	}

	/**
	 * Get a CoreEntity from the cache.
	 *
	 * @param cacheName The cache to get from
	 * @param key       The key to the entity to fetch
	 * @return The entity
	 */
	public CoreEntitySerializable getEntityFromCache(ECacheRef cacheRef, CoreEntityKey key) {
		CoreEntityPersistable persistableCoreEntity = getPersistableEntityFromCache(cacheRef, key);
		if (persistableCoreEntity == null) {
			return null;
		}
		return persistableCoreEntity.toSerializableCoreEntity();
	}

	/**
	 * Get a CoreEntity from the cache.
	 *
	 * @param cacheName The cache to get from
	 * @param key The key to the entity to fetch
	 * @return The persistable core entity
	 */
	public CoreEntityPersistable getPersistableEntityFromCache(ECacheRef cacheRef, CoreEntityKey key) {
		RemoteCache<CoreEntityKey, CoreEntityPersistable> cache = getRemoteCacheForEntity(cacheRef);
		if (cache == null) {
			throw new NullPointerException("Could not find a cache called " + cacheRef.cacheName);
		}
		return cache.get(key);
	}

	/**
	 * Put a CoreEntity into the cache.
	 * 
	 * @param cacheName The cache to get from
	 * @param key       The key to put the entity under
	 * @param value     The entity
	 * @return <b>true</b> if value was persisted successfully or value passed was null, <b>false</b>
	 */
	public boolean putEntityIntoCache(ECacheRef cacheRef, CoreEntityKey key, CoreEntityPersistable value) {
		if (remoteCacheManager == null) {
			initRemoteCacheManager();
		}
		RemoteCache<CoreEntityKey, CoreEntityPersistable> cache = remoteCacheManager.getCache(cacheRef.cacheName);
		if (cache == null) {
			throw new NullPointerException("Cache not found: " + cacheRef.cacheName);
		}
		if(value == null) {
			log.warn("[" + cacheRef.cacheName + "]: Value for " + key.getKeyString() + " is null, nothing to be added.");
			return true;
		}

		try {
			if(value.getCreated() == null) {
				value.setCreated(LocalDateTime.now());
			}
			value.setUpdated(LocalDateTime.now());
			cache.put(key, value);
		} catch (Exception e) {
			log.error(ANSIColour.doColour("Exception when inserting entity (key=" + key.getKeyString() + ") into cache: " + cacheRef.cacheName, ANSIColour.RED));
			log.error("Key: " + key.getKeyString());
			log.error("Value: " + value.toString());
			log.error(e.getMessage());
			StringBuilder sb = new StringBuilder();
			for(StackTraceElement stack : e.getStackTrace()) {
				sb.append(ANSIColour.doColour(stack.toString(), ANSIColour.RED))
					.append('\n');
			}
			log.trace(sb.toString());
			throw e;
		}
		
		return true;
	}

	/**
	 * Put a CoreEntity into the cache.
	 *
	 * @param cacheName The cache to get from
	 * @param key The key to put the entity under
	 * @param value The serializable entity
	 * @return True if the entity is successfully inserted into cache, False otherwise
	 */
	public boolean putEntityIntoCache(ECacheRef cacheRef, CoreEntityKey key, CoreEntitySerializable value) {
		return putEntityIntoCache(cacheRef, key, value.toPersistableCoreEntity());
	}

	/**
	 * Remove CoreEntity from the cache.
	 *
	 * @param cacheName The cache to get from
	 * @param key The key to the entity to remove
	 * @return The removed persistable core entity
	 */
	public CoreEntityPersistable removeEntityFromCache(ECacheRef cacheRef, CoreEntityKey key) {
		RemoteCache<CoreEntityKey, CoreEntityPersistable> cache = getRemoteCacheForEntity(cacheRef);
		if (cache == null) {
			throw new NullPointerException("Could not find a cache called " + cacheRef.cacheName);
		}
		return cache.remove(key);
	}

	public Long getEntityLastUpdatedAt(String entityName, String productCode) {
		RemoteCache<String, Long> entityLastUpdatedAtCache = remoteCacheManager.getCache(ECacheRef.ENTITY_LAST_UPDATED_AT.cacheName);
		if (entityLastUpdatedAtCache == null) {
			log.debugf("Cache doesn't exist.. Creating...");
			entityLastUpdatedAtCache = createEntityLastUpdatedAtCache();
			if (entityLastUpdatedAtCache == null) {

				log.debugf("Cache creation failed for some reason!!");
				throw new GennyRuntimeException("Cache creation of " + ECacheRef.ENTITY_LAST_UPDATED_AT.cacheName + " failed") {
					
				};
			}
		}
		return entityLastUpdatedAtCache.get(entityName + ":" + productCode);
	}

	private RemoteCache<String, Long> createEntityLastUpdatedAtCache() {
		return remoteCacheManager.administration().withFlags(CacheContainerAdmin.AdminFlag.VOLATILE).createCache(ECacheRef.ENTITY_LAST_UPDATED_AT.cacheName, DefaultTemplate.DIST_SYNC);
	}

	public void updateEntityLastUpdatedAt(String entityName, String productCode, Long updatedTime) {
		RemoteCache<String, Long> entityLastUpdatedAtCache = remoteCacheManager.getCache(ECacheRef.ENTITY_LAST_UPDATED_AT.cacheName);
		if (entityLastUpdatedAtCache == null) {
			entityLastUpdatedAtCache = createEntityLastUpdatedAtCache();
		}
		entityLastUpdatedAtCache.put(entityName + ":" + productCode, updatedTime);
	}

	public void reindexCache(ECacheRef cache) {
		if(!cache.reindexable) {
			log.error("Cache: " + cache.cacheName + " is not programattically indexable! It would take a very long time to index this cache. If it is absolutely necessary please index through the ISPN management console");
			return;
		}
		log.debug("Reindexing cache: " + cache.cacheName);
		remoteCacheManager.administration().reindexCache(cache.cacheName);
	}
}
