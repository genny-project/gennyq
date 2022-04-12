package life.genny.qwandaq.datatype;

import java.io.Serializable;
import java.util.Objects;

public class Allowed implements Serializable {

	public String code;
	public CapabilityMode mode;
	
	public Allowed(final String code, final CapabilityMode mode) {
		this.code = code;
		this.mode = mode;
	}

	
	/** 
	 * @return String
	 */
	@Override
	public String toString() {
		return "Allowed [" + (code != null ? "code=" + code + ", " : "") + (mode != null ? "mode=" + mode : "") + "]";
	}

	
	/** 
	 * @return int
	 */
	@Override
	public int hashCode() {
		return Objects.hash(code, mode);
	}

	
	/** 
	 * Check equality
	 *
	 * @param obj the object to compare to
	 * @return boolean
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Allowed))
			return false;
		Allowed other = (Allowed) obj;
		return Objects.equals(code, other.code) && mode == other.mode;
	}
	
}
