package life.genny.qwandaq.datatype;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.exception.runtime.BadDataException;

/*
 * Capability Class to encapsulate necessary data to determine capabilities 
 * @author Bryn Meachem
 */
@RegisterForReflection
public class CapabilityNode {
	// Leave this here please
	public static final String DELIMITER = ":";

	/**
	 * An enum to declare what mode this capability concerns
	 */
	public static enum CapabilityMode {
		// Priority to be determined by .ordinal()
		VIEW('V'),
		EDIT('E'),
		ADD('A'),
		DELETE('D');

		private final char identifier;

		private CapabilityMode(char identifier) {
			this.identifier = identifier;
		}

		public char getIdentifier() {
			return this.identifier;
		}

		public static CapabilityMode getByIdentifier(char identifier) {
			for(CapabilityMode mode : values()) {
				if(mode.identifier == identifier)
					return mode;
			}

			return null;
		}
	}

	/**
	 * An enum to declare what permissions this capability has
	 */
	public static enum PermissionMode {
		ALL('A'),
		SELF('S'),
		NONE('N');

		private final char identifier;

		private PermissionMode(char identifier) {
			this.identifier = identifier;
		}

		public char getIdentifier() {
			return this.identifier;
		}

		public static PermissionMode getByIdentifier(char identifier) {
			for(PermissionMode mode : values()) {
				if(mode.identifier == identifier)
					return mode;
			}

			return null;
		}
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
	public CapabilityNode getMostPermissiveNode(CapabilityNode other) {
		if(!this.capMode.equals(other.capMode))
			return this;
		// if -1 then this is less permissive
		// if 0 then they are equal
		// if 1 then this is more permissive
		int ord = this.permMode.compareTo(other.permMode);
		if(ord > 0)
			return other;
		else
			return this;
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

		capMode = CapabilityMode.getByIdentifier(capabilityString.charAt(0));
		permMode = PermissionMode.getByIdentifier(capabilityString.charAt(2));

		return new CapabilityNode(capMode, permMode);
	}

	@Override
	public String toString() {
		return capMode.identifier + DELIMITER + permMode.identifier;
	}

	@Override
	public boolean equals(Object other) {
		if(!this.getClass().equals(other.getClass())) {
			return false;
		}
		CapabilityNode cap = (CapabilityNode)other;
		if(cap.capMode.identifier != this.capMode.identifier) {
			return false;
		}
		if(cap.permMode.identifier != this.permMode.identifier) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.append(capMode.identifier)
			.append(permMode.identifier)
			.build();
	}
}
