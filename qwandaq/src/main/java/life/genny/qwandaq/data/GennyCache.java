package life.genny.qwandaq.data;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import life.genny.qwandaq.serialization.attribute.AttributeInitializerImpl;
import life.genny.qwandaq.serialization.attribute.AttributeKeyInitializerImpl;
import life.genny.qwandaq.serialization.baseentityattribute.BaseEntityAttributeInitializerImpl;
import life.genny.qwandaq.serialization.baseentityattribute.BaseEntityAttributeKeyInitializerImpl;
import life.genny.qwandaq.serialization.entityentity.EntityEntityInitializerImpl;
import life.genny.qwandaq.serialization.entityentity.EntityEntityKeyInitializerImpl;
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

import life.genny.qwandaq.CoreEntity;
import life.genny.qwandaq.serialization.baseentity.BaseEntityInitializerImpl;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKeyInitializerImpl;
import life.genny.qwandaq.serialization.common.CoreEntityKey;

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
			remoteCacheManager.getConfiguration().marshallerClass();
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
		SerializationContextInitializer baseEntityAttributeSCI = new BaseEntityAttributeInitializerImpl();
		serCtxInitList.add(baseEntityAttributeSCI);
		SerializationContextInitializer baseEntityAttributeKeySCI = new BaseEntityAttributeKeyInitializerImpl();
		serCtxInitList.add(baseEntityAttributeKeySCI);
		SerializationContextInitializer attributeSCI = new AttributeInitializerImpl();
		serCtxInitList.add(attributeSCI);
		SerializationContextInitializer attributeKeySCI = new AttributeKeyInitializerImpl();
		serCtxInitList.add(attributeKeySCI);
		SerializationContextInitializer entityEntitySCI = new EntityEntityInitializerImpl();
		serCtxInitList.add(entityEntitySCI);
		SerializationContextInitializer entityEntityKeySCI = new EntityEntityKeyInitializerImpl();
		serCtxInitList.add(entityEntityKeySCI);
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
	 * @param entityName
	 * 		the name of the associated entity the desired cache
	 * @return RemoteCache&lt;String, String&gt;
	 * 		the remote cache associated with the entity
	 */
	public RemoteCache<CoreEntityKey, CoreEntityPersistable> getRemoteCacheForEntity(final String entityName) {
		if (remoteCacheManager == null) {
			initRemoteCacheManager();
		}
		return remoteCacheManager.getCache(entityName);
	}

	/**
	 * Get a CoreEntity from the cache.
	 *
	 * @param cacheName The cache to get from
	 * @param key       The key to the entity to fetch
	 * @return The entity
	 */
	public CoreEntitySerializable getEntityFromCache(String cacheName, CoreEntityKey key) {
		CoreEntityPersistable persistableCoreEntity = getPersistableEntityFromCache(cacheName, key);
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
	public CoreEntityPersistable getPersistableEntityFromCache(String cacheName, CoreEntityKey key) {
		if (remoteCacheManager == null) {
			initRemoteCacheManager();
		}

		RemoteCache<CoreEntityKey, CoreEntityPersistable> cache = getRemoteCacheForEntity(cacheName);
		if (cache == null) {
			throw new NullPointerException("Could not find a cache called " + cacheName);
		}

		CoreEntityPersistable coreEntityPersistable = cache.get(key);
		if (coreEntityPersistable == null) {
			return null;
		}
		return coreEntityPersistable;
	}

	/**
	 * Put a CoreEntity into the cache.
	 * 
	 * @param cacheName The cache to get from
	 * @param key       The key to put the entity under
	 * @param value     The entity
	 * @return The Entity
	 */
	public boolean putEntityIntoCache(String cacheName, CoreEntityKey key, CoreEntityPersistable value) {
		return putEntityIntoCache(cacheName, key, value.toSerializableCoreEntity());
	}

	/**
	 * Put a CoreEntity into the cache.
	 *
	 * @param cacheName The cache to get from
	 * @param key The key to put the entity under
	 * @param value The serializable entity
	 * @return True if the entity is successfully inserted into cache, False otherwise
	 */
	public boolean putEntityIntoCache(String cacheName, CoreEntityKey key, CoreEntitySerializable value) {
		if (remoteCacheManager == null) {
			initRemoteCacheManager();
		}
		RemoteCache<CoreEntityKey, CoreEntityPersistable> cache = remoteCacheManager.getCache(cacheName);
		if (cache == null) {
			throw new NullPointerException("Cache not found: " + cacheName);
		}
		try {
			if(value != null) {
				cache.put(key, value.toPersistableCoreEntity());
			} else {
				log.warn("[" + cacheName + "]: Value for " + key.getKeyString() + " is null, nothing to be added.");
			}
		} catch (Exception e) {
			log.error("Exception when inserting entity into cache: " + e.getMessage());
			log.error(e.getStackTrace());
			e.printStackTrace();
			return false;
		}
		return true;
	}
}

