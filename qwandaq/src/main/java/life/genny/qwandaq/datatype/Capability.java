package life.genny.qwandaq.datatype;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.exception.runtime.BadDataException;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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
	public enum CapabilityMode {
		// Priority to be determined by .ordinal()
		VIEW('V'),
		EDIT('E'),
		ADD('A'),
		DELETE('D');

		private final char identifier;

		CapabilityMode(char identifier) {
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
	public enum PermissionMode {
		ALL('A'),
		SELF('S'),
		NONE('N');

		private final char identifier;

		PermissionMode(char identifier) {
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
	 * @param capMode the {@link Capability} to assign 
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
		CapabilityMode capMode;
		PermissionMode permMode;

		capMode = CapabilityMode.getByIdentifier(capabilityString.charAt(0));
		permMode = PermissionMode.getByIdentifier(capabilityString.charAt(2));

		return new Capability(capMode, permMode);
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
		Capability cap = (Capability)other;
		if(cap.capMode.identifier != this.capMode.identifier) {
			return false;
		}
		return cap.permMode.identifier == this.permMode.identifier;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.append(capMode.identifier)
			.append(permMode.identifier)
			.build();
	}
}
