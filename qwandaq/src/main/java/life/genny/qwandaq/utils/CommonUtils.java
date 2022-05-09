package life.genny.qwandaq.utils;

import java.util.List;

import org.jboss.logging.Logger;

import life.genny.qwandaq.utils.callbacks.FIGetStringCallBack;

/**
 * A few Common Utils to use throughout Genny.
 * 
 * @author Bryn
 * @author Jasper
 */
public class CommonUtils {
	static final Logger log = Logger.getLogger(CommonUtils.class);

    /**
     * A method to retrieve a system environment variable, and optionally log it if it is missing (default, do log)
     * @param env Env to retrieve
     * @param alert whether or not to log if it is missing or not (default: true)
     * @return the value of the environment variable, or null if it cannot be found
     */
    public static String getSystemEnv(String env, boolean alert) {
        String result = System.getenv(env);
        if(result == null && alert) {
            log.error("Could not find System Environment Variable: " + env);
        }

        return result;
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
     * String Array Builder to get Stringified arrays of custom components
     * TODO: Make this hella pretty
     */
    public static class StringArrayBuilder<T> {

        /**
         * Get a JSON style array of objects. Pass a callback for custom values. Will default to {@link Object#toString()} otherwise
         * @param list - list to get array of
         * @param stringCallback - callback to use to retrieve a string value of the object
         * @return a JSON style array of objects, where each item is the value returned from stringCallback
         */
        public String getArrayString(List<T> list, FIGetStringCallBack<T> stringCallback) {
            String result = "";
            for(T object : list) {
                result += "\"" + stringCallback.getString(object) + "\",";
            }
            return "[" + result.substring(0, result.length() - 1) + "]";
        }

        /**
         * Get a JSON style array of objects. Pass a callback for custom values. Will default to {@link Object#toString()} otherwise
         * @param array - list to get array of
         * @param stringCallback - callback to use to retrieve a string value of the object
         * @return a JSON style array of objects, where each item is the value returned from stringCallback
         */
        public String getArrayString(T[] array, FIGetStringCallBack<T> stringCallback) {
            String result = "";
            for(T object : array) {
                result += "\"" + stringCallback.getString(object) + "\",";
            }
            return "[" + result.substring(0, result.length() - 1) + "]";
        }
    }
}
