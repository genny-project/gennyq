package life.genny.qwandaq.utils;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.attribute.AttributeKey;
import life.genny.qwandaq.serialization.datatype.DataTypeKey;
import life.genny.qwandaq.serialization.validation.ValidationKey;
import life.genny.qwandaq.validation.Validation;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.List;
import java.util.Map;

/**
 * A non-static utility class used for standard
 * operations involving Attributes.
 *
 * @author Varun Shastry
 */
@ApplicationScoped
public class AttributeUtils {
    Jsonb jsonb = JsonbBuilder.create();
    @Inject
    Logger log;

    @Inject
    UserToken userToken;

    @Inject
    CacheManager cm;

    /**
     * Create and save a new id-safe {@link Attribute} object (if it does not already exist)
     * @param productCode - product to store attribute in
     * @param attributeCode - attribute code of the attribute
     * @param dataType - datatype of the attribute
     * @return the new attribute object, or the currently existing one if found in the cache
     * @throws ItemNotFoundException if the existing Attribute's DataType is unable to be found
     */
    public Attribute getOrCreateAttribute(String productCode, String attributeCode, DataType dataType) {
        return getOrCreateAttribute(productCode, attributeCode, dataType, Map.of());
    }

    /**
     * Create and save a new id-safe {@link Attribute} object (if it does not already exist)
     * @param productCode - product to store attribute in
     * @param attributeCode - attribute code of the attribute
     * @param dataType - datatype of the attribute
     * @param opts - extra optional flags/properties to add to the attribute. If not set, will use defaults based on the Attribute class
     * <p>Current opts:\n
     *  <ul>
     *      <li>boolean privacy</li>
     *      <li>String description</li>
     *      <li>String help</li>
     *      <li>String placeholder</li>
     *      <li>String defaultValue</li>
     *      <li>String icon</li>
     *  </ul>
     * </p>
     * @return the new attribute object, or the currently existing one if found in the cache
     * @throws ItemNotFoundException if the existing
     */
    public Attribute getOrCreateAttribute(String productCode, String attributeCode, DataType dataType, Map<String, String> opts) {
        Attribute attribute;
        try {
            attribute = getAttribute(productCode, attributeCode, true);
            log.trace("Found existing attribute: " + productCode + ":" + attribute.getCode() + " with id: " + attribute.getId() + ". Stopping creation");
            return attribute;
        } catch(ItemNotFoundException e) {
            log.debug("Creating attribute: " + productCode + ":" + attributeCode + " using datatype: " + dataType.getDttCode());
        }

        long id = cm.getMaxAttributeId() + 1;
        attribute = new Attribute(attributeCode, opts.getOrDefault("name", attributeCode));
        attribute.setId(id);
        attribute.setDataType(dataType);
        attribute.setRealm(productCode);

        // bunch of optional niceties (respecting defaults from Attribute class)
        attribute.setDefaultPrivacyFlag(Boolean.parseBoolean(opts.getOrDefault("privacy", "true")));
        attribute.setDescription(opts.getOrDefault("description", null));
        attribute.setHelp(opts.getOrDefault("help", null));
        attribute.setPlaceholder(opts.getOrDefault("placeholder", null));
        attribute.setDefaultValue(opts.getOrDefault("defaultvalue", null));
        attribute.setIcon(opts.getOrDefault("icon", null));
        saveAttribute(attribute);
        return attribute;
    }

    /**
     * @param productCode
     * @param code
     * @return
     */
    public Validation getValidation(String productCode, String code) {
        ValidationKey key = new ValidationKey(productCode, code);
        Validation validation = (Validation) cm.getPersistableEntity(GennyConstants.CACHE_NAME_VALIDATION, key);
        if (validation == null) {
            throw new ItemNotFoundException(productCode, code);
        }
        return validation;
    }

    /**
     * Retrieve an Attribute from Infinispan Cache using the user's product code
     * @param code - the code of the requested attribute
     * @return The Attribute without its {@link Attribute#getDataType() DataType}
     * 
     * @throws {@link ItemNotFoundException} if Attribute cannot be found in the user product's attribute cache
     * 
     * @see {@link UserToken#getProductCode() User Product Code}
     * @see {@link DataType}
     */
    public Attribute getAttribute(String code) {
        return getAttribute(userToken.getProductCode(), code);
    }

    /**
     * Retrieve an Attribute from Infinispan Cache using the user's product code
     * @param code - the code of the requested attribute
     * @param bundleDataType - whether or not to include the {@link DataType} in the Attribute object without its {@link DataType#getValidationList() Validation List}
     * @return The Attribute and optionally the DataType, but no Validation List
     * 
     * @throws {@link ItemNotFoundException} if Attribute cannot be found in the user product's attribute cache
     * 
     * @see {@link UserToken#getProductCode() User Product Code}
     * @see {@link DataType}
     */
    public Attribute getAttribute(String code, boolean bundleDataType) {
        return getAttribute(userToken.getProductCode(), code, bundleDataType);
    }

    /**
     * Retrieve an Attribute from Infinispan Cache using the user's product code
     * @param code - the code of the requested attribute
     * @param bundleDataType - whether or not to include the {@link DataType} in the Attribute object
     * @param bundleValidationList - whether or not to include the {@link DataType#getValidationList() Validation List} in the DataType object of the attribute
     * @return The Attribute and optionally the DataType and associated Validation List
     * 
     * @throws {@link ItemNotFoundException} if Attribute cannot be found in the user product's attribute cache
     * 
     * @see {@link UserToken#getProductCode() User Product Code}
     * @see {@link DataType}
     * @see {@link Validation}
     */
    public Attribute getAttribute(String code, boolean bundleDataType, boolean bundleValidationList) {
        return getAttribute(userToken.getProductCode(), code, bundleDataType, bundleValidationList);
    }


    /**
     * Retrieve an Attribute from Infinispan Cache
     * @param productCode - the product code the attribute belongs in
     * @param code - the code of the requested attribute
     * @return The Attribute without its DataType
     * 
     * @throws {@link ItemNotFoundException} if Attribute cannot be found in the requested attribute cache
     * @see {@link DataType}
     */
    public Attribute getAttribute(String productCode, String code) {
        return getAttribute(productCode, code, false);
    }

    /**
     * Retrieve an Attribute from Infinispan Cache
     * @param productCode - the product code the attribute belongs in
     * @param code - the code of the requested attribute
     * @param bundleDataType - whether or not to include the {@link DataType} in the Attribute object without its {@link DataType#getValidationList() Validation List}
     * @return The Attribute and optionally the DataType, but no Validation List
     * 
     * @throws {@link ItemNotFoundException} if Attribute cannot be found in the requested attribute cache
     * @see {@link DataType}
     * @see {@link Validation}
     */
    public Attribute getAttribute(String productCode, String code, boolean bundleDataType) {
        return getAttribute(productCode, code, bundleDataType, false);
    }

    /**
     * Retrieve an Attribute from Infinispan Cache
     * @param productCode - the product code the attribute belongs in
     * @param code - the code of the requested attribute
     * @param bundleDataType - whether or not to include the {@link DataType} in the Attribute object
     * @param bundleValidationList - whether or not to include the {@link DataType#getValidationList() Validation List} in the DataType object of the attribute
     * @return The Attribute and optionally the DataType and associated Validation List
     * 
     * @throws {@link ItemNotFoundException} if Attribute cannot be found in the requested attribute cache
     * @see {@link DataType}
     * @see {@link Validation}
     */
    public Attribute getAttribute(String productCode, String code, boolean bundleDataType, boolean bundleValidationList) {
        Attribute attribute = cm.getAttribute(productCode, code);
        if (attribute == null) {
            throw new ItemNotFoundException(productCode, "attribute: " + code);
        }
        if(bundleDataType) {
            try {
            DataType dataType = getDataType(attribute, bundleValidationList);
            attribute.setDataType(dataType);
            } catch(ItemNotFoundException e) {
                log.error("DataType not found for Attribute: " + code);
                log.error("DataType code: " + attribute.getDttCode());
                throw new ItemNotFoundException("DataType not found for attribute: " + productCode + ":" + code, e);
            }
        }
        return attribute;
    }

    /**
	   * Retrieve a DataType from the specified cache
	   *
     * @param productCode - product to retrieve the datatype from
     * @param dttCode - code of the datatype
     * @param bundleValidationList - whether or not to include its {@link DataType#getValidationList()}
     * @return the datatype and optionally its Validation List
     * 
     * @throws {@link ItemNotFoundException} if the DataType cannot be found
     * @see {@link Validation}
     */
    public DataType getDataType(String productCode, String dttCode, boolean bundleValidationList) {
        DataTypeKey key = new DataTypeKey(productCode, dttCode);
        DataType dataType = (DataType) cm.getPersistableEntity(GennyConstants.CACHE_NAME_DATATYPE, key);
        if(dataType == null) {
            throw new ItemNotFoundException(productCode, "DataType Code: " + dttCode);
        }
        if (bundleValidationList) {
            List<Validation> validationList = getValidationList(dataType);
            dataType.setValidationList(validationList);
        }
        return dataType;
	}

    /**
	   * Retrieve a DataType for an {@link Attribute} from the Infinispan cache that is tied to the Attribute (same product)
	   * Will return DataType already attached to the specified Attribute object if one exists, otherwise will check cache
     * 
     * @param attribute - attribute to retrieve the datatype of
     * @param bundleValidationList - whether or not to include the DataType's {@link DataType#getValidationList()}
     * @return the datatype tied to the attribute (if any) and optionally its Validation List
     * 
     * @throws {@link ItemNotFoundException} if the DataType cannot be found
     * @see {@link Validation}
     */
    public DataType getDataType(Attribute attribute, boolean bundleValidationList) {
        if(attribute == null) {
            throw new NullParameterException("attribute");
        }
        if(attribute.getDataType() != null) {
            return attribute.getDataType();
        }
        DataType dataType = getDataType(attribute.getRealm(), attribute.getDttCode(), bundleValidationList);
        if(dataType == null) {
            throw new ItemNotFoundException("productCode: " + attribute.getRealm() + ". DataType attached to Attribute: " + attribute.getCode() + ". DataType Code: " + attribute.getDttCode());
        }
        return dataType;
    }

    /**
     * Get the datatype for an attribute using an attribute.
     *
     * @param dataType
     * @return List of validations associated with a data type
     */
    public List<Validation> getValidationList(DataType dataType) {
        return cm.getValidations(dataType.getRealm(), dataType.getValidationCodes());
    }

    /**
     * @param validation The validation to be saved
     */
    public void saveValidation(Validation validation) {
        String productCode = validation.getRealm();
        ValidationKey key = new ValidationKey(productCode, validation.getCode());
        cm.saveEntity(GennyConstants.CACHE_NAME_VALIDATION, key, validation);
        updateAttributesLastUpdatedAt(productCode, System.currentTimeMillis());
    }

    /**
     * @param dataType The data type to be saved
     */
    public void saveDataType(DataType dataType) {
        String productCode = dataType.getRealm();
        DataTypeKey key = new DataTypeKey(productCode, dataType.getDttCode());
        cm.saveEntity(GennyConstants.CACHE_NAME_DATATYPE, key, dataType);
        updateAttributesLastUpdatedAt(productCode, System.currentTimeMillis());
    }

    /**
     * @param attribute The attribute to be saved
     */
    public void saveAttribute(Attribute attribute) {
        String productCode = attribute.getRealm();
        AttributeKey key = new AttributeKey(productCode, attribute.getCode());
        cm.saveEntity(GennyConstants.CACHE_NAME_ATTRIBUTE, key, attribute);
        updateAttributesLastUpdatedAt(productCode, System.currentTimeMillis());
    }

	/**
     * Fetch all attributes for a product.
     *
     * @return Collection of all attributes in the system across all products
     */
    public List<Attribute> getAllAttributes() {
        return cm.getAllAttributes();
    }

    /**
     * Fetch all attributes for a product.
     *
     * @param productCode
     * @return Collection of all attributes for a product.
     */
    public List<Attribute> getAttributesForProduct(String productCode) {
        return cm.getAttributesForProduct(productCode);
    }

    /**
     * Fetch all attributes with a given prefix value in code for a product.
     *
     * @param prefix
     * @return
     */
    public List<Attribute> getAttributesWithPrefix(String prefix) {
        return cm.getAttributesWithPrefix(prefix);
    }

    /**
     * Fetch all attributes with a given prefix value in code for a product.
     *
     * @param productCode
     * @param prefix
     * @return
     */
    public List<Attribute> getAttributesWithPrefixForProduct(String productCode, String prefix) {
        return cm.getAttributesWithPrefixForProduct(productCode, prefix);
    }

    public Long getAttributesLastUpdatedAt(String productCode) {
        Long entityLastUpdatedAt = cm.getEntityLastUpdatedAt(GennyConstants.CACHE_NAME_ATTRIBUTE, productCode);
        if(entityLastUpdatedAt != null)
            return entityLastUpdatedAt;
        entityLastUpdatedAt = System.currentTimeMillis();
        updateAttributesLastUpdatedAt(productCode, entityLastUpdatedAt);
        return entityLastUpdatedAt;
    }

    public void updateAttributesLastUpdatedAt(String productCode, Long updatedTime) {
        cm.updateEntityLastUpdatedAt(GennyConstants.CACHE_NAME_ATTRIBUTE, productCode, updatedTime);
    }
}
