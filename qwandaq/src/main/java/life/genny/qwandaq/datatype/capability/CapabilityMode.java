package life.genny.qwandaq.datatype.capability;

import java.util.HashMap;
import java.util.Map;

/**
 * An enum to declare what mode this capability concerns
 * @author Bryn Meachem
 */
public enum CapabilityMode {
    // Priority to be determined by .ordinal()
    VIEW('V'),
    EDIT('E'),
    ADD('A'),
    DELETE('D');

    private static Map<Character, CapabilityMode> idMap = new HashMap<>(values().length);

    static {
        for(CapabilityMode value : values()) {
            idMap.put(value.identifier, value);
        }
    }

    private final char identifier;

    private CapabilityMode(char identifier) {
        this.identifier = identifier;
    }

    public char getIdentifier() {
        return this.identifier;
    }

    public static CapabilityMode getByIdentifier(char identifier) {
        return idMap.get(identifier);
    }

    public static CapabilityMode getByOrd(int ordinal) {
        return CapabilityMode.values()[ordinal];
    }
}