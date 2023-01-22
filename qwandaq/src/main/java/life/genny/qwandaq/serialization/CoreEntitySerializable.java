package life.genny.qwandaq.serialization;

import life.genny.qwandaq.CoreEntityPersistable;

import java.io.Serializable;

/*
 * Interface for the representation of any entity in the cache
 *
 * @author Varun Shastry
 */
public interface CoreEntitySerializable extends Serializable {
    CoreEntityPersistable toPersistableCoreEntity();
}