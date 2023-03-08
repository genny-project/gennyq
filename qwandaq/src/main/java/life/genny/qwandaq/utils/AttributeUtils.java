package life.genny.qwandaq.utils;

import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.attribute.AttributeKey;
import life.genny.qwandaq.serialization.common.CoreEntityKey;
import life.genny.qwandaq.serialization.datatype.DataTypeKey;
import life.genny.qwandaq.serialization.validation.ValidationKey;
import life.genny.qwandaq.validation.Validation;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        AttributeKey key = new AttributeKey(productCode, code);
        Attribute attribute = (Attribute) cm.getPersistableEntity(GennyConstants.CACHE_NAME_ATTRIBUTE, key);
        if (attribute == null) {
            throw new ItemNotFoundException(productCode, code);
        }
        if(bundleDataType) {
            DataType dataType = getDataType(attribute, bundleValidationList);
            attribute.setDataType(dataType);
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
            throw new ItemNotFoundException(productCode, dttCode);
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

        return getDataType(attribute.getRealm(), attribute.getDttCode(), bundleValidationList);
    }

    public List<Validation> getValidationList(DataType dataType) {
        return cm.getValidations(dataType.getRealm(), dataType.getValidationCodes());
    }

    /**
     * @param validation
     */
    public void saveValidation(Validation validation) {
        ValidationKey key = new ValidationKey(validation.getRealm(), validation.getCode());
        cm.saveEntity(GennyConstants.CACHE_NAME_VALIDATION, key, validation);
    }

    /**
     * @param dataType
     */
    public void saveDataType(DataType dataType) {
        DataTypeKey key = new DataTypeKey(dataType.getRealm(), dataType.getDttCode());
        cm.saveEntity(GennyConstants.CACHE_NAME_DATATYPE, key, dataType);
    }

    /**
     * @param attribute
     */
    public void saveAttribute(Attribute attribute) {
        AttributeKey key = new AttributeKey(attribute.getRealm(), attribute.getCode());
        cm.saveEntity(GennyConstants.CACHE_NAME_ATTRIBUTE, key, attribute);
    }

    /**
     * @param attributes
     */
    public void saveAttributes(List<Attribute> attributes) {
        Map<CoreEntityKey, CoreEntityPersistable> attributesMap = new HashMap<>(attributes.size());
        for (Attribute attr : attributes) {
            attributesMap.put(new AttributeKey(attr.getRealm(), attr.getCode()), attr);
        }
        cm.saveEntities(GennyConstants.CACHE_NAME_ATTRIBUTE, attributesMap);
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
     * @return
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

    public Long getAttributesLastUpdatedAt() {
        Long entityLastUpdatedAt = cm.getEntityLastUpdatedAt(GennyConstants.CACHE_NAME_ATTRIBUTE);
        if(entityLastUpdatedAt != null)
            return entityLastUpdatedAt;
        entityLastUpdatedAt = System.currentTimeMillis();
        updateAttributesLastUpdatedAt(entityLastUpdatedAt);
        return entityLastUpdatedAt;
    }

    public void updateAttributesLastUpdatedAt(Long updatedTime) {
        cm.updateEntityLastUpdatedAt(GennyConstants.CACHE_NAME_ATTRIBUTE, updatedTime);
    }
}
