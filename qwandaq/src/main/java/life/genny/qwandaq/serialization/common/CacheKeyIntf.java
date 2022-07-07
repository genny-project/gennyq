package life.genny.qwandaq.serialization.common;

import java.io.Serializable;

// Interface for infinispan cache keys
public interface CacheKeyIntf extends Serializable {
    public static final String DEFAULT_DELIMITER = ":";

    public String getFullKeyString();
    
    public CacheKeyIntf fromKey(String key);

    public String getProductCode();

    public default String getDelimiter() {
        return DEFAULT_DELIMITER;
    }

    public default String getBaseKeyString() {
        return getProductCode() + getDelimiter();
    }
    
    public default String[] getComponents() {
        return getComponents(getFullKeyString());
    }

    public default String[] getComponents(String key) {
        if(getDelimiter() != null) {
            return key.split(getDelimiter());
        } else {
            String[] returnString = new String[1];
            returnString[0] = key;
            return returnString;
        }
    }
}
