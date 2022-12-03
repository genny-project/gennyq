package life.genny.qwandaq.datatype.capability.core.node;

import javax.json.bind.annotation.JsonbTransient;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.exception.runtime.BadDataException;

/**
 * Capability Class to encapsulate necessary data to determine capabilities 
 * @author Bryn Meachem
 */
@RegisterForReflection
public class CapabilityNode {
	private static final Logger log = Logger.getLogger(CapabilityNode.class);
	
	// Leave this here please
	public static final String DELIMITER = ":";
	public static final String NEGATE_IDENTIFIER = "!";

	// // Some optimisation through statics
	// private static final Map<CapabilityMode, Map<PermissionMode, CapabilityNode>> NODE_MAP = new EnumMap<>(CapabilityMode.class);
	// static {
	// 	for(CapabilityMode mode : CapabilityMode.values()) {
	// 		Map<PermissionMode, CapabilityNode> scopeMap = new EnumMap<>(PermissionMode.class);
	// 		for(PermissionMode scope : PermissionMode.values()) {
	// 			scopeMap.put(scope, new CapabilityNode(mode, scope));
	// 		}
	// 		NODE_MAP.put(mode, scopeMap);
	// 	}
	// 	log.info("Init " + (CapabilityMode.values().length * PermissionMode.values().length) + " CapabilityNodes");
	// }

	// public static CapabilityNode get(CapabilityMode mode, PermissionMode scope) {
	// 	return NODE_MAP.get(mode).get(scope);
	// }

	// public static CapabilityNode get(CapabilityMode mode) {
	// 	return get(mode, PermissionMode.SELF);
	// }

	// public static CapabilityNode get(Entry<CapabilityMode, PermissionMode> node) {
	// 	return get(node.getKey(), node.getValue());
	// }

	// public static CapabilityNode get(char modeId, char permId) {
	// 	return get(CapabilityMode.getByIdentifier(modeId),
	// 		PermissionMode.getByIdentifier(permId));
	// }

	/**
	 * This capability's mode
	 */
	public CapabilityMode capMode;

	/**
	 * This capability's permission for the given mode
	 */
	public PermissionMode permMode;

	/**
	 * Whether or not to negate this particular node or not
	 */
	public boolean negate;

	/**
	 * Create a new capability with the given mode and permissions
	 * @param capMode the {@link CapabilityNode} to assign 
	 * @param permMode the {@link PermissionMode} to assign
	 * <p>
	 * <pre>
	 * new Capability(VIEW, ALL)
	 * </pre>
	 * will create a new Capability as VIEW:ALL
	 * </p>
	 * 
	 * @see {@link CapabilityMode}, {@link PermissionMode}
	 */
	public CapabilityNode(CapabilityMode capMode, PermissionMode permMode, boolean negate) {
		this.capMode = capMode;
		this.permMode = permMode;
		this.negate = negate;
	}

	public CapabilityNode(CapabilityMode capMode, PermissionMode permMode) {
		this(capMode, permMode, false);
	}

	@Deprecated
	public CapabilityNode() {}

	/**
	 * Get the most permissive node between this and another Node
	 * @param other - the other node to compare
	 * @return the most permissive node between this and the other node or this if the two modes are different
	 */
	public CapabilityNode compareNodes(CapabilityNode other, boolean mostPermissive) {
		if(!this.capMode.equals(other.capMode))
			return this;
		// if -1 then this is less permissive
		// if 0 then they are equal
		// if 1 then this is more permissive
		int ord = other.permMode.compareTo(this.permMode);
		
		CapabilityNode result;
		if(ord > 0)
			result = mostPermissive ? other : this;
		else
			result = mostPermissive ? this : other;

		return result;
	}

	/**
	 * Get all CapabilityNodes with less permissions than this one for it's given Mode
	 * @return
	 */
	@JsonbTransient
	public CapabilityNode[] getLesserNodes() {
		int size = this.permMode.ordinal();
		CapabilityNode[] lesserNodes = new CapabilityNode[size];
		for(int i = 0; i < size; i++) {
			PermissionMode mode = PermissionMode.getByOrd(size - (i + 1));
			CapabilityNode node = new CapabilityNode(capMode, mode);
			node.negate = negate;
			lesserNodes[i] = node;
		}

		return lesserNodes;
	}

	/**
	 * Parse a new capability given a String such as
	 * <pre>
	 * VIEW:ALL
	 * </pre>
	 * 
	 * Each component of the string is to be separated by {@link CapabilityNode#DELIMITER} (currently ':')
	 * @param capabilityString - the capabilityString to deserialize
	 * @return a new Capability based on the CapabilityMode and PermissionMode in the String
	 * @throws BadDataException if the capabilityString is malformed in some way/the corresponding CapabilityMode or PermissionMode could not be found
	 * 
	 * @see {@link CapabilityMode}, {@link PermissionMode}
	 */
	public static CapabilityNode parseNode(String capabilityString) 
		throws BadDataException {
		CapabilityMode capMode;
		PermissionMode permMode;
		boolean negate = false;

		int len = capabilityString.length();
		if(!(len == 3 || len == 4)) {
			log.error("Expected length 3 - 4. Got: " + len);
			throw new BadDataException("Could not deserialize capability node: " + capabilityString);
		}
		int offset = 0;
		if(capabilityString.startsWith(NEGATE_IDENTIFIER)) {
			offset = 1;
			negate = true;
		}
		capMode = CapabilityMode.getByIdentifier(capabilityString.charAt(offset));
		permMode = PermissionMode.getByIdentifier(capabilityString.charAt(offset + 2));

		CapabilityNode node = new CapabilityNode(capMode, permMode);
		node.negate = negate;
		
		return node;
	}

	public String toString(boolean verbose) {
		StringBuilder sb = new StringBuilder();
		if(negate)
			sb.append(NEGATE_IDENTIFIER);
		if(verbose) {
			return sb.append(capMode.name())
					.append(DELIMITER)
					.append(permMode.name())
					.toString();
		} else {
			return sb.append(capMode.getIdentifier())
					.append(DELIMITER)
					.append(permMode.getIdentifier())
					.toString();
		}
	}

	@Override
	public String toString() {
		return toString(false);
	}

	@Override
	public boolean equals(Object other) {
		// Ref: https://stackoverflow.com/questions/596462/any-reason-to-prefer-getclass-over-instanceof-when-generating-equals
		if(!this.getClass().equals(other.getClass())) {
			return false;
		}
		CapabilityNode cap = (CapabilityNode)other;
		if(cap.capMode.getIdentifier() != this.capMode.getIdentifier()) {
			return false;
		}
		if(cap.permMode.getIdentifier() != this.permMode.getIdentifier()) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.append(negate)
			.append(capMode.getIdentifier())
			.append(permMode.getIdentifier())
			.build();
	}
}
