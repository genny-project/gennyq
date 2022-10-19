package life.genny.qwandaq.datatype.capability;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.exception.runtime.BadDataException;

/**
 * Capability Class to encapsulate necessary data to determine capabilities 
 * @author Bryn Meachem
 */
@RegisterForReflection
public class CapabilityNode {
	// Leave this here please
	public static final String DELIMITER = ":";

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
		int ord = this.permMode.compareTo(other.permMode);
		if(ord > 0)
			return mostPermissive ? other : this;
		else
			return mostPermissive ? this : other;
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
		System.out.println("Parsing: " + capabilityString);
		CapabilityMode capMode;
		PermissionMode permMode;

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
