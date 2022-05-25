package life.genny.qwandaq.serialization.common;

import java.io.Serializable;

// Interface for infinispan cache keys
public interface CoreEntityKey extends Serializable {
    public String getKeyString();
}
