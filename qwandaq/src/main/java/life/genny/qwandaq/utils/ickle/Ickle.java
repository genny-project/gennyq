package life.genny.qwandaq.utils.ickle;

import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.serialization.common.CoreEntityKey;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.QueryFactory;

public class Ickle {
    public IckleQueryBuilder getQueryBuilder(RemoteCache<CoreEntityKey, CoreEntityPersistable> remoteCache) {
        QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
        //queryFactory.
        return new IckleQueryBuilder();
    }
}
