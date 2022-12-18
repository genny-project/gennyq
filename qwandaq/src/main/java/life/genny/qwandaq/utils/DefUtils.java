package life.genny.qwandaq.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

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
import life.genny.qwandaq.entity.search.trait.Ord;
import life.genny.qwandaq.entity.search.trait.Sort;
import life.genny.qwandaq.exception.runtime.DefinitionException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
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
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	SearchUtils searchUtils;

	@Inject
	UserToken userToken;

	public DefUtils() {
	}

	/**
	 * Initialize the in memory DEF store
	 *
	 * @param productCode The product of DEFs to initialize
	 */
	// TODO: remove this soon
	@Deprecated
	public void initializeDefPrefixs(String productCode) {

		SearchEntity searchEntity = new SearchEntity(SBE_DEF, "DEF check")
				.add(new Sort(Attribute.PRI_NAME, Ord.ASC))
				.add(new Filter(Attribute.PRI_CODE, Operator.STARTS_WITH, Prefix.DEF))
				.setPageStart(0)
				.setPageSize(10000);

		log.info("########  PRODUCT CODE ===== " + productCode);
		searchEntity.setRealm(productCode);

		log.info("Search Entity: " + jsonb.toJson(searchEntity));

		List<String> codes = searchUtils.searchBaseEntityCodes(searchEntity);

		if (codes == null) {
			log.error("Could not fetch DEF codes!");
			return;
		}

		log.info("DEF code search returned " + codes.size() + " results for product " + productCode);
		defPrefixMap.put(productCode, new ConcurrentHashMap<>());

		for (String code : codes) {

			if (code == null) {
				log.error("Code is null, skipping DEF prefix");
				continue;
			}
			Definition def = Definition.from(beUtils.getBaseEntity(productCode, code));
			String prefix = def.getValue(Attribute.PRI_PREFIX, null);
			if (prefix == null)
				continue;

			log.info("(" + productCode + ") Saving Prefix for " + def.getCode());
			defPrefixMap.get(productCode).put(prefix, code);
			CacheUtils.putObject(productCode, def.getCode() + ":PREFIX", prefix);
		}
	}

	/**
	 * Find the corresponding definition for a given {@link BaseEntity}.
	 *
	 * @param entity The {@link BaseEntity} to check
	 * @return BaseEntity The corresponding definition {@link BaseEntity}
	 */
	public Definition getDEF(final BaseEntity entity) {

		if (entity == null)
			throw new NullParameterException("entity");

		// save processing time on particular entities
		if (entity.getCode().startsWith(Prefix.DEF))
			return Definition.from(entity);

		List<String> codes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(entity, Attribute.LNK_DEF);

		// if no defs specified, go by prefix
		if ((codes == null) || codes.isEmpty()) {
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
			for (EntityAttribute ea : definition.getBaseEntityAttributes()) {
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
		if (targetCode.startsWith(Prefix.ROL) && attributeCode.startsWith(Prefix.PRM)) {
			return true;
		} else if (targetCode.startsWith(Prefix.SBE) && (attributeCode.startsWith(Prefix.COL)
				|| attributeCode.startsWith(Prefix.SRT) || attributeCode.startsWith(Prefix.ACT))) {
			return true;
		}

		// just make use of the faster attribute lookup
		if (!definition.containsEntityAttribute(Prefix.ATT + attributeCode)) {
			log.error(ANSIColour.RED + "Invalid attribute " + attributeCode + " for " + answer.getTargetCode()
					+ " with def= " + definition.getCode() + ANSIColour.RESET);
			return false;
		}
		return true;
	}

	/**
	 * A function to determine the whether or not an attribute and value is allowed
	 * to be
	 * saved to a {@link BaseEntity}
	 *
	 * @param defBE     the defBE to check with
	 * @param attribute the attribute to check
	 * @param value     the value to check
	 * @return Boolean
	 */
	public Boolean attributeValueValidForDEF(BaseEntity defBE, AttributeCodeValueString acvs) {

		if (defBE == null)
			throw new NullParameterException("defBE");

		if (acvs == null)
			throw new NullParameterException("acvs");

		Attribute attribute = qwandaUtils.getAttribute(acvs.getAttributeCode());

		if (attribute == null)
			throw new NullParameterException("attribute");

		// allow if it is Capability saved to a Role
		if (defBE.getCode().equals("DEF_ROLE") && attribute.getCode().startsWith("PRM_")) {
			return true;
		} else if (defBE.getCode().equals("DEF_SEARCH")
				&& (attribute.getCode().startsWith("COL_") || attribute.getCode().startsWith("CAL_")
						|| attribute.getCode().startsWith("SRT_") || attribute.getCode().startsWith("ACT_"))) {
			return true;
		}

		// just make use of the faster attribute lookup
		if (!defBE.containsEntityAttribute("ATT_" + attribute.getCode())) {
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
	public SearchEntity mergeFilterValueVariables(SearchEntity searchBE, Map<String, Object> ctxMap) {

		for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {
			// iterate all Filters
			if (ea.getAttributeCode().startsWith("PRI_") || ea.getAttributeCode().startsWith("LNK_")) {

				// grab the Attribute for this Code, using array in case this is an associated
				// filter
				String[] attributeCodeArray = ea.getAttributeCode().split("\\.");
				String attributeCode = attributeCodeArray[attributeCodeArray.length - 1];
				// fetch the corresponding attribute
				Attribute att = qwandaUtils.getAttribute(attributeCode);
				DataType dataType = att.getDataType();

				Object attributeFilterValue = ea.getValue();
				if (attributeFilterValue != null) {
					// ensure EntityAttribute Dataype is Correct for Filter
					Attribute searchAtt = new Attribute(ea.getAttributeCode(), ea.getAttributeName(), dataType);
					ea.setAttribute(searchAtt);
					String attrValStr = attributeFilterValue.toString();

					// first check if merge is required
					Boolean requiresMerging = MergeUtils.requiresMerging(attrValStr);

					if (requiresMerging != null && requiresMerging) {
						// update Map with latest baseentity
						ctxMap.keySet().forEach(key -> {
							Object value = ctxMap.get(key);
							if (value.getClass().equals(BaseEntity.class)) {
								BaseEntity baseEntity = (BaseEntity) value;
								BaseEntity savedEntity = beUtils.getBaseEntityByCode(baseEntity.getCode());
								if (savedEntity != null)
									baseEntity = savedEntity;
								ctxMap.put(key, baseEntity);
							}
						});

						// check if contexts are present
						if (MergeUtils.contextsArePresent(attrValStr, ctxMap)) {
							// TODO: mergeUtils should be taking care of this bracket replacement - Jasper
							// (6/08/2021)
							Object mergedObj = MergeUtils.wordMerge(attrValStr.replace("[[", "").replace("]]", ""),
									ctxMap);
							// Ensure Datatype is Correct, then set Value
							ea.setValue(mergedObj);
						} else {
							log.warn(ANSIColour.RED + "Not all contexts are present for " + attrValStr
									+ ANSIColour.RESET);
							return null;
						}
					} else {
						// this should filter out any values of incorrect datatype
						ea.setValue(attributeFilterValue);
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
