package life.genny.qwandaq.utils;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.CoreEntity;
import life.genny.qwandaq.constants.CacheName;
import life.genny.qwandaq.data.GennyCache;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.serialization.common.CacheKeyIntf;
import life.genny.qwandaq.serialization.common.key.core.CoreEntityKey;
import life.genny.qwandaq.serialization.key.baseentity.BaseEntityKey;

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
	public static void clear(CacheName cacheName) {

		cache.getRemoteCache(cacheName).clear();
	}

	/**
	 * Read a stringified item from a realm cache.
	 *
	 * @param realm the realm to read from
	 * @param key the key to read
	 * @return Object
	 */
	public static Object readCache(CacheName cacheName, CacheKeyIntf key) {

		Object ret = cache.getRemoteCache(cacheName).get(key);
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
	public static String writeCache(CacheName cacheName, CacheKeyIntf key, String value) {
		log.info("[!] Caching: [" + key.getFullKeyString() + "=" + value + "] into cache: " + cacheName.cacheName);

		RemoteCache<CacheKeyIntf, String> remoteCache = cache.getRemoteCache(cacheName);
		log.debug("remoteCache was returned");
		remoteCache.put(key, value);

		return remoteCache.get(key);
	}

	/**
	* Remove an entry from a realm cache.
	*
	* @param realm The realm cache to remove from.
	* @param key The key of the entry to remove.
	 */
	public static void removeEntry(CacheName cacheName, CacheKeyIntf key) {
		
		cache.getRemoteCache(cacheName).remove(key);
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
	public static <T> T getObject(CacheName cacheName, CacheKeyIntf key, Class<T> c) {

		log.debug("Cache Realm is " + cacheName.cacheName);

		String data = (String) readCache(cacheName, key);
		if (StringUtils.isEmpty(data)) {
			log.debug("key: " + key + ", data: " + data);
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
	public static <T> T getObject(CacheName cacheName, CacheKeyIntf key, Type t) {

		String data = (String) readCache(cacheName, key);
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
	 * @param key the {@link CacheKeyIntf} to put object under
	 * @param obj the obj to put
	 */
	public static void putObject(CacheName cacheName, CacheKeyIntf key, Object obj) {

		String json = jsonb.toJson(obj);
		cache.getRemoteCache(cacheName).put(key, json);
	}

	/**
	* Get a CoreEntity object from the cache using a CoreEntityKey.
	* 
	* @param cacheName The cache to read from
	* @param key The key they item is saved against
	* @return The CoreEntity returned
	 */
	public static CoreEntity getEntity(CacheName cacheName, CacheKeyIntf key) {
		return cache.getEntityFromCache(cacheName, key);
	}

	/**
	* Get a CoreEntity object from the cache using a CoreEntityKey.
	* 
	* @param cacheName The cache to read from
	* @param key The key they item is saved against
	* @return The CoreEntity returned
	 */
	public static BaseEntity getBaseEntity(BaseEntityKey key) {
		return (BaseEntity) getEntity(CacheName.BASEENTITY, key);
	}

	/**
	* Save a {@link CoreEntity} to the cache using a CoreEntityKey.
	*
	* @param cacheName The cache to save to
	* @param key The key to save against
	* @param entity The CoreEntity to save
	* @return The CoreEntity being saved
	 */
	public static CoreEntity saveEntity(CacheName cacheName, CacheKeyIntf key, CoreEntity entity) {
		return cache.putEntityIntoCache(cacheName, key, entity);
	}

	/**
	 * Get a list of {@link CoreEntity}s for a specific product code (if present) from cache by prefix.
	 * @param cacheName - Product Code / Cache to retrieve from
	 * @param prefix - Prefix of the Core Entity code to use
	 * @param productCode - the productCode to fetch the entities for. Will fetch all entities with prefix if this is null 
	 * @param callback - Callback to construct a {@link CacheKeyIntf} for cache retrieval
	 * @return a list of core entities with matching prefixes
	 * 
	 * See Also: {@link CacheKeyIntf}, {@link FICacheKeyCallback}
	 */
	static List<CoreEntity> getEntitiesByPrefix(CacheName cacheName, String productCode, String prefix) {
		List<CoreEntity> entities = cache.getRemoteCache(cacheName)
		.entrySet().stream()
		.filter((Map.Entry<CacheKeyIntf, String> entry) -> entry instanceof CoreEntityKey)
		.map((Map.Entry<CacheKeyIntf, String> entry) -> {
			CoreEntityKey currentKey = (CoreEntityKey)entry.getKey();
			
			// If a realm check is in place, filter by realm
			if(productCode != null) {
				if(!productCode.equals(currentKey.getProductCode()))
					return null;
			}

			return currentKey.getEntityCode().startsWith(prefix) ? jsonb.fromJson(entry.getValue(), CoreEntity.class) : null;
		})
		.filter((CoreEntity entity) -> {
			return entity != null;
		}).collect(Collectors.toList());

		return entities;
	}

	static List<CoreEntity> getEntitiesByPrefix(CacheName cacheName, String prefix) {
		return getEntitiesByPrefix(cacheName, null, prefix);
	}

	/**
	 * Get a list of {@link BaseEntity}s from cache for ALL product codes by prefix.
	 * @param cacheName - Product Code / Cache to retrieve from
	 * @param prefix - Prefix of the Core Entity code to use
	 * @return a list of base entities with matching prefixes
	 * 
	 * See Also: {@link BaseEntityKey}, {@link CacheKeyIntf#fromKey}, {@link CacheUtils#getEntitiesByPrefix}
	 */
	public static List<BaseEntity> getBaseEntitiesByPrefix(String prefix) {
		return getEntitiesByPrefix(CacheName.BASEENTITY, prefix).stream()
		.map((CoreEntity entity) -> (BaseEntity)entity).collect(Collectors.toList());
	}

	public static List<BaseEntity> getBaseEntitiesByPrefix(String productCode, String prefix) {
		return getEntitiesByPrefix(CacheName.BASEENTITY, productCode, prefix).stream()
		.map((CoreEntity entity) -> (BaseEntity)entity).collect(Collectors.toList());
	}
}
