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
     * Get the attribute using the code and productCode wherein the data type is not bundled
     *
     * @param code
     * @return The attribute identified by the code without data type
     */
    public Attribute getAttribute(String code) {
        return getAttribute(userToken.getProductCode(), code);
    }

    /**
     * Get the attribute using the code wherein the data type is optionally bundled
     *
     * @param code
     * @return The attribute identified by the code with optionally bundled data type
     */
    public Attribute getAttribute(String code, boolean bundleDataType) {
        return getAttribute(userToken.getProductCode(), code, bundleDataType);
    }

    /**
     * Get the attribute using the code wherein the data type and validation lists are optionally bundled
     *
     * @param code
     * @return The attribute identified by the code with optionally bundled data type and validation lists
     */
    public Attribute getAttribute(String code, boolean bundleDataType, boolean bundleValidationList) {
        return getAttribute(userToken.getProductCode(), code, bundleDataType, bundleValidationList);
    }

    /**
     * Get the attribute using the code and productCode wherein the data type is not bundled
     *
     * @param productCode
     * @param code
     * @return The attribute identified by the code and productCode without data type
     */
    public Attribute getAttribute(String productCode, String code) {
        return getAttribute(productCode, code, false);
    }

    /**
     * Get the attribute using the code and productCode wherein the data type is optionally bundled without validation lists
     *
     * @param productCode
     * @param code
     * @param bundleDataType
     * @return The attribute identified by the code with optionally bundled data type and without validation lists
     */
    public Attribute getAttribute(String productCode, String code, boolean bundleDataType) {
        return getAttribute(productCode, code, bundleDataType, false);
    }

    /**
     * Get the attribute using the code and productCode wherein the data type and validation lists are optionally bundled
     *
     * @param productCode
     * @param code
     * @param bundleDataType
     * @param bundleValidationList
     * @return The attribute identified by code and productCode optionally bundled data type and validation lists
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
	 * Get the datatype using the dttCode and productCode
	 *
     * @param productCode
     * @param dttCode
     * @param bundleValidationList
     * @return The data type identified by dttCode and productCode with optionally bundled validation lists
     */
    public DataType getDataType(String productCode, String dttCode, boolean bundleValidationList) {
        DataTypeKey key = new DataTypeKey(productCode, dttCode);
        DataType dataType = (DataType) cm.getPersistableEntity(GennyConstants.CACHE_NAME_DATATYPE, key);
        if(dataType == null) {
            throw new ItemNotFoundException("productCode: " + productCode + ". DataType Code: " + dttCode);
        }
        if (bundleValidationList) {
            List<Validation> validationList = getValidationList(dataType);
            dataType.setValidationList(validationList);
        }
        return dataType;
	}

    /**
	 * Get the datatype for an attribute using an attribute.
	 *
     * @param attribute
     * @param bundleValidationList
     * @return The data type associated with the attribute with optionally bundled validation lists
     */
    public DataType getDataType(Attribute attribute, boolean bundleValidationList) {
        if(attribute == null) {
            throw new NullParameterException("attribute");
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
