package life.genny.qwandaq.serialization.common;

import java.io.Serializable;

// Interface for infinispan cache keys
public interface CoreEntityKeyIntf extends Serializable {
    public static final String DEFAULT_DELIMITER = ":";

    public String getKeyString();
    
    public CoreEntityKeyIntf fromKey(String key);

    public default String getDelimiter() {
        return DEFAULT_DELIMITER;
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

    public default String[] getComponents() {
        return getComponents(getKeyString());
    }

    public String getEntityCode();

    public String getProductCode();
}
