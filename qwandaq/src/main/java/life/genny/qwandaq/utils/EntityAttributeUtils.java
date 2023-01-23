package life.genny.qwandaq.utils;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.serialization.entityattribute.EntityAttributeKey;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;

/**
 * A non-static utility class used for standard
 * operations involving BaseEntityAttributes.
 * 
 * @author Varun Shastry
 */
@ApplicationScoped
public class EntityAttributeUtils {
    static final Logger log = Logger.getLogger(EntityAttributeUtils.class);

	@Inject
	CacheManager cm;

	/**
	 * Fetch a {@link EntityAttribute} from the cache using a realm:baseEntityCode:attributeCode.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the EntityAttribute to fetch
	 * @param attributeCode        The Attribute code of the EntityAttribute to fetch
	 * @return The corresponding EntityAttribute with embedded "Attribute", or null if not found.
	 */
	public EntityAttribute getEntityAttribute(String productCode, String baseEntityCode, String attributeCode) {
		return getEntityAttribute(productCode, baseEntityCode, attributeCode, true);
	}

    /**
	 * Fetch a {@link EntityAttribute} from the cache using a realm:baseEntityCode:attributeCode.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the EntityAttribute to fetch
     * @param attributeCode        The Attribute code of the EntityAttribute to fetch
     * @param embedAttribute       Defines if "Attribute" will be embedded into the returned EntityAttribute
	 * @return The corresponding BaseEntityAttribute, or null if not found.
	 */
    public EntityAttribute getEntityAttribute(String productCode, String baseEntityCode, String attributeCode, boolean embedAttribute) {
        EntityAttributeKey key = new EntityAttributeKey(productCode, baseEntityCode, attributeCode);
        EntityAttribute entityAttribute = (EntityAttribute) cm.getPersistableEntity(CacheManager.CACHE_NAME_BASEENTITY_ATTRIBUTE, key);
		if (embedAttribute && entityAttribute != null) {
			entityAttribute.setAttribute(cm.getAttribute(productCode, attributeCode));
		}
		return entityAttribute;
    }

	/**
	 * Fetch a {@link EntityAttribute} from the cache using a realm:baseEntityCode:attributeCode.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the BaseEntityAttribute to fetch
	 * @param attributeCode        The Attribute code of the BaseEntityAttribute to fetch
	 * @return The corresponding BaseEntityAttribute, or null if not found.
	 */
	public life.genny.qwandaq.serialization.entityattribute.EntityAttribute getSerializableEntityAttribute(String productCode, String baseEntityCode, String attributeCode) {
		EntityAttributeKey key = new EntityAttributeKey(productCode, baseEntityCode, attributeCode);
		return (life.genny.qwandaq.serialization.entityattribute.EntityAttribute) getEntityAttribute(productCode, baseEntityCode, attributeCode).toSerializableCoreEntity();
	}

	/**
	 * Update a {@link EntityAttribute} in the cache
	 *
	 * @param baseEntityAttribute        The BaseEntityAttribute to be updated
	 * @return True if update is successful, false otherwise.
	 */
	public boolean updateEntityAttribute(EntityAttribute baseEntityAttribute) {
		EntityAttributeKey key = new EntityAttributeKey(baseEntityAttribute.getRealm(), baseEntityAttribute.getBaseEntityCode(), baseEntityAttribute.getAttributeCode());
		return cm.saveEntity(CacheManager.CACHE_NAME_BASEENTITY_ATTRIBUTE, key, baseEntityAttribute);
	}

    /**
	 * Fetch a list of {@link EntityAttribute} from the cache using realm:baseEntityCode:attributeCodes.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the BaseEntityAttribute to fetch
     * @param attributeCodes        The list of Attribute codes of the BaseEntityAttributes to fetch
	 * @return The list of corresponding BaseEntityAttributes, or empty list if not found.
	 */
	public List<life.genny.qwandaq.serialization.entityattribute.EntityAttribute> getSerializableEntityAttributes(String productCode, String baseEntityCode,
																												  List<String> attributeCodes) {
		List<life.genny.qwandaq.serialization.entityattribute.EntityAttribute> baseEntityAttributes = new LinkedList<>();
		attributeCodes.parallelStream().forEach(attributeCode -> baseEntityAttributes
				.add(getSerializableEntityAttribute(productCode, baseEntityCode, attributeCode)));
		return baseEntityAttributes;
	}

    /**
	 * Fetch a list of {@link EntityAttribute} from the cache using a realm:baseEntityCode and attributeCodes list.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the BaseEntityAttribute to fetch
     * @param attributeCodes        The list of Attribute codes of the BaseEntityAttributes to fetch
	 * @return The list of corresponding EntityAttributes, or empty list if not found.
	 */
    public List<EntityAttribute> getPersistableEntityAttributes(String productCode, String baseEntityCode,
																							 List<String> attributeCodes) {
		List<EntityAttribute> baseEntityAttributes = new LinkedList<>();
		attributeCodes.parallelStream().forEach(attributeCode -> baseEntityAttributes
				.add(getEntityAttribute(productCode, baseEntityCode, attributeCode)));
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
        return getAllEntityAttributesForBaseEntity(productCode, baseEntityCode, true);
    }

	/**
	 * Fetch a list of {@link EntityAttribute} from the cache using a realm:baseEntityCode.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the BaseEntityAttributes to fetch
	 * @return The corresponding list of all EntityAttributes, or empty list if not found.
	 */
	public List<EntityAttribute> getAllEntityAttributesForBaseEntity(String productCode, String baseEntityCode, boolean embedAttribute) {
		List<EntityAttribute> baseEntityAttributes = new LinkedList<>();
		cm.getAllBaseEntityAttributesForBaseEntity(productCode, baseEntityCode).stream()
				.forEach((ea) -> {
					if(embedAttribute) {
						Attribute attribute = cm.getAttribute(productCode, ea.getAttributeCode());
						if (attribute == null) {
							log.debugf("Attribute not found for BaseEntityAttribute [%s:%s:%s]", productCode, baseEntityCode, ea.getAttributeCode());
							throw new ItemNotFoundException(productCode, ea.getAttributeCode());
						}
						log.debugf("Attribute embedded into BaseEntityAttribute [%s:%s:%s]", productCode, baseEntityCode, ea.getAttributeCode());
						ea.setAttribute(attribute);
					}
					baseEntityAttributes.add(ea);
				});
		return baseEntityAttributes;
	}
}
