package life.genny.qwandaq.utils.collections;

import java.util.HashMap;
import java.util.Map;

public class MapDecorator<K, V> {
    private final Map<K, V> map;

    public MapDecorator(Map<K, V> map) {
        this.map = map;
    }

    public MapDecorator() {
        this(new HashMap<>());
    }

    public MapDecorator<K, V> put(K key, V value) {
        map.put(key, value);
        return this;
    }

    public Map<K, V> get() {
        return map;
    }
}
