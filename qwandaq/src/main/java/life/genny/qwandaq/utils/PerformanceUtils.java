package life.genny.qwandaq.utils;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.Logger;

public class PerformanceUtils {
	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

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
