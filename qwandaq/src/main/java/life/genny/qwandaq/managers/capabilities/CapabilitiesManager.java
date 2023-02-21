package life.genny.qwandaq.managers.capabilities;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.HEntityAttribute;
import life.genny.qwandaq.entity.HBaseEntity;
import life.genny.qwandaq.intf.ICapabilityFilterable;
import life.genny.qwandaq.utils.*;
import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;

import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.managers.Manager;
import life.genny.qwandaq.managers.capabilities.role.RoleManager;
import life.genny.qwandaq.models.UserToken;

/*
 * A non-static utility class for managing roles and capabilities.
 * 
 * @author Jasper Robison
 * @author Bryn Meachem
 */
@ApplicationScoped
public class CapabilitiesManager extends Manager {

	@Inject
	Logger log;

	@Inject
	private RoleManager roleMan;

	@Inject
	CacheManager cm;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	QuestionUtils questionUtils;

	@Inject
	AttributeUtils attributeUtils;

	public CapabilitiesManager() {
		super();
	}

	// == TODO LIST
	// 1. I want to get rid of the productCode chain here. When we have multitenancy
	// properly established this should be possible
	// but until then this is my best bet for getting this working reliably (don't
	// trust the tokens just yet, as service token has productCode improperly set)

	/**
	 * Return a Set of Capabilities based on a BaseEntity's LNK_ROLE and its own set
	 * of capabilities
	 * 
	 * @param target - the BaseEntity to fetch user capabilities for
	 * 
	 * @return a new {@link CapabilitySet} (a HashSet of Capabilities with knowledge of the target)
	 */
	@Deprecated(forRemoval = false)
	public CapabilitySet getUserCapabilities(BaseEntity target) {
// this is a necessary log, since we are trying to minimize how often this
		// function is called
		// it is good to see how often it comes up
		log.debug("[!][!] Generating new User Capabilities for " + userToken.getUserCode());

		List<BaseEntity> roles = roleMan.getRoles(target);
		CapabilitySet capabilities;
		
		if(!roles.isEmpty()) {
			log.debug("User Roles:");
			
			BaseEntity role = roles.get(0);
			capabilities = getEntityCapabilities(role);
			for (int i = 1; i < roles.size(); i++) {
				role = roles.get(i);
				CapabilitySet roleCaps = getEntityCapabilities(role);
				// Being careful about accidentally duplicating capabilities
				// (given the nature of the hashCode and equals methods in Capability.java)
				for (Capability cap : roleCaps) {
					// Find preexisting capability. If it exists, merge the Nodes in the way that
					// grants the most permission possible
					Capability preexistingCap = cap.hasCodeInSet(capabilities);
					if (preexistingCap != null) {
						capabilities.remove(preexistingCap);
						cap = preexistingCap.merge(cap, true);
					}
					capabilities.add(cap);
				}
			}
		} else {
			capabilities = new CapabilitySet(target);
		}

		// Now overwrite with user capabilities
		CapabilitySet userCapabilities = getEntityCapabilities(target);
		for (Capability capability : userCapabilities) {
			// Try and find a preexisting capability to overwrite.
			// If it exists, remove so we can override the role-based capability
			Capability otherCapability = capability.hasCodeInSet(capabilities);
			if (otherCapability != null) {
				capabilities.remove(otherCapability);
				capability = otherCapability.merge(capability, false);
			}
			capabilities.add(capability);
		}
		return capabilities;
	}

	/**
	 * Return a Set of Capabilities based on a BaseEntity's LNK_ROLE and its own set
	 * of capabilities
	 * @return a new {@link CapabilitySet} (a HashSet of Capabilities with knowledge of the target)
	 */
	@Deprecated(forRemoval = false)
	public CapabilitySet getUserCapabilities() {
		return getUserCapabilities(beUtils.getUserBaseEntity());
	}

	/**
	 * Get a single entity's capabilities (excluding roles)
	 *
	 * @param target
	 * @return
	 */
	public CapabilitySet getEntityCapabilities(final BaseEntity target) {
		Set<EntityAttribute> capabilities = new HashSet<>(beaUtils.getBaseEntityAttributesForBaseEntityWithAttributeCodePrefix(target.getRealm(), target.getCode(), Prefix.CAP_));
		log.debug("		- " + target.getCode() + "(" + capabilities.size() + " capabilities)");
		if(capabilities.isEmpty()) {
			return new CapabilitySet(target);
		}
		CapabilitySet cSet = new CapabilitySet(target);
		cSet.addAll(capabilities.stream()
			.map((EntityAttribute ea) -> {
				log.trace("	[!] " + ea.getAttributeCode() + " = " + ea.getValueString());
				return Capability.getFromEA(ea);
			})
			.collect(Collectors.toSet()));
		return cSet;
	}

	/**
	 * Add a capability to a BaseEntity.
	 * 
	 * @param productCode The product code
	 * @param target      The target entity
	 * @param capability        The capability attribute
	 * @param nodes       The nodes to set
	 */
	private void updateCapability(String productCode, BaseEntity target, final Attribute capability,
			final CapabilityNode... nodes) {
		// Update base entity
		if (capability == null) {
			throw new NullParameterException("capability");
		}

		if (target == null) {
			throw new NullParameterException("target");
		}

		EntityAttribute addedAttribute = target.addAttribute(capability, 0.0, getModeString(nodes));
		beaUtils.updateEntityAttribute(addedAttribute);
		beUtils.updateBaseEntity(target);
	}

	private void updateCapability(String productCode, BaseEntity target, final Attribute capability,
			final List<CapabilityNode> modeList) {
		// Update base entity
		if (capability == null) {
			throw new NullParameterException("capability");
		}

		if (target == null) {
			throw new NullParameterException("target");
		}

		EntityAttribute addedAttribute = target.addAttribute(capability, 0.0, getModeString(modeList));
		beaUtils.updateEntityAttribute(addedAttribute);
		beUtils.updateBaseEntity(target);
	}

	public Attribute createCapability(final String productCode, final String rawCapabilityCode, final String name) {
		return createCapability(productCode, rawCapabilityCode, name, false);
	}

	public Attribute createCapability(final String productCode, final String rawCapabilityCode, final String name,
			boolean cleanedCode) {
		String cleanCapabilityCode = cleanedCode ? rawCapabilityCode : cleanCapabilityCode(rawCapabilityCode);
		Attribute attribute = null;
		try {
			attribute = attributeUtils.getAttribute(productCode, cleanCapabilityCode, true);
		} catch(ItemNotFoundException e) {
			log.debug("Could not find Attribute: " + cleanCapabilityCode + ". Creating new Capability");
		}

		if (attribute == null) {
			log.trace("Creating Capability : " + cleanCapabilityCode + " : " + name);
			attribute = new Attribute(cleanCapabilityCode, name, new DataType(String.class));
			attribute.setRealm(productCode);
			attributeUtils.saveAttribute(attribute);
		}

		return attribute;
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity targetBe, Attribute capabilityAttribute,
			final CapabilityNode... modes) {
		if (capabilityAttribute == null) {
			throw new ItemNotFoundException(productCode, "Capability Attribute");
		}

		// Check the user token has required capabilities
		// if (!shouldOverride()) {
		// 	log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " + capabilityAttribute.getCode()
		// 			+ " TO BASE ENTITITY: " + targetBe.getCode());
		// 	return targetBe;
		// }

		// ===== Old capability check ===
		// if (!hasCapability(cleanCapabilityCode, true, modes)) {
		// log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " +
		// cleanCapabilityCode
		// + " TO BASE ENTITITY: " + targetBe.getCode());
		// return targetBe;
		// }

		updateCapability(productCode, targetBe, capabilityAttribute, modes);
		return targetBe;
	}

	public BaseEntity removeCapabilityFromBaseEntity(String productCode, BaseEntity targetBe, String capabilityCode) {
		capabilityCode = cleanCapabilityCode(capabilityCode);
		Attribute attr = qwandaUtils.getAttribute(productCode, capabilityCode);
		return removeCapabilityFromBaseEntity(productCode, targetBe, attr);
	}

	public BaseEntity removeCapabilityFromBaseEntity(String productCode, BaseEntity targetBe, Attribute capabilityAttribute) {
		if (capabilityAttribute == null) {
			throw new ItemNotFoundException(productCode, "Capability Attribute");
		}


		targetBe.addAttribute(capabilityAttribute, 0.0, "[]");
		cm.putObject(productCode, targetBe.getCode() + ":" + capabilityAttribute.getCode(), "[]");
		beUtils.updateBaseEntity(targetBe);
		return targetBe;
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity targetBe, Attribute capabilityAttribute,
			final List<CapabilityNode> modes) {
		if (capabilityAttribute == null) {
			throw new ItemNotFoundException(productCode, "Capability Attribute");
		}

		// Check the user token has required capabilities
		// if (!shouldOverride()) {
		// 	log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " + capabilityAttribute.getCode()
		// 			+ " TO BASE ENTITITY: " + targetBe.getCode());
		// 	return targetBe;
		// }

		// ===== Old capability check ===
		// if (!hasCapability(cleanCapabilityCode, true, modes)) {
		// log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " +
		// cleanCapabilityCode
		// + " TO BASE ENTITITY: " + targetBe.getCode());
		// return targetBe;
		// }

		updateCapability(productCode, targetBe, capabilityAttribute, modes);
		return targetBe;
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity targetBe, final String rawCapabilityCode,
			final CapabilityNode... modes) {
		// Ensure the capability is well defined
		String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);

		// Don't need to catch here since we don't want to create
		Attribute attribute = attributeUtils.getAttribute(productCode, cleanCapabilityCode, true);

		return addCapabilityToBaseEntity(productCode, targetBe, attribute, modes);
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity target, final String rawCapCode,
			final List<CapabilityNode> capabilityList) {
		// Ensure the capability is well defined
		String cleanCapabilityCode = cleanCapabilityCode(rawCapCode);

		// Don't need to catch here since we don't want to create
		Attribute attribute = attributeUtils.getAttribute(productCode, cleanCapabilityCode, true);
		return addCapabilityToBaseEntity(productCode, target, attribute, capabilityList);
	}

	public static Capability deserializeCapability(String capabilityCode, String modeString) {
		List<CapabilityNode> caps = deserializeCapArray(modeString);
		return new Capability(capabilityCode, caps);
	}

	/**
	 * Deserialise a stringified array of modes to a set of {@link CapabilityNode}
	 * 
	 * @param modeString
	 * @return
	 */
	@Deprecated
	public static Set<CapabilityNode> deserializeCapSet(String modeString) {
		return CommonUtils.getSetFromString(modeString, CapabilityNode::parseNode);
	}

	/**
	 * Deserialise a stringified array of modes to an array of
	 * {@link CapabilityNode}
	 * 
	 * @param modeString
	 * @return
	 */
	@Deprecated
	public static List<CapabilityNode> deserializeCapArray(String modeString) {
		return CommonUtils.getListFromString(modeString, CapabilityNode::parseNode);
	}

	/**
	 * Clean a raw capability code.
	 * Prepends the Capability Code Prefix if missing and forces uppercase
	 * 
	 * @param rawCapabilityCode
	 * @return
	 */
	public static String cleanCapabilityCode(final String rawCapabilityCode) {
		String cleanCapabilityCode = rawCapabilityCode.toUpperCase();
		if (!cleanCapabilityCode.startsWith(Prefix.CAP_)) {
			cleanCapabilityCode = Prefix.CAP_ + cleanCapabilityCode;
		}

		return cleanCapabilityCode;
	}

	/**
	 * Generate a capability map from a 2D string of attributes
	 * 
	 * @param productCode - product code to create capability attributes from
	 * @param attribData  - 2D array of Strings (each entry in the first array is an
	 *                    array of 2 strings, one for name and one for code)
	 *                    - e.g [ ["CAP_ADMIN", "Manipulate Admin"], ["CAP_STAFF",
	 *                    "Manipulate Staff"]]
	 * @return a map going from attribute code (capability code) to attribute
	 *         (capability)
	 */
	public Map<String, Attribute> getCapabilityAttributeMap(String productCode, String[][] attribData) {
		Map<String, Attribute> capabilityMap = new HashMap<String, Attribute>();

		Arrays.asList(attribData).stream()
				// Map data to capability. If capability name/tag is missing then use the code
				// with standard capitalisation
				.map((String[] item) -> createCapability(productCode, item[0],
						(item[1] != null ? item[1] : CommonUtils.normalizeString(item[0]))))
				// add each capability attribute to the capability map, stripping the CAP_
				// prefix to be used with the constants
				.forEach((Attribute attr) -> capabilityMap.put(attr.getCode(), attr));

		return capabilityMap;
	}

	/**
	 * Serialize an array of {@link CapabilityNode}s to a string
	 * 
	 * @param capabilities
	 * @return
	 */
	public static String getModeString(CapabilityNode... capabilities) {
		return CommonUtils.getArrayString(capabilities, (capability) -> capability.toString());
	}

	public static String getModeString(Collection<CapabilityNode> capabilities) {
		return CommonUtils.getArrayString(capabilities, (capability) -> capability.toString());
	}

	private boolean shouldOverride() {
		// allow keycloak admin and devcs to do anything
		return (userToken.hasRole("service", "admin", "dev") || ("service".equals(userToken.getUsername())));
	}

	// For use in builder patterns
	public RoleManager getRoleManager() {
		return roleMan;
	}

	/**
	 * @param filterable
	 * @param capabilityRequirements
	 * @return
	 */
	public boolean updateCapabilityRequirements(String realm, ICapabilityFilterable filterable, Capability... capabilityRequirements) {
		if(capabilityRequirements == null) {
			log.error("Attempted to set Capability Requirements to null. Call updateCapabilityRequirements(filterable) instead of updateCapabilityRequirements(filterable, null)");
			throw new NullParameterException("capabilityRequirements");
		}

		filterable.setCapabilityRequirements(capabilityRequirements);

		// TODO: Turn this into a sustainable solution

		if(filterable instanceof HBaseEntity) {
			HBaseEntity be = (HBaseEntity)filterable;
			log.info("Attaching Capability Requirements: " + CommonUtils.getArrayString(capabilityRequirements) + " to BaseEntity: " + realm + ":" + be.getCode());
			beUtils.updateBaseEntity(be.toBaseEntity());
			return true;
		}

		if(filterable instanceof QuestionQuestion) {
			QuestionQuestion qq = (QuestionQuestion)filterable;
			log.info("Attaching Capability Requirements: " + CommonUtils.getArrayString(capabilityRequirements) + " to QuestionQuestion: " + realm + ":" + qq.getParentCode() + ":" + qq.getChildCode());
			// TODO: Potentially update sub questions
			return questionUtils.saveQuestionQuestion(qq);
		}

		if(filterable instanceof Question) {
			Question q = (Question)filterable;
			log.info("Attaching Capability Requirements: " + CommonUtils.getArrayString(capabilityRequirements) + " to Question: " + realm + ":" + q.getCode());
			return questionUtils.saveQuestion(q);
		}

		if(filterable instanceof HEntityAttribute) {
			HEntityAttribute ea = (HEntityAttribute)filterable;
			log.info("Attaching Capability Requirements: " + CommonUtils.getArrayString(capabilityRequirements) + " to EntityAttribute: " + realm + ":" + ea.getBaseEntityCode() + ":" + ea.getAttributeCode());
			HBaseEntity be = ea.getBaseEntity();
			beUtils.updateBaseEntity(be.toBaseEntity());
			return true;
		}
		return false;
	}
}
