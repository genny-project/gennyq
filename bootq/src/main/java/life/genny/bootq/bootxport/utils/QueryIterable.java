package life.genny.bootq.bootxport.utils;

import java.util.Iterator;

import org.hibernate.query.Query;
import org.hibernate.engine.HibernateIterator;

public class QueryIterable<T> implements Iterable<T> {

    private Query<?> query;

    public QueryIterable(Query<?> query) {
        this.query = query;
    }

    @Override
    public Iterator<T> iterator() {
        return new CloseableIterator<T>((HibernateIterator) query);
    }
}
