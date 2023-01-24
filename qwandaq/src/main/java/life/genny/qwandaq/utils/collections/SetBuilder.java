package life.genny.qwandaq.utils.collections;

import java.util.HashSet;
import java.util.Set;

public class SetBuilder<T> extends CollectionBuilder<T> {

    public SetBuilder() {
        super(new HashSet<T>());
    }

    public Set<T> buildSet() {
        return (HashSet<T>)build();
    }
}
