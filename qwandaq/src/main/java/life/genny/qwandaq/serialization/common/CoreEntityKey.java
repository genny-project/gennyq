package life.genny.qwandaq.serialization.common;

import java.io.Serializable;

// Interface for infinispan cache keys
public interface CoreEntityKey extends Serializable {
    String KEY_DELIMITER = "|";

    String getKeyString();
    
    CoreEntityKey fromKey(String key);

    default String getDelimiter() {
        return KEY_DELIMITER;
    }

    default String[] getComponents() {
        return getKeyString().split(getDelimiter());
    }

    String getEntityCode();
}
