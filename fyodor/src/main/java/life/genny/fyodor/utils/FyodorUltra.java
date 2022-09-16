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
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.CriteriaBuilder.Case;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.fyodor.models.TolstoysCauldron;
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
import life.genny.qwandaq.models.Page;
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

		// setup search query
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<BaseEntity> baseEntity = query.from(BaseEntity.class);

		// log arguments of search
		searchEntity.getClauseContainers().stream().forEach(cont -> {
			log.info("Arg: " + jsonb.toJson(cont));
		});

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
		brewQueryInCauldron(query, countCauldron);

		count.select(cb.count(countBaseEntity)).distinct(true);
		count.where(countCauldron.getPredicates().toArray(Predicate[]::new));
		count.orderBy(countCauldron.getOrders().toArray(Order[]::new));

		// perform count
		Long total = entityManager.createQuery(count).getSingleResult();

		Page page = new Page();
		page.setCodes(codes);
		page.setTotal(total);
		page.setPageSize(pageSize);
		page.setPageStart(Long.valueOf(pageStart));

		// TODO
		// page.setPageNumber();

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
		searchEntity.getSorts().stream().forEach(sort -> {
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

		log.info("Search Status: [" + status.toString() + "]");
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
		Predicate predicateA = findClausePredicate(cauldron, clause.getA());
		Predicate predicateB = findClausePredicate(cauldron, clause.getB());

		if (and != null)
			return cb.and(predicateA, predicateB);
		else if (or != null)
			return cb.or(predicateA, predicateB);
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
	 * 
	 * @param baseEntity
	 * @param cauldron
	 * @param filter
	 * @return
	 */
	public Predicate findChronoPredicate(TolstoysCauldron cauldron, Filter filter) {

		Expression<?> expression = findExpression(cauldron, filter.getCode());

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

		// Only look in targetCode if both are null
		if (sourceCode == null && targetCode == null) {
			predicates.add(cb.equal(root.get("code"), entityEntity.get("link").get("targetCode")));
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

		cauldron.setLink(entityEntity);

		return predicates;
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

		if (code.startsWith("PRI_CREATED"))
			expression = root.<LocalDateTime>get("created");
		else if (code.startsWith("PRI_UPDATED"))
			expression = root.<LocalDateTime>get("updated");
		else if (code.equals("PRI_CODE"))
			expression = root.<String>get("code");
		else if (code.equals("PRI_NAME"))
			expression = root.<String>get("name");
		else {
			Join<BaseEntity, EntityAttribute> entityAttribute = cauldron.get(cb, code);
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

		if (code.startsWith("PRI_CREATED"))
			return root.<LocalDateTime>get("created");
		else if (code.startsWith("PRI_UPDATED"))
			return root.<LocalDateTime>get("updated");
		else if (code.equals("PRI_CODE"))
			return root.<String>get("code");
		else if (code.equals("PRI_NAME"))
			return root.<String>get("name");

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		Join<BaseEntity, EntityAttribute> entityAttribute = cauldron.get(cb, code);

		Attribute attr = qwandaUtils.getAttribute(code, cauldron.getProductCode());
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
