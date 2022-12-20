package life.genny.qwandaq.managers;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.serialization.attribute.AttributeKey;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * A non-static utility class used for standard
 * operations involving Attributes.
 *
 * @author Varun Shastry
 */
@ApplicationScoped
public class AttributeManager {

	@Inject
	CacheManager cm;

    public Attribute getAttributeByCode(String productCode, String attributeCode) {
        AttributeKey key = new AttributeKey(productCode, attributeCode);
        return (Attribute) cm.getPersistableEntity(GennyConstants.CACHE_NAME_ATTRIBUTE, key);
    }
}
