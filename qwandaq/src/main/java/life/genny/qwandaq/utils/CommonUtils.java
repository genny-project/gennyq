package life.genny.qwandaq.utils;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import life.genny.qwandaq.exception.runtime.config.MissingEnvironmentVariableException;
import life.genny.qwandaq.exception.runtime.entity.GennyPrefixException;
import life.genny.qwandaq.utils.callbacks.FIGetObjectCallback;
import life.genny.qwandaq.utils.callbacks.FIGetStringCallback;
import life.genny.qwandaq.utils.callbacks.FILogCallback;

/**
 * A few Common Utils to use throughout Genny.
 *
 * @author Bryn
 * @author Jasper
 */
public class CommonUtils {
	static final Logger log = Logger.getLogger(CommonUtils.class);

    public static final String STR_ARRAY_EMPTY = "[]";

    private CommonUtils() {/* utilities class */}

    /**
     * Normalize a String by forcing uppercase on first character and lowercase on the rest
     * e.g: 
     * <ul>
     *  <li>string -> String</li>
     *  <li>STRING -> String</li>
     * </ul>
     * @param string
     * @return
     */
	public static String normalizeString(String string) {
		return string.substring(0, 1).toUpperCase().concat(string.substring(1).toLowerCase());
	}

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

    public static <T> void printCollection(T[] collection, FILogCallback logCallback, FIGetStringCallback<T> logLine) {
        if(collection == null) {
            logCallback.log("[!] Empty Collection");
            new Exception().printStackTrace();
            return;
        }

        for(T item : collection) {
            logCallback.log(logLine.getString(item));
        }
    }

    public static <T>void printCollection(Collection<T> collection, FILogCallback logCallback, FIGetStringCallback<T> logLine) {
        if(collection == null) {
            logCallback.log("Could not find collection");
            new Exception("stack trace exception").printStackTrace();
            return;
        }
        for(T item : collection) {
            logCallback.log(logLine.getString(item));
        }
    }

    public static <T> void printCollection(Collection<T> collection, FILogCallback logCallback) {
        printCollection(collection, logCallback, Object::toString);
    }

    public static <T>void printCollection(Collection<T> collection, FIGetStringCallback<T> logLine) {
        printCollection(collection, log::info, logLine);
    }

    public static <T>void printCollection(Collection<T> collection) {
        printCollection(collection, log::info, Object::toString);
    }

    public static <K, V> void printMap(Map<K, V> map) {
        printMap(map, log::info);
    }

    /**
     * Prints a map over multiple lines
     * works well assuming that the toString methods of the keys and values are well defined
     * @param map map to print
     */
    public static <K, V> void printMap(Map<K, V> map, FILogCallback log) {
        for(Map.Entry<K, V> entry : map.entrySet()) {
            log.log(entry.getKey() + "=" + entry.getValue());
        }
    }

    public static <K, V> void printMap(Map<K, V> map, FILogCallback logCallback, FIGetStringCallback<K> keyCallback, FIGetStringCallback<V> valueCallback) {
        for(Map.Entry<K, V> entry : map.entrySet()) {
            String msg = new StringBuilder(keyCallback.getString(entry.getKey()))
                            .append(" = ")
                            .append(valueCallback.getString(entry.getValue()))
                            .toString();
            logCallback.log(msg);
        }
    }

    public static <K, V> void printMap(Map<K, V> map, FIGetStringCallback<K> keyCallback, FIGetStringCallback<V> valueCallback) {
        printMap(map, log::info, keyCallback, valueCallback);
    }

    /**
     * Safe-compare two Objects (null-safe)
     * @param <T> type
     * @param objA Object1 to compare
     * @param objB Object2 to compare
     * @return true if both values are the same or false if not. 2 values are assumed equals if objA.equals(objB) OR both objA and objB are null
     */
    public static <T> Boolean compare(T objA, T objB) {
        if(objA == null || objB == null) {
            return objB == null && objA == null;
        }

        return objA.equals(objB);
    }

    /**
     * A method to retrieve a system environment variable, and optionally log it if it is missing (default, do log)
     * @param env Env to retrieve
     * @param fallback the fallback to use if the env cannot be found.
     * @return the value of the environment variable, or null if it cannot be found
     * 
     * @throws {@link MissingEnvironmentVariableException} if env can be null 
     */
    public static String getSystemEnv(String env, String fallback) {
        String result = System.getenv(env);
        
        if(result == null && fallback == null) {
            throw new MissingEnvironmentVariableException(env);
        } else if(result == null) {
            log.warn("Could not find System Environment Variable: " + env);
            return fallback;
        }

        return result;
    }

    /**
     * A method to retrieve a system environment variable, and optionally log it if it is missing (default, do log)
     * @param env Env to retrieve
     * @return the value of the environment variable, or null if it cannot be found
     */
    public static String getSystemEnv(String env) {
        return getSystemEnv(env, null);
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
        if(arr == null || arr.length == 0) {
            return STR_ARRAY_EMPTY;
        }
        return getArrayString(arr, (item) -> {
            return item != null ? item.toString() : "null";
        });
    }

    /**
     * Get a JSON style array of objects. Pass a callback for custom values. Will default to {@link Object#toString()} otherwise
     * @param <T> type
     * @param list - list to get array of
     * @param stringCallback - callback to use to retrieve a string value of the object
     * @return a JSON style array of objects, where each item is the value returned from stringCallback
     */
    public static <T> String getArrayString(Collection<T> list, FIGetStringCallback<T> stringCallback) {
        if(list == null)
            return "null";
        StringBuilder result = new StringBuilder("[");
        Iterator<T> iterator = list.iterator();
        for(int i = 0; i < list.size(); i++) {
            T object = iterator.next();
            result.append("\"")
                .append(stringCallback.getString(object));
            if(iterator.hasNext())
                result.append("\",");
            else
                result.append("\"");
        }

        // T object = iterator.next();
        // result.append("\"")
        //     .append(stringCallback.getString(object))
        result.append("]");
        return result.toString();
    }

    /**
     * Get a JSON style array of objects. Pass a callback for custom values. Will default to {@link Object#toString()} otherwise
     * @param <T> type
     * @param array - list to get array of
     * @param stringCallback - callback to use to retrieve a string value of the object
     * @return a JSON style array of objects, where each item is the value returned from stringCallback
     */
    public static <T> String getArrayString(T[] array, FIGetStringCallback<T> stringCallback) {
        if(array == null) return null;
        if(array.length == 0) return STR_ARRAY_EMPTY;
        
        StringBuilder result = new StringBuilder("[");
        int i;
        for(i = 0; i < array.length - 1; i++) {
            result.append("\"")
                    .append(stringCallback.getString(array[i]))
                    .append("\",");
        }

        result.append("\"")
                .append(stringCallback.getString(array[i]))
                .append("\"]");

        return result.toString();
    }

    /**
     * Fetch an instance of a class from {@link io.quarkus.Arc}
     * @param <T>
     * @param clazz - Class to fetch instance for
     * @return the instance from CDI
     */
    public static <T> T getArcInstance(Class<T> clazz) {
        T instance = Arc.container().select(clazz).get();
        if(instance == null) {
            log.error("Could not find instance of " + clazz.getSimpleName() + " in the context!");
        }

        return instance;
    }

    /**
     * Get a String Array of A JSONified String Array
     * @param arrayString - the JSONified String Array
     * @return A String array with each entry the mapped equivalent through {@link Object#toString} on each entry in the Jsonified String array
     * 
     * <p>E.g
     * <pre>
     *  String intData = "[\"1\",\"2\",\"3\"]"; // this reads as ["1","2","3"]
     * 
     * String[] ints = getArrayFromString(intData);
     * 
     * // ints is now a *String* array containing: ["1","2","3"]
     * </pre>
     * </p>
     */
    public static String[] getArrayFromString(String arrayString) {
        return getArrayFromString(arrayString, Object::toString);
    }

    /**
     * Get a String Array of A JSONified String Array
     * @param arrayString - the JSONified String Array
     * @param mapCallback - the mapping function to apply on each entry
     * @return A String array with each entry the mapped equivalent using the mapCallback on each entry in the Jsonified String array
     * 
     * <p>E.g
     * <pre>
     *  String intData = "[\"1\",\"2\",\"3\"]"; // this reads as ["1","2","3"]
     * 
     * String[] ints = getArrayFromString(intData, (integer) -> integer.concat("abcde"));
     * 
     * // ints is now a *String* array containing: ["1abcde","2abcde","3abcde"]
     * </pre>
     * </p>
     */
    public static String[] getArrayFromString(String arrayString, FIGetObjectCallback<String> mapCallback) {
        return getArrayFromString(arrayString, String.class, mapCallback);
    }

    /**
     * Get an Array From A JSONified String Array
     * @param <T> - Type of the array to collect
     * @param arrayString - the JSONified String Array
     * @param type - the Class referring to {@link T the type}
     * @param objectCallback - a lambda that maps one entry in the string array to the requested Type
     * @return an array of type {@link T} with each entry the mapped equivalent according to objectCallback of each entry in the Jsonified String array
     * 
     * <p>E.g
     * <pre>
     *  String intData = "[\"1\",\"2\",\"3\"]"; // this reads as ["1","2","3"]
     * 
     * // Both of these are logically equivalent
     * Integer[] ints = getArrayFromString(intData, Integer.class, (data -> Integer.parseInt(data)));
     * 
     * // SonarLint Compliant Solution
     * Integer[] ints = getArrayFromString(intData, Integer.class, Integer::parseInt);
     * 
     * // ints is now an Integer array containing {1, 2, 3}
     * </pre>
     * </p>
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] getArrayFromString(String arrayString, Class<T> type, FIGetObjectCallback<T> objectCallback) {
        arrayString = arrayString.substring(1, arrayString.length() - 1).replace("\"", "").strip();
        
		if(StringUtils.isBlank(arrayString))
            return (T[])Array.newInstance(type, 0);

        String[] components = arrayString.split(",");
        T[] array = (T[])Array.newInstance(type, components.length);
                
        for(int i = 0; i < components.length; i++) {
            String component = components[i];
            array[i] = objectCallback.getObject(component);
        }

        return array;
    }

    public static String getCommaAndSingleQuoteSeparatedString(Collection<String> tokens) {
        List<String> newList = new CopyOnWriteArrayList<>();
        tokens.forEach(token -> newList.add("'"+token+"'"));
        return newList.toString();
    }

    public static String getCommaSeparatedString(Collection<String> tokens) {
        return new CopyOnWriteArrayList<>(tokens).toString();
    }

    public static List<String> getListFromCommaSeparatedString(String arrayString) {
        List<String> newList;
        if (StringUtils.isBlank(arrayString)) {
            return new ArrayList<>(0);
        }
        arrayString = arrayString.trim();
        if (arrayString.startsWith("[") && arrayString.endsWith("]")) {
            arrayString = arrayString.substring(1, arrayString.length() - 1);
        }
        arrayString = arrayString.replaceAll("\"", "").strip();
        if (StringUtils.isBlank(arrayString))
            return new ArrayList<>(0);
        String tokens[] = StringUtils.split(arrayString, ",");
        newList = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            String trimmedToken = token.trim();
            if(!StringUtils.isBlank(trimmedToken)) {
                newList.add(trimmedToken);
            }
        }
        return newList;
    }

    /**
     * Assuming arrayString is of the form "[a,b,c,d]"
     * @param <T>
     * @param arrayString
     * @param objectCallback
     * @return
     */
    public static <T> List<T> getListFromString(String arrayString, FIGetObjectCallback<T> objectCallback) {
        if(StringUtils.isBlank(arrayString))
            return new ArrayList<>(0);

        arrayString = arrayString.substring(1, arrayString.length() - 1).replaceAll("\"", "").strip();

		if(StringUtils.isBlank(arrayString))
            return new ArrayList<>(0);

        String components[] = arrayString.split(",");
        List<T> newList = new ArrayList<>(components.length);
        for(String component : components) {
            newList.add(objectCallback.getObject(component));
        }

        return newList;
    }


    /**
     * 
     * @param <T>
     * @param arrayString
     * @param objectCallback
     * @return
     */
    public static <T> Set<T> getSetFromString(String arrayString, FIGetObjectCallback<T> objectCallback) {
        String[] components = arrayString.substring(1, arrayString.length() - 1).replaceAll("\"", "").split(",");
        Set<T> newSet = new HashSet<>();
        for(String component : components) {
            newSet.add(objectCallback.getObject(component));
        }

        return newSet;
    }

    /**
     * Create an equals break (======) of size len
     * @param len length of the equals break
     * @return The equals string
     */
    public static String equalsBreak(int len) {
        StringBuilder ret = new StringBuilder();
        for(int i = 0; i < len; i++) {
            ret.append("=");
        }

        return ret.toString();
    }

    /**
     * Check if item is in array
     */
    public static <T>boolean isInArray(T[] array, T obj) {
        for(T o : array) {
            if(o == null && obj != null || o != null && obj == null) {
                continue;
            }

            // Now o and obj can only be both null or not null
            // so testing if 1 is null tests if both are null
            if(o == null || o.equals(obj))
                return true;
        }

        return false;
    }

	/**
	 * Classic Genny style string clean up. This will remove any double quotes,
	 * whitespaces and square brackets from the string.
	 * <p>
	 * Hope this makes our code look a little
	 * nicer :)
	 * <p>
	 * 
	 * @param value The value to clean
	 * @return A clean string
	 */
    public static String cleanUpAttributeValue(String value) {
		return value.replace("\"", "").replace("[", "").replace("]", "").replace(" ", "");
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

    public static String substitutePrefix(String code, String prefix) {
        if(prefix.length() == 4) {
            if(prefix.charAt(3) != '_') {
                log.error("Could not substitute prefix: " + prefix + ". Prefix length is not 3 characters or 4 characters including an '_'");
                return code;
            }

            // ensure 3 character length after underscore check
            prefix = prefix.substring(0, 3);
        }

		if(prefix.length() != 3) {
			log.error("Could not substitute prefix: " + prefix + ". Prefix length is not 3 characters or 4 characters including an '_'");
			return code;
		}

        // all prefixes are now 3 characters at this point. Yay
		return prefix.concat(code.substring(3));
	}

	/**
	 * Strip the prefix assuming there is a prefix of 3 characters on the code
	 * @param code
	 * @return the code without the prefix (if there is no prefix of 3 characters, there is no change)
	 */
	public static String safeStripPrefix(String code) {
		String[] components = code.split("_");
		if(components.length <= 1) { // no prefix
			return code;
		} else {
			if(components[0].length() != 3) {
				return code;
			}

			return code.substring(4);
		}
	}

    /**
     * Remove an entry or entries from a jsonified string array
     * @param array
     * @param entries
     * @return the new array (an empty array if array is null)
     */
    public static String removeFromStringArray(String array, String... entries) {
        // no entries array == no entries to remove
        if(entries == null)
            return StringUtils.isBlank(array) ? STR_ARRAY_EMPTY : array;
        
        // return new array if there is no array
        if(StringUtils.isBlank(array)) {
            return STR_ARRAY_EMPTY;
        }
        
        StringBuilder sb = new StringBuilder(array);
        for(String entry : entries) {
            // ensure we're not trying to remove from empty array
            if(sb.charAt(0) == '[') {
                if(sb.charAt(1) == ']')
                    return STR_ARRAY_EMPTY;
            }

            if(StringUtils.isBlank(entry))
                continue;
            int start = sb.indexOf(entry);
            if(start == -1)
                continue;

            // Deal with quotes
            int end = start + entry.length() + 1;
            start -= 1;
            // deal with start/end
            if(start == 1) {
                if(sb.length() - end != 1)
                    end += 1;
            }
            else
                start -= 1;
            sb.delete(start, end);
        }

        return sb.toString();
    }

    /**
     * Add an entry or entries to a jsonified String array. This assumes the String array
     * is not malformed, but it can be null or empty
     * 
     * If the set of entries is null, the array will be returned
     * @param array - array to append to
     * @param entries - entries to append
     * @return the array with the entry appended to it or the preexisting array if
     *  the entries param is null. If the array is null, a new stringified array containing the entries
     * will be created
     */
    public static String addToStringArray(String array, String... entries) {
        // no entries array == no entries to add
        if(entries == null)
            return StringUtils.isBlank(array) ? STR_ARRAY_EMPTY : array;

        // return the entries as an array if there is no array
        if(StringUtils.isBlank(array)) {
            return CommonUtils.getArrayString(entries);
        }
        
        // chop off the ending "]"
        array = array.substring(0, array.length() - 1);
        StringBuilder sb = new StringBuilder(array);
        

        // add all entries such that each entry is "entry",
        // with the exception of the last one, which should not have a comma
        if(entries.length > 0) {
            // check if we're adding to a preexisting array
            if(!array.equals("["))
                sb.append(",");

            for(int i = 0; i < entries.length - 1; i++) {
                sb.append("\"")
                    .append(entries[i])
                    .append("\",");
            }

            sb.append("\"")
            .append(entries[entries.length - 1])
            .append("\"");
        }

        // reattach our missing "]" in all cases
        return sb.append("]").toString();
    }
}
