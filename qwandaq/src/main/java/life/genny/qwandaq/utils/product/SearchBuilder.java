package life.genny.qwandaq.utils.product;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.oracle.svm.core.annotate.Inject;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.clause.Or;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.managers.CacheManager;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;

/**
 * A Simple Utility class for creating a variety of different SearchEntities (developer QoL improvements)
 * 
 * @author Bryn Meachem
 */
@ApplicationScoped
public class SearchBuilder {
    private static final String SBE_SER_LNK_ROLE = "SBE_SER_LNK_ROLE";

    @Inject
    Logger log;

    @Inject
    CacheManager cm;

    @ConfigProperty(name = "genny.product")
    String CONFIG_PRODUCT_CODE;
    
    public SearchBuilder() { /* no-args constructor */ }

    /**
     * Create a new Dropdown SearchEntity
     * @param dropdownCode - {@link BaseEntity#getCode() baseentity code} for the dropdown SearchEntity
     * @param filters - filters to apply to get the base entities to return from teh dropdown SearchEntity
     * @return a dropdown SearchEntity with the filters supplied and the name set to the supplied dropdownCode
     */
    public SearchEntity dropdown(String dropdownCode, Filter... filters) {
        return dropdown(dropdownCode, dropdownCode, filters);
    }

    /**
     * Create a new Dropdown SearchEntity
     * @param dropdownCode - {@link BaseEntity#getCode() baseentity code} for the dropdown SearchEntity
     * @param selectionParentCode - the parent code that all selections have as their {@link Attribute#LNK_PARENT} (for single filter dropdowns)
     * @return - a dropdown SearchEntity for the given parent code with the name set to the supplied dropdownCode
     */
    public SearchEntity dropdown(String dropdownCode, String selectionParentCode) {
        return dropdown(dropdownCode, dropdownCode, selectionParentCode);
    }

    /**
     * Create a new Dropdown SearchEntity
     * @param dropdownCode - {@link BaseEntity#getCode() baseentity code} for the dropdown SearchEntity
     * @param dropdownName - {@link BaseEntity#getName() name} of the dropdown
     * @param selectionParentCode - the parent code that all selections have as their {@link Attribute#LNK_PARENT} (for single filter dropdowns)
     * @return - a dropdown SearchEntity for the given parent code
     */
    public SearchEntity dropdown(String dropdownCode, String dropdownName, String selectionParentCode) {
        return dropdown(dropdownCode, dropdownName, new Filter(Attribute.LNK_PARENT, Operator.CONTAINS, selectionParentCode));
    }

    /**
     * Create a new Dropdown SearchEntity
     * @param dropdownCode - {@link BaseEntity#getCode() baseentity code} for the dropdown SearchEntity
     * @param dropdownName - {@link BaseEntity#getName() name} of the dropdown
     * @param filters - filters to apply to get the base entities to return from teh dropdown SearchEntity
     * @return a dropdown SearchEntity with the filters supplied
     */
    public SearchEntity dropdown(String dropdownCode, String dropdownName, Filter... filters) {
        if(!dropdownCode.startsWith(Prefix.SBE_))
            dropdownCode = Prefix.SBE_.concat(dropdownCode);
        
        SearchEntity dropdown = new SearchEntity(dropdownCode, dropdownName);
        for(Filter filter : filters) {
            if(filter.getCode().startsWith("PRI_IS_")) {
                log.trace("PRI_IS_ filter detected!");
            }
            dropdown.add(filter);
        }

        if(filters.length == 0) {
            log.warn("Dropdown: " + dropdownCode + " created with no filters! This may cause unintended behaviour");
        }

        return dropdown;
    }

    /**
     * Create a new Role Dropdown SearchEntity (Default Code: {@link SearchBuilder#SBE_SER_LNK_ROLE SBE_SER_LNK_ROLE})
     * @param dropdownName - {@link BaseEntity#getName() name} of the dropdown
     * @param filters - filters to apply to get the base entities to return from teh dropdown SearchEntity
     * @return a role SearchEntity with the filters set to find the matching roles
     */
    public SearchEntity roleDropdown(String dropdownName, String... roleCodes) {
        Filter[] filters = new Filter[roleCodes.length];
        for(int i = 0; i < roleCodes.length; i++) {
            filters[i] = new Filter(Attribute.PRI_CODE, Operator.EQUALS, roleCodes[i]);
        }
        
        return new SearchEntity(SBE_SER_LNK_ROLE, dropdownName)
            .add(new Filter(Attribute.LNK_DEF, Operator.CONTAINS, Definition.DEF_ROLE))
            .add(new Or(filters));
    }

    /**
     * Cache a group of Dropdowns for a given Definition
     * @param productCode - product to cache under
     * @param definitionCode - the {@link Definition#getCode() code} of the definition to cache for
     * @param dropdowns - the dropdowns to cache 
     */
    public void cacheDropdowns(String productCode, String definitionCode, SearchEntity... dropdowns) {
		for(SearchEntity dropdown : dropdowns) {
			dropdown.setRealm(productCode);
			String key = new StringBuilder(definitionCode).append(":").append(dropdown.getCode()).toString();
			cm.putObject(productCode, key, dropdown);
		}
    }

    /**
     * Cache a group of Dropdowns for a given Definition in the configured product code
     * @param definitionCode - the {@link Definition#getCode() code} of the definition to cache for
     * @param dropdowns - the dropdowns to cache
     * 
     * @see {@link SearchBuilder#CONFIG_PRODUCT_CODE}
     */
    public void cacheDropdowns(String definitionCode, SearchEntity... dropdowns) {
        cacheDropdowns(CONFIG_PRODUCT_CODE, definitionCode, dropdowns);
    }
}
