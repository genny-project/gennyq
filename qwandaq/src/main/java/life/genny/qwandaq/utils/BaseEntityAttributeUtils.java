package life.genny.qwandaq.utils;

import java.util.LinkedList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.serialization.baseentityattribute.BaseEntityAttribute;
import life.genny.qwandaq.serialization.baseentityattribute.BaseEntityAttributeKey;

/**
 * A non-static utility class used for standard
 * operations involving BaseEntitys.
 * 
 * @author Varun Shastry
 */
@ApplicationScoped
public class BaseEntityAttributeUtils {
    static final Logger log = Logger.getLogger(BaseEntityAttributeUtils.class);

    /**
	 * Fetch a {@link BaseEntityAttribute} from the cache using a realm:baseEntityCode:attributeCode.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the BaseEntityAttribute to fetch
     * @param attributeCode        The Attribute code of the BaseEntityAttribute to fetch
	 * @return The corresponding BaseEntityAttribute, or null if not found.
	 */
    public BaseEntityAttribute getBaseEntityAttribute(String productCode, String baseEntityCode, String attributeCode) {
        BaseEntityAttributeKey key = new BaseEntityAttributeKey(productCode, baseEntityCode, attributeCode);
        return (BaseEntityAttribute) CacheUtils.getEntity(GennyConstants.CACHE_NAME_BASEENTITY_ATTRIBUTE, key);
    }

    /**
	 * Fetch a list of {@link BaseEntityAttribute} from the cache using a realm:baseEntityCode.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the BaseEntityAttribute to fetch
     * @param attributeCodes        The list of Attribute codes of the BaseEntityAttributes to fetch
	 * @return The list of corresponding BaseEntityAttributes, or empty list if not found.
	 */
    public List<BaseEntityAttribute> getBaseEntityAttributes(String productCode, String baseEntityCode,
            List<String> attributeCodes) {
        List<BaseEntityAttribute> baseEntityAttributes = new LinkedList<>();
        attributeCodes.parallelStream().forEach(attributeCode -> baseEntityAttributes
                .add(getBaseEntityAttribute(productCode, baseEntityCode, attributeCode)));
        return baseEntityAttributes;
    }

    /**
	 * Fetch a {@link BaseEntityAttribute} from the cache using a realm:baseEntityCode:attributeCode.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the BaseEntityAttribute to fetch
	 * @return The corresponding list of all BaseEntityAttributes, or empty list if not found.
	 */
    public List<BaseEntityAttribute> getAllBaseEntityAttributesForBaseEntity(String productCode, String baseEntityCode) {
        return CacheUtils.getBaseEntityAttributesForBaseEntityUsingIckle(productCode, baseEntityCode);
    }

    /**
	 * Fetch a {@link BaseEntityAttribute} from the cache using a realm:baseEntityCode:attributeCode.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the BaseEntityAttribute to fetch
     * @param attributeCode        The Attribute code of the BaseEntityAttribute to fetch
	 * @return The corresponding EntityAttribute, or null if not found.
	 */
    public EntityAttribute getEntityAttribute(String productCode, String baseEntityCode, String attributeCode) {
        return (EntityAttribute) getBaseEntityAttribute(productCode, baseEntityCode, attributeCode).toCoreEntity();
    }

    /**
	 * Fetch a list of {@link BaseEntityAttribute} from the cache using a realm:baseEntityCode and attributeCodes list.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the BaseEntityAttribute to fetch
     * @param attributeCodes        The list of Attribute codes of the BaseEntityAttributes to fetch
	 * @return The list of corresponding EntityAttributes, or empty list if not found.
	 */
    public List<EntityAttribute> getEntityAttributes(String productCode, String baseEntityCode,
            List<String> attributeCodes) {
        List<EntityAttribute> baseEntityAttributes = new LinkedList<>();
        attributeCodes.parallelStream().forEach(attributeCode -> baseEntityAttributes
                .add((EntityAttribute) getBaseEntityAttribute(productCode, baseEntityCode, attributeCode)
                        .toCoreEntity()));
        return baseEntityAttributes;
    }

    /**
	 * Fetch a list of {@link EntityAttribute} from the cache using a realm:baseEntityCode.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the BaseEntityAttributes to fetch
	 * @return The corresponding list of all EntityAttributes, or empty list if not found.
	 */
    public List<EntityAttribute> getAllEntityAttributesForBaseEntity(String productCode, String baseEntityCode) {
        List<EntityAttribute> baseEntityAttributes = new LinkedList<>();
        getAllBaseEntityAttributesForBaseEntity(productCode, baseEntityCode).parallelStream()
                .forEach(baseEntityAttribute -> baseEntityAttributes
                        .add((EntityAttribute) baseEntityAttribute.toCoreEntity()));
        return baseEntityAttributes;
    }
}
