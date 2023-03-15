package life.genny.qwandaq.models;

import life.genny.qwandaq.utils.callbacks.FILogCallback;

/**
 * A Class for storing ANSI text colours 
 * used in logging, or anything else.
 */
public enum ANSIColour {
    RED("\033[0;31m"),
    BLACK("\033[0;30m"),
    GREEN("\033[0;32m"),
    YELLOW("\033[0;33m"),
    BLUE("\033[0;34m"),
    PURPLE("\033[0;35m"),
    CYAN("\033[0;36m"),
    WHITE("\033[0;37m"),
    RESET("\033[0m");

    private String colour;

    private ANSIColour(String colour) {
        this.colour = colour;
    }

    public static final void logColour(FILogCallback log, Object msg, ANSIColour colour) {
        log.log(doColour(msg, colour));
    }

    public static final String doColour(Object msg, ANSIColour colour) {
        return colour.colour + msg + ANSIColour.RESET.colour;
    }

    public static final String strip(String text) {
        for(ANSIColour col : values()) {
            text = text.replace(col.colour, "");
        }
        return text;
    }

    public String getColour() {
        return colour;
    }

}
