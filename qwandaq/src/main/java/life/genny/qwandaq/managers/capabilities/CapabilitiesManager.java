package life.genny.qwandaq.managers.capabilities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import life.genny.qwandaq.exception.checked.RoleException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.managers.Manager;
import life.genny.qwandaq.managers.capabilities.role.RoleManager;
import life.genny.qwandaq.models.ANSIColour;

/*
 * A non-static utility class for managing roles and capabilities.
 * 
 * @author Jasper Robison
 * @author Bryn Meachem
 */
@ApplicationScoped
public class CapabilitiesManager extends Manager {

	protected static final Set<String> ACCEPTED_PREFIXES = Set.of(Prefix.ROL_, Prefix.PER_);

	@Inject
	Logger log;

	@Inject
	private RoleManager roleMan;

	public CapabilitiesManager() {
		super();
	}

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
		if(target == null) {
			log.warn("[Capabilities] Got null target for capability set compilation. Returning blank capabilities");
			return new CapabilitySet(target);
		}
		
		
		validateTarget(target);
		
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
		validateTarget(target);

		Set<EntityAttribute> capabilities = new HashSet<>(beaUtils.getBaseEntityAttributesForBaseEntityWithAttributeCodePrefix(target.getRealm(), target.getCode(), Prefix.CAP_));
		log.debug("\t- " + target.getCode() + "(" + capabilities.size() + " capabilities)");
		if(capabilities.isEmpty()) {
			return new CapabilitySet(target);
		}
		CapabilitySet cSet = new CapabilitySet(target);
		cSet.addAll(capabilities.stream()
			.filter(ea -> ea != null)
			.map((EntityAttribute ea) -> {
				log.trace("\t[!] " + ea.getAttributeCode() + " = " + ea.getValueString());
				return Capability.getFromEA(ea);
			})
			.collect(Collectors.toSet()));
		return cSet;
	}

	/**
	 * Add a capability to a BaseEntity.
	 * 
	 * @param CONFIG_PRODUCT_CODE The product code
	 * @param target      The target entity
	 * @param capability        The capability attribute
	 * @param nodes       The nodes to set
	 */
	private void updateCapability(BaseEntity target, final Attribute capability,
			final CapabilityNode... nodes) {
		validateTarget(target);

		EntityAttribute addedAttribute = target.addAttribute(capability, 0.0, CommonUtils.getArrayString(nodes));
		beaUtils.updateEntityAttribute(addedAttribute);
	}

	private void updateCapability(BaseEntity target, final Attribute capability,
			final List<CapabilityNode> modeList) {
		// Update base entity
		if (capability == null) {
			throw new NullParameterException("capability");
		}

		if (target == null) {
			throw new NullParameterException("target");
		}

		EntityAttribute addedAttribute = target.addAttribute(capability, 0.0, CommonUtils.getArrayString(modeList));
		beaUtils.updateEntityAttribute(addedAttribute);
		beUtils.updateBaseEntity(target);
	}

	/**
	 * Validate whether or not a given target can accept capabilities as entity attributes
	 * @param target - the target base entity
	 * @throws RoleException if the target is not a valid base entity
	 */
	private void validateTarget(BaseEntity target) throws RoleException {
		validateTargetCode(target.getCode());
	}

	/**
	 * Validate whether or not a given target code can accept capabilities as entity attributes
	 * @param targetCode - the code of the target base entity
	 * @throws RoleException if the target is not a valid base entity
	 */
	private void validateTargetCode(String targetCode) 
		throws RoleException {
		// This feels pretty primitive and can be amplified with BFS later if necessary.
		// If this begins to break (e.g more prefixes are needed we can move to LNK_INCLUDE quite easily)
		if(!ACCEPTED_PREFIXES.contains(targetCode.substring(0, 4)))
			throw new RoleException(ANSIColour.doColour(targetCode + " IS NOT A CAPABILITY BEARING BASE ENTITY. PLEASE USE ANYTHING WITH THE FOLLOWING PREFIXES: " + CommonUtils.getArrayString(ACCEPTED_PREFIXES), ANSIColour.RED));
	}

	public Attribute createCapability(final String productCode, final String rawCapabilityCode, final String name) {
		return createCapability(productCode, rawCapabilityCode, name, false);
	}

	/**
	 * Create a new Capability Attribute
	 * @param productCode - the product to store it in
	 * @param rawCapabilityCode - the (potentially unrefined) capability code
	 * @param name - the name of the capability
	 * @param cleanedCode - whether or not the rawCapabilityCode has already been system cleaned (false as default)
	 * @return the new (saved) capability attribute if it doesn't already exit
	 * @throws ItemNotFoundException - if the {@link DataType#DTT_CAPABILITY DTT_CAPABILITY} does not exist in the supplied product
	 */
	public Attribute createCapability(final String productCode, final String rawCapabilityCode, final String name,
			boolean cleanedCode) throws ItemNotFoundException {
		String cleanCapabilityCode = cleanedCode ? rawCapabilityCode : cleanCapabilityCode(rawCapabilityCode);

		DataType capabilityDtt = attributeUtils.getDataType(productCode, DataType.DTT_CAPABILITY, true);
		return attributeUtils.getOrCreateAttribute(productCode, cleanCapabilityCode, capabilityDtt);
	}

	public BaseEntity addCapabilityToBaseEntity(BaseEntity targetBe, Attribute capabilityAttribute,
			final CapabilityNode... modes) {

		updateCapability(targetBe, capabilityAttribute, modes);
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

	public BaseEntity addCapabilityToBaseEntity(BaseEntity targetBe, Capability capability) {
		return addCapabilityToBaseEntity(targetBe.getRealm(), targetBe, capability.code, capability.nodes.toArray(new CapabilityNode[0]));
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity targetBe, Attribute capabilityAttribute,
			final List<CapabilityNode> modes) {
		if (capabilityAttribute == null) {
			throw new ItemNotFoundException(productCode, "Capability Attribute");
		}

		updateCapability(targetBe, capabilityAttribute, modes);
		return targetBe;
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity targetBe, final String rawCapabilityCode,
			final CapabilityNode... modes) {
		// Ensure the capability is well defined
		String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);

		// Don't need to catch here since we don't want to create
		Attribute attribute = attributeUtils.getAttribute(productCode, cleanCapabilityCode, true);

		return addCapabilityToBaseEntity(targetBe, attribute, modes);
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

		if(filterable instanceof Question q) {
			log.info("Attaching Capability Requirements: " + CommonUtils.getArrayString(capabilityRequirements) + " to Question: " + realm + ":" + q.getCode());
			return questionUtils.saveQuestion(q);
		}

		if(filterable instanceof QuestionQuestion qq) {
			log.info("Attaching Capability Requirements: " + CommonUtils.getArrayString(capabilityRequirements) + " to QuestionQuestion: " + realm + ":" + qq.getParentCode() + ":" + qq.getChildCode());
			// TODO: Potentially update sub questions
			return questionUtils.saveQuestionQuestion(qq);
		}

		// TODO: Turn this into a sustainable solution

		if(filterable instanceof BaseEntity be) {
			log.info("Attaching Capability Requirements: " + CommonUtils.getArrayString(capabilityRequirements) + " to BaseEntity: " + realm + ":" + be.getCode());
			beUtils.updateBaseEntity(be);
			return true;
		}

		if(filterable instanceof EntityAttribute ea) {
			log.info("Attaching Capability Requirements: " + CommonUtils.getArrayString(capabilityRequirements) + " to EntityAttribute: " + realm + ":" + ea.getBaseEntityCode() + ":" + ea.getAttributeCode());
			beaUtils.updateEntityAttribute(ea);
			return true;
		}

		log.error("Unhandled filterable: " + filterable.getClass());
		return false;
	}
}
