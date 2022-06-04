package life.genny.qwandaq.serialization.common;

import java.io.Serializable;

// Interface for infinispan cache keys
public interface CoreEntityKey extends Serializable {
    public String getKeyString();
    
    public CoreEntityKey fromKey(String key);

    public String getDelimiter();

    public default String[] getComponents() {
        return getKeyString().split(getDelimiter());
    }

    public String getEntityCode();
}
