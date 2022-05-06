package life.genny.qwandaq.data;

import java.io.IOException;
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
import javax.inject.Inject;

import life.genny.qwandaq.CoreEntity;
import life.genny.qwandaq.serialization.baseentity.BaseEntityInitializerImpl;
import life.genny.qwandaq.serialization.common.CoreEntityKey;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;

import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.commons.util.Util;
import org.infinispan.protostream.SerializationContextInitializer;

/**
 * A remote cache management class for accessing realm caches.
 * 
 * @author Jasper Robison
 */
@RegisterForReflection
@ApplicationScoped
public class GennyCache {
	
	static final Logger log = Logger.getLogger(GennyCache.class);

    Set<String> realms = new HashSet<String>();

    private Map<String, RemoteCache> caches = new HashMap<>();

    private RemoteCacheManager remoteCacheManager;

	public static final String HOTROD_CLIENT_PROPERTIES = "hotrod-client.properties";

	/*@Inject GennyCache(RemoteCacheManager remoteCacheManager) {
       this.remoteCacheManager = remoteCacheManager;
    }*/

	@PostConstruct
	public void init() {
		log.info("Initialiing RemoteCacheManager");
		initRemoteCacheManager();
		log.info("RemoteCacheManager Initialized!");
	}

	private void initRemoteCacheManager() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		builder.classLoader(cl);
		InputStream stream = FileLookupFactory.newInstance().lookupFile(HOTROD_CLIENT_PROPERTIES, cl);
		if (stream == null) {
			log.error("Could not find infinispan hotrod client properties file: " + HOTROD_CLIENT_PROPERTIES);
			return;
		} else {
			try {
				builder.withProperties(loadFromStream(stream));
			} finally {
				Util.close(stream);
			}
		}
		getAllSerializationContextInitializers().stream().forEach(builder::addContextInitializer);
		Configuration config = builder.build();
		remoteCacheManager = new RemoteCacheManager(config);
		remoteCacheManager.getConfiguration().marshallerClass();
	}
 
	private List<SerializationContextInitializer> getAllSerializationContextInitializers() {
		List<SerializationContextInitializer> serCtxInitList = new LinkedList<>();
		SerializationContextInitializer sci = new BaseEntityInitializerImpl();
		serCtxInitList.add(sci);
		return serCtxInitList;
	}

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
	* 		the associated realm of the desired cache
	* @return RemoteCache&lt;String, String&gt; 
	* 		the remote cache associatd with the realm
	 */
	public RemoteCache<String, String> getRemoteCache(final String realm) {

		if (realms.contains(realm)) {
			return caches.get(realm); 
		} else {
			remoteCacheManager.administration().withFlags(CacheContainerAdmin.AdminFlag.VOLATILE).getOrCreateCache(realm, DefaultTemplate.DIST_SYNC);
			realms.add(realm);
			caches.put(realm, remoteCacheManager.getCache(realm)); 
			return caches.get(realm); 
		}
	}

	public CoreEntity getEntityFromCache(String cacheName, String key) {
		if (remoteCacheManager == null) {
			initRemoteCacheManager();
		}
		RemoteCache<String, CoreEntity> cache = remoteCacheManager.getCache(cacheName);
		return cache.get(key);
	}

	public CoreEntity putEntityIntoCache(String cacheName, CoreEntityKey key, CoreEntity value) {
		if (remoteCacheManager == null) {
			initRemoteCacheManager();
		}
		RemoteCache<CoreEntityKey, CoreEntity> cache = remoteCacheManager.getCache(cacheName);
		return cache.put(key, value);
	}

}
