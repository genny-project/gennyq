package life.genny.qwandaq.datatype.capability.core.node;

import java.util.HashMap;
import java.util.Map;

/**
 * An enum to declare what mode a capability concerns
 * @author Bryn Meachem
 */
public enum CapabilityMode {
    VIEW('V'), 
    EDIT('E'),
    ADD('A'),
    DELETE('D');

    /**
     * Cached map to speed up identifier lookup
     * 
     */
    private static final Map<Character, CapabilityMode> idMap = new HashMap<>(values().length);

    static {
        for(CapabilityMode value : values()) {
            idMap.put(value.identifier, value);
        }
    }

    /**
     * Single character identifier to help with processing
     */
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