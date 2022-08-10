package life.genny.qwandaq.data;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

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
import life.genny.qwandaq.constants.CacheName;
import life.genny.qwandaq.serialization.common.CacheKeyIntf;
import life.genny.qwandaq.serialization.key.baseentity.BaseEntityInitializerImpl;
import life.genny.qwandaq.serialization.key.baseentity.BaseEntityKeyInitializerImpl;

/**
 * A remote cache management class for accessing realm caches.
 * 
 * @author Jasper Robison
 * @author Varun Shastry
 */
@ApplicationScoped
public class GennyCache {

	static final Logger log = Logger.getLogger(GennyCache.class);

	private Map<CacheName, RemoteCache<CacheKeyIntf, String>> caches = new HashMap<>();

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
		getAllSerializationContextInitializers().stream().forEach(builder::addContextInitializer);
		Configuration config = builder.build();
		remoteCacheManager = new RemoteCacheManager(config);
		remoteCacheManager.getConfiguration().marshallerClass();
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
	 * @param cacheName the name of the desired cache
	 * @return RemoteCache&lt;String, String&gt; 
	 * 		the remote cache associatd with the realm
	 */
	public RemoteCache<CacheKeyIntf, String> getRemoteCache(final CacheName cacheName) {

		if (caches.keySet().contains(cacheName)) {
			return caches.get(cacheName); 
		}

		RemoteCache<CacheKeyIntf, String> cache = remoteCacheManager.administration().withFlags(CacheContainerAdmin.AdminFlag.VOLATILE).getOrCreateCache(cacheName.cacheName, DefaultTemplate.DIST_SYNC);
		
		// Ensure last value of put is always null, otherwise this means we have overwritten a cache
		if(caches.put(cacheName, cache) != null) {
			log.error("Overwitten cache: " + cacheName.cacheName + "! This should never happen!!!");
		} 

		return caches.get(cacheName);
	}

	/**
	* Get a CoreEntity from the cache.
	*
	* @param cacheName The cache to get from
	* @param key The key to the entity to fetch
	* @return The entity
	 */
	public CoreEntity getEntityFromCache(CacheName cacheName, CacheKeyIntf key) {

		if (remoteCacheManager == null) {
			initRemoteCacheManager();
		}

		RemoteCache<CacheKeyIntf, CoreEntity> cache = remoteCacheManager.getCache(cacheName.cacheName);
		if (cache == null) {
			log.error("Could not find a cache called " + cacheName.cacheName);
		}

		return cache.get(key);
	}

	/**
	* Put a CoreEntity into the cache.
	* 
	* @param cacheName The cache to get from
	* @param key The key to put the entity under
	* @param value The entity
	* @return The Entity
	 */
	public CoreEntity putEntityIntoCache(CacheName cacheName, CacheKeyIntf key, CoreEntity value) {
		if(value == null) {
			log.warn("[" + cacheName.cacheName + "]: Value for " + key.getFullKeyString() + " is null");
		}

		if (remoteCacheManager == null) {
			initRemoteCacheManager();
		}

		RemoteCache<CacheKeyIntf, CoreEntity> cache = remoteCacheManager.getCache(cacheName.cacheName);
		if (cache == null) {
			log.error("Could not find a cache called " + cacheName);
		}

		// TODO: Remove this try catch very soon
		try {
			cache.put(key, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cache.get(key);
	}
}
