package life.genny.qwandaq.datatype;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.exception.runtime.BadDataException;

/*
 * Capability Class to encapsulate necessary data to determine capabilities 
 * @author Bryn Meachem
 */
@RegisterForReflection
public class Capability {
	// Leave this here please
	public static final String DELIMITER = ":";

	/**
	 * An enum to declare what mode this capability concerns
	 */
	public static enum CapabilityMode {
		// Priority to be determined by .ordinal()
		VIEW("VIEW"),
		EDIT("EDIT"),
		ADD("ADD"),
		DELETE("DELETE");

		private final String identifier;

		private CapabilityMode(String identifier) {
			this.identifier = identifier;
		}
	}

	/**
	 * An enum to declare what permissions this capability has
	 */
	public static enum PermissionMode {
		ALL("ALL"),
		SELF("SELF"),
		NONE("NONE");

		private final String identifier;

		private PermissionMode(String identifier) {
			this.identifier = identifier;
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
	 * @param capMode the {@link Capability} to assign 
	 * @param permMode the {@link PermissionMode} to assign
	 * 
	 * @see {@link CapabilityMode}, {@link PermissionMode}
	 */
	public Capability(CapabilityMode capMode, PermissionMode permMode) {
		this.capMode = capMode;
		this.permMode = permMode;
	}

	/**
	 * Create a new capability with the given mode and permissions
	 * @param capMode
	 * 
	 * @see {@link CapabilityMode}, {@link PermissionMode}
	 */
	public Capability(CapabilityMode capMode) {
		this(capMode, PermissionMode.SELF);
	}

	/**
	 * Parse a new capability given a String such as
	 * <pre>
	 * VIEW:ALL
	 * </pre>
	 * 
	 * Each component of the string is to be separated by {@link Capability#DELIMITER} (currently ':')
	 * @param capabilityString - the capabilityString to deserialize
	 * @return a new Capability based on the CapabilityMode and PermissionMode in the String
	 * @throws BadDataException if the capabilityString is malformed in some way/the corresponding CapabilityMode or PermissionMode could not be found
	 * 
	 * @see {@link CapabilityMode}, {@link PermissionMode}
	 */
	public static Capability parseCapability(String capabilityString) 
		throws BadDataException {
		String[] identifiers = capabilityString.split(":");
		if(identifiers.length != 2) {
			throw new BadDataException("There must be exactly 2 identifiers detected in capability strings. this string has: " + identifiers.length + ". (Delimited by '" + DELIMITER + "')");
		}
		
		CapabilityMode capMode;
		PermissionMode permMode;

		try {
			capMode = CapabilityMode.valueOf(identifiers[0]);
		} catch(IllegalArgumentException e) {
			throw new BadDataException("No Capability Mode with identifier: " + identifiers[0]);
		}
		try {
			permMode = PermissionMode.valueOf(identifiers[1]);
		} catch(IllegalArgumentException e) {
			throw new BadDataException("No Permission Mode with identifier: " + identifiers[1]);
		}

		return new Capability(capMode, permMode);
	}

	public String toString() {
		return capMode.identifier + DELIMITER + permMode.identifier;
	}

	public boolean equals(Object other) {
		if(!this.getClass().equals(other.getClass()))
			return false;
		Capability cap = (Capability)other;
		if(!cap.capMode.equals(this.capMode))
			return false;
		if(!cap.permMode.equals(this.permMode))
			return false;

		return true;
	}
}
