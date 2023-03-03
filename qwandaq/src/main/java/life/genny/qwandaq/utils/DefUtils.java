package life.genny.qwandaq.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.entity.search.trait.Ord;
import life.genny.qwandaq.entity.search.trait.Sort;
import org.jboss.logging.Logger;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.exception.runtime.DefinitionException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.AttributeCodeValueString;
import life.genny.qwandaq.models.UserToken;

/*
 * A static utility class for operations regarding 
 * definition BaseEntitys.
 * 
 * @author Adam Crow
 * @author Jasper Robison
 */
@ApplicationScoped
public class DefUtils {

	public static final String SBE_DEF = "SBE_DEF";
	public static final String SBE_DEFINITION_PREFIX = "SBE_DEFINITION_PREFIX";

	static final Logger log = Logger.getLogger(DefUtils.class);

	static Map<String, Map<String, String>> defPrefixMap = new ConcurrentHashMap<>();

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	CacheManager cm;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	EntityAttributeUtils beaUtils;

	@Inject
	SearchUtils searchUtils;

	@Inject
	UserToken userToken;

	@Inject
	MergeUtils mergeUtils;

	@Inject
	CacheManager cacheManager;

	@Inject
	AttributeUtils attributeUtils;

	public DefUtils() {
	}

	/**
	 * Find the corresponding definition for a given {@link BaseEntity}.
	 *
	 * @param entity The {@link BaseEntity} to check
	 * @return BaseEntity The corresponding definition {@link BaseEntity}
	 */
	public Definition getDEF(final BaseEntity entity) {
		entity.setBaseEntityAttributes(beaUtils.getAllEntityAttributesForBaseEntity(entity));

		if (entity == null)
			throw new NullParameterException("entity");

		// save processing time on particular entities
		if (entity.getCode().startsWith(Prefix.DEF_))
			return Definition.from(entity);

		List<String> codes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(entity, Attribute.LNK_DEF);

		// if no defs specified, go by prefix
		if (codes == null || codes.isEmpty()) {
			String prefix = entity.getCode().substring(0, 3);
			SearchEntity prefixSearch = new SearchEntity(SBE_DEFINITION_PREFIX, "Definition Prefix Search")
					.add(new Filter(Attribute.PRI_PREFIX, Operator.EQUALS, prefix))
					.setAllColumns(true)
					.setPageSize(1)
					.setRealm(userToken.getProductCode());

			List<BaseEntity> results = searchUtils.searchBaseEntitys(prefixSearch);
			if (results == null || results.isEmpty())
				throw new DefinitionException("No definition with prefix: " + prefix);

			return Definition.from(results.get(0));
		}

		// fetch DEF if no merging is needed
		if (codes.size() == 1)
			return beUtils.getDefinition(codes.get(0));

		String mergedCode = "DEF_" + String.join("_", codes);
		BaseEntity mergedDef = new BaseEntity(mergedCode, mergedCode);
		log.info("Detected combination DEF - " + mergedCode);

		// reverse order and begin filling new def
		Collections.reverse(codes);
		for (String code : codes) {

			Definition definition = beUtils.getDefinition(code);

			// merge into new def
			for (EntityAttribute ea : beaUtils.getAllEntityAttributesForBaseEntity(definition, true)) {
				try {
					mergedDef.addAttribute(ea);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return Definition.from(mergedDef);
	}

	/**
	 * Find the corresponding definition for a given {@link BaseEntity} code.
	 *
	 * @param baseEntityCode The {@link BaseEntity} code to check
	 * @return BaseEntity The corresponding definition {@link BaseEntity}
	 */
	public Definition getDEF(final String baseEntityCode) {
		if(baseEntityCode == null)
			throw new NullParameterException(baseEntityCode);
		BaseEntity target = beUtils.getBaseEntity(baseEntityCode);
		return getDEF(target);
	}

	/**
	 * A function to determine the whether or not an attribute is allowed to be
	 * saved to a {@link BaseEntity}.
	 *
	 * @param answer the answer to check
	 * @return Boolean
	 */
	public Boolean answerValidForDEF(Answer answer) {

		if (answer == null)
			throw new NullParameterException("answer");

		BaseEntity target = beUtils.getBaseEntity(answer.getTargetCode());
		Definition definition = getDEF(target);

		return answerValidForDEF(definition, answer);
	}

	/**
	 * A function to determine the whether or not an attribute is allowed to be
	 * saved to a {@link BaseEntity}
	 *
	 * @param definition the defBE to check with
	 * @param answer     the answer to check
	 * @return Boolean
	 */
	public Boolean answerValidForDEF(Definition definition, Answer answer) {

		if (definition == null)
			throw new NullParameterException("definition");
		if (answer == null)
			throw new NullParameterException("answer");

		String targetCode = answer.getTargetCode();
		String attributeCode = answer.getAttributeCode();

		// allow if it is Capability saved to a Role
		if (targetCode.startsWith(Prefix.ROL_) && attributeCode.startsWith(Prefix.PRM_)) {
			return true;
		} else if (targetCode.startsWith(Prefix.SBE_) && (attributeCode.startsWith(Prefix.COL_)
				|| attributeCode.startsWith(Prefix.SRT_) || attributeCode.startsWith(Prefix.ACT_))) {
			return true;
		}

		// just make use of the faster attribute lookup
		EntityAttribute ea = beaUtils.getEntityAttribute(definition.getRealm(), definition.getCode(), Prefix.ATT_ + attributeCode);
		if (ea != null) {
			return true;
		}
		// if not found, we can check in the parent defs
		List<String> parentCodes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(definition, Attribute.LNK_INCLUDE);
		if (parentCodes != null) {
			for (String code : parentCodes) {
				Definition parent = beUtils.getDefinition(code, false);
				if (answerValidForDEF(parent, answer)) {
					return true;
				}
			}
		}
		log.error(ANSIColour.RED + "Invalid attribute " + attributeCode + " for " + answer.getTargetCode()
				+ " with def= " + definition.getCode() + ANSIColour.RESET);
			
		return false;
	}

	/**
	 * Get the prefix for a definition.
	 *
	 * @param productCode
	 * @param definitionCode
	 * @return
	 */
	public String getDefinitionPrefix(String productCode, String definitionCode) {
		EntityAttribute prefixAttr = beaUtils.getEntityAttribute(productCode, definitionCode, Attribute.PRI_PREFIX, false);
		if(prefixAttr == null) {
			// if not found, we can check in the parent defs
			List<String> parentCodes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(definitionCode, Attribute.LNK_INCLUDE);
			if (parentCodes != null) {
				for (String code : parentCodes) {
					try {
						return getDefinitionPrefix(productCode, code);
					} catch (DefinitionException e) {
						continue;
					}
				}
			}
		}
		throw new DefinitionException("No prefix set for the def: " + definitionCode);
	}

	/**
	 * A function to determine the whether or not an attribute and value is allowed
	 * to be
	 * saved to a {@link BaseEntity}
	 *
	 * @param defBE     the defBE to check with
	 * @param acvs     the attribute code value to check
	 * @return Boolean
	 */
	public Boolean attributeValueValidForDEF(BaseEntity defBE, AttributeCodeValueString acvs) {

		if (defBE == null) {
			throw new NullParameterException("defBE");
		}
		if (acvs == null) {
			throw new NullParameterException("acvs");
		}

		Attribute attribute = attributeUtils.getAttribute(acvs.getAttributeCode(), true, true);

		if (attribute == null)
			throw new NullParameterException("attribute");

		// allow if it is Capability saved to a Role
		if (defBE.getCode().equals("DEF_ROLE") && attribute.getCode().startsWith(Prefix.PRM_)) {
			return true;
		} else if (defBE.getCode().equals("DEF_SEARCH")
				&& (attribute.getCode().startsWith(Prefix.COL_) 
				|| attribute.getCode().startsWith(Prefix.SRT_) || attribute.getCode().startsWith(Prefix.ACT_))) {
			return true;
		}

		// just make use of the faster attribute lookup
		if (!defBE.containsEntityAttribute(Prefix.ATT_.concat(attribute.getCode()))) {
			log.error(ANSIColour.RED + "Invalid attribute " + attribute.getCode() + " for "
					+ defBE.getCode() + ANSIColour.RESET);
			return false;
		}

		// Now do a value validation check
		Boolean result = qwandaUtils.validationsAreMet(attribute, acvs.getValue());
		return result;
	}

	/**
	 * Ensure any filter values requiring merging have been handled.
	 *
	 * @param searchBE The {@link SearchEntity} to process
	 * @param ctxMap   Map of merge contexts
	 * @return SearchEntity The updated {@link SearchEntity}
	 */
	@Deprecated
	public SearchEntity mergeFilterValueVariables(SearchEntity searchBE, Map<String, Object> ctxMap) {
		List<EntityAttribute> entityAttributes = beaUtils.getAllEntityAttributesForBaseEntity(searchBE);
		for (EntityAttribute entityAttribute : entityAttributes) {
			// iterate all Filters
			String attributeCode = entityAttribute.getAttributeCode();
			if (attributeCode.startsWith("PRI_") || attributeCode.startsWith("LNK_")) {

				// grab the Attribute for this Code, using array in case this is an associated
				// filter
				String[] attributeCodeArray = attributeCode.split("\\.");
				String attributeCodeLast = attributeCodeArray[attributeCodeArray.length - 1];
				// fetch the corresponding attribute
				Attribute att = attributeUtils.getAttribute(attributeCodeLast, true);
				DataType dataType = att.getDataType();

				Object attributeFilterValue = entityAttribute.getValue();
				if (attributeFilterValue != null) {
					// ensure EntityAttribute Dataype is Correct for Filter
					Attribute searchAtt = new Attribute(attributeCode, entityAttribute.getAttributeName(), dataType);
					entityAttribute.setAttribute(searchAtt);
					String attrValStr = attributeFilterValue.toString();

					// first check if merge is required
					Boolean requiresMerging = mergeUtils.requiresMerging(attrValStr);

					if (requiresMerging != null && requiresMerging) {
						// update Map with latest baseentity
						ctxMap.keySet().forEach(key -> {
							Object value = ctxMap.get(key);
							if (value.getClass().equals(BaseEntity.class)) {
								BaseEntity baseEntity = (BaseEntity) value;
								BaseEntity savedEntity = beUtils.getBaseEntity(baseEntity.getCode());
								if (savedEntity != null)
									baseEntity = savedEntity;
								ctxMap.put(key, baseEntity);
							}
						});

						// check if contexts are present
						if (mergeUtils.contextsArePresent(attrValStr, ctxMap)) {
							// TODO: mergeUtils should be taking care of this bracket replacement - Jasper
							// (6/08/2021)
							Object mergedObj = mergeUtils.wordMerge(attrValStr.replace("[[", "").replace("]]", ""),
									ctxMap);
							// Ensure Datatype is Correct, then set Value
							entityAttribute.setValue(mergedObj);
						} else {
							log.warn(ANSIColour.RED + "Not all contexts are present for " + attrValStr
									+ ANSIColour.RESET);
							return null;
						}
					} else {
						// this should filter out any values of incorrect datatype
						entityAttribute.setValue(attributeFilterValue);
					}
				} else {
					log.error(
							ANSIColour.RED + "Value is NULL for entity attribute " + attributeCode + ANSIColour.RESET);
					return null;
				}
			}
		}

		return searchBE;
	}
}
