package life.genny.qwandaq.datatype.capability.core.node;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.bind.annotation.JsonbTransient;

import org.apache.commons.lang3.StringUtils;
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

	// Some optimisation through statics
	private static final Map<CapabilityMode, Map<PermissionMode, CapabilityNode>> NODE_MAP = new EnumMap<>(CapabilityMode.class);
	static {
		for(CapabilityMode mode : CapabilityMode.values()) {
			Map<PermissionMode, CapabilityNode> scopeMap = new EnumMap<>(PermissionMode.class);
			for(PermissionMode scope : PermissionMode.values()) {
				scopeMap.put(scope, new CapabilityNode(mode, scope));
			}
			NODE_MAP.put(mode, scopeMap);
		}
		log.info("Init " + (CapabilityMode.values().length * PermissionMode.values().length) + "CapabilityNodes");
	}

	public static CapabilityNode get(CapabilityMode mode, PermissionMode scope) {
		return NODE_MAP.get(mode).get(scope);
	}

	public static CapabilityNode get(CapabilityMode mode) {
		return get(mode, PermissionMode.SELF);
	}

	public static CapabilityNode get(Entry<CapabilityMode, PermissionMode> node) {
		return get(node.getKey(), node.getValue());
	}

	public static CapabilityNode get(char modeId, char permId) {
		return get(CapabilityMode.getByIdentifier(modeId),
			PermissionMode.getByIdentifier(permId));
	}

	/**
	 * This capability's mode
	 */
	public CapabilityMode capMode;

	/**
	 * This capability's permission for the given mode
	 */
	public PermissionMode permMode;

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
	private CapabilityNode(CapabilityMode capMode, PermissionMode permMode) {
		this.capMode = capMode;
		this.permMode = permMode;
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

	public CapabilityNode compareNodes(Entry<CapabilityMode, PermissionMode> other, boolean mostPermissive) {
		return compareNodes(get(other), mostPermissive);
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
			lesserNodes[i] = get(capMode, PermissionMode.getByOrd(size - (i + 1)));
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
	public static CapabilityNode parseCapability(String capabilityString)
		throws BadDataException {
		CapabilityMode capMode;
		PermissionMode permMode;

		// TODO: merge latest to remove this
		capabilityString = StringUtils.removeStart(capabilityString, "!");

		if (capabilityString.length() != 3) {
			log.error("Expected length 3. Got: " + capabilityString.length());
			throw new BadDataException("Could not deserialize capability node: " + capabilityString);
		}

		capMode = CapabilityMode.getByIdentifier(capabilityString.charAt(0));
		permMode = PermissionMode.getByIdentifier(capabilityString.charAt(2));

		return get(capMode, permMode);
	}

	public String toString(boolean verbose) {
		if(verbose) {
			return capMode.name() + DELIMITER + permMode.name();
		} else {
			return capMode.getIdentifier() + DELIMITER + permMode.getIdentifier();
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
			.append(capMode.getIdentifier())
			.append(permMode.getIdentifier())
			.build();
	}
}
