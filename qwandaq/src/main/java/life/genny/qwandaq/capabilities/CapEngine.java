package life.genny.qwandaq.capabilities;

import java.util.HashSet;

import java.util.List;

import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.attribute.HEntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;

import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.HBaseEntity;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.exception.runtime.entity.BaseEntityException;
import life.genny.qwandaq.intf.ICapabilityFilterable;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.AttributeUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.qwandaq.utils.QwandaUtils;

/*
 * An Engine Class that is accessed solely through {@link CapabilitiesController}
 * 
 * @author Jasper Robison
 * @author Bryn Meachem
 */
@ApplicationScoped
public class CapEngine {

	static final String[] CAPABILITY_BEARING_ENTITY_PREFIXES = {Prefix.PER_, Prefix.ROL_};

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

	@Inject
	DatabaseUtils dbUtils;

	@Inject
	QuestionUtils questionUtils;

	@Inject
	AttributeUtils attributeUtils;

	/**
     * @deprecated (Marked as deprecated to show use cases in code. This is going to be moved to somewhere where it can be cached with User Session)
	 * Return a Set of Capabilities based on a BaseEntity's LNK_ROLE and its own set
	 * of capabilities
	 * 
	 * @param target - the BaseEntity to fetch user capabilities for
	 * 
	 * @return a new {@link CapabilitySet} (a HashSet of Capabilities with knowledge of the target)
	 */
	@Deprecated(forRemoval = false)
	CapabilitySet getEntityCapabilities(BaseEntity target) {
		if(!CommonUtils.isInArray(CAPABILITY_BEARING_ENTITY_PREFIXES, target.getPrefix())) {
			throw new BadDataException("Target BaseEntity does not have an accepted prefix for capabilities: " + target);
		}
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
		log.debug("\t - " + target.getCode() + "(" + capabilities.size() + " capabilities)");
		if(capabilities.isEmpty()) {
			return new CapabilitySet(target);
		}
		CapabilitySet cSet = new CapabilitySet(target);
		cSet.addAll(capabilities.stream()
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
	 * @param productCode The product code
	 * @param target      The target entity (must be a PER or ROL)
	 * @param capability        The capability to add to the role
	 * @param nodes       The {@link CapabilityNode CapabilityNodes} to set 
	 * @param persist Whether or not to persist in this call, or at the end of role building. Useful for speed/decluttering 
	 * 				  logs by reducing repeat persists when role building
	 * 
     * @throws {@link NullParameterException} if the {@link BaseEntity targetBe} or {@link Attribute capabilityAttribute} is missing
     * @throws {@link BaseEntityException} if the {@link BaseEntity#getCode() targetBe's code} is not in the 
    *                                       {@link CapEngine#CAPABILITY_BEARING_ENTITY_PREFIXES Accepted Capability Bearing Prefixes}
	 */
	void addCapability(String productCode, BaseEntity target, final Attribute capability, boolean persist,
			final CapabilityNode... nodes) {
		if (capability == null) {
			throw new NullParameterException("capability");
		}

		if (target == null) {
			throw new NullParameterException("target");
		}

		if(!CommonUtils.isInArray(CAPABILITY_BEARING_ENTITY_PREFIXES, target.getPrefix()))
			throw new BaseEntityException(target, 
				"Incorrect BaseEntity Type (Prefix) when adding a capability to target BaseEntity " + 
				"\nAccepted Prefixes (any of): " + CAPABILITY_BEARING_ENTITY_PREFIXES);
    
		// Check the user token has required capabilities
		// if (!shouldOverride()) {
		// 	log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " + capabilityAttribute.getCode()
		// 			+ " TO BASE ENTITITY: " + targetBe.getCode());
		// 	return targetBe;
		// }

		// Update base entity
		target.addEntityAttribute(capability, 0.0, false, CapabilitiesController.getModeString(nodes));
		if(persist)
			beUtils.updateBaseEntity(target);
	}

	Attribute createCapability(final String productCode, final String capabilityCode, final String name) {
		
		Attribute attribute = null;
		try {
			attribute = attributeUtils.getAttribute(productCode, capabilityCode, true);
		} catch (ItemNotFoundException e) {
			log.debug("Could not find Capability Attribute: " + capabilityCode + ". Creating new Capability");
			attribute = new Attribute(capabilityCode, name, new DataType(String.class));
			attributeUtils.saveAttribute(productCode, attribute);
		}

		return attribute;
	}

    BaseEntity deleteCapability(String productCode, BaseEntity targetBe, String capabilityCode) {
		if(targetBe == null)
			throw new NullParameterException("targetBe");
		log.debug("Removing Capability: " + capabilityCode + " from Entity: " + targetBe.getCode());
		// only update database if remove operation finds something to remove
        if(targetBe.removeAttribute(capabilityCode)) {
			dbUtils.deleteEntityAttribute(productCode, targetBe.getCode(), capabilityCode);
		} else {
			log.debug("[!] Entity: " + targetBe.getCode() + " did not have capability: " + capabilityCode);
		}
		
		return targetBe;
    }

	BaseEntity resetCapability(String productCode, BaseEntity targetBe, String capabilityCode) {
		capabilityCode = CapabilitiesController.cleanCapabilityCode(capabilityCode);
		Attribute attr = qwandaUtils.getAttribute(productCode, capabilityCode);
		try {
			return resetCapability(productCode, targetBe, attr);
		} catch (ItemNotFoundException e) {
			// Here we know more information about the attribute we are trying to fetch
			// so we can add more to the exception
			throw new ItemNotFoundException(productCode, capabilityCode, e);
		}
	}

	BaseEntity resetCapability(String productCode, BaseEntity targetBe, Attribute capabilityAttribute) {
		if (capabilityAttribute == null) {
			throw new ItemNotFoundException(productCode, "Capability Attribute");
		}


		targetBe.addAttribute(capabilityAttribute, 0.0, "[]");
		CacheUtils.putObject(productCode, targetBe.getCode() + ":" + capabilityAttribute.getCode(), "[]");
		beUtils.updateBaseEntity(targetBe);
		return targetBe;
	}

	/**
	 * @param filterable
	 * @param capabilityRequirements
	 * @return
	 */
	boolean updateCapabilityRequirements(String realm, ICapabilityFilterable filterable, Capability... capabilityRequirements) {
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

	private boolean shouldOverride() {
		// allow keycloak admin and devcs to do anything
		return (userToken.hasRole("service", "admin", "dev") || ("service".equals(userToken.getUsername())));
	}


	// Unit Testing Helpers
	public void setAttributeUtils(AttributeUtils attributes) {
		this.attributeUtils = attributes;
	}
}
