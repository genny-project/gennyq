package life.genny.qwandaq.utils.collections;

import java.util.ArrayList;

public class ListBuilder<T> extends CollectionBuilder<T> {
    
    public ListBuilder() {
        super(new ArrayList<T>());
    }
}
