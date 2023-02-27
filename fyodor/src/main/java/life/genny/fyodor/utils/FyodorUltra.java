package life.genny.fyodor.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.persistence.criteria.CriteriaBuilder.Case;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import life.genny.qwandaq.entity.*;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.fyodor.models.JoinContext;
import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.HEntityAttribute;
import life.genny.qwandaq.datatype.DataType;
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
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.exception.runtime.QueryBuilderException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.models.Page;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.MergeUtils;

@ApplicationScoped
public class FyodorUltra {

	@Inject
	Logger log;

	@Inject
	UserToken userToken;

	@Inject
	EntityManager entityManager;

	@Inject
	private CacheManager cm;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	EntityAttributeUtils beaUtils;

	@Inject
	AttributeUtils attributeUtils;

	@Inject
	MergeUtils mergeUtils;

	@Inject
	CapHandler capHandler;

	private static Jsonb jsonb = JsonbBuilder.create();

	/**
	 * Fetch an array of BaseEntities using a SearchEntity.
	 *
	 * @param searchEntity
	 * @return
	 */
	public Page fetchWithAttributes(SearchEntity searchEntity) {

		// find codes and total
		Page page = fetchBaseEntities(searchEntity);
		Set<String> allowed = searchEntity.allowedColumns();
		// apply filter
		int index = 0;
		for (BaseEntity baseEntity : page.getItems()) {
			baseEntity.setIndex(index);
			beUtils.addNonLiteralAttributes(baseEntity);
			for (String attributeCode : allowed) {
				EntityAttribute ea;
				if (attributeCode.startsWith("_")) {
					// handle asociated columns
					ea = getAssociatedColumnValue(baseEntity, attributeCode);
					// set attr codes to associated code
					ea.setAttributeCode(attributeCode);
					ea.getAttribute().setCode(attributeCode);
				} else {
					// otherwise fetch entity attribute
					ea = beaUtils.getEntityAttribute(baseEntity.getRealm(), baseEntity.getCode(), attributeCode, true, true);
				}
				if (ea != null) {
					baseEntity.addAttribute(ea);
				}
			}
		}

		return page;
	}

	/**
	 * Fetch an array of BaseEntitiy codes using a SearchEntity.
	 *
	 * @param searchEntity
	 * @return
	 */
	public Page fetchBaseEntities(SearchEntity searchEntity) {

		if (searchEntity == null)
			throw new NullParameterException("searchEntity");

		log.infof("Performing Search: code = (%s), realm = (%s)", searchEntity.getCode(), searchEntity.getRealm());
		log.debug("SearchEntity: " + jsonb.toJson(searchEntity));
		// apply capabilities to traits
		capHandler.refineSearchFromCapabilities(searchEntity);
		if (!CapHandler.hasSecureToken(userToken)) {
			Map<String, Object> ctxMap = new HashMap<>();
			ctxMap.put("SOURCE", beUtils.getUserBaseEntity());
			ctxMap.put("USER", beUtils.getUserBaseEntity());

			searchEntity.getTraits(Filter.class).stream()
					.filter(f -> f.getC() == String.class).forEach(f -> {
						f.setValue(mergeUtils.wordMerge((String) f.getValue(), ctxMap));
			});
		}

		// setup search query
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<HBaseEntity> baseEntity = query.from(HBaseEntity.class);

		// log arguments of search
		searchEntity.getClauseContainers().stream().forEach(cont -> {
			log.debug("Arg: " + jsonb.toJson(cont));
		});
		log.debug("Search Status: [" + searchEntity.getSearchStatus().toString() + "]");

		// build the search query
		JoinContext jctx = new JoinContext(searchEntity);
		jctx.setRoot(baseEntity);
		aggregateQueryElements(query, jctx);

		// build query
		query.multiselect(baseEntity.get("code")).distinct(true);
		query.where(jctx.getPredicates().toArray(Predicate[]::new));
		if (!jctx.getOrders().isEmpty()) {
			query.orderBy(jctx.getOrders().toArray(Order[]::new));
		} else {
			// else, order by weight of entity attributes
			for (Join<HBaseEntity, HEntityAttribute> join : jctx.getJoinMap().values()) {
				query.orderBy(cb.asc(join.get("weight")));
			}
		}

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
		
		List<BaseEntity> items = new ArrayList<>();
		for (String code : codes) {
			try {
				BaseEntity be = beUtils.getBaseEntity(code);
				items.add(be);
			} catch (ItemNotFoundException e) {
				e.printStackTrace();
			}
		}

		// build count query
		CriteriaQuery<Long> count = cb.createQuery(Long.class);
		Root<HBaseEntity> countBaseEntity = count.from(HBaseEntity.class);

		// build the search query
		JoinContext countCtx = new JoinContext(searchEntity);
		countCtx.setRoot(countBaseEntity);
		aggregateQueryElements(count, countCtx);

		count.select(cb.count(countBaseEntity)).distinct(true);
		count.where(countCtx.getPredicates().toArray(Predicate[]::new));
		count.orderBy(countCtx.getOrders().toArray(Order[]::new));

		// perform count
		Long total = entityManager
				.createQuery(count)
				.getSingleResult();

		log.info("Total Results: " + total);

		Page page = new Page();
		page.setTotal(total);
		page.setItems(items);
		page.setPageSize(pageSize);
		page.setPageStart(Long.valueOf(pageStart));

		Integer pageNumber = Math.floorDiv(pageStart, pageSize);
		page.setPageNumber(pageNumber);

		return page;
	}

	/**
	 * Use a join context to build a search query from a CriteriaQuery base.
	 *
	 * @param query
	 * @param baseEntity
	 * @param searchEntity
	 * @return
	 */
	public void aggregateQueryElements(CriteriaQuery<?> query, JoinContext jctx) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		String realm = jctx.getProductCode();
		SearchEntity searchEntity = jctx.getSearchEntity();
		Root<HBaseEntity> root = jctx.getRoot();

		// find filter by predicates
		searchEntity.getClauseContainers().stream().forEach(cont -> {
			jctx.add(findClausePredicate(jctx, cont));
		});

		// handle wildcard search
		String wildcard = searchEntity.getWildcard();
		if (wildcard != null)
			jctx.add(findWildcardPredicate(jctx, wildcard));

		// find orders
		List<Order> orders = new ArrayList<>();
		searchEntity.getTraits(Sort.class).stream().forEach(sort -> {
			jctx.add(findSortPredicate(jctx, sort));
		});

		// ensure realms are correct
		jctx.add(cb.equal(root.get("realm"), realm));
		jctx.getJoinMap().forEach((code, join) -> {
			jctx.add(cb.equal(join.get("realm"), realm));
		});

		// handle status (defaults to ACTIVE)
		EEntityStatus status = searchEntity.getSearchStatus();
		Case<Number> sc = selectCaseEntityStatus(root);

		jctx.add(cb.le(sc, status.ordinal()));
	}

	/**
	 * Find predicates for a clause.
	 *
	 * @param join context
	 * @param clauseContainer
	 * @return
	 */
	public Predicate findClausePredicate(JoinContext jctx, ClauseContainer clauseContainer) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		Filter filter = clauseContainer.getFilter();
		if (filter != null)
			return findFilterPredicate(jctx, filter);

		And and = clauseContainer.getAnd();
		Or or = clauseContainer.getOr();

		Clause clause = (and != null ? and : or);

		// find predicate for each clause argument
		List<Predicate> predicates = new ArrayList<>();
		for (ClauseContainer child : clause.getClauseContainers())
			predicates.add(findClausePredicate(jctx, child));

		if (and != null)
			return cb.and(predicates.toArray(Predicate[]::new));
		else if (or != null)
			return cb.or(predicates.toArray(Predicate[]::new));
		else
			throw new QueryBuilderException("Invalid ClauseContainer: " + clauseContainer);
	}

	/**
	 * Find a predicate of a filter.
	 *
	 * @param baseEntity
	 * @param jctx
	 * @param filter
	 * @return Predicate
	 */
	@SuppressWarnings("unchecked")
	public Predicate findFilterPredicate(JoinContext jctx, Filter filter) {

		Class<?> c = filter.getC();
		if (isChronoClass(c))
			return findChronoPredicate(jctx, filter);

		Expression<?> expression = findExpression(jctx, filter.getCode());

		Operator operator = filter.getOperator();
		Object value = filter.getValue();

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		return switch (operator) {
			// use locate to find the expression within the value
			// if locate returns 0, expression is not found
			case IN -> cb.notEqual(cb.locate(cb.literal(String.class.cast(value)), (Expression<String>)expression), 0);
			case NOT_IN -> cb.equal(cb.locate(cb.literal(String.class.cast(value)), (Expression<String>)expression), 0);
			case LIKE -> cb.like((Expression<String>) expression, (String) value);
			case NOT_LIKE -> cb.notLike((Expression<String>) expression, (String) value);
			case CONTAINS -> cb.like((Expression<String>) expression, "%\"" + (String) value + "\"%");
			case NOT_CONTAINS -> cb.notLike((Expression<String>) expression, "%\"" + (String) value + "\"%");
			case STARTS_WITH -> cb.like((Expression<String>) expression, (String) value + "%");
			case NOT_STARTS_WITH -> cb.notLike((Expression<String>) expression, (String) value + "%");
			case EQUALS -> cb.equal(expression, value);
			case NOT_EQUALS -> cb.notEqual(expression, value);
			case GREATER_THAN -> cb.gt((Expression<Number>) expression, (Number) value);
			case LESS_THAN -> cb.lt((Expression<Number>) expression, (Number) value);
			case GREATER_THAN_OR_EQUAL -> cb.ge((Expression<Number>) expression, (Number) value);
			case LESS_THAN_OR_EQUAL -> cb.le((Expression<Number>) expression, (Number) value);
			default -> throw new QueryBuilderException("Invalid Operator: " + operator);
		};
	}

	/**
	 * Find a predicate of a DateTime type filter.
	 * <br>
	 * This method requires that the the incoming stringified
	 * chrono unit is in the most standard format, effectively
	 * toString.
	 *
	 * @param baseEntity
	 * @param jctx
	 * @param filter
	 * @return
	 */
	public Predicate findChronoPredicate(JoinContext jctx, Filter filter) {

		Expression<?> expression = findExpression(jctx, filter.getCode());

		Operator operator = filter.getOperator();
		String value = (String) filter.getValue();
		Class<?> c = filter.getC();

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		switch (operator) {
			case EQUALS:
				if (c == LocalDateTime.class)
					return cb.equal(expression.as(LocalDateTime.class), LocalDateTime.parse(value));
				if (c == LocalDate.class)
					return cb.equal(expression.as(LocalDate.class), LocalDate.parse(value));
				if (c == LocalTime.class)
					return cb.equal(expression.as(LocalTime.class), LocalTime.parse(value));
				return cb.equal(expression, value);
			case NOT_EQUALS:
				if (c == LocalDateTime.class)
					return cb.notEqual(expression.as(LocalDateTime.class), LocalDateTime.parse(value));
				if (c == LocalDate.class)
					return cb.notEqual(expression.as(LocalDate.class), LocalDate.parse(value));
				if (c == LocalTime.class)
					return cb.notEqual(expression.as(LocalTime.class), LocalTime.parse(value));
				return cb.notEqual(expression, value);
			case GREATER_THAN:
				// TODO: Remove triple ifs (Bryn)
				if (c == LocalDateTime.class)
					return cb.greaterThan(expression.as(LocalDateTime.class), LocalDateTime.parse(value));
				if (c == LocalDate.class)
					return cb.greaterThan(expression.as(LocalDate.class), LocalDate.parse(value));
				if (c == LocalTime.class)
					return cb.greaterThan(expression.as(LocalTime.class), LocalTime.parse(value));
			case LESS_THAN:
				if (c == LocalDateTime.class)
					return cb.lessThan(expression.as(LocalDateTime.class), LocalDateTime.parse(value));
				if (c == LocalDate.class)
					return cb.lessThan(expression.as(LocalDate.class), LocalDate.parse(value));
				if (c == LocalTime.class)
					return cb.lessThan(expression.as(LocalTime.class), LocalTime.parse(value));
			case GREATER_THAN_OR_EQUAL:
				if (c == LocalDateTime.class)
					return cb.greaterThanOrEqualTo(expression.as(LocalDateTime.class), LocalDateTime.parse(value));
				if (c == LocalDate.class)
					return cb.greaterThanOrEqualTo(expression.as(LocalDate.class), LocalDate.parse(value));
				if (c == LocalTime.class)
					return cb.greaterThanOrEqualTo(expression.as(LocalTime.class), LocalTime.parse(value));
			case LESS_THAN_OR_EQUAL:
				if (c == LocalDateTime.class)
					return cb.lessThanOrEqualTo(expression.as(LocalDateTime.class), LocalDateTime.parse(value));
				if (c == LocalDate.class)
					return cb.lessThanOrEqualTo(expression.as(LocalDate.class), LocalDate.parse(value));
				if (c == LocalTime.class)
					return cb.lessThanOrEqualTo(expression.as(LocalTime.class), LocalTime.parse(value));
			default:
				throw new QueryBuilderException("Invalid Chrono Operator: " + operator + ", class: " + c);
		}
	}

	/**
	 * Return a clean entity code to use in query for valueString containing a
	 * single entity code array.
	 *
	 * @param root
	 * @return
	 */
	public Expression<String> cleanCodeExpression(Path<?> path) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		return cb.function("replace",
				String.class,
				(cb.function("replace",
						String.class,
						path, cb.literal("[\""), cb.literal(""))),
				cb.literal("\"]"), cb.literal(""));
	}

	/**
	 * Find a predicate for a wildcard filter.
	 *
	 * @param root
	 * @param jctx
	 * @param wildcard
	 * @return
	 */
	public Predicate findWildcardPredicate(JoinContext jctx, String wildcard) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		Root<HBaseEntity> root = jctx.getRoot();

		Join<HBaseEntity, HEntityAttribute> join = root.join("baseEntityAttributes", JoinType.LEFT);
		join.on(cb.equal(root.get("id"), join.get("pk").get("baseEntity").get("id")));
		jctx.getJoinMap().put("WILDCARD", join);

		return cb.like(join.get("valueString"), "%" + wildcard + "%");
	}

	/**
	 * Find a search order for a sort.
	 *
	 * @param baseEntity
	 * @param jctx
	 * @param sort
	 * @return
	 */
	public Order findSortPredicate(JoinContext jctx, Sort sort) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		String code = sort.getCode();
		Ord order = sort.getOrder();
		Expression<?> expression = null;

		Root<HBaseEntity> root = jctx.getRoot();

		if (code.startsWith(Attribute.PRI_CREATED))
			expression = root.<LocalDateTime>get("created");
		else if (code.startsWith(Attribute.PRI_UPDATED))
			expression = root.<LocalDateTime>get("updated");
		else if (code.equals(Attribute.PRI_CODE))
			expression = root.<String>get("code");
		else if (code.equals(Attribute.PRI_NAME))
			expression = root.<String>get("name");
		else {
			expression = findExpression(jctx, code);
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
	 * Find the expression for an attribute code.
	 *
	 * @param jctx
	 * @param code
	 * @return
	 */
	public Expression<?> findExpression(JoinContext jctx, String code) {

		Root<HBaseEntity> root = jctx.getRoot();

		if (code.startsWith(Attribute.PRI_CREATED))
			return root.<LocalDateTime>get("created");
		else if (code.startsWith(Attribute.PRI_UPDATED))
			return root.<LocalDateTime>get("updated");
		else if (code.equals(Attribute.PRI_CODE))
			return root.<String>get("code");
		else if (code.equals(Attribute.PRI_NAME))
			return root.<String>get("name");

		Join<HBaseEntity, HEntityAttribute> entityAttribute = createOrFindJoin(jctx, code);

		Attribute attr = attributeUtils.getAttribute(jctx.getProductCode(), code, true);
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
		else if (c == Boolean.class)
			return entityAttribute.<Boolean>get("valueBoolean");
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
			throw new QueryBuilderException("Invalid path for " + c);
	}

	/**
	 * Create a select case to use in status check. When used, this instructs the
	 * search to select a status' ordinal so that number comparison can be done.
	 *
	 * @param root
	 * @return
	 */
	public Case<Number> selectCaseEntityStatus(Root<HBaseEntity> root) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		Path<EEntityStatus> eStatus = root.get("status");
		Case<Number> sc = cb.selectCase();

		// add a case for each status
		Stream.of(EEntityStatus.values()).forEach(s -> {
			sc.when(cb.equal(eStatus, s), s.ordinal());
		});

		return sc;
	}

	/**
	 * Check if a class is of DateTime type
	 *
	 * @param c
	 * @return
	 */
	public Boolean isChronoClass(Class<?> c) {
		return (c == LocalDateTime.class || c == LocalDate.class || c == LocalTime.class);
	}

	/**
	 * Get an existing join for an attribute code, or create if not existing
	 * already.
	 *
	 * @param cb
	 * @param code
	 * @return
	 */
	public Join<HBaseEntity, HEntityAttribute> createOrFindJoin(JoinContext jctx, String code) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		// add to map if not already there
		if (!jctx.getJoinMap().containsKey(code)) {
			Join<HBaseEntity, HEntityAttribute> join = jctx.getRoot().join("baseEntityAttributes", JoinType.LEFT);
			join.on(cb.equal(join.get("pk").get("attribute").get("code"), code));
			jctx.getJoinMap().put(code, join);
		}

		return jctx.getJoinMap().get(code);
	}

	/**
	 * @param searchEntity
	 * @return
	 */
	public static Set<String> getSearchColumnFilterArray(SearchEntity searchEntity) {

		Set<String> columns = searchEntity.getBaseEntityAttributes().stream()
				.filter(ea -> ea.getAttributeCode().startsWith(Column.PREFIX))
				.map(ea -> ea.getAttributeCode())
				.map(code -> StringUtils.removeStart(code, Column.PREFIX))
				.collect(Collectors.toSet());

		return columns;
	}

	/**
	 * Get an entity value of an associated column code.
	 *
	 * @param entity
	 * @param code
	 * @return
	 */
	public EntityAttribute getAssociatedColumnValue(BaseEntity entity, String code) {

		String cleanCode = StringUtils.removeStart(code, "_");

		// recursively find value
		EntityAttribute ea = getRecursiveColumnLink(entity, cleanCode);
		if (ea == null) {
			return null;
		}

		// update attribute code for frontend
		ea.setAttributeCode(code);

		return ea;
	}

	/**
	 * Recursively search an entity using an associated column code and return the
	 * value.
	 *
	 * @param entity
	 * @param code
	 * @return
	 */
	public EntityAttribute getRecursiveColumnLink(BaseEntity entity, String code) {

		if (entity == null)
			return null;

		// split code to find next attribute in line
		String[] array = code.split("__");
		String attributeCode = array[0];
		code = Stream.of(array).skip(1).collect(Collectors.joining("__"));

		// recursion
		if (array.length > 1) {
			entity = beUtils.getBaseEntityFromLinkAttribute(entity, attributeCode);
			if (entity == null) {
				return null;
			}
			return getRecursiveColumnLink(entity, code);
		}

		// find value
		String value;
		if (Attribute.PRI_NAME.equals(attributeCode)) {
			value = entity.getName();
		} else if (Attribute.PRI_CODE.equals(attributeCode)) {
			value = entity.getCode();
		} else {
			EntityAttribute entityAttribute = beaUtils.getEntityAttribute(entity.getRealm(), entity.getCode(), attributeCode, true, true);
			if (entityAttribute == null) {
				return null;
			}
			value = entityAttribute.getAsString();
		}

		// create ea
		Attribute attribute = attributeUtils.getAttribute(entity.getRealm(), attributeCode, true);
		EntityAttribute ea = new EntityAttribute(entity, attribute, 1.0, value);

		return ea;
	}

}
