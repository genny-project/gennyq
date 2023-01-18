package life.genny.fyodor.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.persistence.criteria.CriteriaBuilder.Case;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.fyodor.models.TolstoysCauldron;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.EntityEntity;
import life.genny.qwandaq.entity.search.SearchEntity;
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
import life.genny.qwandaq.models.Page;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.MergeUtils;
import life.genny.qwandaq.utils.QwandaUtils;

@ApplicationScoped
public class FyodorUltra {

	@Inject
	Logger log;

	@Inject
	UserToken userToken;

	@Inject
	EntityManager entityManager;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	CapHandler capHandler;

	private static Jsonb jsonb = JsonbBuilder.create();

	/**
	 * Fetch an array of BaseEntities using a SearchEntity.
	 * 
	 * @param searchEntity
	 * @return
	 */
	public Page fetch26(SearchEntity searchEntity) {

		// find codes and total
		Page page = search26(searchEntity);
		List<String> codes = page.getCodes();

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
		page.setItems(entities);

		return page;
	}

	/**
	 * Fetch an array of BaseEntitiy codes using a SearchEntity.
	 * 
	 * @param searchEntity
	 * @return
	 */
	public Page search26(SearchEntity searchEntity) {

		if (searchEntity == null)
			throw new NullParameterException("searchEntity");

		log.infof("Performing Search: code = (%s), realm = (%s)", searchEntity.getCode(), searchEntity.getRealm());
		log.debug("Applying capabilities...");
		log.debug("SearchEntity: " + jsonb.toJson(searchEntity));
		// apply capabilities to traits
		capHandler.refineSearchFromCapabilities(searchEntity);

		if (!CapHandler.hasSecureToken(userToken)) {
			Map<String, Object> ctxMap = new HashMap<>();
			ctxMap.put("SOURCE", beUtils.getUserBaseEntity());
			ctxMap.put("USER", beUtils.getUserBaseEntity());
			List<ClauseContainer> filters = searchEntity.getClauseContainers();
			filters.stream()
				.filter(f -> f.getFilter() != null && f.getFilter().getC() == String.class)
				.peek(f -> log.info(f.getFilter().getValue()))
				.forEach(f -> {
					String value = MergeUtils.merge((String) f.getFilter().getValue(), ctxMap);
					f.getFilter().setValue(value);
			});
		}

		// setup search query
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<BaseEntity> baseEntity = query.from(BaseEntity.class);

		// log arguments of search
		searchEntity.getClauseContainers().stream().forEach(cont -> {
			log.info("Arg: " + jsonb.toJson(cont));
		});
		log.info("Search Status: [" + searchEntity.getSearchStatus().toString() + "]");

		// build the search query
		TolstoysCauldron cauldron = new TolstoysCauldron(searchEntity);
		cauldron.setRoot(baseEntity);
		brewQueryInCauldron(query, cauldron);

		// build query
		query.multiselect(baseEntity.get("code")).distinct(true);
		query.where(cauldron.getPredicates().toArray(Predicate[]::new));
		query.orderBy(cauldron.getOrders().toArray(Order[]::new));

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

		// build the search query
		TolstoysCauldron countCauldron = new TolstoysCauldron(searchEntity);
		countCauldron.setRoot(countBaseEntity);
		brewQueryInCauldron(count, countCauldron);

		count.select(cb.count(countBaseEntity)).distinct(true);
		count.where(countCauldron.getPredicates().toArray(Predicate[]::new));
		count.orderBy(countCauldron.getOrders().toArray(Order[]::new));

		// perform count
		Long total = entityManager
				.createQuery(count)
				.getSingleResult();

		log.info("Total Results: " + total);

		Page page = new Page();
		page.setCodes(codes);
		page.setTotal(total);
		page.setPageSize(pageSize);
		page.setPageStart(Long.valueOf(pageStart));

		Integer pageNumber = Math.floorDiv(pageStart, pageSize);
		page.setPageNumber(pageNumber);

		return page;
	}

	/**
	 * Use a cauldron to build a search query from a CriteriaQuery base.
	 * 
	 * @param query
	 * @param baseEntity
	 * @param searchEntity
	 * @return
	 */
	public void brewQueryInCauldron(CriteriaQuery<?> query, TolstoysCauldron cauldron) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		String realm = cauldron.getProductCode();
		SearchEntity searchEntity = cauldron.getSearchEntity();
		Root<BaseEntity> root = cauldron.getRoot();

		// find filter by predicates
		searchEntity.getClauseContainers().stream().forEach(cont -> {
			cauldron.add(findClausePredicate(cauldron, cont));
		});

		// link search
		cauldron.getPredicates().addAll(findLinkPredicates(query, cauldron, searchEntity));

		// handle wildcard search
		String wildcard = searchEntity.getWildcard();
		if (wildcard != null)
			cauldron.add(findWildcardPredicate(cauldron, wildcard));

		// find orders
		List<Order> orders = new ArrayList<>();
		searchEntity.getTraits(Sort.class).stream().forEach(sort -> {
			orders.add(findSortPredicate(cauldron, sort));
		});

		// ensure realms are correct
		cauldron.add(cb.equal(root.get("realm"), realm));
		cauldron.getJoinMap().forEach((code, join) -> {
			cauldron.add(cb.equal(join.get("realm"), realm));
		});

		// ensure link join realm is correct
		Root<EntityEntity> link = cauldron.getLink();
		if (link != null) {
			cauldron.add(cb.equal(link.get("realm"), realm));

			// order by weight of link if no orders are set
			if (cauldron.getOrders().isEmpty())
				cauldron.add(cb.asc(link.get("weight")));
		}

		// handle status (defaults to ACTIVE)
		EEntityStatus status = searchEntity.getSearchStatus();
		Case<Number> sc = selectCaseEntityStatus(root);

		cauldron.add(cb.le(sc, status.ordinal()));
	}

	/**
	 * Find predicates for a clause.
	 * 
	 * @param cauldron
	 * @param clauseContainer
	 * @return
	 */
	public Predicate findClausePredicate(TolstoysCauldron cauldron, ClauseContainer clauseContainer) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		Filter filter = clauseContainer.getFilter();
		if (filter != null)
			return findFilterPredicate(cauldron, filter);

		And and = clauseContainer.getAnd();
		Or or = clauseContainer.getOr();

		Clause clause = (and != null ? and : or);

		// find predicate for each clause argument
		List<Predicate> predicates = new ArrayList<>();
		for (ClauseContainer child : clause.getClauseContainers())
			predicates.add(findClausePredicate(cauldron, child));

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
	 * @param cauldron
	 * @param filter
	 * @return Predicate
	 */
	@SuppressWarnings("unchecked")
	public Predicate findFilterPredicate(TolstoysCauldron cauldron, Filter filter) {

		Class<?> c = filter.getC();
		if (isChronoClass(c))
			return findChronoPredicate(cauldron, filter);

		Expression<?> expression = findExpression(cauldron, filter.getCode());

		Operator operator = filter.getOperator();
		Object value = filter.getValue();

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		switch (operator) {
			// use locate to find the expression within the value
			// if locate returns 0, expression is not found
			case IN:
				// we want to check in, so check locate does not equal 0
				return cb.notEqual(cb.locate(cb.literal(String.class.cast(value)), (Expression<String>)expression), 0);
			case NOT_IN:
				// we want to check not in, so check locate equals 0
				return cb.equal(cb.locate(cb.literal(String.class.cast(value)), (Expression<String>)expression), 0);
			case LIKE:
				return cb.like((Expression<String>) expression, String.class.cast(value));
			case NOT_LIKE:
				return cb.notLike((Expression<String>) expression, String.class.cast(value));
			case CONTAINS:
				return cb.like((Expression<String>) expression, "%\"" + String.class.cast(value) + "\"%");
			case NOT_CONTAINS:
				return cb.notLike((Expression<String>) expression, "%\"" + String.class.cast(value) + "\"%");
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
	 * Find a predicate of a DateTime type filter.
	 * <br>
	 * This method requires that the the incoming stringified 
	 * chrono unit is in the most standard format, effectively 
	 * toString.
	 * 
	 * @param baseEntity
	 * @param cauldron
	 * @param filter
	 * @return
	 */
	public Predicate findChronoPredicate(TolstoysCauldron cauldron, Filter filter) {

		Expression<?> expression = findExpression(cauldron, filter.getCode());

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
	 * Find predicates for a link related fields.
	 * 
	 * @param root
	 * @param cauldron
	 * @param searchEntity
	 */
	public List<Predicate> findLinkPredicates(CriteriaQuery<?> query, TolstoysCauldron cauldron,
			SearchEntity searchEntity) {

		String sourceCode = searchEntity.getSourceCode();
		String targetCode = searchEntity.getTargetCode();
		String linkCode = searchEntity.getLinkCode();
		String linkValue = searchEntity.getLinkValue();

		List<Predicate> predicates = new ArrayList<>();

		if (sourceCode == null && targetCode == null && linkCode == null && linkValue == null)
			return predicates;

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		Root<BaseEntity> root = cauldron.getRoot();
		Root<EntityEntity> entityEntity = query.from(EntityEntity.class);
		cauldron.setLink(entityEntity);

		// Only look in targetCode if both are null
		if (sourceCode == null && targetCode == null) {
			predicates.add(cb.equal(entityEntity.get("link").get("targetCode"), root.get("code")));
		} else if (sourceCode != null) {
			predicates.add(cb.and(
					cb.equal(entityEntity.get("link").get("sourceCode"), sourceCode),
					cb.equal(root.get("code"), entityEntity.get("link").get("targetCode"))));
		} else if (targetCode != null) {
			predicates.add(cb.and(
					cb.equal(entityEntity.get("link").get("targetCode"), targetCode),
					cb.equal(root.get("code"), entityEntity.get("link").get("sourceCode"))));
		}

		if (linkCode != null) {
			predicates.add(cb.equal(entityEntity.get("link").get("attributeCode"), linkCode));
		}
		if (linkValue != null)
			predicates.add(cb.equal(entityEntity.get("link").get("linkValue"), linkValue));

		return predicates;
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
	 * @param cauldron
	 * @param wildcard
	 * @return
	 */
	public Predicate findWildcardPredicate(TolstoysCauldron cauldron, String wildcard) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		Root<BaseEntity> root = cauldron.getRoot();

		Join<BaseEntity, EntityAttribute> join = root.join("baseEntityAttributes", JoinType.LEFT);
		join.on(cb.equal(root.get("id"), join.get("pk").get("baseEntity").get("id")));
		cauldron.getJoinMap().put("WILDCARD", join);

		return cb.like(join.get("valueString"), "%" + wildcard + "%");
	}

	/**
	 * Find a search order for a sort.
	 * 
	 * @param baseEntity
	 * @param cauldron
	 * @param sort
	 * @return
	 */
	public Order findSortPredicate(TolstoysCauldron cauldron, Sort sort) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		String code = sort.getCode();
		Ord order = sort.getOrder();
		Expression<?> expression = null;

		Root<BaseEntity> root = cauldron.getRoot();

		if (code.startsWith(Attribute.PRI_CREATED))
			expression = root.<LocalDateTime>get("created");
		else if (code.startsWith(Attribute.PRI_UPDATED))
			expression = root.<LocalDateTime>get("updated");
		else if (code.equals(Attribute.PRI_CODE))
			expression = root.<String>get("code");
		else if (code.equals(Attribute.PRI_NAME))
			expression = root.<String>get("name");
		else {
			expression = findExpression(cauldron, code);
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
	 * @param cauldron
	 * @param code
	 * @return
	 */
	public Expression<?> findExpression(TolstoysCauldron cauldron, String code) {

		Root<BaseEntity> root = cauldron.getRoot();

		if (code.startsWith(Attribute.PRI_CREATED))
			return root.<LocalDateTime>get("created");
		else if (code.startsWith(Attribute.PRI_UPDATED))
			return root.<LocalDateTime>get("updated");
		else if (code.equals(Attribute.PRI_CODE))
			return root.<String>get("code");
		else if (code.equals(Attribute.PRI_NAME))
			return root.<String>get("name");

		Join<BaseEntity, EntityAttribute> entityAttribute = createOrFindJoin(cauldron, code);

		Attribute attr = qwandaUtils.getAttribute(cauldron.getProductCode(), code);
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
	public Case<Number> selectCaseEntityStatus(Root<BaseEntity> root) {

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
	public Join<BaseEntity, EntityAttribute> createOrFindJoin(TolstoysCauldron cauldron, String code) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		// add to map if not already there
		if (!cauldron.getJoinMap().containsKey(code)) {
			Join<BaseEntity, EntityAttribute> join = cauldron.getRoot().join("baseEntityAttributes", JoinType.LEFT);
			join.on(cb.equal(join.get("pk").get("attribute").get("code"), code));
			cauldron.getJoinMap().put(code, join);
		}

		return cauldron.getJoinMap().get(code);
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
	 * Get an entity value of an associated column code.
	 * 
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
	 * Recursively search an entity using an associated column code and return the
	 * value.
	 * 
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
			Optional<EntityAttribute> ea = entity.findEntityAttribute(attributeCode);
			if (ea.isEmpty()) {
				return null;
			}
			value = ea.get().getAsString();
		}

		// create answer
		Answer answer = new Answer(entity.getCode(), entity.getCode(), attributeCode, value);
		Attribute attribute = qwandaUtils.getAttribute(attributeCode);
		answer.setAttribute(attribute);

		return answer;
	}

}
