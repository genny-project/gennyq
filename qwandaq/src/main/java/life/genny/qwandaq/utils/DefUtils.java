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
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.models.ANSIColour;
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

	static final Logger log = Logger.getLogger(DefUtils.class);

	static Map<String, Map<String, String>> defPrefixMap = new ConcurrentHashMap<>();

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	UserToken userToken;

	public DefUtils() { }

	/**
	 * Initialize the in memory DEF store
	 *
	 * @param productCode The product of DEFs to initialize
	 */
	public void initializeDefPrefixs(String productCode) {

		SearchEntity searchBE = new SearchEntity("SBE_DEF", "DEF check")
				.addSort("PRI_NAME", "Created", SearchEntity.Sort.ASC)
				.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "DEF_%")
				.setPageStart(0)
				.setPageSize(10000);

		searchBE.setRealm(productCode);

		List<String> codes = beUtils.getBaseEntityCodes(searchBE);

		if (codes == null) {
			log.error("Could not fetch DEF codes!");
			return;
		}

		log.info("DEF code search returned " + codes.size() + " results for product " + productCode);
		defPrefixMap.put(productCode, new ConcurrentHashMap<String, String>());

		for (String code : codes) {
			if (code == null) {
				log.error("Code is null, skipping DEF prefix");
				continue;
			}

			BaseEntity def = beUtils.getBaseEntityByCode(productCode, code);
			if (def == null) {
				log.error("Def is null!");
				continue;
			}

			String prefix = def.getValue("PRI_PREFIX", null);
			if (prefix == null) {
				continue;
			}

			log.info("(" + productCode + ") Saving Prefix for " + def.getCode());
			defPrefixMap.get(productCode).put(prefix, code);
		}
	}

	/**
	 * Find the corresponding definition for a given {@link BaseEntity}.
	 *
	 * @param entity The {@link BaseEntity} to check
	 * @return BaseEntity The corresponding definition {@link BaseEntity}
	 */
	public BaseEntity getDEF(final BaseEntity entity) {

		if (entity == null) {
			log.error("The passed BaseEntity is NULL, supplying trace");
			return null;
		}

		// save processing time on particular entities
		if (entity.getCode().startsWith("DEF_")) {
			return entity;
		}
		if (entity.getCode().startsWith("PRJ_")) {
			return beUtils.getBaseEntityByCode("DEF_PROJECT");
		}

		// NOTE: temporary special check for internmatch
		String productCode = userToken.getProductCode();
		if (productCode.equals("alyson") || productCode.equals("internmatch")) {
			return getInternmatchDEF(entity);
		}

		List<String> roles = beUtils.getBaseEntityCodeArrayFromLinkAttribute(entity, "LNK_ROLE");

		// null/empty check the role attribute
		if (roles == null) {
			log.error("Entity " + entity.getCode() + " does not contain LNK_ROLE attribute");
			return null;
		}
		if (roles.isEmpty()) {
			log.error("LNK_ROLE is empty for " + entity.getCode());
			return null;
		}

		// fetch DEF if no merging is needed
		if (roles.size() == 1) {
			return beUtils.getBaseEntityByCode("DEF_"+roles.get(0).substring("ROL_".length()));
		}

		String mergedCode = "DEF_" + String.join("_", roles);
		BaseEntity mergedDef = new BaseEntity(mergedCode, mergedCode);
		log.info("Detected combination DEF - " + mergedCode);

		// reverse order and begin filling new def
		Collections.reverse(roles);
		for (String role : roles) {

			BaseEntity def = beUtils.getBaseEntityByCode("DEF_"+role.substring("ROL_".length()));
			if (def == null) {
				log.warn("No DEF for " + role);
				continue;
			}

			// merge into new def
			for (EntityAttribute ea : def.getBaseEntityAttributes()) {
				try {
					mergedDef.addAttribute(ea);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return mergedDef;
	}

	/**
	 * Find the corresponding definition for a given {@link BaseEntity}. 
	 * NOTE: Temporary special method for Internmatch only.
	 *
	 * @param entity The {@link BaseEntity} to check
	 * @return BaseEntity The corresponding definition {@link BaseEntity}
	 */
	public BaseEntity getInternmatchDEF(final BaseEntity entity) {

		String productCode = userToken.getProductCode();

		List<EntityAttribute> isAs = entity.findPrefixEntityAttributes("PRI_IS_");

		// remove the non DEF ones
		Iterator<EntityAttribute> i = isAs.iterator();
		while (i.hasNext()) {

			EntityAttribute ea = i.next();

			if (ea.getAttributeCode().startsWith("PRI_IS_APPLIED_")) {
				i.remove();
				continue;
			}

			// filter out bad is as attributes
			switch (ea.getAttributeCode()) {
				case "PRI_IS_DELETED":
				case "PRI_IS_EXPANDABLE":
				case "PRI_IS_FULL":
				case "PRI_IS_INHERITABLE":
				case "PRI_IS_PHONE":
				case "PRI_IS_AGENT_PROFILE_GRP":
				case "PRI_IS_BUYER_PROFILE_GRP":
				case "PRI_IS_EDU_PROVIDER_STAFF_PROFILE_GRP":
				case "PRI_IS_REFERRER_PROFILE_GRP":
				case "PRI_IS_SELLER_PROFILE_GRP":
				case "PRI_IS SKILLS":
				case "PRI_IS_DISABLED":
				case "PRI_IS_LOGBOOK":
					log.warn("getDEF -> detected non DEFy attributeCode " + ea.getAttributeCode());
					i.remove();
					break;
				default:
			}
		}

		if (!isAs.isEmpty()) {

			// create sorted merge code
			List<String> codes = isAs.stream()
					.sorted(Comparator.comparingDouble(EntityAttribute::getWeight))
					.map(EntityAttribute::getAttributeCode)
					.collect(Collectors.toList());

			log.info(codes.toString());

			// check for single PRI_IS
			if (codes.size() == 1) {
				BaseEntity def = beUtils.getBaseEntityByCode("DEF_"+codes.get(0).substring("PRI_IS_".length()));
				return def;
			}

			String mergedCode = "DEF_" + String.join("_", codes);
			BaseEntity mergedDef = new BaseEntity(mergedCode, mergedCode);
			log.info("Detected NEW Combination DEF - " + mergedCode);

			// reverse order and begin filling new def
			Collections.reverse(codes);
			for (String code : codes) {

				// get def for PRI_IS
				BaseEntity def = beUtils.getBaseEntityByCode("DEF_"+code.substring("PRI_IS_".length()));
				if (def == null) {
					continue;
				}

				// merge into new def
				for (EntityAttribute ea : def.getBaseEntityAttributes()) {
					try {
						mergedDef.addAttribute(ea);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			return mergedDef;
		}

		// search for a def with same prefix
		String prefix = entity.getCode().substring(0, 3);
		log.info("Prefix = " + prefix);

		Map<String, String> map = defPrefixMap.get(productCode);
		String defCode = map.get(prefix);

		BaseEntity def = beUtils.getBaseEntityByCode(defCode);

		if (def != null) {
			return def;
		}

		// default to error def
		log.error("No DEF associated with entity " + entity.getCode());
		return new BaseEntity("ERR_DEF", "No DEF");
	}

	/**
	 * A function to determine the whether or not an attribute is allowed to be
	 * saved to a {@link BaseEntity}.
	 *
	 * @param answer the answer to check
	 * @return Boolean
	 */
	public Boolean answerValidForDEF(Answer answer) {

		BaseEntity target = beUtils.getBaseEntityByCode(answer.getTargetCode());
		if (target == null) {
			log.error("TargetCode " + answer.getTargetCode() + " does not exist");
			return false;
		}
		BaseEntity defBE = getDEF(target);

		return answerValidForDEF(defBE, answer);
	}

	/**
	 * A function to determine the whether or not an attribute is allowed to be
	 * saved to a {@link BaseEntity}
	 *
	 * @param defBE  the defBE to check with
	 * @param answer the answer to check
	 * @return Boolean
	 */
	public Boolean answerValidForDEF(BaseEntity defBE, Answer answer) {

		String targetCode = answer.getTargetCode();
		String attributeCode = answer.getAttributeCode();

		// allow if it is Capability saved to a Role
		if (targetCode.startsWith("ROL_") && attributeCode.startsWith("PRM_")) {
			return true;
		} else if (targetCode.startsWith("SBE_")
				&& (attributeCode.startsWith("COL_") || attributeCode.startsWith("CAL_")
						|| attributeCode.startsWith("SRT_") || attributeCode.startsWith("ACT_"))) {
			return true;
		}

		if (defBE == null) {
			log.error("Cannot work out DEF " + answer.getTargetCode());
			return true;
		}

		// just make use of the faster attribute lookup
		if (!defBE.containsEntityAttribute("ATT_" + attributeCode)) {
			log.error(ANSIColour.RED + "Invalid attribute " + attributeCode + " for " + answer.getTargetCode()
					+ " with def= " + defBE.getCode() + ANSIColour.RESET);
			return false;
		}
		return true;
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
								ctxMap.put(key, beUtils.getBaseEntityByCode(baseEntity.getCode()));
							}
						});
						// check if contexts are present
						if (MergeUtils.contextsArePresent(attrValStr, ctxMap)) {
							// TODO: mergeUtils should be taking care of this bracket replacement - Jasper
							// (6/08/2021)
							Object mergedObj = MergeUtils.wordMerge(attrValStr.replace("[[", "").replace("]]", ""),
									ctxMap);
							// Ensure Dataype is Correct, then set Value
							ea.setValue(mergedObj);
						} else {
							log.error(ANSIColour.RED + "Not all contexts are present for " + attrValStr
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
