package life.genny.qwandaq.datatype;

import org.jboss.logging.Logger;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
		if (obj instanceof Allowed other) {
//		Allowed other = (Allowed) obj;
			return Objects.equals(code, other.code) && modes.equals(other.modes);
		} else
			return false;
	}
	
	public static boolean isValidCode(String capCode) {
		return capCode.startsWith(CAP_CODE_PREFIX);
		// String[] components = capCode.split("_");
		// if(components.length < 3) {
		// 	log.error("Missing OWN or OTHER in " + capCode);
		// 	return false;
		// }
	}

	private String getModesAsString() {
		return modes.stream().map(Enum::name).collect(Collectors.joining(","));
	}

	@Override
	public String toString() {
		return "Allowed [" + (code != null ? "code=" + code + ", " : "") + (!modes.isEmpty() ? "modes=" + getModesAsString() : "") + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, modes);
	}
}