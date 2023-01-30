package life.genny.qwandaq.capabilities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import java.util.List;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.QwandaUtils;

/*
 * A non-static utility class for managing roles and capabilities.
 * 
 * @author Jasper Robison
 * @author Bryn Meachem
 */
@ApplicationScoped
class CapEngine {

	@Inject
	UserToken userToken;

	@Inject
	Logger log;

	@Inject
	RoleManager roleMan;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	/**
	 * Return a Set of Capabilities based on a BaseEntity's LNK_ROLE and its own set
	 * of capabilities
	 * 
	 * @param target - the BaseEntity to fetch user capabilities for
	 * 
	 * @return a new {@link CapabilitySet} (a HashSet of Capabilities with knowledge of the target)
	 */
	@Deprecated(forRemoval = false)
	CapabilitySet getEntityCapabilities(BaseEntity target) {
		// this is a necessary log, since we are trying to minimize how often this
		// function is called
		// it is good to see how often it comes up
		log.debug("[!][!] Generating new Entity Capabilities for " + target.getCode());

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
		CapabilitySet userCapabilities = getOnlyEntityCapabilities(target);
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
	 * Get a single entity's capabilities (excluding roles)
	 * 
	 * @param productCode
	 * @param target
	 * @return
	 */
	private CapabilitySet getOnlyEntityCapabilities(final BaseEntity target) {
		Set<EntityAttribute> capabilities = new HashSet<>(target.findPrefixEntityAttributes(Prefix.CAP_));
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
	 * @param code        The capability code
	 * @param nodes       The nodes to set
	 */
	void addCapability(String productCode, BaseEntity target, final Attribute capability,
			final CapabilityNode... nodes) {
		// Update base entity
		if (capability == null) {
			throw new NullParameterException("capability");
		}

		if (target == null) {
			throw new NullParameterException("target");
		}
    
		// Check the user token has required capabilities
		// if (!shouldOverride()) {
		// 	log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " + capabilityAttribute.getCode()
		// 			+ " TO BASE ENTITITY: " + targetBe.getCode());
		// 	return targetBe;
		// }

		target.addEntityAttribute(capability, 0.0, false, CapabilitiesController.getModeString(nodes));
		beUtils.updateBaseEntity(target);
	}

	Attribute createCapability(final String productCode, final String rawCapabilityCode, final String name,
			boolean cleanedCode) {
		String cleanCapabilityCode = cleanedCode ? rawCapabilityCode : CapabilitiesController.cleanCapabilityCode(rawCapabilityCode);
		Attribute attribute = null;
		try {
			attribute = qwandaUtils.getAttribute(productCode, cleanCapabilityCode);
		} catch (ItemNotFoundException e) {
			log.debug("Could not find Attribute: " + cleanCapabilityCode + ". Creating new Capability");
		}

		if (attribute == null) {
			log.trace("Creating Capability : " + cleanCapabilityCode + " : " + name);
			attribute = new Attribute(cleanCapabilityCode, name, new DataType(String.class));
			qwandaUtils.saveAttribute(productCode, attribute);
		}

		return attribute;
	}

	BaseEntity removeCapabilityFromBaseEntity(String productCode, BaseEntity targetBe, String capabilityCode) {
		capabilityCode = CapabilitiesController.cleanCapabilityCode(capabilityCode);
		Attribute attr = qwandaUtils.getAttribute(productCode, capabilityCode);
		return removeCapabilityFromBaseEntity(productCode, targetBe, attr);
	}

	BaseEntity removeCapabilityFromBaseEntity(String productCode, BaseEntity targetBe, Attribute capabilityAttribute) {
		if (capabilityAttribute == null) {
			throw new ItemNotFoundException(productCode, "Capability Attribute");
		}


		targetBe.addAttribute(capabilityAttribute, 0.0, "[]");
		CacheUtils.putObject(productCode, targetBe.getCode() + ":" + capabilityAttribute.getCode(), "[]");
		beUtils.updateBaseEntity(targetBe);
		return targetBe;
	}

	boolean shouldOverride() {
		// allow keycloak admin and devcs to do anything
		return (userToken.hasRole("service", "admin", "dev") || ("service".equals(userToken.getUsername())));
	}
}
