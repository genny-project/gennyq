package life.genny.qwandaq.utils.collections;

import java.util.HashMap;

public class BiDirectionalHashMap<K, V> extends HashMap<K, V> {
    private HashMap<V, K> reverseMap = new HashMap<>();

    @Override
    public V put(K key, V value) {
        reverseMap.put(value, key);
        return super.put(key, value);
    }

    @Override
    public V remove(Object key) {
        V val = super.remove(key);
        reverseMap.remove(val);
        return val;
    }
}
