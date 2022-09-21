package life.genny.qwandaq.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.search.clause.And;
import life.genny.qwandaq.entity.search.clause.ClauseContainer;
import life.genny.qwandaq.entity.search.clause.Or;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.AssociatedColumn;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Sort;

/* 
 * SearchEntity class implements the search of base entities applying 
 * different filters/search to the baseEntity and its attributes.
 */
@RegisterForReflection
public class SearchEntity extends BaseEntity {

	private static final Logger log = Logger.getLogger(SearchEntity.class);

	private static final long serialVersionUID = 1L;

	private List<ClauseContainer> clauseContainers = new ArrayList<>();
	private List<Sort> sorts = new ArrayList<>();
	private List<Column> columns = new ArrayList<>();
	private List<Action> actions = new ArrayList<>();
	private Boolean allColumns = false;

	// TODO: redesign filters
	Double flcIndex = 1.0;

	/**
   * Default constructor.
   */
	public SearchEntity() {
	}

	/**
   * Constructor.
   * 
   * @param code SearchEntity code
   * @param name SearchEntity name
   */
	public SearchEntity(final String code, final String name) {
		super(code, name);
		setPageStart(0);
		setPageSize(20);
		setTitle(name);
	}

	/*
   * (non-Javadoc)
   * 
   * @see life.genny.qwandaq.CoreEntity#setRealm(java.lang.String)
   */
	public SearchEntity setRealm(final String realm) {
		super.setRealm(realm);
		return this;
	}

	public static Logger getLog() {
		return log;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public List<ClauseContainer> getClauseContainers() {
		return clauseContainers;
	}

	public void setClauseContainers(List<ClauseContainer> clauseContainers) {
		this.clauseContainers = clauseContainers;
	}

	public List<Sort> getSorts() {
		return sorts;
	}

	public void setSorts(List<Sort> sorts) {
		this.sorts = sorts;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public void setColumns(List<Column> columns) {
		this.columns = columns;
	}

	public List<Action> getActions() {
		return actions;
	}

	public void setActions(List<Action> actions) {
		this.actions = actions;
	}

	public Boolean getAllColumns() {
		return allColumns;
	}

	public SearchEntity setAllColumns(Boolean allColumns) {
		this.allColumns = allColumns;
		return this;
	}

	/*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
	@Override
	public String toString() {
		return "SearchEntity[ code = " + this.getCode() + "]";
	}

	/**
   * Add an column to the search results
   * 
   * @param column Column object
   * @return SearchEntity
   */
	public SearchEntity add(Column column) {
		this.columns.add(column);
		return this;
	}

	/**
   * Add an associated entity column to the search results
   * 
   * @param associatedColumn AssociatedColumn object
   * @return SearchEntity
   */
	public SearchEntity add(AssociatedColumn associatedColumn) {
		this.columns.add(associatedColumn);
		return this;
	}

	/**
   * Add an attribute sort order to a search
   * 
   * @param sort Sort object
   * @return SearchEntity
   */
	public SearchEntity add(Sort sort) {
		sorts.add(sort);
		return this;
	}

	/**
   * Add an Action to each search result
   * 
   * @param action
   * @return SearchEntity
   */
	public SearchEntity add(Action action) {
		this.actions.add(action);
		return this;
	}

	/**
   * Add a search filter
   * 
   * @param filter Filter object
   * @return SearchEntity
   */
	public SearchEntity add(Filter filter) {
		this.clauseContainers.add(new ClauseContainer(filter));
		return this;
	}

	/**
   * Add an And clause
   * 
   * @param and And clause
   * @return SearchEntity
   */
	public SearchEntity add(And and) {
		this.clauseContainers.add(new ClauseContainer(and));
		return this;
	}

	/**
   * Add an Or clause
   * 
   * @param or Or clause
   * @return SearchEntity
   */
	public SearchEntity add(Or or) {
		this.clauseContainers.add(new ClauseContainer(or));
		return this;
	}

	/**
   * @param code the code of the attribute to add a sort attribute for
   * @param name the name of the sort attribute
   * @return SearchEntity
   */
	public SearchEntity addSortAttribute(final String code, final String name) {

		Attribute attributeSort = new Attribute("ATTRSRT_" + code, name, new DataType(String.class));
		addAttribute(attributeSort, 1.0);

		return this;
	}

	/**
   * Add a conditional attribute.
   * 
   * @param code      the attribute to apply the condition to
   * @param condition the condition to apply
   * @return SearchEntity
   */
	public SearchEntity addConditional(String code, String condition) {

		String cnd = String.format("CND_%s", code);
		Attribute attribute = new Attribute(cnd, cnd, new DataType(String.class));
		addAttribute(attribute, 1.0, condition);

		return this;
	}

	/**
   * Add a whitelist attribute
   * 
   * @param code the attribute code to add to the whitelist
   * @return SearchEntity
   */
	public SearchEntity addWhitelist(String code) {

		Attribute attribute = new Attribute("WTL_" + code, code, new DataType(String.class));
		addAttribute(attribute, 1.0, code);

		return this;
	}

	/**
   * Add a blacklist attribute
   * 
   * @param code the attribute code to add to the blacklist
   * @return SearchEntity
   */
	public SearchEntity addBlacklist(String code) {

		Attribute attribute = new Attribute("BKL_" + code, code, new DataType(String.class));
		addAttribute(attribute, 1.0, code);

		return this;
	}

	/**
   * This method allows to set the title of the results data to be sent
   * 
   * @param title The page title
   * @return SearchEntity
   */
	public SearchEntity setTitle(final String title) {

		Attribute attribute = new Attribute("SCH_TITLE", "Title", new DataType(String.class));
		addAttribute(attribute, 5.0, title);

		return this;
	}

	/**
   * This method allows to set the parentCode of the SearchEntity
   * 
   * @param parentCode the parent entity code
   * @return SearchEntity
   */
	public SearchEntity setParentCode(final String parentCode) {

		Attribute attribute = new Attribute("SCH_PARENT_CODE", "Parent Code", new DataType(String.class));
		addAttribute(attribute, 1.0, parentCode);

		return this;
	}

	/**
   * This method allows to set the start/begining number of the range(page) of the
   * results data to be sent
   * 
   * @param pageStart the start of the page number
   * @return SearchEntity
   */
	public SearchEntity setPageStart(final Integer pageStart) {

		Attribute attribute = new Attribute("SCH_PAGE_START", "PageStart", new DataType(Integer.class));
		addAttribute(attribute, 3.0, pageStart);

		return this;
	}

	/**
   * This method allows to set size of the selection allowed for a searchEntity
   * 
   * @param selectSize size of selection
   * @return SearchEntity
   */
	public SearchEntity setSelectSize(final Integer selectSize) {

		Attribute attribute = new Attribute("SCH_SELECT_SIZE", "SelectSize", new DataType(Integer.class));
		addAttribute(attribute, 1.0, selectSize);

		return this;
	}

	/**
   * This method allows to set the total number of the results (BaseEntites) to be
   * sent
   * 
   * @param pageSize number of items to be sent in each page
   * @return SearchEntity
   */
	public SearchEntity setPageSize(final Integer pageSize) {

		Attribute attribute = new Attribute("SCH_PAGE_SIZE", "PageSize", new DataType(Integer.class));
		addAttribute(attribute, 1.0, pageSize);

		return this;
	}

	/**
   * This method allows to set the stakeholder/user code to the search. It will
   * search for the BaseEntites that the given user is stakeholder of.
   * 
   * @param stakeholder the userCode of the stakeHolder
   * @return SearchEntity
   */
	public SearchEntity setStakeholder(final String stakeholder) {

		Attribute attribute = new Attribute("SCH_STAKEHOLDER_CODE", "Stakeholder", new DataType(String.class));
		addAttribute(attribute, 1.0, stakeholder);

		return this;
	}

	/**
   * This method allows to set the stakeholder/user code to the parent/source
   * Basentity involved in the search. It will search for the BaseEntites under
   * the give source BE that the given user is stakeholder of.
   * 
   * @param sourceStakeholder the userCode of the source stakeHolder
   * @return SearchEntity
   */
	public SearchEntity setSourceStakeholder(final String sourceStakeholder) {

		Attribute attribute = new Attribute("SCH_SOURCE_STAKEHOLDER_CODE", "SourceStakeholder", new DataType(String.class));
		addAttribute(attribute, 1.0, sourceStakeholder);

		return this;
	}

	/**
   * This method allows to set the stakeholder/user code to the parent/source
   * Basentity involved in the search. It will search for the BaseEntites under
   * the give source BE that the given user is stakeholder of.
   * 
   * @param linkCode the linkCode
   * @return SearchEntity
   */
	public SearchEntity setLinkCode(final String linkCode) {

		Attribute attribute = new Attribute("SCH_LINK_CODE", "LinkCode", new DataType(String.class));
		addAttribute(attribute, 1.0, linkCode);

		return this;
	}

	/**
   * This method allows to set the link value the result of the search.
   * 
   * @param linkValue - linkValue of the sourceCode to the results (BaseEntities)
   *                  of the search
   * @return SearchEntity
   */
	public SearchEntity setLinkValue(final String linkValue) {

		Attribute attribute = new Attribute("SCH_LINK_VALUE", "LinkValue", new DataType(String.class));
		addAttribute(attribute, 1.0, linkValue);

		return this;
	}

	/**
   * @param sourceCode the sourceCode to set
   * @return SearchEntity
   */
	public SearchEntity setSourceCode(final String sourceCode) {

		Attribute attribute = new Attribute("SCH_SOURCE_CODE", "SourceCode", new DataType(String.class));
		addAttribute(attribute, 1.0, sourceCode);

		return this;
	}

	/**
   * @param targetCode the targetCode to set
   * @return SearchEntity
   */
	public SearchEntity setTargetCode(final String targetCode) {

		Attribute attribute = new Attribute("SCH_TARGET_CODE", "TargetCode", new DataType(String.class));
		addAttribute(attribute, 1.0, targetCode);

		return this;
	}

	/**
   * This method allows to set the wildcard of the results data to be sent
   * 
   * @param wildcard the widlcard
   * @return SearchEntity
   */
	public SearchEntity setWildcard(String wildcard) {

		Attribute attribute = new Attribute("SCH_WILDCARD", "Wildcard", new DataType(String.class));
		addAttribute(attribute, 1.0, wildcard);

		return this;
	}

	/**
   * This method allows to set the wildcard depth level for associated wildcards
   * 
   * @param depth the widlcard depth level
   * @return SearchEntity
   */
	public SearchEntity setWildcardDepth(Integer depth) {

		Attribute attribute = new Attribute("SCH_WILDCARD_DEPTH", "Wildcard", new DataType(Integer.class));
		addAttribute(attribute, 1.0, depth);

		return this;
	}

	/**
   * This method allows to set the status of the result BEs
   * 
   * @param status the search status to set
   * @return SearchEntity
   */
	public SearchEntity setSearchStatus(EEntityStatus status) {

		Attribute attribute = new Attribute("SCH_STATUS", "Status", new DataType(String.class));
		addAttribute(attribute, 1.0, status.toString());

		return this;
	}

	/**
   * This method allows to set the cachable of the result BEs for initial page.
   * 
   * @param cachable true or false. true means cache the result for subsequent
   *                 lookup
   * @return SearchEntity
   */
	public SearchEntity setCachable(Boolean cachable) {

		Attribute attribute = new Attribute("SCH_CACHABLE", "Cachable", new DataType(Boolean.class));
		addAttribute(attribute, 1.0, cachable);

		return this;
	}

	/**
   * This method allows to set the total number of the results (BaseEntites) from
   * the search.
   * 
   * @param totalResults the total results count to set
   * @return SearchEntity
   */
	public SearchEntity setTotalResults(final Integer totalResults) {

		Attribute attribute = new Attribute("PRI_TOTAL_RESULTS", "Total Results", new DataType(Integer.class));
		addAttribute(attribute, 1.0, totalResults);

		return this;
	}

	/**
   * This method allows to set the page index of the search
   * 
   * @param pageIndex the page index to set
   * @return SearchEntity
   */
	public SearchEntity setPageIndex(final Integer pageIndex) {

		Attribute attribute = new Attribute("PRI_INDEX", "Page Index", new DataType(Integer.class));
		addAttribute(attribute, 3.0, pageIndex);

		return this;
	}

	/**
   * Get the page start
   * 
   * @return Integer
   */
	public Integer getPageStart() {
		return getValue("SCH_PAGE_START", null);
	}

	/**
   * Get the page size
   * 
   * @return Integer
   */
	public Integer getPageSize() {
		return getValue("SCH_PAGE_SIZE", null);
	}

	public String getSourceCode() {
		return getValue("SCH_SOURCE_CODE", null);
	}

	public String getTargetCode() {
		return getValue("SCH_TARGET_CODE", null);
	}

	public String getLinkCode() {
		return getValue("SCH_LINK_CODE", null);
	}

	public String getLinkValue() {
		return getValue("SCH_LINK_VALUE", null);
	}

	public String getWildcard() {
		return getValue("SCH_WILDCARD", null);
	}

	public EEntityStatus getSearchStatus() {
		return EEntityStatus.valueOf(getValue("SCH_STATUS", EEntityStatus.ACTIVE.toString()));
	}

	/**
   * This method allows to remove the attributes from the SearchEntity.
   * 
   * @param attributeCode the code of the column to remove
   * @return SearchEntity
   */
	public SearchEntity removeColumn(final String attributeCode) {
		removeAttribute("COL_" + attributeCode);
		return this;
	}

	/**
   * Get the allowed column codes
   * 
   * @return Set
   */
	public Set<String> allowedColumns() {
		return this.columns.stream()
		.map(c -> c.getCode())
		.collect(Collectors.toSet());
	}

	/**
   * Convert to a saveable entity
   * 
   * @return SearchEntity
   */
	public SearchEntity convertToSaveable() {
		return this;
	}

	/**
   * Convert to a sendable entity
   * 
   * @return SearchEntity
   */
	public SearchEntity convertToSendable() {

		// add action attributes
		IntStream.range(0, this.columns.size())
			.forEach(i -> {
				Column column = columns.get(i);
				Attribute attribute = new Attribute(Column.PREFIX + column.getCode(), column.getName(),
					new DataType(String.class));
				EntityAttribute ea = this.addAttribute(attribute, Double.valueOf(i));
				ea.setIndex(i);
			});

		// add action attributes
		IntStream.range(0, this.actions.size())
			.forEach(i -> {
				Action action = actions.get(i);
				Attribute attribute = new Attribute(Action.PREFIX + action.getCode(), action.getName(),
					new DataType(String.class));
				EntityAttribute ea = this.addAttribute(attribute, Double.valueOf(i));
				ea.setIndex(i);
			});

		return this;
	}

	/**
	 * This Method allows specifying columns that can be further filtered on by the user
	 * @param attributeCode The code of the attribute
	 * @param fName The name given to the filter column
	 * @return SearchEntity the updated search base entity
	 */
	public SearchEntity addFilterableColumn(final String attributeCode, final String fName) {

		// TODO: redesign filters
		Attribute attributeFLC = new Attribute("FLC_" + attributeCode, fName, new DataType(String.class));
		addAttribute(attributeFLC, flcIndex);
		flcIndex += 1.0;

		return this;
	}

}
