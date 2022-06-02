package life.genny.qwandaq.utils;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.lang.reflect.Type;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import life.genny.qwandaq.CoreEntity;
import life.genny.qwandaq.data.GennyCache;
import life.genny.qwandaq.serialization.common.CoreEntityKey;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.logging.Logger;




/*
 * A static utility class used for standard read and write 
 * operations to the cache.
 * 
 * @author Jasper Robison
 */
@RegisterForReflection
public class CacheUtils {

	static final Logger log = Logger.getLogger(CacheUtils.class);

	static Jsonb jsonb = JsonbBuilder.create();

	private static GennyCache cache = null;

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
	 */
	public static void writeCache(String realm, String key, String value) {
		log.info("realm is " + realm);
		log.info("key is " + key);
		RemoteCache<String, String> remoteCache = cache.getRemoteCache(realm);
		log.info("remoteCache was returned");
		remoteCache.put(key, value);
		log.info("cache finished writing for "+realm+" "+key);
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
	public static <T> T getObject(String realm, String key, Class c) {

		log.debug("Cache Realm is " + realm);

		String data = (String) readCache(realm, key);
		if (StringUtils.isEmpty(data)) {
			log.debug("key: " + key + ", data: " + data);
			return null;
		}
		Object object = jsonb.fromJson(data, c);
		return (T) object;
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
		Object object = jsonb.fromJson(data, t);
		return (T) object;
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
	}

	/**
	* Get a CoreEntity object from the cache using a CoreEntityKey.
	* 
	* @param cacheName The cache to read from
	* @param key The key they item is saved against
	* @return The CoreEntity returned
	 */
	public static CoreEntity getEntity(String cacheName, CoreEntityKey key) {
		return cache.getEntityFromCache(cacheName, key);
	}

	/**
	* Save a CoreEntity to the cache using a CoreEntityKey.
	*
	* @param cacheName The cache to save to
	* @param key The key to save against
	* @param entity The CoreEntity to save
	* @return The CoreEntity being saved
	 */
	public static CoreEntity saveEntity(String cacheName, CoreEntityKey key, CoreEntity entity) {
		return cache.putEntityIntoCache(cacheName, key, entity);
	}
}
