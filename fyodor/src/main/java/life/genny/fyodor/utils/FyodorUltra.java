package life.genny.fyodor.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import org.jboss.logging.Logger;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQuery;

import life.genny.qwandaq.attribute.QEntityAttribute;
import life.genny.qwandaq.entity.QBaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.Filter;
import life.genny.qwandaq.entity.search.Operator;
import life.genny.qwandaq.entity.search.Successor;
import life.genny.qwandaq.exception.runtime.QueryBuilderException;
import life.genny.qwandaq.message.QSearchBeResult;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.Service;

@ApplicationScoped
public class FyodorUltra {

	private static final Logger log = Logger.getLogger(FyodorUltra.class);

	@Inject
	EntityManager entityManager;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	Service service;

	@Inject
	UserToken userToken;

	@Inject
	BaseEntityUtils beUtils;

	Jsonb jsonb = JsonbBuilder.create();

	/**
	 * Perform a safe search using named parameters to
	 * protect from SQL Injection.
	 * @param searchEntity      SearchEntity used to search.
	 * @param countOnly     Only perform a count.
	 * @param fetchEntities Fetch Entities, or only codes.
	 * @return Search Result Object.
	 */
	public QSearchBeResult findBySearch25(final SearchEntity searchEntity, Boolean countOnly, Boolean fetchEntities) {

		log.info("About to search (" + searchEntity.getCode() + ")");

		// Init necessary vars
		QSearchBeResult result = null;

		// Get page start and page size from SBE
		Integer defaultPageSize = 20;
		Integer pageSize = searchEntity.getPageSize() != null ? searchEntity.getPageSize() : defaultPageSize;
		Integer pageStart = searchEntity.getPageStart() != null ? searchEntity.getPageStart() : 0;

		QBaseEntity baseEntity = new QBaseEntity("baseEntity");
		JPAQuery<?> query = new JPAQuery<Void>(entityManager);
		query.from(baseEntity);

		BooleanBuilder builder = new BooleanBuilder();

		// Ensure only Entities from our realm are returned
		String realm = searchEntity.getRealm();
		log.info("realm is " + realm);
		builder.and(baseEntity.realm.eq(realm));

		Map<String, QEntityAttribute> map = new HashMap<>();

		searchEntity.getFilters().stream().forEach(filter -> {
			builder.and(buildFilterExpression(map, filter));
		});

		map.forEach((code, join) -> {
			query.leftJoin(join)
					.on(join.pk.baseEntity.id.eq(baseEntity.id)
							.and(join.attributeCode.eq(code)));
		});

		searchEntity.getSorts().stream().forEach(sort -> {
		});

		return result;
	}

	public BooleanExpression buildFilterExpression(Map<String, QEntityAttribute> map, Filter filter) {

		String code = filter.getCode();

		// add to map if not already there
		if (!map.containsKey(code))
			map.put(code, new QEntityAttribute(code));

		QEntityAttribute entityAttribute = map.get(code);
		BooleanExpression expression = findBooleanExpression(entityAttribute, filter);

		if (filter.hasSuccessor()) {
			Successor successor = filter.getSuccessor();
			BooleanExpression successorExpression = buildFilterExpression(map, successor.getFilter());

			if (successor.getOperation() == Successor.Operation.AND)
				expression.and(successorExpression);
			else if (successor.getOperation() == Successor.Operation.OR)
				expression.or(successorExpression);
			else
				throw new QueryBuilderException("Invalid successor Operation " + successor.getOperation());
		}
		
		return expression;
	}

	public BooleanExpression findBooleanExpression(QEntityAttribute entityAttribute, Filter filter) {
		
		// TODO: This maybe should come from the attribute

		Class c = filter.getC();
		SimpleExpression simpleExpression = null;

		if (c == String.class)
			simpleExpression =  entityAttribute.valueString;
		else if (c == Integer.class)
			simpleExpression = entityAttribute.valueInteger;
		else if (c == Long.class)
			simpleExpression = entityAttribute.valueLong;
		else if (c == Double.class)
			simpleExpression = entityAttribute.valueDouble;
		else if (c == LocalDate.class)
			simpleExpression = entityAttribute.valueDate;
		else if (c == LocalDateTime.class)
			simpleExpression = entityAttribute.valueDateTime;
		else
			throw new QueryBuilderException("Invalid path for class " + c);

		Operator operator = filter.getOperator();
		Object value = filter.getValue();

		// TODO: BEWARE!!! this could cause issues
		if (simpleExpression instanceof StringPath)
			return string((StringPath) simpleExpression, operator, (String) value);
		else if (simpleExpression instanceof NumberPath)
			return number((NumberPath) simpleExpression, operator, (Number) value);
		else if (simpleExpression instanceof DateTimePath)
			return datetime((DateTimePath) simpleExpression, operator, (LocalDateTime) value);

		throw new QueryBuilderException("Invalid type for path " + simpleExpression);
	}
	
	/**
	 * Switch for finding the expression based on filter
	 * @param field
	 * @param operator
	 * @param value
	 * @return
	 */
	public BooleanExpression string(StringPath field, Operator operator, String value) {

		switch (operator) {
			case LIKE:
				return field.like(value);
			case NOT_LIKE:
				return field.notLike(value);
			case EQUALS:
				return field.eq(value);
			case NOT_EQUALS:
				return field.ne(value);
			default:
				throw new QueryBuilderException("Invalid String operator " + operator);
		}
	}

	/**
	 * Switch for finding the expression based on filter
	 * @param field
	 * @param operator
	 * @param value
	 * @return
	 */
	public BooleanExpression number(NumberPath field, Operator operator, Number value) {
	
		switch (operator) {
			case EQUALS:
				return field.eq(value);
			case NOT_EQUALS:
				return field.ne(value);
			case GREATER_THAN:
				return field.gt(value);
			case LESS_THAN:
				return field.lt(value);
			case GREATER_THAN_OR_EQUAL:
				return field.goe(value);
			case LESS_THAN_OR_EQUAL:
				return field.loe(value);
			default:
				throw new QueryBuilderException("Invalid Number Case " + operator);
		}
	}

	/**
	 * @param field
	 * @param operator
	 * @param dateTime
	 * @return
	 */
	public static BooleanExpression datetime(DateTimePath field, Operator operator, LocalDateTime dateTime) {

		switch (operator) {
			case EQUALS:
			return field.eq(dateTime);
			case NOT_EQUALS:
				return field.ne(dateTime);
			case GREATER_THAN:
			case GREATER_THAN_OR_EQUAL:
				return field.after(dateTime);
			case LESS_THAN:
			case LESS_THAN_OR_EQUAL:
				return field.before(dateTime);
			default:
				throw new QueryBuilderException("Invalid Number Case " + operator);
		}
	}

}
