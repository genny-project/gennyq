package life.genny.qwandaq.utils.ickle;

import life.genny.qwandaq.exception.runtime.QueryBuilderException;
import life.genny.qwandaq.utils.ickle.predicate.IcklePredicate;
import org.apache.commons.lang3.ArrayUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.criteria.Order;
import java.util.List;

@ApplicationScoped
public class IckleQueryBuilder {

    private static final Logger log = Logger.getLogger(IckleQueryBuilder.class);

    public static final char SPACE = ' ';

    public static final char COMMA = ',';

    public static final String SELECT = "SELECT ";

    public static final String FROM = "FROM ";

    public static final String WHERE = "WHERE ";

    public static final String GROUP_BY = "GROUP BY ";

    public static final String HAVING = "HAVING ";

    public static final String ORDER_BY = "ORDER BY ";

    private String selectClause;

    private boolean distinct;

    private String fromClause;

    private String whereClause;

    private String groupByClause;

    private String havingClause;

    private String orderByClause;
    private Order order;

    public IckleQueryBuilder() {
    }

    public IckleQueryBuilder selectClause(String... columnNames) {
        if(ArrayUtils.isEmpty(columnNames)) {
            selectClause = "";//getCommaSeparatedNames(SELECT, "*");
        } else {
            selectClause = getCommaSeparatedNames(SELECT, columnNames);
        }
        return this;
    }

    public IckleQueryBuilder distinct(boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    public IckleQueryBuilder fromClause(String... tableNames) {
        if(ArrayUtils.isEmpty(tableNames)) {
            throw new QueryBuilderException("From clause cannot be empty.");
        }
        fromClause = getCommaSeparatedNames(FROM, tableNames);
        return this;
    }

    public IckleQueryBuilder whereClause(IcklePredicate... restrictions) {
        if(!ArrayUtils.isEmpty(restrictions)) {
            /*for(Predicate predicate : restrictions) {
                predicate.
            }*/
        }
        return this;
    }

    public IckleQueryBuilder whereClause(List<IcklePredicate> restrictions) {
        if(!restrictions.isEmpty()) {
            for(IcklePredicate predicate : restrictions) {
                log.info("$$$$$$$$$$$$$$$$ expression alias: " + predicate.getAlias());
                predicate.getExpressions().stream().forEach(expression -> {
                    log.info("$$$$$$$$$$$$$$$$ expression alias: " + expression.getAlias());
                });
            }
        }
        return this;
    }

    public IckleQueryBuilder groupByClause(String... columnNames) {
        if(ArrayUtils.isEmpty(columnNames)) {
            this.groupByClause = getCommaSeparatedNames(GROUP_BY, columnNames);
        }
        return this;
    }

    public IckleQueryBuilder havingClause(String... havingClause) {
        if(ArrayUtils.isEmpty(havingClause)) {
            //this.havingClause = havingClause;
        }
        return this;
    }

    public IckleQueryBuilder orderByClause(Order... orders) {
        if(!ArrayUtils.isEmpty(orders)) {
            String[] columnNames = new String[orders.length];
            /*for(Order order : orders) {
                order
            }*/
            this.orderByClause = getCommaSeparatedNames(ORDER_BY, columnNames);
        }
        return this;
    }

    public String toIckleQueryString() {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(selectClause).append(SPACE);
        queryBuilder.append(fromClause).append(SPACE);
        queryBuilder.append(whereClause).append(SPACE);
        queryBuilder.append(groupByClause).append(SPACE);
        queryBuilder.append(havingClause).append(SPACE);
        queryBuilder.append(orderByClause).append(SPACE);
        return queryBuilder.toString();
    }

    private String getCommaSeparatedNames(String clause, String... columnNames) {
        if(ArrayUtils.isEmpty(columnNames)) {
            throw new QueryBuilderException("Cannot build from empty array.");
        }
        StringBuilder builder = new StringBuilder(clause);
        boolean isFirst = true;
        for(String column : columnNames) {
            if(!isFirst) {
                builder.append(COMMA);
            }
            builder.append(SPACE).append(column);
            isFirst = false;
        }
        return builder.toString();
    }
}
