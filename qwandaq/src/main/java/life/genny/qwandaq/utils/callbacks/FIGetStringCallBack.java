package life.genny.qwandaq.utils.callbacks;

/**
 * Simple functional interface to allow custom strings for stringified arrays
 */
public interface FIGetStringCallBack<T> {
    
    /**
     * Takes in an object of type T and returns a stringified representation of the object
     * @param object The object to get string from
     * @return A String
     */
    String getString(T object);
}
