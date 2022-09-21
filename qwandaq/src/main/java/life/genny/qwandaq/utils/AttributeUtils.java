package life.genny.qwandaq.utils;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.serialization.attribute.AttributeKey;

import javax.enterprise.context.ApplicationScoped;

/**
 * A non-static utility class used for standard
 * operations involving Attributes.
 *
 * @author Varun Shastry
 */
@ApplicationScoped
public class AttributeUtils {

    public Attribute getAttributeByCode(String productCode, String attributeCode) {
        AttributeKey key = new AttributeKey(productCode, attributeCode);
        return (Attribute) CacheUtils.getEntity(GennyConstants.CACHE_NAME_ATTRIBUTE, key);
    }
}
