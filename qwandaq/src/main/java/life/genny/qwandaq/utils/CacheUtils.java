package life.genny.qwandaq.utils;

import java.lang.reflect.Type;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.data.GennyCache;

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

		cache.getRemoteCache(realm).put(key, value);
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

		String data = (String) readCache(realm, key);
		if (StringUtils.isEmpty(data)) {
			log.info("DEBUG, key:" + key + ", data:" + data);
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
}
