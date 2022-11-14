package life.genny.qwandaq.utils;

import java.util.HashSet;
import java.util.Set;

public class SetBuilder<T> {
    private Set<T> set;

    public SetBuilder() {
        this.set = new HashSet<T>();
    }

    public SetBuilder(Set<T> set) {
        this.set = set;
    }

    public SetBuilder<T> add(T item) {
        set.add(item);
        return this;
    }

    public Set<T> build() {
        return set;
    }
}
