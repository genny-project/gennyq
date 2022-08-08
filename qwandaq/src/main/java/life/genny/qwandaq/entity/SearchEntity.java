package life.genny.qwandaq.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.attribute.AttributeBoolean;
import life.genny.qwandaq.attribute.AttributeDate;
import life.genny.qwandaq.attribute.AttributeDateTime;
import life.genny.qwandaq.attribute.AttributeDouble;
import life.genny.qwandaq.attribute.AttributeInteger;
import life.genny.qwandaq.attribute.AttributeLong;
import life.genny.qwandaq.attribute.AttributeText;
import life.genny.qwandaq.attribute.AttributeTime;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.exception.runtime.BadDataException;

/* 
 * SearchEntity class implements the search of base entities applying 
 * different filters/search to the baseEntity and its attributes.
 */
@RegisterForReflection
public class SearchEntity extends BaseEntity {

	private static final Logger log = Logger.getLogger(SearchEntity.class);

	private static final long serialVersionUID = 1L;
	public static final String DEFAULT_PAGETYPE = EPageType.DEFAULT.toString();

	Double filterIndex = 1.0;
	Double colIndex = 1.0;

	Double actionIndex = 1.0;
	Double searchActionIndex = 1.0;

	Double sortIndex = 0.0;
	Double flcIndex = 1.0;
	Double groupIndex = 1.0;
	Double searchIndex = 1.0;
	Double combinedSearchIndex = 1.0;
	Double aliasIndex = 1.0;
	Map<String, Map<String, String>> formatters = new HashMap<>();

	/*
	 * Enum used to sort the search results in either ascending or 
	 * descending order.
	 */
	public enum Sort {
		ASC {
			public String toString() {
				return "ASC";
			}
		},
		DESC {
			public String toString() {
				return "DESC";
			}
		}
	}

	/*
	 * Enum used to determine the operator of a string filter 
	 * added to the SearchEntity.
	 */
	public enum StringFilter {
		EQUAL {
			public String toString() {
				return SearchEntity.convertToSaveable("=");
			}
		},
		NOT_EQUAL {
			public String toString() {
				return SearchEntity.convertToSaveable("!=");
			}
		},
		LIKE {
			public String toString() {
				return "LIKE";
			}
		},
		NOT_LIKE {
			public String toString() {
				return "NOT LIKE";
			}
		},
		RLIKE {
			public String toString() {
				return "RLIKE";
			}
		},
		NOT_RLIKE {
			public String toString() {
				return "NOT RLIKE";
			}
		},
		REGEXP {
			public String toString() {
				return "REGEXP";
			}
		},
		NOT_REGEXP {
			public String toString() {
				return "NOT REGEXP";
			}
		}

	}

	/*
	 * Enum used to determine the operator of a standard filter 
	 * added to the SearchEntity.
	 */
	public enum Filter {
		EQUALS {
			public String toString() {
				return SearchEntity.convertToSaveable("=");
			}
		},
		NOT_EQUALS {
			public String toString() {
				return SearchEntity.convertToSaveable("!=");
			}
		},
		GREATER_THAN {
			public String toString() {
				return SearchEntity.convertToSaveable(">");
			}
		},
		GREATER_THAN_AND_EQUAL {
			public String toString() {
				return SearchEntity.convertToSaveable(">=");
			}
		},
		LESS_THAN_AND_EQUAL {
			public String toString() {
				return SearchEntity.convertToSaveable("<=");
			}
		},
		LESS_THAN {
			public String toString() {
				return SearchEntity.convertToSaveable("<");
			}
		},
		BIT_MASK_POSITIVE {
			public String toString() {
				return SearchEntity.convertToSaveable("&+");
			}
		},
		BIT_MASK_ZERO {
			public String toString() {
				return SearchEntity.convertToSaveable("&0");
			}
		}
	}
	
	/** 
	 * @param operator the operator to convert
	 * @return Filter
	 */
	static public SearchEntity.Filter convertOperatorToFilter(final String operator) {

		switch (operator) {
			case ">": 
				return SearchEntity.Filter.GREATER_THAN;
			case "<": 
				return SearchEntity.Filter.LESS_THAN;
			case ">=": 
				return SearchEntity.Filter.GREATER_THAN_AND_EQUAL;
			case "<=": 
				return SearchEntity.Filter.LESS_THAN_AND_EQUAL;
			case "<>":
			case "!=": 
				return SearchEntity.Filter.NOT_EQUALS;
			default:
				return SearchEntity.Filter.EQUALS;
		}
	}
	
	/** 
	 * @param operator the operator to convert
	 * @return StringFilter
	 */
	static public SearchEntity.StringFilter convertOperatorToStringFilter(final String operator) {

		SearchEntity.StringFilter ret = null;
		switch (operator) {
			case "REGEXP": ret =  SearchEntity.StringFilter.REGEXP; break;
			case "NOT REGEXP": ret =  SearchEntity.StringFilter.NOT_REGEXP; break;
			case "RLIKE": ret =  SearchEntity.StringFilter.RLIKE; break;
			case "NOT RLIKE": ret =  SearchEntity.StringFilter.NOT_RLIKE; break;
			case "LIKE": ret =  SearchEntity.StringFilter.LIKE; break;
			case "NOT LIKE": ret =  SearchEntity.StringFilter.NOT_LIKE; break;
			case "<>":
			case "!=": ret =  SearchEntity.StringFilter.NOT_EQUAL;break;
			default:
					   ret = SearchEntity.StringFilter.EQUAL;
		}

		return ret;
	}
	
	public void convertBEToSaveable() {
		for (EntityAttribute ea : this.getBaseEntityAttributes()) {
			if (!((ea.getAttributeCode().startsWith("COL_")) || (ea.getAttributeCode().startsWith("CAL_"))
					|| (ea.getAttributeCode().startsWith("SCH_")))) {
				String name = ea.getAttributeName();
				name = name.replaceAll(">", "_GT_");
				name = name.replaceAll("<", "_LT_");
				name = name.replaceAll(">=", "_GTE_");
				name = name.replaceAll("<=", "_LTE_");
				name = name.replaceAll("=", "_EQ_");
				name = name.replaceAll("&", "_AMP_");
				name = name.replaceAll("+", "_PLUS_");
				name = name.replaceAll("-", "_MINUS_");
				name = name.replaceAll("&+", "_BPLUS_");
				name = name.replaceAll("&0", "_BZERO_");
				ea.setAttributeName(name);
			}
		}
	}

	public void convertBEFromSaveable() {
		for (EntityAttribute ea : this.getBaseEntityAttributes()) {
			if (!((ea.getAttributeCode().startsWith("COL_")) || (ea.getAttributeCode().startsWith("CAL_"))
					|| (ea.getAttributeCode().startsWith("SCH_")))) {
				String name = ea.getAttributeName();
				name = name.replaceAll("_GT_", ">");
				name = name.replaceAll("_GTE_", ">=");
				name = name.replaceAll("_LT_", "<");
				name = name.replaceAll("_LTE_", "<=");
				name = name.replaceAll("_EQ_", "=");
				name = name.replaceAll("_AMP_", "&");
				name = name.replaceAll("_PLUS_", "+");
				name = name.replaceAll("_MINUS_", "-");
				name = name.replaceAll("_BPLUS_", "&+");
				name = name.replaceAll("_BZERO_", "&0");
				ea.setAttributeName(name);
			}
		}
	}

	/** 
	 * @param value the value to convert
	 * @return String
	 */
	public static String convertToSaveable(String value) {
		String name = value.replaceAll("\\>", "_GT_");
		name = name.replaceAll("\\<", "_LT_");
		name = name.replaceAll("\\=", "_EQ_");
		name = name.replaceAll("\\&", "_AMP_");
		name = name.replaceAll("\\+", "_PLUS_");
		name = name.replaceAll("\\-", "_MINUS_");
		name = name.replaceAll("\\!", "_NOT_");
		return name;
	}
	
	/** 
	 * @param value the value to convert
	 * @return String
	 */
	public static String convertFromSaveable(String value) {
		if (value != null) {
			String name = value;
			name = name.replaceAll("_GT_", ">");
			name = name.replaceAll("_LT_", "<");
			name = name.replaceAll("_EQ_", "=");
			name = name.replaceAll("_AMP_", "&");
			name = name.replaceAll("_PLUS_", "+");
			name = name.replaceAll("_MINUS_", "-");
			name = name.replaceAll("_NOT_", "!");
			return name;
		} else {
			return null;
		}
	}

	/* Constructor to create SearchEntity with code and name */
	public SearchEntity() { }

	/* Constructor to create SearchEntity with code and name */
	public SearchEntity(final String code, final String name) {
		super(code, name);
		setPageStart(0);
		setPageSize(20);
		setTitle(name);
		setPageType(DEFAULT_PAGETYPE);
	}

	/* Constructor to create SearchEntity passing BaseEntity */
	public SearchEntity(final BaseEntity be) {
		super(be.getCode(), be.getName());
		this.setCreated(be.getCreated());
		this.setUpdated(be.getUpdated());
		this.setBaseEntityAttributes(be.getBaseEntityAttributes());
	}

	/** 
	 * This method allows to add the attributes to the SearchEntity that is required
	 * in the result BaseEntities
	 *
	 * @param attributeCode the code of the column
	 * @param columnName the name of the column
	 * @return SearchEntity
	 */
	public SearchEntity addColumn(final String attributeCode, final String columnName) {
		AttributeText attributeColumn = new AttributeText("COL_" + attributeCode, columnName);
		try {
			EntityAttribute ea = addAttribute(attributeColumn, colIndex);
			ea.setIndex(colIndex.intValue());
			colIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad Column Initialisation");
		}
		return this;
	}

	/** 
	 * This method allows to add the action attributes to the SearchEntity that is
	 * required in the result BaseEntities
	 *
	 * @param attributeCode the code of the action
	 * @param columnName the name of the action
	 * @return SearchEntity
	 */
	public SearchEntity addAction(final String attributeCode, final String columnName) {
		return this.addAction(attributeCode, columnName, false);
	}

	/** 
	 * This method allows to add the action attributes to the SearchEntity that is
	 * required in the result BaseEntities
	 *
	 * @param attributeCode the code of the action
	 * @param columnName the name of the action
	 * @param confirmationFlag the confirmation flag
	 * @return SearchEntity
	 */
	public SearchEntity addAction(final String attributeCode, final String columnName, Boolean confirmationFlag) {
		AttributeText attributeColumn = new AttributeText("ACT_" + attributeCode, columnName);
		try {
			EntityAttribute ea = addAttribute(attributeColumn, actionIndex);
			ea.setIndex(actionIndex.intValue());
			ea.setConfirmationFlag(confirmationFlag);
			actionIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad Column Initialisation");
		}
		return this;
	}
	
	/** 
	 * This method allows to add the action attributes to the SearchEntity that is
	 * required in the result BaseEntities
	 *
	 * @param attributeCode the code of the default action
	 * @param columnName the name of the default action
	 * @return SearchEntity
	 */
	public SearchEntity addDefaultAction(final String attributeCode, final String columnName) {
		AttributeText attributeColumn = new AttributeText("ACT_" + attributeCode, columnName);
		try {
			EntityAttribute ea = addAttribute(attributeColumn, 0.0);
			ea.setIndex(0);
		} catch (BadDataException e) {
			log.error("Bad Column Initialisation");
		}
		return this;
	}
	
	/** 
	 * This method allows to add the search action attributes to the SearchEntity
	 * not each result BaseEntities
	 *
	 * @param attributeCode the code of the search action
	 * @param columnName the name of the search action
	 * @return SearchEntity
	 */
	public SearchEntity addSearchAction(final String attributeCode, final String columnName) {
		AttributeText attributeColumn = new AttributeText("SCH_ACT_" + attributeCode, columnName);
		try {
			EntityAttribute ea = addAttribute(attributeColumn, searchActionIndex);
			ea.setIndex(searchActionIndex.intValue());
			searchActionIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad Column Initialisation");
		}
		return this;
	}
	
	/** 
	 * This method allows to add the default search action attributes to the
	 * SearchEntity not each result BaseEntities
	 *
	 * @param attributeCode the code of the default search action
	 * @param columnName the name of the default search action
	 * @return SearchEntity
	 */
	public SearchEntity addDefaultSearchAction(final String attributeCode, final String columnName) {
		AttributeText attributeColumn = new AttributeText("SCH_ACT_" + attributeCode, columnName);
		try {
			EntityAttribute ea = addAttribute(attributeColumn, 0.0);
			ea.setIndex(0);
		} catch (BadDataException e) {
			log.error("Bad Column Initialisation");
		}
		return this;
	}

	
	/** 
	 * This method allows to add the row action attribute to the
	 * each result BaseEntities
	 *
	 * @param attributeCode the code of the row action
	 * @param columnName the name of the column
	 * @return SearchEntity
	 */
	public SearchEntity addRowAction(final String attributeCode, final String columnName) {
		AttributeText attributeColumn = new AttributeText("ROW_ACT_" + attributeCode, columnName);
		try {
			EntityAttribute ea = addAttribute(attributeColumn, 0.0);
			ea.setIndex(0);
		} catch (BadDataException e) {
			log.error("Bad Column Initialisation");
		}
		return this;
	}

	
	/** 
	 * This method allows to add the linked searchcodes to the SearchEntity as required
	 *
	 * @param searchCode the code of the search to link
	 * @param columnName the name of the column
	 * @return SearchEntity
	 */
	public SearchEntity addLinkedSearch(final String searchCode, final String columnName) {
		AttributeText attributeColumn = new AttributeText(searchCode, columnName);
		try {
			EntityAttribute ea = addAttribute(attributeColumn, searchIndex);
			ea.setIndex(searchIndex.intValue());
			searchIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad Linked Search Initialisation");
		}
		return this;
	}

	
	/** 
	 * Add a Linked Search with an association code
	 *
	 * @param searchCode the code of the search to link
	 * @param association the association attribute code
	 * @param columnName the name of the column
	 * @return SearchEntity
	 */
	public SearchEntity addLinkedSearch(final String searchCode, final String association, final String columnName) {
		AttributeText attributeColumn = new AttributeText(searchCode+"."+association, columnName);
		try {
			EntityAttribute ea = addAttribute(attributeColumn, searchIndex);
			ea.setIndex(searchIndex.intValue());
			searchIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad Linked Search Initialisation");
		}
		return this;
	}

	
	/** 
	 * This method allows to add the combined searches to the SearchEntity.
	 * This will combine the results with the two searches
	 * NOTE: has only been implemented for counts so far
	 *
	 * @param searchCode the code of the search to combine
	 * @param columnName the name of the column
	 * @return SearchEntity
	 */
	public SearchEntity addCombinedSearch(final String searchCode, final String columnName) {
		AttributeText attributeColumn = new AttributeText("CMB_"+searchCode, columnName);
		try {
			EntityAttribute ea = addAttribute(attributeColumn, searchIndex);
			ea.setIndex(combinedSearchIndex.intValue());
			combinedSearchIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad Combined Search Initialisation");
		}
		return this;
	}

	
	/** 
	 * This method allows to add the associated attributes to the SearchEntity that
	 * is required in the result BaseEntities
	 *
	 * @param attributeCode the code of the associated attribute
	 * @param associatedLinkedBaseEntityCodeAttribute the the code of the attribute to fetch as a column
	 * @param columnName the name of the column
	 * @return SearchEntity
	 */
	public SearchEntity addAssociatedColumn(final String attributeCode, final String associatedLinkedBaseEntityCodeAttribute,
			final String columnName) {
		AttributeText attributeColumn = new AttributeText("COL__" + attributeCode.toUpperCase()+"__"+associatedLinkedBaseEntityCodeAttribute.toUpperCase(), columnName);
		try {
			EntityAttribute ea = addAttribute(attributeColumn, colIndex);
			ea.setValue(associatedLinkedBaseEntityCodeAttribute);
			ea.setIndex(colIndex.intValue());
			colIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad Associated Column Initialisation");
		}
		return this;
	}

	
	/** 
	 * This method allows nested associated columns
	 *
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
	 *
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
	 * This method allows to add sorting to the attributes of the search results It
	 * can either sort in ascending or descending order
	 *
	 * @param attributeCode the code of the attribute to add a sort for
	 * @param sortHelpText the help text for the sort
	 * @param sortType the type of sort
	 * @return SearchEntity
	 */
	public SearchEntity addSort(final String attributeCode, final String sortHelpText, final Sort sortType) {
		AttributeText attributeSort = new AttributeText("SRT_" + attributeCode, sortHelpText);
		try {
			addAttribute(attributeSort, sortIndex, sortType.toString());
			sortIndex += 1.0;

		} catch (BadDataException e) {
			log.error("Bad Sort Initialisation");
		}

		return this;
	}

	/** 
	 * @param attributeCode the code of the attribute to add a sort attribute for
	 * @param name the name of the sort attribute
	 * @return SearchEntity
	 */
	public SearchEntity addSortAttribute(final String attributeCode, final String name) {
		AttributeText attributeSort = new AttributeText("ATTRSRT_" + attributeCode, name);
		try {
			addAttribute(attributeSort, 1.0);
		} catch (BadDataException e) {
			log.error("Bad Sort Initialisation");
		}
		return this;
	}

	/**
	* This Method allows specifying columns that can be further filtered on by the user
	* @param attributeCode The code of the attribute
	* @param fName The name given to the filter column
	* @return SearchEntity the updated search base entity
	 */
	public SearchEntity addFilterableColumn(final String attributeCode, final String fName) {
		AttributeText attributeFLC = new AttributeText("FLC_" + attributeCode, fName);
		try {
			addAttribute(attributeFLC, flcIndex);
			flcIndex += 1.0;

		} catch (BadDataException e) {
			log.error("Bad Filterable Column Initialisation");
		}

		return this;
	}

	
	/** 
	 * This method allows to add grouping by a specific attribute. all BEs who's
	 * value corresponding to this attribute will be grouped together.
	 * NOTE: not implemented yet
	 *
	 * @param groupBy the group by code to set
	 * @return SearchEntity
	 */
	public SearchEntity addGroupBy(final String groupBy) {
		AttributeText attribute = new AttributeText("GPB_" + groupBy, "GroupBy");
		try {
			addAttribute(attribute, groupIndex, groupBy);
			groupIndex += 1.0;

		} catch (BadDataException e) {
			log.error("Bad Group By Initialisation");
		}

		return this;
	}

	
	/** 
	 * @param attributeCode the attribute code to set an alias for
	 * @param alias the alias to set for the attribute
	 * @return SearchEntity
	 */
	public SearchEntity addAlias(final String attributeCode, final String alias) {
		AttributeText attribute = new AttributeText("ALS_" + attributeCode, alias);
		try {
			addAttribute(attribute, aliasIndex);
			aliasIndex += 1.0;

		} catch (BadDataException e) {
			log.error("Bad Alias Initialisation");
		}

		return this;
	}

	
	/*
	 * This method allows to set the filter for the integer value in the search
	 * 
	 * @param attributeCode the attributeCode which holds integer value where we
	 * apply the filter
	 * @param filterType type of the filter
	 * @param value filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addFilter(final String attributeCode, final Filter filterType, final Integer value) {
		AttributeInteger attribute = new AttributeInteger(attributeCode, filterType.toString());

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad Integer Filter Initialisation");
		}

		return this;
	}

	/**
	 * This method allows to set the filter for the Long value in the search
	 * 
	 * @param attributeCode - the attributeCode which holds long value where we
	 * apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addFilter(final String attributeCode, final Filter filterType, final Long value) {
		AttributeLong attribute = new AttributeLong(attributeCode, filterType.toString());

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad Long Filter Initialisation");
		}

		return this;
	}
	
	/**
	 * This method allows to set the filter for the Double value in the search
	 * 
	 * @param attributeCode - the attributeCode which holds long value where we
	 * apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addFilter(final String attributeCode, final Filter filterType, final Double value) {
		AttributeDouble attribute = new AttributeDouble(attributeCode, filterType.toString());

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad Double Filter Initialisation");
		}

		return this;
	}

	
	/** 
	 * This method allows to set the filter for the LocalDateTime value in the
	 * search
	 * 
	 * @param attributeCode - the attributeCode which holds LocalDateTime value
	 * where we apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addFilter(final String attributeCode, final Filter filterType, final LocalDateTime value) {
		AttributeDateTime attribute = new AttributeDateTime(attributeCode, filterType.toString());

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad DateTime Filter Initialisation");
		}

		return this;
	}

	
	/** 
	 * This method allows to set the filter for the LocalDate value in the
	 * search
	 * 
	 * @param attributeCode - the attributeCode which holds LocalDate value
	 * where we apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addFilter(final String attributeCode, final Filter filterType, final LocalDate value) {
		AttributeDate attribute = new AttributeDate(attributeCode, filterType.toString());

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad Date Filter Initialisation");
		}

		return this;
	}

	
	/** 
	 * This method allows to set the filter for the LocalDate value in the
	 * search
	 * 
	 * @param attributeCode - the attributeCode which holds LocalDate value
	 * where we apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addFilter(final String attributeCode, final Filter filterType, final LocalTime value) {
		AttributeTime attribute = new AttributeTime(attributeCode, filterType.toString());

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad Date Filter Initialisation");
		}

		return this;
	}


	
	/** 
	 * This method allows to set the filter for the Boolean value in the search
	 * 
	 * @param attributeCode - the attributeCode which holds Boolean value where we
	 * apply the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addFilter(final String attributeCode, final Boolean value) {
		AttributeBoolean attribute = new AttributeBoolean(attributeCode, "=");

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad Double Filter Initialisation");
		}

		return this;
	}

	
	/**
	 * This method allows to set the filter for the String value in the search
	 * 
	 * @param attributeCode - the attributeCode which holds String value where we
	 * apply the filter
	 * 
	 * @param filterType - type of the string filter
	 * 
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addFilter(final String attributeCode, final StringFilter filterType, final String value) {
		AttributeText attribute = new AttributeText(attributeCode, filterType.toString());

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad String Filter Initialisation");
		}
		
		return this;
	}

	/** 
	 * This method allows to set the filter for the LocalDate value in the
	 * search
	 * 
	 * @param attributeCode - the attributeCode which holds LocalDate value
	 * where we apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addFilterAsString(final String attributeCode, final Filter filterType, final String value) {
		AttributeText attribute = new AttributeText(attributeCode, filterType.toString());

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad Filter As String Filter Initialisation");
		}

		return this;
	}

	/** 
	 * This method allows to set an OR filter for an attribute
	 * 
	 * @param attributeCode - the attributeCode which holds String value where we
	 * apply the filter
	 * 
	 * @param filterType - type of the filter
	 * 
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
    
	public SearchEntity addOr(final String attributeCode, final Filter filterType, final Integer value) {
		AttributeInteger attribute = new AttributeInteger(attributeCode, filterType.toString());
		Integer count = countOccurrences(attributeCode, "OR") + 1;

		for (int i = 0; i < count; i++) {
			attribute.setCode("OR_"+attribute.getCode());
		}

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad OR Integer Filter Initialisation");
		}

		return this;
	}
    
	
	/** 
	 * This method allows to set an OR filter for an attribute
	 * 
	 * @param attributeCode - the attributeCode which holds String value where we
	 * apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addOr(final String attributeCode, final Filter filterType, final Long value) {
		AttributeLong attribute = new AttributeLong(attributeCode, filterType.toString());
		Integer count = countOccurrences(attributeCode, "OR") + 1;

		for (int i = 0; i < count; i++) {
			attribute.setCode("OR_"+attribute.getCode());
		}

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad OR Long Filter Initialisation");
		}

		return this;
	}

	
	/** 
	 * This method allows to set an OR filter for an attribute
	 * 
	 * @param attributeCode - the attributeCode which holds String value where we
	 * apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addOr(final String attributeCode, final Filter filterType, final Double value) {
		AttributeDouble attribute = new AttributeDouble(attributeCode, filterType.toString());
		Integer count = countOccurrences(attributeCode, "OR") + 1;

		for (int i = 0; i < count; i++) {
			attribute.setCode("OR_"+attribute.getCode());
		}

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad OR Double Filter Initialisation");
		}

		return this;
	}

	
	/** 
	 * This method allows to set an OR filter for an attribute
	 * 
	 * @param attributeCode - the attributeCode which holds String value where we
	 * apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addOr(final String attributeCode, final Filter filterType, final LocalDateTime value) {
		AttributeDateTime attribute = new AttributeDateTime(attributeCode, filterType.toString());
		Integer count = countOccurrences(attributeCode, "OR") + 1;

		for (int i = 0; i < count; i++) {
			attribute.setCode("OR_"+attribute.getCode());
		}

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad OR LocalDateTime Filter Initialisation");
		}

		return this;
	}

	
	/** 
	 * This method allows to set an OR filter for an attribute
	 * 
	 * @param attributeCode - the attributeCode which holds String value where we
	 * apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addOr(final String attributeCode, final Filter filterType, final LocalDate value) {
		AttributeDate attribute = new AttributeDate(attributeCode, filterType.toString());
		Integer count = countOccurrences(attributeCode, "OR") + 1;

		for (int i = 0; i < count; i++) {
			attribute.setCode("OR_"+attribute.getCode());
		}

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad OR LocalDate Filter Initialisation");
		}

		return this;
	}

	
	/** 
	 * This method allows to set an OR filter for an attribute
	 * 
	 * @param attributeCode - the attributeCode which holds String value where we
	 * apply the filter
	 * @param filterType - type of the string filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addOr(final String attributeCode, final StringFilter filterType, final String value) {
		AttributeText attribute = new AttributeText(attributeCode, filterType.toString());
		Integer count = countOccurrences(attributeCode, "OR") + 1;

		for (int i = 0; i < count; i++) {
			attribute.setCode("OR_"+attribute.getCode());
		}

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad OR String Filter Initialisation");
		}

		return this;
	}
    
	
	/** 
	 * This method allows to set an AND filter for an attribute
	 * 
	 * @param attributeCode - the attributeCode which holds String value where we
	 * apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addAnd(final String attributeCode, final Filter filterType, final Integer value) {
		AttributeInteger attribute = new AttributeInteger(attributeCode, filterType.toString());
		Integer count = countOccurrences(attributeCode, "AND") + 1;

		for (int i = 0; i < count; i++) {
			attribute.setCode("AND_"+attribute.getCode());
		}

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad AND Integer Filter Initialisation");
		}

		return this;
	}
    
	
	/** 
	 * This method allows to set an AND filter for an attribute
	 * 
	 * @param attributeCode - the attributeCode which holds String value where we
	 * apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addAnd(final String attributeCode, final Filter filterType, final Long value) {
		AttributeLong attribute = new AttributeLong(attributeCode, filterType.toString());
		Integer count = countOccurrences(attributeCode, "AND") + 1;

		for (int i = 0; i < count; i++) {
			attribute.setCode("AND_"+attribute.getCode());
		}

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad AND Long Filter Initialisation");
		}

		return this;
	}

	
	/** 
	 * This method allows to set an AND filter for an attribute
	 * 
	 * @param attributeCode - the attributeCode which holds String value where we
	 * apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addAnd(final String attributeCode, final Filter filterType, final Double value) {
		AttributeDouble attribute = new AttributeDouble(attributeCode, filterType.toString());
		Integer count = countOccurrences(attributeCode, "AND") + 1;

		for (int i = 0; i < count; i++) {
			attribute.setCode("AND_"+attribute.getCode());
		}

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad AND Double Filter Initialisation");
		}

		return this;
	}

	
	/** 
	 * This method allows to set an AND filter for an attribute
	 * 
	 * @param attributeCode - the attributeCode which holds String value where we
	 * apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addAnd(final String attributeCode, final Filter filterType, final LocalDateTime value) {
		AttributeDateTime attribute = new AttributeDateTime(attributeCode, filterType.toString());
		Integer count = countOccurrences(attributeCode, "AND") + 1;

		for (int i = 0; i < count; i++) {
			attribute.setCode("AND_"+attribute.getCode());
		}

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad AND LocalDateTime Filter Initialisation");
		}

		return this;
	}

	
	/** 
	 * This method allows to set an AND filter for an attribute
	 * 
	 * @param attributeCode - the attributeCode which holds String value where we
	 * apply the filter
	 * @param filterType - type of the filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addAnd(final String attributeCode, final Filter filterType, final LocalDate value) {
		AttributeDate attribute = new AttributeDate(attributeCode, filterType.toString());
		Integer count = countOccurrences(attributeCode, "AND") + 1;

		for (int i = 0; i < count; i++) {
			attribute.setCode("AND_"+attribute.getCode());
		}

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad AND LocalDate Filter Initialisation");
		}

		return this;
	}

	
	/** 
	 * This method allows to set an AND filter for an attribute
	 * 
	 * @param attributeCode - the attributeCode which holds String value where we
	 * apply the filter
	 * @param filterType - type of the string filter
	 * @param value - filter against (search for) this value
	 * @return SearchEntity
	 */
	public SearchEntity addAnd(final String attributeCode, final StringFilter filterType, final String value) {
		AttributeText attribute = new AttributeText(attributeCode, filterType.toString());
		Integer count = countOccurrences(attributeCode, "AND") + 1;

		for (int i = 0; i < count; i++) {
			attribute.setCode("AND_"+attribute.getCode());
		}

		try {
			addAttribute(attribute, filterIndex, value);
			filterIndex += 1.0;
		} catch (BadDataException e) {
			log.error("Bad AND String Filter Initialisation");
		}

		return this;
	}

	
	/** 
	 * Add a conditional attribute
	 *
	 * @param attributeCode the attribute to apply the condition to
	 * @param condition the condition to apply
	 * @return SearchEntity
	 */
	public SearchEntity addConditional(String attributeCode, String condition) {

		Optional<EntityAttribute> existing = findEntityAttribute("CND_"+attributeCode);
		if (existing.isPresent()) {
			existing.get().setValue(existing.get().getValue().toString() + ":::" + condition);
		} else {
			AttributeText attribute = new AttributeText("CND_"+attributeCode, "CND_"+attributeCode);
			try {
				addAttribute(attribute, 1.0, condition);
				filterIndex += 1.0;
			} catch (BadDataException e) {
				log.error("Bad Conditional Initialisation");
			}
		}
		return this;
	}

	
	/** 
	 * Add a whitelist attribute
	 *
	 * @param attributeCode the attribute code to add to the whitelist
	 * @return SearchEntity
	 */
	public SearchEntity addWhitelist(String attributeCode) {
		AttributeText attribute = new AttributeText("WTL_" + attributeCode, attributeCode);

		try {
			addAttribute(attribute, filterIndex, attributeCode);
		} catch (BadDataException e) {
			log.error("Bad Whitelist Filter Initialisation");
		}
		
		return this;
	}

	
	/** 
	 * Add a blacklist attribute
	 *
	 * @param attributeCode the attribute code to add to the blacklist
	 * @return SearchEntity
	 */
	public SearchEntity addBlacklist(String attributeCode) {
		AttributeText attribute = new AttributeText("BKL_" + attributeCode, attributeCode);

		try {
			addAttribute(attribute, filterIndex, attributeCode);
		} catch (BadDataException e) {
			log.error("Bad Whitelist Filter Initialisation");
		}
		
		return this;
	}
	
	/** 
	 * This method allows to set the LinkWeight to the resulted BaseEntities to its
	 * parent
	 * 
	 * @param value - value/linkWeight to be set
	 * @return SearchEntity
	 */
	public SearchEntity setLinkWeight(final Double value) {
		AttributeDouble attribute = new AttributeDouble("SCH_LINK_WEIGHT", "LinkWeight");
		try {
			addAttribute(attribute, 1.0, value);
		} catch (BadDataException e) {
			log.error("Bad String Filter Initialisation");
		}

		return this;
	}

	
	/** 
	 * This method allows to set the filter based on the linkweight value of
	 * BaseEntities to its parent
	 * 
	 * @param filterType - type of the filter set to the linkWeight
	 * @return SearchEntity
	 */
	public SearchEntity addFilterToLinkWeight(final Filter filterType) {
		AttributeText attribute = new AttributeText("SCH_LINK_FILTER", "LinkFilterByWeight");
		try {
			addAttribute(attribute, 1.0, filterType.toString());
		} catch (BadDataException e) {
			log.error("Bad String Filter Initialisation");
		}

		return this;
	}

	
	/** 
	 * This method allows to set the title of the results data to be sent
	 * 
	 * @param title - The page Title
	 * @return SearchEntity
	 */
	public SearchEntity setTitle(final String title) {
		AttributeText attributeTitle = new AttributeText("SCH_TITLE", "Title");
		try {
			addAttribute(attributeTitle, 5.0, title);
		} catch (BadDataException e) {
			log.error("Bad Title ");
		}

		return this;
	}

		
	/** 
	 * This method allows to set the parentCode of the SearchEntity
	 * 
	 * @param parentCode the parent entity code
	 * @return SearchEntity
	 */
	public SearchEntity setParentCode(final String parentCode) {
		AttributeText attributeTitle = new AttributeText("SCH_PARENT_CODE", "Parent Code");
		try {
			addAttribute(attributeTitle, 1.0, parentCode);
		} catch (BadDataException e) {
			log.error("Bad Title ");
		}

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
		AttributeInteger attributePageStart = new AttributeInteger("SCH_PAGE_START", "PageStart");
		try {
			addAttribute(attributePageStart, 3.0, pageStart);
		} catch (BadDataException e) {
			log.error("Bad Page Start ");
		}

		return this;
	}

	/** 
	 * This method allows to set size of the selection allowed for a searchEntity
	 * 
	 * @param selectSize size of selection
	 * @return SearchEntity
	 */
	public SearchEntity setSelectSize(final Integer selectSize) {
		AttributeInteger attributeSelectSize = new AttributeInteger("SCH_SELECT_SIZE", "SelectSize");
		try {
			addAttribute(attributeSelectSize, 1.0, selectSize);
		} catch (BadDataException e) {
			log.error("Bad Page Start ");
		}

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
		AttributeInteger attributePageSize = new AttributeInteger("SCH_PAGE_SIZE", "PageSize");
		try {
			addAttribute(attributePageSize, 3.0, pageSize);
		} catch (BadDataException e) {
			log.error("Bad Page Size");
		}

		return this;
	}

	/** 
	 * This method allows to set the stakeholder/user code to the search. It will
	 * search for the BaseEntites that the given user is stakeholder of.
	 * 
	 * @param stakeholderCode the userCode of the stakeHolder
	 * @return SearchEntity
	 */
	public SearchEntity setStakeholder(final String stakeholderCode) {
		AttributeText attribute = new AttributeText("SCH_STAKEHOLDER_CODE", "Stakeholder");
		try {
			addAttribute(attribute, 1.0, stakeholderCode);
		} catch (BadDataException e) {
			log.error("Bad Stakeholder");
		}

		return this;
	}
	
	/** 
	 * This method allows to set the stakeholder/user code to the parent/source
	 * Basentity involved in the search. It will search for the BaseEntites under
	 * the give source BE that the given user is stakeholder of.
	 * 
	 * @param sourceStakeholderCode the userCode of the source stakeHolder
	 * @return SearchEntity
	 */
	public SearchEntity setSourceStakeholder(final String sourceStakeholderCode) {
		AttributeText attribute = new AttributeText("SCH_SOURCE_STAKEHOLDER_CODE", "SourceStakeholder");
		try {
			addAttribute(attribute, 1.0, sourceStakeholderCode);
		} catch (BadDataException e) {
			log.error("Bad Source Stakeholder");
		}

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
		AttributeText attribute = new AttributeText("SCH_LINK_CODE", "LinkCode");
		try {
			addAttribute(attribute, 1.0, linkCode);
		} catch (BadDataException e) {
			log.error("Bad Stakeholder");
		}

		return this;
	}
	
	/** 
	 * This method allows to set the link value the result of the search.
	 * 
	 * @param linkValue - linkValue of the sourceCode to the results (BaseEntities)
	 * of the search
	 * @return SearchEntity
	 */
	public SearchEntity setLinkValue(final String linkValue) {
		AttributeText attribute = new AttributeText("SCH_LINK_VALUE", "LinkValue");
		try {
			addAttribute(attribute, 1.0, linkValue);
		} catch (BadDataException e) {
			log.error("Bad Link Value");
		}

		return this;
	}

	/** 
	 * @param sourceCode the sourceCode to set
	 * @return SearchEntity
	 */
	public SearchEntity setSourceCode(final String sourceCode) {
		AttributeText attribute = new AttributeText("SCH_SOURCE_CODE", "SourceCode");
		try {
			addAttribute(attribute, 1.0, sourceCode);
		} catch (BadDataException e) {
			log.error("Bad SourceCode");
		}
		return this;
	}
	
	/** 
	 * @param targetCode the targetCode to set
	 * @return SearchEntity
	 */
	public SearchEntity setTargetCode(final String targetCode) {
		AttributeText attribute = new AttributeText("SCH_TARGET_CODE", "TargetCode");
		try {
			addAttribute(attribute, 1.0, targetCode);
		} catch (BadDataException e) {
			log.error("Bad Target Code");
		}

		return this;
	}

	
	/** 
	 * @param displayMode the displayMode to set
	 * @return SearchEntity
	 */
	public SearchEntity setDisplayMode(final String displayMode) {
		AttributeText attribute = new AttributeText("SCH_DISPLAY_MODE", "DisplayMode");
		try {
			addAttribute(attribute, 1.0, displayMode);
		} catch (BadDataException e) {
			log.error("Bad Display Mode");
		}

		return this;
	}

	
	/** 
	 * @param questionCode the questionCode to set
	 * @return SearchEntity
	 */
	public SearchEntity setSearchQuestionCode(final String questionCode) {
		AttributeText attribute = new AttributeText("SCH_QUESTION_CODE", "Question Code");
		try {
			addAttribute(attribute, 1.0, questionCode);
		} catch (BadDataException e) {
			log.error("Bad Question Code!");
		}

		return this;
	}

	/**
	* Set the validation attribute.
	* The search will then look to this attribute to find its validation state.
	*
	* @param validationAttribute the validation attribute code to set
	* @return SearchEntity
	 */
	public SearchEntity setValidationAttribute(final String validationAttribute) {
		AttributeText attribute = new AttributeText("SCH_VALIDATION_ATTRIBUTE", "ValidationAttribute");
		try {
			addAttribute(attribute, 1.0, validationAttribute);
		} catch (BadDataException e) {
			log.error("Bad Validation Attribute");
		}

		return this;
	}

	/**
	* This method allows users to set the dropdown target.
	* Used to pass information about the entity concerning a dropdown.
	*
	* @param dropdownTarget A code, or other information about the target
	* entity of a dropdown.
	* @return SearchEntity
	 */
	public SearchEntity setDropdownTarget(final String dropdownTarget) {
		AttributeText attribute = new AttributeText("SCH_DROPDOWN_TARGET", "Dropdown Target");
		try {
			addAttribute(attribute, 1.0, dropdownTarget);
		} catch (BadDataException e) {
			log.error("Bad Dropdown Target");
		}

		return this;
	}

	/** 
	 * This method allows to set the wildcard of the results data to be sent
	 * 
	 * @param wildcard the widlcard
	 * @return SearchEntity
	 */
	public SearchEntity setWildcard(String wildcard) {

		AttributeText attributeWildcard = new AttributeText("SCH_WILDCARD", "Wildcard");
		try {
			addAttribute(attributeWildcard, 1.0, wildcard);
		} catch (BadDataException e) {
			log.error("Bad Wildcard!");
		}

		return this;
	}

	/** 
	 * This method allows to set the wildcard depth level for associated wildcards
	 * 
	 * @param depth the widlcard depth level
	 * @return SearchEntity
	 */
	public SearchEntity setWildcardDepth(Integer depth) {

		AttributeInteger attributeWildcard = new AttributeInteger("SCH_WILDCARD_DEPTH", "Wildcard");
		try {
			addAttribute(attributeWildcard, 1.0, depth);
		} catch (BadDataException e) {
			log.error("Bad Wildcard Depth!");
		}

		return this;
	}
	
	/** 
	 * This method allows to set the status of the result BEs
	 * 
	 * @param status the search status to set
	 * @return SearchEntity
	 */
	public SearchEntity setSearchStatus(EEntityStatus status) {

		AttributeInteger attributeStatus = new AttributeInteger("SCH_STATUS", "Status");
		try {
			addAttribute(attributeStatus, 1.0, status.ordinal());
		} catch (BadDataException e) {
			log.error("Bad Search Status");
		}

		return this;
	}
	
	/** 
	 * This method allows to set the cachable of the result BEs for initial page
	 * 
	 * @param cachable true or false. true means cache the result for subsequent lookup
	 * @return SearchEntity
	 */
	public SearchEntity setCachable(Boolean cachable) {

		AttributeBoolean attributeCachable = new AttributeBoolean("SCH_CACHABLE", "Cachable");
		try {
			addAttribute(attributeCachable, 1.0, cachable);
		} catch (BadDataException e) {
			log.error("Bad Search cachable");
		}
		
		return this;
	}

	/** 
	 * This method allows to set the type of range data that the search relates to.
	 * This is important for pagination that needs to page across data spans such as
	 * Months, days, weeks, years, etc.
	 * 
	 * @param pageType the pageType to set
	 * @return SearchEntity
	 */
	public SearchEntity setPageType(final String pageType) {
		return setPageType(EPageType.valueOf(pageType));
	}

	/** 
	 * This method allows to set the type of range data that the search relates to.
	 * This is important for pagination that needs to page across data spans such as
	 * Months, days, weeks, years, etc.
	 *
	 * @param pageType the pageType to set
	 * @return SearchEntity
	 */
	public SearchEntity setPageType(final EPageType pageType) {
		AttributeText attributePageStart = new AttributeText("SCH_PAGE_TYPE", "PageType");
		try {
			addAttribute(attributePageStart, 3.0, pageType.toString());
			if (!EPageType.DEFAULT.equals(pageType)) {
				switch (pageType.getCategory()) {
					case DATE:
						// Now set a default PageIndexDate
						AttributeDate pageDate = new AttributeDate("SCH_PAGE_DATE", "Page Date");
						try {
							addAttribute(pageDate, 1.0, LocalDateTime.now());
						} catch (BadDataException e) {
							log.error("Bad Wildcard ");
						}

						break;
					case DATETIME:
						// Now set a default PageIndexDate
						AttributeDateTime pageDateTime = new AttributeDateTime("SCH_PAGE_DATETIME", "Page DateTime");
						try {
							addAttribute(pageDateTime, 1.0, LocalDateTime.now());
						} catch (BadDataException e) {
							log.error("Bad Wildcard ");
						}

						break;

					case GROUP:
						AttributeText pageText = new AttributeText("SCH_PAGE_TEXT", "Page Text");
						try {
							addAttribute(pageText, 1.0, "");
						} catch (BadDataException e) {
							log.error("Bad Wildcard ");
						}

						break;
					default:
				}
			}
		} catch (BadDataException e) {
			log.error("Bad Page Start ");
		}

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
	 *
	 * @param defaultPageStart the default page start if nothing is found
	 * @return Integer
	 */
	public Integer getPageStart(Integer defaultPageStart) {
		Integer pageStart = getValue("SCH_PAGE_START", defaultPageStart);
		return pageStart;
	}

	
	/** 
	 * Get the page size
	 *
	 * @param defaultPageSize the default page size if nothing is found
	 * @return Integer
	 */
	public Integer getPageSize(Integer defaultPageSize) {
		Integer pageSize = getValue("SCH_PAGE_SIZE", defaultPageSize);
		return pageSize;
	}

	
	/** 
	 * @return Double
	 */
	public Double getColIndex() {
		return colIndex;
	}

	
	/** 
	 * @param colIndex the column index to set
	 */
	public void setColIndex(Double colIndex) {
		this.colIndex = colIndex;
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
	 * @return Double
	 */
	public Double getGroupIndex() {
		return groupIndex;
	}

	
	/** 
	 * @param groupIndex the group index to set
	 */
	public void setGroupIndex(Double groupIndex) {
		this.groupIndex = groupIndex;
	}

	
	/** 
	 * @return Double
	 */
	public Double getSearchIndex() {
		return searchIndex;
	}

	
	/** 
	 * @param searchIndex the search index to set
	 */
	public void setSearchIndex(Double searchIndex) {
		this.searchIndex = searchIndex;
	}
	
	/** 
	 * This method allows to remove the attributes from the SearchEntity
	 *
	 * @param attributeCode the code of the column to remove
	 * @return SearchEntity
	 */
	public SearchEntity removeColumn(final String attributeCode) {
		removeAttribute("COL_" + attributeCode);
		return this;
	}
	
	/** 
	 * This method allows to set the total number of the results (BaseEntites) from
	 * the search
	 * 
	 * @param totalResults the total results count to set
	 * @return SearchEntity
	 */
	public SearchEntity setTotalResults(final Integer totalResults) {
		AttributeInteger attributeTotalResults = new AttributeInteger("PRI_TOTAL_RESULTS", "Total Results");
		try {
			addAttribute(attributeTotalResults, 3.0, totalResults);
		} catch (BadDataException e) {
			log.error("Bad Total Results");
		}

		return this;
	}

	
	/** 
	 * This method allows to set the page index of the search
	 * 
	 * @param pageIndex the page index to set
	 * @return SearchEntity
	 */
	public SearchEntity setPageIndex(final Integer pageIndex) {
		AttributeInteger attributePageIndex = new AttributeInteger("PRI_INDEX", "Page Index");
		try {
			addAttribute(attributePageIndex, 3.0, pageIndex);
		} catch (BadDataException e) {
			log.error("Bad Page Index");
		}

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

	/*
	 * This method helps you format the search data
	 */
	public SearchEntity addFormatter(final String attributeCode, String formatKey, String formatValue) {
		Map<String, String> format = new HashMap<>();
		format.put(formatKey, formatValue);
		this.formatters.put(attributeCode, format);
		return this;
	}

	public Map<String, Map<String, String>> getFormatters(){
		return this.formatters;
	}
}
