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
     * Get the current memory usage in bytes
     * @return memory in bytes
     */
    public static Long getMemoryUsage() {
        return getMemoryUsage(MemoryMeasurement.BYTES);
    }

    /**
     * Get the current memory usage in terms of the {@link MemoryMeasurement} supplied
     * @param measurement - any of
     * <ul>
     * <li>BYTES</li>
     * <li>KILOBYTES</li>
     * <li>MEGABYTES</li>
     * <li>GIGABYTES</li>
     * </ul>
     * @return memory usage
     */
    public static Long getMemoryUsage(MemoryMeasurement measurement) {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / measurement.divisor;
    }

    /**
     * Get the current memory usage in terms of the {@link MemoryMeasurement} supplied
     * @param measurement - any of
     * <ul>
     * <li>BYTES</li>
     * <li>KILOBYTES</li>
     * <li>MEGABYTES</li>
     * <li>GIGABYTES</li>
     * </ul>
     * @return memory usage
     */
    public static Long getMemoryUsage(String measurement) {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / MemoryMeasurement.valueOf(measurement).divisor;
    }

    /**
     * Get percent memory used
     * @return percent of total memory used
     */
    public static float getPercentMemoryUsed() {
        return (float)getMemoryUsage() / (float)getTotalMemory();
    }

    /**
     * Get the total memory used
     * @return total memory used (bytes)
     */
    public static Long getTotalMemory() {
        return getTotalMemory(MemoryMeasurement.BYTES);
    }

    /**
     * 
     * @param measurement - any of
     * <ul>
     * <li>BYTES</li>
     * <li>KILOBYTES</li>
     * <li>MEGABYTES</li>
     * <li>GIGABYTES</li>
     * </ul>
     * @return total memory used in the measurement specified
     */
    public static Long getTotalMemory(MemoryMeasurement measurement) {
        return Runtime.getRuntime().totalMemory() / measurement.divisor;
    }

    /**
     * Safe-compare two Objects (null-safe)
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
     * Get a JSON style array of objects. Pass a callback for custom values. Will default to {@link Object#toString()} otherwise
     * @param list - list to get array of
     * @param stringCallback - callback to use to retrieve a string value of the object
     * @return a JSON style array of objects, where each item is the value returned from stringCallback
     */
    public static <T> String getArrayString(List<T> list, FIGetStringCallBack<T> stringCallback) {
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
    public static <T> String getArrayString(T[] array, FIGetStringCallBack<T> stringCallback) {
        String result = "";
        for(T object : array) {
            result += "\"" + stringCallback.getString(object) + "\",";
        }
        return "[" + result.substring(0, result.length() - 1) + "]";
    }

    public static enum MemoryMeasurement {
        BYTES(1),
        KILOBYTES(1000),
        MEGABYTES(1000000),
        GIGABYTES(1000000000);

        final int divisor;

        private MemoryMeasurement(int divisor) {
            this.divisor = divisor;
        }
    }
}