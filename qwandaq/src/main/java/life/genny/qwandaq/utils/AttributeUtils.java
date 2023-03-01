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
     * @param code
     * @return
     */
    public Attribute getAttribute(String code) {
        return getAttribute(userToken.getProductCode(), code);
    }

    /**
     * @param code
     * @return
     */
    public Attribute getAttribute(String code, boolean bundleDataType) {
        return getAttribute(userToken.getProductCode(), code, bundleDataType);
    }

    /**
     * @param code
     * @return
     */
    public Attribute getAttribute(String code, boolean bundleDataType, boolean bundleValidationList) {
        return getAttribute(userToken.getProductCode(), code, bundleDataType, bundleValidationList);
    }

    /**
     * @param productCode
     * @param code
     * @return
     */
    public Attribute getAttribute(String productCode, String code) {
        return getAttribute(productCode, code, false);
    }

    /**
     * @param productCode
     * @param code
     * @return
     */
    public Attribute getAttribute(String productCode, String code, boolean bundleDataType) {
        return getAttribute(productCode, code, bundleDataType, false);
    }

    /**
     * @param productCode
     * @param code
     * @return
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
	 * Get the datatype for an attribute using an attribute code.
	 *
     * @param attributeCode
     * @param bundleValidationList
     * @return
     */
    public DataType getDataType(String attributeCode, boolean bundleValidationList) {
		return getDataType(getAttribute(attributeCode), bundleValidationList);
	}

    /**
	 * Get the datatype for an attribute using an attribute.
	 *
     * @param attribute
     * @param bundleValidationList
     * @return
     */
    public DataType getDataType(Attribute attribute, boolean bundleValidationList) {
        if(attribute == null) {
            throw new NullParameterException("attribute");
        }

        DataTypeKey key = new DataTypeKey(attribute.getRealm(), attribute.getDttCode());
        DataType dataType = (DataType) cm.getPersistableEntity(GennyConstants.CACHE_NAME_DATATYPE, key);

        if(dataType == null) {
            throw new ItemNotFoundException("DataType attached to Attribute: " + attribute.getCode());
        }

        if (bundleValidationList) {
            List<Validation> validationList = getValidationList(dataType);
            dataType.setValidationList(validationList);
        }
        return dataType;
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
}
