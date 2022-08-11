package life.genny.qwandaq.serialization;

import java.io.Serializable;

import life.genny.qwandaq.CoreEntity;

/*
 * Interface for the representation of any entity in the cache
 * 
 * @author Varun Shastry
 */
public interface CoreEntitySerializable extends Serializable {
    CoreEntity toCoreEntity();
}
