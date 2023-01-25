package life.genny.qwandaq;

import life.genny.qwandaq.serialization.CoreEntitySerializable;

import java.io.Serializable;

/*
 * Interface for the representation of any entity in hibernate form
 *
 * @author Varun Shastry
 */
public interface CoreEntityPersistable extends Serializable {
    public CoreEntitySerializable toSerializableCoreEntity();
}
