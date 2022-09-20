package life.genny.qwandaq.utils;

import life.genny.qwandaq.exception.runtime.entity.GennyPrefixException;
import life.genny.qwandaq.utils.callbacks.FIGetStringCallBack;
import life.genny.qwandaq.utils.callbacks.FILogCallback;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * A few Common Utils to use throughout Genny.
 *
 * @author Bryn
 * @author Jasper
 */
public class CommonUtils {
	static final Logger log = Logger.getLogger(CommonUtils.class);

    /**
     * Log on a specific log level in a specific log and return an object
     * @param level - level to log on in the logger
     * @param msg - message to log
     * @return msg
     */
    public static Object logAndReturn(FILogCallback level, Object msg) {
        level.log(msg);
        return msg;
    }

    /**
     * Log info and return an object
     * @param log - log stream to log on (for class specific logs)
     * @param msg - message to log
     * @return msg
     */
    public static Object logAndReturn(Logger log, Object msg) {
        log.info(msg);
        return msg;
    }

    /**
     * Prints a list over multiple lines
     * works well assuming that the toString method of the item is well defined
     * @param list list to print
     */
    public static void printList(List<?> list) {
        for(Object item : list) {
            log.info(item);
        }
    }

    /**
     * Prints a map over multiple lines
     * works well assuming that the toString methods of the keys and values are well defined
     * @param map map to print
     */
    public static void printMap(Map<?, ?> map) {
        for(Object key : map.keySet()) {
            log.info(key + "=" + map.get(key));
        }
    }

    /**
     * Safe-compare two Objects (null-safe)
     * @param <T> type
     * @param objA Object1 to compare
     * @param objB Object2 to compare
     * @return true if both strings are the same or false if not
     */
    public static <T> Boolean compare(T objA, T objB) {
        // Case string a is null
        if(objA == null) {
            return (objB == null);
        }

        // Case string b is null
        if(objB == null) {
            return (objA == null);
        }

        return objA.equals(objB);
    }

    /**
     * A method to retrieve a system environment variable, and optionally log it if it is missing (default, do log)
     * @param env Env to retrieve
     * @param alert whether or not to log if it is missing or not (default: true)
     * @return the value of the environment variable, or null if it cannot be found
     */
    public static String getSystemEnv(String env, boolean alert) {
        String result = System.getenv(env);
        if(result == null && alert) {
            String msg = "Could not find System Environment Variable: " + env;
            if(alert) {
                log.error(msg);
            } else {
                log.warn(msg);
            }
        }

        return result;
    }

    public static <T> boolean arrayContains(T[] array, T object) {
        for(T obj : array) {
            if(obj.equals(object))
                return true;
        }

        return false;
    }

    /**
     * A method to retrieve a system environment variable, and optionally log it if it is missing (default, do log)
     * @param env Env to retrieve
     * @return the value of the environment variable, or null if it cannot be found
     */
    public static String getSystemEnv(String env) {
        return getSystemEnv(env, true);
    }

    /**
     * Get a JSON style array of objects using {@link Object#toString()} for each object
     * @param <T> type
     * @param list - list to get stringified array of
     * @return a JSON style array of object
     */
    public static <T> String getArrayString(Collection<T> list) {
        return getArrayString(list, (item) -> item.toString());
    }

    /**
     * Get a JSON style array of objects using {@link Object#toString()} for each object
     * @param <T> type
     * @param arr - array to get stringified array of
     * @return a JSON style array of object
     */
    public static <T> String getArrayString(T[] arr) {
        return getArrayString(arr, (item) -> {
            System.out.println("Item: " + item.toString());
            return item.toString();
        });
    }

    /**
     * Get a JSON style array of objects. Pass a callback for custom values. Will default to {@link Object#toString()} otherwise
     * @param <T> type
     * @param list - list to get array of
     * @param stringCallback - callback to use to retrieve a string value of the object
     * @return a JSON style array of objects, where each item is the value returned from stringCallback
     */
    public static <T> String getArrayString(Collection<T> list, FIGetStringCallBack<T> stringCallback) {
        String result = "";
        for(T object : list) {
            result += "\"" + stringCallback.getString(object) + "\",";
        }
        return "[" + result.substring(0, result.length() - 1) + "]";
    }

    /**
     * Get a JSON style array of objects. Pass a callback for custom values. Will default to {@link Object#toString()} otherwise
     * @param <T> type
     * @param array - list to get array of
     * @param stringCallback - callback to use to retrieve a string value of the object
     * @return a JSON style array of objects, where each item is the value returned from stringCallback
     */
    public static <T> String getArrayString(T[] array, FIGetStringCallBack<T> stringCallback) {
        String result = "";
        for(T object : array) {
            result += "\"" + stringCallback.getString(object) + "\",";
        }
        return "[" + result.substring(0, result.length() - 1) + "]";
    }

    /**
     * Create an equals break (======) of size len
     * @param len length of the equals break
     * @return The equals string
     */
    public static String equalsBreak(int len) {
        String ret = "";
        for(int i = 0; i < len; i++) {
            ret += "=";
        }

        return ret;
    }

	/**
	 * Replace the three character prefix of a string with another prefix.
	 * @param str The string on which to perform replacement.
	 * @param prefix The prefix to replace with.
	 * @return The updated string
	 */
	public static String replacePrefix(String str, String prefix) {

		if (str.charAt(3) != '_') {
			throw new GennyPrefixException(str + " is does not have a valid three character prefix");
		}
		return prefix + str.substring(3);
	}

	/**
	 * Remove a prefix from a string.
	 * @param str The string to operate on
	 * @return The string without the prefix
	 */
	public static String removePrefix(String str) {
		return str.substring(str.indexOf("_")+1);
	}

}
