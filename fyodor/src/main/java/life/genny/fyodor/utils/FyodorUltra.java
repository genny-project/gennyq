package life.genny.fyodor.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.tuples.Tuple2;
import life.genny.fyodor.models.JoinMap;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.EntityEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.clause.And;
import life.genny.qwandaq.entity.search.clause.Clause;
import life.genny.qwandaq.entity.search.clause.ClauseContainer;
import life.genny.qwandaq.entity.search.clause.Or;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.entity.search.trait.Ord;
import life.genny.qwandaq.entity.search.trait.Sort;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
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

	static Jsonb jsonb = JsonbBuilder.create();

	/**
	 * Fetch an array of BaseEntities using a SearchEntity.
	 * 
	 * @param searchEntity
	 * @return
	 */
	public Tuple2<List<BaseEntity>, Long> fetch26(SearchEntity searchEntity) {

		// find codes and total
		Tuple2<List<String>, Long> results = search26(searchEntity);
		List<String> codes = results.getItem1();
		Long count = results.getItem2();

		Set<String> allowed = searchEntity.allowedColumns();

		// apply filter
		List<BaseEntity> entities = new ArrayList<>();
		for (int i = 0; i < codes.size(); i++) {

			BaseEntity be = beUtils.getBaseEntity(codes.get(i));
			be.setIndex(i);
			be = beUtils.addNonLiteralAttributes(be);

			// handle associated columns
			Set<String> associatedCodes = allowed.stream()
					.filter(code -> code.startsWith("_"))
					.collect(Collectors.toSet());

			for (String code : associatedCodes) {
				Answer ans = getAssociatedColumnValue(be, code);

				if (ans != null)
					be.addAnswer(ans);
			}

			if (!searchEntity.getAllColumns())
				be = beUtils.privacyFilter(be, allowed);

			entities.add(be);
		}

		return Tuple2.of(entities, count);
	}

	/**
	 * @param searchEntity
	 * @return
	 */
	public Tuple2<List<String>, Long> search26(SearchEntity searchEntity) {

		if (searchEntity == null)
			throw new NullParameterException("searchEntity");

		log.infof("Performing Search: code = (%s), realm = (%s)", searchEntity.getCode(), searchEntity.getRealm());

		// setup query
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<BaseEntity> baseEntity = query.from(BaseEntity.class);

		String realm = searchEntity.getRealm();
		JoinMap map = new JoinMap(realm);

		// find filter by predicates
		List<Predicate> predicates = new ArrayList<>();
		searchEntity.getClauseContainers().stream().forEach(cont -> {
			log.info("Arg: " + jsonb.toJson(cont));
			predicates.add(findClausePredicate(baseEntity, map, cont));
		});

		// link search
		predicates.addAll(findLinkPredicates(query, baseEntity, map, searchEntity));

		// handle wildcard search
		String wildcard = searchEntity.getWildcard();
		if (wildcard != null)
			predicates.add(findWildcardPredicate(baseEntity, map, wildcard));

		// find orders
		List<Order> orders = new ArrayList<>();
		searchEntity.getSorts().stream().forEach(sort -> {
			orders.add(findSortPredicate(baseEntity, map, sort));
		});

		// ensure realms are correct
		predicates.add(cb.equal(baseEntity.get("realm"), realm));
		map.getMap().forEach((code, join) -> {
			predicates.add(cb.equal(join.get("realm"), realm));
		});

		// ensure link join realm is correct
		Root<EntityEntity> linkJoin = map.getLinkJoin();
		if (linkJoin != null) {
			predicates.add(cb.equal(linkJoin.get("realm"), realm));

			// order by weight of link if no orders are set
			if (orders.isEmpty())
				orders.add(cb.asc(linkJoin.get("weight")));
		}

		// handle status (defaults to ACTIVE)
		EEntityStatus status = searchEntity.getSearchStatus();
		log.info("Search Status: [" + status.toString() + "]");
		predicates.add(cb.le(baseEntity.get("status").as(Integer.class), status.ordinal()));

		// build query
		query.multiselect(baseEntity.get("code")).distinct(true);
		query.where(predicates.toArray(Predicate[]::new));
		query.orderBy(orders.toArray(Order[]::new));

		// page start and page size
		Integer defaultPageSize = 20;
		Integer pageSize = searchEntity.getPageSize() != null ? searchEntity.getPageSize() : defaultPageSize;
		Integer pageStart = searchEntity.getPageStart() != null ? searchEntity.getPageStart() : 0;

		// perform main query
		List<Tuple> tuples = entityManager
				.createQuery(query)
				.setFirstResult(pageStart)
				.setMaxResults(pageSize)
				.getResultList();

		List<String> codes = tuples.stream().map(t -> (String) t.get(0)).collect(Collectors.toList());

		// build count query
		CriteriaQuery<Long> count = cb.createQuery(Long.class);
		Root<BaseEntity> countBaseEntity = count.from(BaseEntity.class);

		count.select(cb.count(countBaseEntity)).distinct(true);
		count.where(predicates.toArray(Predicate[]::new));
		count.orderBy(orders.toArray(Order[]::new));

		// perform count
		Long total = entityManager.createQuery(count).getSingleResult();

		return Tuple2.of(codes, total);
	}

	/**
	 * @param query
	 * @param baseEntity
	 * @param map
	 * @param clauseContainer
	 * @return
	 */
	public Predicate findClausePredicate(Root<BaseEntity> baseEntity, JoinMap map, ClauseContainer clauseContainer) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		Filter filter = clauseContainer.getFilter();
		if (filter != null)
			return findFilterPredicate(baseEntity, map, filter);

		And and = clauseContainer.getAnd();
		Or or = clauseContainer.getOr();

		Clause clause = (and != null ? and : or);

		// find predicate for each clause argument
		Predicate predicateA = findClausePredicate(baseEntity, map, clause.getA());
		Predicate predicateB = findClausePredicate(baseEntity, map, clause.getB());

		if (and != null)
			return cb.and(predicateA, predicateB);
		else if (or != null)
			return cb.or(predicateA, predicateB);
		else
			throw new QueryBuilderException("Invalid ClauseContainer: " + clauseContainer);
	}

	/**
	 * @param baseEntity
	 * @param map
	 * @param filter
	 * @return Predicate
	 */
	@SuppressWarnings("unchecked")
	public Predicate findFilterPredicate(Root<BaseEntity> baseEntity, JoinMap map, Filter filter) {

		Class<?> c = filter.getC();
		if (isChronoClass(c))
			return findChronoPredicate(baseEntity, map, filter);

		Expression<?> expression = findExpression(baseEntity, map, filter.getCode(), map.getProductCode());

		Operator operator = filter.getOperator();
		Object value = filter.getValue();

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		switch (operator) {
			case LIKE:
				return cb.like((Expression<String>) expression, String.class.cast(value));
			case NOT_LIKE:
				return cb.notLike((Expression<String>) expression, String.class.cast(value));
			case CONTAINS:
				return cb.like((Expression<String>) expression, "\"" + String.class.cast(value) + "\"");
			case NOT_CONTAINS:
				return cb.notLike((Expression<String>) expression, "\"" + String.class.cast(value) + "\"");
			case STARTS_WITH:
				return cb.like((Expression<String>) expression, String.class.cast(value) + "%");
			case NOT_STARTS_WITH:
				return cb.notLike((Expression<String>) expression, String.class.cast(value) + "%");
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

	/**
	 * @param baseEntity
	 * @param map
	 * @param filter
	 * @return
	 */
	public Predicate findChronoPredicate(Root<BaseEntity> baseEntity, JoinMap map, Filter filter) {

		Expression<?> expression = findExpression(baseEntity, map, filter.getCode(), map.getProductCode());

		Operator operator = filter.getOperator();
		Object value = filter.getValue();
		Class<?> c = filter.getC();

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		switch (operator) {
			case EQUALS:
				return cb.equal(expression, value);
			case NOT_EQUALS:
				return cb.notEqual(expression, value);
			case GREATER_THAN:
				// TODO: Remove triple ifs (Bryn)
				if (c == LocalDateTime.class)
					return cb.greaterThan(expression.as(LocalDateTime.class), LocalDateTime.class.cast(value));
				if (c == LocalDate.class)
					return cb.greaterThan(expression.as(LocalDate.class), LocalDate.class.cast(value));
				if (c == LocalTime.class)
					return cb.greaterThan(expression.as(LocalTime.class), LocalTime.class.cast(value));
			case LESS_THAN:
				if (c == LocalDateTime.class)
					return cb.lessThan(expression.as(LocalDateTime.class), LocalDateTime.class.cast(value));
				if (c == LocalDate.class)
					return cb.lessThan(expression.as(LocalDate.class), LocalDate.class.cast(value));
				if (c == LocalTime.class)
					return cb.lessThan(expression.as(LocalTime.class), LocalTime.class.cast(value));
			case GREATER_THAN_OR_EQUAL:
				if (c == LocalDateTime.class)
					return cb.greaterThanOrEqualTo(expression.as(LocalDateTime.class), LocalDateTime.class.cast(value));
				if (c == LocalDate.class)
					return cb.greaterThanOrEqualTo(expression.as(LocalDate.class), LocalDate.class.cast(value));
				if (c == LocalTime.class)
					return cb.greaterThanOrEqualTo(expression.as(LocalTime.class), LocalTime.class.cast(value));
			case LESS_THAN_OR_EQUAL:
				if (c == LocalDateTime.class)
					return cb.lessThanOrEqualTo(expression.as(LocalDateTime.class), LocalDateTime.class.cast(value));
				if (c == LocalDate.class)
					return cb.lessThanOrEqualTo(expression.as(LocalDate.class), LocalDate.class.cast(value));
				if (c == LocalTime.class)
					return cb.lessThanOrEqualTo(expression.as(LocalTime.class), LocalTime.class.cast(value));
			default:
				throw new QueryBuilderException("Invalid Chrono Operator: " + operator + ", class: " + c);
		}
	}

	/**
	 * @param baseEntity
	 * @param map
	 * @param searchEntity
	 */
	public List<Predicate> findLinkPredicates(CriteriaQuery<Tuple> query, Root<BaseEntity> baseEntity, JoinMap map,
			SearchEntity searchEntity) {

		String sourceCode = searchEntity.getSourceCode();
		String targetCode = searchEntity.getTargetCode();
		String linkCode = searchEntity.getLinkCode();
		String linkValue = searchEntity.getLinkValue();

		List<Predicate> predicates = new ArrayList<>();

		if (sourceCode == null && targetCode == null && linkCode == null && linkValue == null)
			return predicates;

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		Root<EntityEntity> entityEntity = query.from(EntityEntity.class);

		// Only look in targetCode if both are null
		if (sourceCode == null && targetCode == null) {
			predicates.add(cb.equal(baseEntity.get("code"), entityEntity.get("link").get("targetCode")));
		} else if (sourceCode != null) {
			predicates.add(cb.and(
					cb.equal(entityEntity.get("link").get("sourceCode"), sourceCode),
					cb.equal(baseEntity.get("code"), entityEntity.get("link").get("targetCode"))));
		} else if (targetCode != null) {
			predicates.add(cb.and(
					cb.equal(entityEntity.get("link").get("targetCode"), targetCode),
					cb.equal(baseEntity.get("code"), entityEntity.get("link").get("sourceCode"))));
		}

		if (linkCode != null) {
			predicates.add(cb.equal(entityEntity.get("link").get("attributeCode"), linkCode));
		}
		if (linkValue != null)
			predicates.add(cb.equal(entityEntity.get("link").get("linkValue"), linkValue));

		map.setLinkJoin(entityEntity);

		return predicates;
	}

	/**
	 * @param baseEntity
	 * @param map
	 * @param wildcard
	 * @return
	 */
	public Predicate findWildcardPredicate(Root<BaseEntity> baseEntity, JoinMap map, String wildcard) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		Join<BaseEntity, EntityAttribute> join = baseEntity.join("baseEntityAttributes", JoinType.LEFT);
		join.on(cb.equal(baseEntity.get("id"), join.get("pk").get("baseEntity").get("id")));
		map.getMap().put("WILDCARD", join);

		return cb.like(join.get("valueString"), "%" + wildcard + "%");
	}

	/**
	 * @param baseEntity
	 * @param map
	 * @param sort
	 * @return
	 */
	public Order findSortPredicate(Root<BaseEntity> baseEntity, JoinMap map, Sort sort) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		String code = sort.getCode();
		Ord order = sort.getOrder();
		Expression<?> expression = null;

		if (code.startsWith("PRI_CREATED"))
			expression = baseEntity.<LocalDateTime>get("created");
		else if (code.startsWith("PRI_UPDATED"))
			expression = baseEntity.<LocalDateTime>get("updated");
		else if (code.equals("PRI_CODE"))
			expression = baseEntity.<String>get("code");
		else if (code.equals("PRI_NAME"))
			expression = baseEntity.<String>get("name");
		else {
			log.info("Sort code = " + code);
			Join<BaseEntity, EntityAttribute> entityAttribute = map.get(cb, baseEntity, code);
			log.info("ea = " + entityAttribute);
			expression = findExpression(baseEntity, map, code, map.getProductCode());
		}

		// select order type
		if (order == Ord.ASC)
			return cb.asc(expression);
		else if (order == Ord.DESC)
			return cb.desc(expression);
		else
			throw new QueryBuilderException("Invalid sort order " + order + " for code " + code);
	}

	/**
	 * @param baseEntity
	 * @param map
	 * @param code
	 * @param productCode
	 * @return
	 */
	public Expression<?> findExpression(Root<BaseEntity> baseEntity, JoinMap map, String code, String productCode) {

		if (code.startsWith("PRI_CREATED"))
			return baseEntity.<LocalDateTime>get("created");
		else if (code.startsWith("PRI_UPDATED"))
			return baseEntity.<LocalDateTime>get("updated");
		else if (code.equals("PRI_CODE"))
			return baseEntity.<String>get("code");
		else if (code.equals("PRI_NAME"))
			return baseEntity.<String>get("name");

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		Join<BaseEntity, EntityAttribute> entityAttribute = map.get(cb, baseEntity, code);

		Attribute attr = qwandaUtils.getAttribute(code, productCode);
		DataType dtt = attr.getDataType();
		String className = dtt.getClassName();
		Class<?> c = null;
		try {
			if (className.contains("BaseEntity"))
				c = String.class;
			else
				c = Class.forName(dtt.getClassName());
		} catch (Exception e) {
			throw new DebugException("Could not form class from path: " + e.getMessage());
		}

		if (c == String.class)
			return entityAttribute.<String>get("valueString");
		else if (c == Integer.class)
			return entityAttribute.<Integer>get("valueInteger");
		else if (c == Long.class)
			return entityAttribute.<Long>get("valueLong");
		else if (c == Double.class)
			return entityAttribute.<Double>get("valueDouble");
		else if (c == LocalDateTime.class)
			return entityAttribute.get("valueDateTime");
		else if (c == LocalDate.class)
			return entityAttribute.get("valueDate");
		else if (c == LocalTime.class)
			return entityAttribute.get("valueTime");
		else
			throw new QueryBuilderException("Invalid path for class " + c);
	}

	public Boolean isChronoClass(Class<?> c) {
		return (c == LocalDateTime.class || c == LocalDate.class || c == LocalTime.class);
	}

	/**
	 * @param searchEntity
	 * @return
	 */
	public static Set<String> getSearchColumnFilterArray(SearchEntity searchEntity) {

		Set<String> columns = searchEntity.getBaseEntityAttributes().stream()
				.filter(ea -> ea.getAttributeCode().startsWith(Column.PREFIX))
				.map(ea -> ea.getAttributeCode())
				.map(code -> (String) StringUtils.removeStart(code, Column.PREFIX))
				.collect(Collectors.toSet());

		return columns;
	}

	/**
	 * @param entity
	 * @param code
	 * @return
	 */
	public Answer getAssociatedColumnValue(BaseEntity entity, String code) {

		String cleanCode = StringUtils.removeStart(code, "_");

		// recursively find value
		Answer answer = getRecursiveColumnLink(entity, cleanCode);
		if (answer == null)
			return null;

		// update attribute code for frontend
		answer.setAttributeCode(code);
		answer.getAttribute().setCode(code);

		return answer;
	}

	/**
	 * @param entity
	 * @param code
	 * @return
	 */
	public Answer getRecursiveColumnLink(BaseEntity entity, String code) {

		if (entity == null)
			return null;

		// split code to find next attribute in line
		String[] array = code.split("__");
		String attributeCode = array[0];
		code = Stream.of(array).skip(1).collect(Collectors.joining("__"));

		// recursion
		if (array.length > 1) {
			entity = beUtils.getBaseEntityFromLinkAttribute(entity, attributeCode);
			return getRecursiveColumnLink(entity, code);
		}

		// find value
		String value;
		if (Attribute.PRI_NAME.equals(attributeCode))
			value = entity.getName();
		if (Attribute.PRI_CODE.equals(attributeCode))
			value = entity.getCode();
		else {
			Optional<EntityAttribute> ea = entity.findEntityAttribute(attributeCode);
			if (ea.isPresent())
				value = ea.get().getAsString();
			else
				return null;
		}

		// create answer
		Answer answer = new Answer(entity.getCode(), entity.getCode(), attributeCode, value);
		log.info(code);
		Attribute attribute = qwandaUtils.getAttribute(attributeCode);
		answer.setAttribute(attribute);

		return answer;
	}

}
