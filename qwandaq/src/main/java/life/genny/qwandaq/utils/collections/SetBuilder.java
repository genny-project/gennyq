package life.genny.qwandaq.utils.collections;

import java.util.HashSet;

public class SetBuilder<T> extends CollectionBuilder<T> {

    public SetBuilder() {
        super(new HashSet<T>());
    }
}
