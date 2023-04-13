package life.genny.qwandaq.datatype.capability.conditionals;

import java.util.HashMap;
import java.util.Map;

public enum EConditionalType {
    NOT("!!"),
    AND("&&"),
    OR("||");

    private static final Map<String, EConditionalType> flagLookup = new HashMap<>(values().length);
    static {
        for(EConditionalType type : values()) {
            flagLookup.put(type.flag, type);
        }
    }

    public final String flag;

    private EConditionalType(String flag) {
        this.flag = flag;
    }

    public EConditionalType getByFlag(String flag) {
        return flagLookup.get(flag);
    }
}
