package life.genny.qwandaq.utils.collections;

import java.util.Collection;

public class CollectionBuilder<CollectionType> {
    private Collection<CollectionType> collection;

    public CollectionBuilder(Collection<CollectionType> collection) {
        this.collection = collection;
    }

    public CollectionBuilder<CollectionType> add(CollectionType item) {
        collection.add(item);
        return this;
    }

    public Collection<CollectionType> build() {
        return collection;
    }
}
