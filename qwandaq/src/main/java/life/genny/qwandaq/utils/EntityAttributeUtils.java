package life.genny.qwandaq.utils;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.serialization.common.CoreEntityKey;
import life.genny.qwandaq.serialization.entityattribute.EntityAttributeKey;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	AttributeUtils attributeUtils;

	/**
	 * Fetch an EntityAttribute value from the cache.
	 *
	 * @param pcm        The BaseEntity to fetch for
	 * @param attributeCode        The Attribute code of the EntityAttribute to fetch
	 * @return The corresponding EntityAttribute with embedded "Attribute", or null if not found.
	 */
	public <T> T getValue(PCM pcm, String attributeCode) {
		return getValue((BaseEntity) pcm, attributeCode);
	}

	/**
	 * Fetch an EntityAttribute value from the cache.
	 *
	 * @param baseEntity        The BaseEntity to fetch for
	 * @param attributeCode        The Attribute code of the EntityAttribute to fetch
	 * @return The corresponding EntityAttribute with embedded "Attribute", or null if not found.
	 */
	public <T> T getValue(BaseEntity baseEntity, String attributeCode) {
		EntityAttribute ea = getEntityAttribute(baseEntity.getRealm(), baseEntity.getCode(), attributeCode, true, true);
		if (ea == null) {
			throw new ItemNotFoundException(baseEntity.getRealm(), baseEntity.getCode(), attributeCode);
		}
		return ea.getValue();
	}

	/**
	 * Fetch a {@link EntityAttribute} from the cache using a realm:baseEntityCode:attributeCode.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the EntityAttribute to fetch
	 * @param attributeCode        The Attribute code of the EntityAttribute to fetch
	 * @return The corresponding EntityAttribute with no embedded "Attribute"
	 * 
	 * @throws {@link ItemNotFoundException} if no EntityAttribute can be found
	 */
	public EntityAttribute getEntityAttribute(String productCode, String baseEntityCode, String attributeCode) {
		return getEntityAttribute(productCode, baseEntityCode, attributeCode, false);
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
		return getEntityAttribute(productCode, baseEntityCode, attributeCode, embedAttribute, false);
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
	public EntityAttribute getEntityAttribute(String productCode, String baseEntityCode, String attributeCode, boolean embedAttribute, boolean embedDataType) {
		return getEntityAttribute(productCode, baseEntityCode, attributeCode, embedAttribute, embedDataType, false);
	}

	/**
	 * Fetch a {@link EntityAttribute} from the cache using a realm:baseEntityCode:attributeCode.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the EntityAttribute to fetch
	 * @param attributeCode        The Attribute code of the EntityAttribute to fetch
	 * @param embedAttribute       Defines if "Attribute" will be embedded into the returned EntityAttribute
	 * @return The corresponding BaseEntityAttribute
	 * 
	 * @throws {@link ItemNotFoundException} if:
	 * 	<ul>
	 * 		<li>the corresponding {@link EntityAttribute} cannot be found</li>
	 * 		<li>the corresponding {@link Attribute} cannot be found and the attribute has been requested <bembedAttribute</b></li>
	 * 		<li>the corresponding {@link DataType}" cannot be found and the dataType and attribute has been requested <b>embedAttribute && embedDataType</b></li>
	 *  </ul>
	 * 
	 * @see {@link AttributeUtils#getAttribute(String, String, Boolean, Boolean) AttributeUtils.getAttribute}
	 * @see {@link AttributeUtils#getDataType}
	 */
	public EntityAttribute getEntityAttribute(String productCode, String baseEntityCode, String attributeCode, boolean embedAttribute, boolean embedDataType, boolean embedValidationInfo) {
		EntityAttributeKey key = new EntityAttributeKey(productCode, baseEntityCode, attributeCode);
		EntityAttribute entityAttribute = (EntityAttribute) cm.getPersistableEntity(GennyConstants.CACHE_NAME_BASEENTITY_ATTRIBUTE, key);

		if(entityAttribute == null) {
			throw new ItemNotFoundException(productCode, "EntityAttribute: " + baseEntityCode + ":" + attributeCode);
		}

		if (embedAttribute) {
			entityAttribute.setAttribute(attributeUtils.getAttribute(productCode, attributeCode, embedDataType, embedValidationInfo));
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
		return cm.saveEntity(GennyConstants.CACHE_NAME_BASEENTITY_ATTRIBUTE, key, baseEntityAttribute);
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
	 * @param baseEntity The baseEntity to use
	 * @return The corresponding list of all EntityAttributes, or empty list if not found.
	 */
	public List<EntityAttribute> getAllEntityAttributesForBaseEntity(BaseEntity baseEntity) {
		return getAllEntityAttributesForBaseEntity(baseEntity, false);
	}

	/**
	 * Fetch a list of {@link EntityAttribute} from the cache using a realm:baseEntityCode.
	 *
	 * @param baseEntity The baseEntity to use
	 * @param embedAttribute Whether attribute needs to be fetched and embedded in the base entity attribute
	 * @return The corresponding list of all EntityAttributes, or empty list if not found.
	 */
	public List<EntityAttribute> getAllEntityAttributesForBaseEntity(BaseEntity baseEntity, boolean embedAttribute) {
		return getAllEntityAttributesForBaseEntity(baseEntity.getRealm(), baseEntity.getCode(), embedAttribute);
	}

    /**
	 * Fetch a list of {@link EntityAttribute} from the cache using a realm:baseEntityCode.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the BaseEntityAttributes to fetch
	 * @return The corresponding list of all EntityAttributes, or empty list if not found.
	 */
    public List<EntityAttribute> getAllEntityAttributesForBaseEntity(String productCode, String baseEntityCode) {
        return getAllEntityAttributesForBaseEntity(productCode, baseEntityCode, false);
    }

	/**
	 * Fetch a list of {@link EntityAttribute} from the cache using a realm:baseEntityCode.
	 *
	 * @param productCode The productCode to use
	 * @param baseEntityCode        The BaseEntity code of the BaseEntityAttributes to fetch
	 * @param embedAttribute Whether attribute needs to be fetched and embedded in the base entity attributes
	 * @return The corresponding list of all EntityAttributes, or empty list if not found.
	 */
	public List<EntityAttribute> getAllEntityAttributesForBaseEntity(String productCode, String baseEntityCode, boolean embedAttribute) {
		List<EntityAttribute> entityAttributes = cm.getAllBaseEntityAttributesForBaseEntity(productCode, baseEntityCode);
		if(!embedAttribute) {
			return entityAttributes;
		}
		return embedAttributesInEntityAttributes(entityAttributes);
	}

	@NotNull
	public List<EntityAttribute> embedAttributesInEntityAttributes(List<EntityAttribute> entityAttributes) {
		List<EntityAttribute> entityAttributesWithEmbeddedAttributes = new LinkedList<>();
		entityAttributes.stream()
				.forEach((ea) -> {
					String productCode = ea.getRealm();
					String baseEntityCode = ea.getBaseEntityCode();
					Attribute attribute = attributeUtils.getAttribute(productCode, ea.getAttributeCode());
					if (attribute == null) {
						log.debugf("Attribute not found for BaseEntityAttribute [%s:%s:%s]", productCode, baseEntityCode, ea.getAttributeCode());
						throw new ItemNotFoundException(productCode, ea.getAttributeCode());
					}
					log.debugf("Attribute embedded into BaseEntityAttribute [%s:%s:%s]", productCode, baseEntityCode, ea.getAttributeCode());
					ea.setAttribute(attribute);
				});
		return entityAttributesWithEmbeddedAttributes;
	}

	/**
	 * Get a list of {@link EntityAttribute}s to from cache for a BaseEntity.
	 *
	 * @param productCode - Product Code / Cache to retrieve from
	 * @param baseEntityCode - Base Entity code to use
	 * @param attributeCodePrefix - Attribute Code Prefix to use
	 * @return a list of base entities with matching prefixes
	 *
	 * See Also: {@link BaseEntityKey}, {@link CoreEntityKey#fromKey}, {@link CacheManager#getEntitiesByPrefix}
	 */
	public List<EntityAttribute> getBaseEntityAttributesForBaseEntityWithAttributeCodePrefix(String productCode, String baseEntityCode, String attributeCodePrefix) {
		return cm.getBaseEntityAttributesForBaseEntityWithAttributeCodePrefix(productCode, baseEntityCode, attributeCodePrefix);
	}

    public void removeBaseEntityAttributesForBaseEntity(BaseEntity baseEntity) {
		removeBaseEntityAttributesForBaseEntity(baseEntity.getRealm(), baseEntity.getCode());
    }

	public void removeBaseEntityAttributesForBaseEntity(String productCode, String baseEntityCode) {
		cm.removeAllEntityAttributesOfBaseEntity(productCode, baseEntityCode);
	}

	/**
	 * Remove an entity attribute for a given product
	 * @param productCode
	 * @param baseEntityCode
	 * @param attributeCode
	 * @return the number of changed entities (if any)
	 */
	public int removeBaseEntityAttribute(String productCode, String baseEntityCode, String attributeCode) {
		return cm.removeEntityAttribute(productCode, baseEntityCode, attributeCode);
	}
}
