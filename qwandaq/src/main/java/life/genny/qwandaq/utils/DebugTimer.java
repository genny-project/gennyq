package life.genny.qwandaq.utils;

import life.genny.qwandaq.utils.callbacks.FILogCallback;
import org.jboss.logging.Logger;

/**
 * Timer class that makes use of {@link System#nanoTime()}
 */
public class DebugTimer {
	private static final Logger defaultLogger = Logger.getLogger(DebugTimer.class);
    public static final String DEFAULT_MESSAGE = "[!] Timing took: ";

    private Long start;
    private Long end;
    
    private String message;

    private FILogCallback logLevel;

    /**
     * Create a new timer and start it
     * @param logLevel - level to log at when {@link DebugTimer#logTime} is called
     * @param message - message to prepend to the time when logging
     */
    public DebugTimer(FILogCallback logLevel, String message) {
        this.logLevel = logLevel;
        logLevel.log("Started new Debug Timer!");
        this.message = message;
        start = System.nanoTime();
    }

    /**
     * Create a new timer and start it
     * @param logLevel - level to log at when {@link DebugTimer#logTime} is called
     * with the {@link DebugTimer#DEFAULT_MESSAGE}
     */
    public DebugTimer(FILogCallback logLevel) {
        this(logLevel, DEFAULT_MESSAGE);
    }

    /**
     * Create a new timer and start it
     * @param log - logger to log on the debug level when {@link DebugTimer#logTime} is called
     * @param message - message to prepend to the time when logging
     */
    public DebugTimer(Logger log, String message) {
        this(log::debug, message);
    }

    /**
     * Create a new timer and start it, using {@link CommonUtils#log} as the logger (on the debug level)
     * @param message - message to prepend to the time when logging
     */
    public DebugTimer(String message) {
        this(defaultLogger::debug, message);
    }
    
    /**
     * Create a new timer and start it, using {@link CommonUtils#log} as the logger (on the debug level)
     * with the {@link DebugTimer#DEFAULT_MESSAGE}
     */
    public DebugTimer() {
        this(DEFAULT_MESSAGE);
    }

    /**
     * Get the duration at the current point in time
     * @return the duration at the current point in time
     */
    public long getDuration() {
        end = System.nanoTime();
        return end - start;
    }

    /**
     * Log the current duration
     */
    public void logTime() {
        long duration = getDuration();
        logLevel.log(message + duration + "ns (nanoseconds)");
        logLevel.log(   " - Took " + (duration / 1000) + "ms (milliseconds)");
    }
}
