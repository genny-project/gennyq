package life.genny.qwandaq.entity;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.AttributeText;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.search.Column;
import life.genny.qwandaq.entity.search.Filter;
import life.genny.qwandaq.entity.search.Sort;

/* 
 * SearchEntity class implements the search of base entities applying 
 * different filters/search to the baseEntity and its attributes.
 */
@RegisterForReflection
public class SearchEntity extends BaseEntity {

	private static final Logger log = Logger.getLogger(SearchEntity.class);

	private static final long serialVersionUID = 1L;

	Double filterIndex = 1.0;
	Double columnIndex = 1.0;

	Double actionIndex = 1.0;
	Double searchActionIndex = 1.0;

	Double sortIndex = 0.0;
	Double flcIndex = 1.0;

	/**
	 * Default constructor.
	 */
	public SearchEntity() { }

	/**
	 * Constructor.
	 * @param code SearchEntity code
	 * @param name SearchEntity name
	 */
	public SearchEntity(final String code, final String name) {
		super(code, name);
		setPageStart(0);
		setPageSize(20);
		setTitle(name);
	}

	/**
	 * Add an column to the search results
	 * @param column Column object
	 * @return SearchEntity
	 */
	public SearchEntity add(Column column) {

		Attribute attributeColumn = new Attribute("COL_" + column.getCode(), column.getName(), new DataType(String.class));
		addAttribute(attributeColumn, columnIndex);
		columnIndex += 1.0;

		return this;
	}

	/**
	 * Add an attribute sort order to a search
	 * @param sort Sort object
	 * @return SearchEntity
	 */
	public SearchEntity add(Sort sort) {

		Attribute attribute = new Attribute("SRT_" + sort.getCode(), sort.getCode(), new DataType(String.class));
		addAttribute(attribute, sortIndex, sort.getOrder());
		sortIndex += 1.0;

		return this;
	}

	/**
	 * Add a search filter
	 * @param filter Filter object
	 * @return SearchEntity
	 */
	public SearchEntity add(Filter filter) {

		Attribute attribute = new Attribute(filter.getCode(), filter.getOperator().name(), 
			new DataType(filter.getC()));
		addAttribute(attribute, filterIndex, filter.getValue());
		filterIndex += 1.0;
		
		return this;
	}

	/**
	 * Add an OR condition search filter.
	 * @param filter Filter object
	 * @return SearchEntity
	 */
	public SearchEntity or(Filter filter) {

		Attribute attribute = new Attribute(filter.getCode(), filter.getOperator().name(), 
			new DataType(filter.getC()));
		Integer count = countOccurrences(filter.getCode(), "OR") + 1;
		for (int i = 0; i < count; i++) {
			attribute.setCode("OR_"+attribute.getCode());
		}

		addAttribute(attribute, filterIndex, filter.getValue());
		filterIndex += 1.0;

		return this;
	}
    
	/**
	 * Add an AND condition search filter.
	 * @param filter Filter object
	 * @return SearchEntity
	 */
	public SearchEntity and(Filter filter) {

		Attribute attribute = new Attribute(filter.getCode(), filter.getOperator().name(), 
			new DataType(filter.getC()));
		Integer count = countOccurrences(filter.getCode(), "AND") + 1;
		for (int i = 0; i < count; i++) {
			attribute.setCode("AND_"+attribute.getCode());
		}

		addAttribute(attribute, filterIndex, filter.getValue());
		filterIndex += 1.0;

		return this;	
	}
	

	/** 
	 * This method allows to add the action attributes to the SearchEntity that is
	 * required in the result BaseEntities
	 * @param code the code of the action
	 * @param name the name of the action
	 * @return SearchEntity
	 */
	public SearchEntity addAction(final String code, final String name) {

		Attribute attributeColumn = new Attribute("ACT_" + code, name, new DataType(String.class));
		addAttribute(attributeColumn, actionIndex);
		actionIndex += 1.0;

		return this;
	}
	
	/** 
	 * This method allows to add the associated attributes to the SearchEntity that
	 * is required in the result BaseEntities
	 * @param attributeCode the code of the associated attribute
	 * @param associatedLinkedBaseEntityCodeAttribute the the code of the attribute to fetch as a column
	 * @param columnName the name of the column
	 * @return SearchEntity
	 */
	public SearchEntity addAssociatedColumn(final String attributeCode, final String associatedLinkedBaseEntityCodeAttribute,
			final String columnName) {
		AttributeText attributeColumn = new AttributeText("COL__" + attributeCode.toUpperCase()+"__"+associatedLinkedBaseEntityCodeAttribute.toUpperCase(), columnName);

		EntityAttribute ea = addAttribute(attributeColumn, columnIndex);
		ea.setValue(associatedLinkedBaseEntityCodeAttribute);
		ea.setIndex(columnIndex.intValue());
		columnIndex += 1.0;

		return this;
	}
	
	/** 
	 * This method allows nested associated columns
	 * @param attributeCode the code of the associated attribute
	 * @param nestedAttributeCode the code of the nested associated attribute
	 * @param associatedLinkedBaseEntityCodeAttribute the the code of the attribute to fetch as a column
	 * @param columnName the name of the column
	 * @return SearchEntity
	 */
	public SearchEntity addAssociatedColumn(final String attributeCode, String nestedAttributeCode,
			final String associatedLinkedBaseEntityCodeAttribute, final String columnName) {
		return addAssociatedColumn(attributeCode + "__" + nestedAttributeCode, associatedLinkedBaseEntityCodeAttribute, columnName);
	}

	
	/** 
	 * This method allows double nested associated columns
	 * @param attributeCode the code of the associated attribute
	 * @param nestedAttributeCode the code of the nested associated attribute
	 * @param doubleNestedAttributeCode the code of the double nested associated attribute
	 * @param associatedLinkedBaseEntityCodeAttribute the the code of the attribute to fetch as a column
	 * @param columnName the name of the column
	 * @return SearchEntity
	 */
	public SearchEntity addAssociatedColumn(final String attributeCode, String nestedAttributeCode, String doubleNestedAttributeCode,
			final String associatedLinkedBaseEntityCodeAttribute, final String columnName) {
		return addAssociatedColumn(attributeCode + "__" + nestedAttributeCode + "__" + doubleNestedAttributeCode, associatedLinkedBaseEntityCodeAttribute, columnName);
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
	* This Method allows specifying columns that can be further filtered on by the user
	* @param attributeCode The code of the attribute
	* @param name The name given to the filter column
	* @return SearchEntity the updated search base entity
	 */
	public SearchEntity addFilterableColumn(final String attributeCode, final String name) {

		Attribute attributeFLC = new Attribute("FLC_" + attributeCode, name, new DataType(String.class));
		addAttribute(attributeFLC, flcIndex);
		flcIndex += 1.0;

		return this;
	}

	/** 
	 * Add a conditional attribute.
	 * @param code the attribute to apply the condition to
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
	 * @param selectSize size of selection
	 * @return SearchEntity
	 */
	public SearchEntity setSelectSize(final Integer selectSize) {

		Attribute attribute = new Attribute("SCH_SELECT_SIZE", "SelectSize", new DataType(Integer.class));
		addAttribute(attribute, 1.0, selectSize);

		return this;
	}

	/** 
	 * This method allows to set the total number of the results (BaseEntites) to be sent
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
	 * @param linkValue - linkValue of the sourceCode to the results (BaseEntities)
	 * of the search
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
	 * @param status the search status to set
	 * @return SearchEntity
	 */
	public SearchEntity setSearchStatus(EEntityStatus status) {

		Attribute attribute = new Attribute("SCH_STATUS", "Status", new DataType(Integer.class));
		addAttribute(attribute, 1.0, status.ordinal());

		return this;
	}
	
	/** 
	 * This method allows to set the cachable of the result BEs for initial page.
	 * @param cachable true or false. true means cache the result for subsequent lookup
	 * @return SearchEntity
	 */
	public SearchEntity setCachable(Boolean cachable) {

		Attribute attribute = new Attribute("SCH_CACHABLE", "Cachable", new DataType(Boolean.class));
		addAttribute(attribute, 1.0, cachable);
		
		return this;
	}

	/** 
	 * This method allows to set the total number of the results (BaseEntites) from the search.
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
	 * @param pageIndex the page index to set
	 * @return SearchEntity
	 */
	public SearchEntity setPageIndex(final Integer pageIndex) {

		Attribute attribute = new Attribute("PRI_INDEX", "Page Index", new DataType(Integer.class));
		addAttribute(attribute, 3.0, pageIndex);

		return this;
	}

	/** 
	 * @return String
	 */
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
	 * Get the page start
	 * @return Integer
	 */
	public Integer getPageStart() {
		return getValue("SCH_PAGE_START", null);
	}
	
	/** 
	 * Get the page size
	 * @return Integer
	 */
	public Integer getPageSize() {
		return getValue("SCH_PAGE_SIZE", null);
	}

	
	/** 
	 * @param filterIndex the filter index to set
	 */
	public void setFilterIndex(Double filterIndex) {
			this.filterIndex = filterIndex;
	}

	/** 
	 * @return Double
	 */
	public Double getFilterIndex() {
			return this.filterIndex;
	}

	/** 
	 * @return Double
	 */
	public Double getColIndex() {
		return columnIndex;
	}

	/** 
	 * @param colIndex the column index to set
	 */
	public void setColIndex(Double colIndex) {
		this.columnIndex = colIndex;
	}

	/** 
	 * @return Double
	 */
	public Double getSortIndex() {
		return sortIndex;
	}

	/** 
	 * @param sortIndex the sort index to set
	 */
	public void setSortIndex(Double sortIndex) {
		this.sortIndex = sortIndex;
	}

	/** 
	 * @return Double
	 */
	public Double getFLCIndex() {
		return flcIndex;
	}

	/** 
	 * @param flcIndex the filter column index to set
	 */
	public void setFLCIndex(Double flcIndex) {
		this.flcIndex = flcIndex;
	}

	/** 
	 * @return Double
	 */
	public Double getActionIndex() {
		return actionIndex;
	}

	/** 
	 * @param actionIndex the action index to set
	 */
	public void setActionIndex(Double actionIndex) {
		this.actionIndex = actionIndex;
	}

	/** 
	 * @return Double
	 */
	public Double getSearchActionIndex() {
		return searchActionIndex;
	}

	/** 
	 * @param searchActionIndex the search action index to set
	 */
	public void setSearchActionIndex(Double searchActionIndex) {
		this.searchActionIndex = searchActionIndex;
	}

	/** 
	 * This method allows to remove the attributes from the SearchEntity.
	 * @param attributeCode the code of the column to remove
	 * @return SearchEntity
	 */
	public SearchEntity removeColumn(final String attributeCode) {
		removeAttribute("COL_" + attributeCode);
		return this;
	}
	
	/*
	 * This method will update the column index.
	 */
	public void updateColumnIndex() {
		Integer index = 1;
		for (EntityAttribute ea : this.getBaseEntityAttributes()) {
			if (ea.getAttributeCode().startsWith("COL_")) {
				index++;
			}
		}
		setColIndex(index.doubleValue());
	}

	/*
	 * This method will update the action index.
	 */
	public void updateActionIndex() {
		Integer index = 1;
		for (EntityAttribute ea : this.getBaseEntityAttributes()) {
			if (ea.getAttributeCode().startsWith("ACT_")) {
				index++;
			}
		}
		setActionIndex(index.doubleValue());
	}

	/** 
	 * This method helps calculate the index of an OR filter
	 * 
	 * @param attributeCode - the attributeCode for which to count
	 * @param prefix - prefix to count occurences of
	 * @return Integer
	 */
	public Integer countOccurrences(final String attributeCode, final String prefix) {

        Integer count = -1;
        for (EntityAttribute ea : this.getBaseEntityAttributes()) {
            if (ea.getAttributeCode().endsWith(attributeCode)) {
                Integer occurs = ( ea.getAttributeCode().split(prefix+"_", -1).length ) - 1;
                if (occurs > count) {
                    count = occurs;
                }
            }
        }
		return count;
	}

	/** 
	 * @return Double
	 */
	public Double getMaximumFilterWeight() {

		Double maxWeight = 0.0;
		for (EntityAttribute ea : getBaseEntityAttributes()) {
			if (ea.getAttributeCode().startsWith("PRI_") || ea.getAttributeCode().startsWith("LNK_") ||
				ea.getAttributeCode().startsWith("AND_") || ea.getAttributeCode().startsWith("OR_")) {
				if (ea.getWeight() > maxWeight) {
					maxWeight = ea.getWeight();
				}
			}
		}
		return maxWeight;
	}

}
