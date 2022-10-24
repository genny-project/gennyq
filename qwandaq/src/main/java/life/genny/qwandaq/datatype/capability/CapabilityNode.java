package life.genny.qwandaq.datatype.capability;

import java.util.EnumMap;
import java.util.Map;

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
	}
	public static CapabilityNode fetch(CapabilityMode mode, PermissionMode scope) {
		return NODE_MAP.get(mode).get(scope);
	}

	/**
	 * This capability's mode
	 */
	public final CapabilityMode capMode;

	/**
	 * This capability's permission for the given mode
	 */
	public final PermissionMode permMode;

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
	public CapabilityNode(CapabilityMode capMode, PermissionMode permMode) {
		this.capMode = capMode;
		this.permMode = permMode;
	}

	/**
	 * Create a new capability with the given mode and permissions
	 * @param capMode
	 * 
	 * @see {@link CapabilityMode}, {@link PermissionMode}
	 */
	public CapabilityNode(CapabilityMode capMode) {
		this(capMode, PermissionMode.SELF);
	}

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
	public CapabilityNode[] getLesserNodes() {
		int size = this.permMode.ordinal();
		CapabilityNode[] lesserNodes = new CapabilityNode[size];
		for(int i = 0; i < size; i++) {
			lesserNodes[i] = new CapabilityNode(capMode, PermissionMode.getByOrd(size - (i + 1)));
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

		if(capabilityString.length() != 3) {
			log.error("Expected length 3. Got: " + capabilityString.length());
			throw new BadDataException("Could not deserialize capability node: " + capabilityString);
		}

		capMode = CapabilityMode.getByIdentifier(capabilityString.charAt(0));
		permMode = PermissionMode.getByIdentifier(capabilityString.charAt(2));

		return new CapabilityNode(capMode, permMode);
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
