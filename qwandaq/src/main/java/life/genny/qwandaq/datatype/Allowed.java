package life.genny.qwandaq.datatype;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

public class Allowed implements Serializable {
	protected static final Logger log = Logger.getLogger(Allowed.class);
			
	public static final String CAP_CODE_PREFIX = "PRM_";
	
	public final String code;
	public final List<CapabilityMode> modes;
	public final boolean validCode;
	
	public Allowed(final String capCode, final CapabilityMode... modes)
	{
		this.code = capCode;
		this.modes = Arrays.asList(modes);
		this.validCode = isValidCode(code);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Allowed))
			return false;
		Allowed other = (Allowed) obj;
		return Objects.equals(code, other.code) && modes.equals(other.modes);
	}
	
	public static boolean isValidCode(String capCode) {
		if(!capCode.startsWith(CAP_CODE_PREFIX))
			return false;
		// String[] components = capCode.split("_");
		// if(components.length < 3) {
		// 	log.error("Missing OWN or OTHER in " + capCode);
		// 	return false;
		// }

		return true;
	}

	private String getModesAsString() {
		return modes.stream().map((mode) -> mode.name()).collect(Collectors.joining(","));
	}

	@Override
	public String toString() {
		return "Allowed [" + (code != null ? "code=" + code + ", " : "") + (modes.size() > 0 ? "modes=" + getModesAsString() : "") + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, modes);
	}
}