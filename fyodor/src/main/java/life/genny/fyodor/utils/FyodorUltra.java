package life.genny.fyodor.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.management.Query;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import org.jboss.logging.Logger;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQuery;

import io.smallrye.mutiny.tuples.Tuple2;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.attribute.QEntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.QBaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.clause.And;
import life.genny.qwandaq.entity.search.clause.ClauseArgument;
import life.genny.qwandaq.entity.search.clause.Or;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.entity.search.trait.Ord;
import life.genny.qwandaq.entity.search.trait.Sort;
import life.genny.qwandaq.exception.runtime.QueryBuilderException;
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
	 * Fetch an array of BaseEntities using a SearchEntity.
	 * @param searchEntity
	 * @return
	 */
	public Tuple2<List<BaseEntity>, Long> findBaseEntities(SearchEntity searchEntity) {

		QBaseEntity baseEntity = new QBaseEntity("baseEntity");
		JPAQuery<QBaseEntity> query = createQuery26(baseEntity, searchEntity);

		List<BaseEntity> entities = query.select(QBaseEntity.baseEntity).distinct().fetch();
		long count = query.fetchCount();

		List<String> allowed = getSearchColumnFilterArray(searchEntity);

		Boolean columnWildcard = searchEntity.findEntityAttribute("COL_*").isPresent();

		// apply filter
		for (int i = 0; i < entities.size(); i++) {

			BaseEntity be = entities.get(i);

			be = beUtils.addNonLiteralAttributes(be);
			if (!columnWildcard) {
				be = beUtils.privacyFilter(be, allowed);
			}

			be.setIndex(i);
		}

		return Tuple2.of(entities, count);
	}

	/**
	 * Fetch an array of BaseEntity codes using a SearchEntity.
	 * @param searchEntity
	 * @return
	 */
	public Tuple2<List<String>, Long> findBaseEntityCodes(SearchEntity searchEntity) {

		QBaseEntity baseEntity = new QBaseEntity("baseEntity");
		JPAQuery<QBaseEntity> query = createQuery26(baseEntity, searchEntity);

		List<String> codes = query.select(baseEntity.code).distinct().fetch();
		long count = query.fetchCount();

		return Tuple2.of(codes, count);
	}

	/**
	 * Count the number of BaseEntities using a SearchEntity.
	 * @param searchEntity
	 * @return
	 */
	public Long countBaseEntities(SearchEntity searchEntity) {

		QBaseEntity baseEntity = new QBaseEntity("baseEntity");
		JPAQuery<QBaseEntity> query = createQuery26(baseEntity, searchEntity);

		// Fetch only the count
		return query.select(baseEntity.code).distinct().fetchCount();
	}

	public void search27(SearchEntity searchEntity) {
		
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();

		Root<BaseEntity> baseEntity = query.from(BaseEntity.class);
		query.select(baseEntity.get("code")).distinct(true);

		List<Predicate> predicates = new ArrayList<>();

		Map<String, Root<EntityAttribute>> map = new HashMap<>();

		List<ClauseArgument> filters = new ArrayList<>();
		// handle filters
		filters.stream().forEach(filter -> {
			findClausePredicate(query, map, filter);
		});

		// query.where(predicate);

		List<Tuple> tupleResult = entityManager.createQuery(query).getResultList();
	}

	public Predicate findClausePredicate(CriteriaQuery<Tuple> query, Map<String, Root<EntityAttribute>> map, ClauseArgument clauseArgument) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		Class clauseClass = clauseArgument.getClass();

		if (clauseClass == And.class) {
			And and = (And) clauseArgument;
			Predicate predicateA = findClausePredicate(query, map, and.getA());
			Predicate predicateB = findClausePredicate(query, map, and.getB());
			return cb.and(predicateA, predicateB);

		} else if (clauseClass == Or.class) {
			Or or = (Or) clauseArgument;
			Predicate predicateA = findClausePredicate(query, map, or.getA());
			Predicate predicateB = findClausePredicate(query, map, or.getB());
			return cb.or(predicateA, predicateB);

		} else if (clauseClass == Filter.class) {

			Filter filter = (Filter) clauseArgument;
			String code = filter.getCode();

			// add to map if not already there
			if (!map.containsKey(code))
			map.put(code, query.from(EntityAttribute.class));

			Root<EntityAttribute> entityAttribute = map.get(code);

			// TODO: return correct operator
			return findFilterPredicate(entityAttribute, filter);

		} else {
			throw new QueryBuilderException("Invalid clauseArgument class " + clauseClass);
		}
	}

	public Expression<?> findExpression(Root<EntityAttribute> entityAttribute, Filter filter) {
		
		// TODO: This maybe should come from the attribute

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		cb.like(entityAttribute.get("valueString"), "");

		Class<?> c = filter.getC();

		if (c == String.class)
			return entityAttribute.<String>get("valueString");
		else if (c == Integer.class)
			return entityAttribute.<Integer>get("valueInteger");
		else if (c == Long.class)
			return entityAttribute.<Long>get("valueLong");
		else if (c == Double.class)
			return entityAttribute.<Double>get("valueDouble");
		else if (c == LocalDate.class)
			return entityAttribute.get("valueDate");
		else if (c == LocalDateTime.class)
			return entityAttribute.get("valueDateTime");
		else
			throw new QueryBuilderException("Invalid path for class " + c);
	}

	@SuppressWarnings("unchecked")
	public Predicate findFilterPredicate(Root<EntityAttribute> entityAttribute, Filter filter) {

		Class<?> c = filter.getC();
		if (isChronoClass(c))
			return findChronoPredicate(entityAttribute, filter);

		Expression<?> expression = findExpression(entityAttribute, filter);

		Operator operator = filter.getOperator();
		Object value = filter.getValue();

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		switch (operator) {
			case LIKE:
				return cb.like((Expression<String>) expression, String.class.cast(value));
			case NOT_LIKE:
				return cb.notLike((Expression<String>) expression, String.class.cast(value));
			case EQUALS:
				return cb.equal(expression, value);
			case NOT_EQUALS:
				return cb.notEqual(expression, value);
			case GREATER_THAN:
				return cb.gt((Expression<Number>) expression, Number.class.cast(value));
			case LESS_THAN:
				return cb.lt((Expression<Number>) expression, Number.class.cast(value));
			case GREATER_THAN_OR_EQUAL:
				return cb.ge((Expression<Number>) expression, Number.class.cast(value));
			case LESS_THAN_OR_EQUAL:
				return cb.le((Expression<Number>) expression, Number.class.cast(value));
			default:
				throw new QueryBuilderException("Invalid Operator: " + operator);
		}
	}

	public Predicate findChronoPredicate(Root<EntityAttribute> entityAttribute, Filter filter) {

		Class<?> c = filter.getC();

		Operator operator = filter.getOperator();
		Object value = filter.getValue();

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		Expression<?> expression = findExpression(entityAttribute, filter);

		switch (operator) {
			case EQUALS:
				return cb.equal(expression, value);
			case NOT_EQUALS:
				return cb.notEqual(expression, value);
			case GREATER_THAN:
				return cb.greaterThan(expression.as(LocalDateTime.class), LocalDateTime.class.cast(value));
			case LESS_THAN:
				return cb.lt((Expression<Number>) expression, Number.class.cast(value));
			case GREATER_THAN_OR_EQUAL:
				return cb.ge((Expression<Number>) expression, Number.class.cast(value));
			case LESS_THAN_OR_EQUAL:
				return cb.le((Expression<Number>) expression, Number.class.cast(value));
			default:
				throw new QueryBuilderException("Invalid Chrono Operator: " + operator);
		}

	}

	public Boolean isChronoClass(Class<?> c) {
		return (c == LocalDateTime.class || c == LocalDate.class || c == LocalTime.class);
	}

	/**
	 * Create a query from a search entity.
	 * @param baseEntity QBaseEntity object
	 * @param searchEntity SearchEntity representing the search
	 * @return JPAQuery object
	 */
	private JPAQuery<QBaseEntity> createQuery26(QBaseEntity baseEntity, SearchEntity searchEntity) {

		log.info("About to search (" + searchEntity.getCode() + ")");

		JPAQuery<QBaseEntity> query = new JPAQuery<QBaseEntity>(entityManager);
		query.from(baseEntity);

		// Ensure only Entities from our realm are returned
		String realm = searchEntity.getRealm();
		log.info("realm is " + realm);
		BooleanBuilder builder = new BooleanBuilder();
		builder.and(baseEntity.realm.eq(realm));

		Map<String, QEntityAttribute> map = new HashMap<>();

		List<Filter> filters = new ArrayList<>();
		List<Sort> sorts = new ArrayList<>();

		// handle filters
		filters.stream().forEach(filter -> {
			builder.and(buildFilterExpression(map, filter));
		});

		// handle filter joins
		map.forEach((code, join) -> {
			query.leftJoin(join)
					.on(join.pk.baseEntity.id.eq(baseEntity.id)
					.and(join.attributeCode.eq(code)));
		});

		// handle sorts
		sorts.stream().forEach(sort -> {
			buildSortExpression(query, baseEntity, sort);
		});

		// Get page start and page size from SBE
		Integer defaultPageSize = 20;
		Integer pageSize = searchEntity.getPageSize() != null ? searchEntity.getPageSize() : defaultPageSize;
		Integer pageStart = searchEntity.getPageStart() != null ? searchEntity.getPageStart() : 0;

		// Add all builder conditions to query
		query.where(builder);
		// Set page start and page size, then fetch codes
		query.offset(pageStart).limit(pageSize);

		return query;
	}

	/**
	 * @param map
	 * @param filter
	 * @return
	 */
	public BooleanExpression buildFilterExpression(Map<String, QEntityAttribute> map, Filter filter) {

		String code = filter.getCode();

		// add to map if not already there
		if (!map.containsKey(code))
			map.put(code, new QEntityAttribute(code));

		QEntityAttribute entityAttribute = map.get(code);
		BooleanExpression expression = findBooleanExpression(entityAttribute, filter);

		// if (filter.hasSuccessor()) {
		// 	Successor successor = filter.getSuccessor();
		// 	BooleanExpression successorExpression = buildFilterExpression(map, successor.getFilter());

		// 	if (successor.getOperation() == Successor.Operation.AND)
		// 		expression.and(successorExpression);
		// 	else if (successor.getOperation() == Successor.Operation.OR)
		// 		expression.or(successorExpression);
		// 	else
		// 		throw new QueryBuilderException("Invalid successor Operation " + successor.getOperation());
		// }
		
		return expression;
	}

	/**
	 * @param entityAttribute
	 * @param filter
	 * @return
	 */
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

		// TODO: BEWARE!!! this is probably wrong
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

	/**
	 * @param query
	 * @param baseEntity
	 * @param sort
	 */
	public void buildSortExpression(JPAQuery<?> query, QBaseEntity baseEntity, Sort sort) {

		String code = sort.getCode();
		Ord order = sort.getOrder();

		QEntityAttribute join = new QEntityAttribute("SORT_" + code);
		query.leftJoin(join)
				.on(join.pk.baseEntity.id.eq(baseEntity.id)
				.and(join.attributeCode.eq(code)));

		ComparableExpressionBase column = null;
		// Use ID because there is no index on created, and this gives same result
		if (code.startsWith("PRI_CREATED"))
			column = baseEntity.id;
		else if (code.startsWith("PRI_UPDATED"))
			column = baseEntity.updated;
		else if (code.equals("PRI_CODE"))
			column = baseEntity.code;
		else if (code.equals("PRI_NAME"))
			column = baseEntity.name;
		else {
			// Use Attribute Code to find the datatype, and thus the DB field to sort on
			Attribute attr = qwandaUtils.getAttribute(code);
			String dtt = attr.getDataType().getClassName();
			column = getPathFromDatatype(dtt, join);
		}

		if (order == Ord.ASC)
			query.orderBy(column.asc());
		else if (order == Ord.DESC)
			query.orderBy(column.desc());
		else
			throw new QueryBuilderException("Invalid sort order " + order + " for code " + code);
	}

	/**
	 * Find the value column based off of the attribute datatype
	 *
	 * @param dtt
	 * @param entityAttribute
	 * @return
	 */
	public static ComparableExpressionBase getPathFromDatatype(String dtt, QEntityAttribute entityAttribute) {

		if (dtt.equals("Text")) {
			return entityAttribute.valueString;
		} else if (dtt.equals("java.lang.String") || dtt.equals("String")) {
			return entityAttribute.valueString;
		} else if (dtt.equals("java.lang.Boolean") || dtt.equals("Boolean")) {
			return entityAttribute.valueBoolean;
		} else if (dtt.equals("java.lang.Double") || dtt.equals("Double")) {
			return entityAttribute.valueDouble;
		} else if (dtt.equals("java.lang.Integer") || dtt.equals("Integer")) {
			return entityAttribute.valueInteger;
		} else if (dtt.equals("java.lang.Long") || dtt.equals("Long")) {
			return entityAttribute.valueLong;
		} else if (dtt.equals("java.time.LocalDateTime") || dtt.equals("LocalDateTime")) {
			return entityAttribute.valueDateTime;
		} else if (dtt.equals("java.time.LocalDate") || dtt.equals("LocalDate")) {
			return entityAttribute.valueDate;
		} else if (dtt.equals("java.time.LocalTime") || dtt.equals("LocalTime")) {
			return entityAttribute.valueTime;
		}

		log.warn("Unable to read datatype");
		return entityAttribute.valueString;
	}

	/**
	 * @param searchEntity
	 * @return
	 */
	public static List<String> getSearchColumnFilterArray(SearchEntity searchEntity) {

		// TODO: refactor this

		List<String> attributeFilter = new ArrayList<String>();
		List<String> assocAttributeFilter = new ArrayList<String>();

		for (EntityAttribute ea : searchEntity.getBaseEntityAttributes()) {
			String attributeCode = ea.getAttributeCode();
			if (attributeCode.startsWith("COL_") || attributeCode.startsWith("CAL_")) {
				if (attributeCode.equals("COL_PRI_ADDRESS_FULL")) {
					attributeFilter.add("PRI_ADDRESS_LATITUDE");
					attributeFilter.add("PRI_ADDRESS_LONGITUDE");
				}
				if (attributeCode.startsWith("COL__")) {
					String[] splitCode = attributeCode.substring("COL__".length()).split("__");
					assocAttributeFilter.add(splitCode[0]);
				} else {
					attributeFilter.add(attributeCode.substring("COL_".length()));
				}
			}
		}
		attributeFilter.addAll(assocAttributeFilter);

		return attributeFilter;
	}

}
