package life.genny.qwandaq.managers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.attribute.AttributeKey;

/**
 * A non-static utility class used for standard
 * operations involving Attributes.
 *
 * @author Varun Shastry
 */
@ApplicationScoped
public class AttributeManager {

	@Inject
	UserToken userToken;

	@Inject
	CacheManager cm;

    public Attribute getAttribute(String code) {
		return getAttribute(userToken.getUserCode(), code);
    }

    public Attribute getAttribute(String productCode, String code) {
        AttributeKey key = new AttributeKey(productCode, code);
        return (Attribute) cm.getPersistableEntity(CacheManager.CACHE_NAME_ATTRIBUTE, key);
    }

}
