package life.genny.qwandaq.datatype;

import java.io.Serializable;

import io.quarkus.runtime.annotations.RegisterForReflection;

/*
 * An enum class used to represent the ability in regards to a capability
 * 
 * @author Adam Crow
 */
@RegisterForReflection
public enum CapabilityMode implements Serializable {

	/**
	 * represents no capability in regards to something
	 */
	NONE("NONE", 0),

	/**
	 * Represents the capability to view something
	 */
	VIEW("VIEW", 1),

	/**
	 * Represents the capability to edit something
	 */
	EDIT("EDIT", 2),

	/**
	 * Represents the capability to add something
	 */
	ADD("ADD", 3),

	/**
	 * Represents the capability to delete something
	 */
	DELETE("DELETE", 4),

	/**
	 * Represents the capability to do something to self
	 */
	SELF("SELF", 5);

	private final String name;
	private final Integer priority;

	private CapabilityMode(String s,Integer p) {
		name = s;
		priority = p;
	}

	public boolean equalsName(String otherName) {
		return name.equals(otherName);
	}

	public String toString() {
		return this.name;
	}
	
	public boolean greaterThan(CapabilityMode other)
	{
		return (this.priority > other.priority);
	}
	
	public static CapabilityMode getMode(final String modeString)
	{
        for (CapabilityMode b : CapabilityMode.values()) {
            if (b.name.equalsIgnoreCase(modeString)) {
                return b;
            }
        }
        return null;
	}
}
