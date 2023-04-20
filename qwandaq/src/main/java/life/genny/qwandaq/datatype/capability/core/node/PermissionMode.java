package life.genny.qwandaq.datatype.capability.core.node;

import java.util.HashMap;
import java.util.Map;

/**
 * An enum to declare what permissions this capability has
 * @author Bryn Meachem
 */
public enum PermissionMode {
    // Priority to be determined by .ordinal()
    NONE('N'),
    SELF('S'),
    GROUP('G'),
    ALL('A');

    private static Map<Character, PermissionMode> idMap = new HashMap<>(values().length);

    static {
        for(PermissionMode value : values()) {
            idMap.put(value.identifier, value);
        }
    }

    private final char identifier;

    private PermissionMode(char identifier) {
        this.identifier = identifier;
    }

    public char getIdentifier() {
        return this.identifier;
    }

    public static PermissionMode getByIdentifier(char identifier) {
        return idMap.get(identifier);
    }

    public static PermissionMode getByOrd(int ordinal) {
        return PermissionMode.values()[ordinal];
    }

    public boolean morePermissiveThan(PermissionMode other) {
        return this.ordinal() > other.ordinal();
    }
}
