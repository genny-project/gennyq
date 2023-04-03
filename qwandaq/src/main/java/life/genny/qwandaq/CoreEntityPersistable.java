package life.genny.qwandaq;

import life.genny.qwandaq.serialization.CoreEntitySerializable;

import java.io.Serializable;
import java.time.LocalDateTime;

/*
 * Interface for the representation of any entity in hibernate form
 *
 * @author Varun Shastry
 */
public interface CoreEntityPersistable extends Serializable {
    LocalDateTime getCreated();
    void setCreated(LocalDateTime created);
    LocalDateTime getUpdated();
    void setUpdated(LocalDateTime updated);
    CoreEntitySerializable toSerializableCoreEntity();
}
