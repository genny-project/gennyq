package life.genny.qwandaq;

import life.genny.qwandaq.serialization.CoreEntitySerializable;

public interface CoreEntityPersistable {
    public CoreEntitySerializable toSerializableCoreEntity();
}
