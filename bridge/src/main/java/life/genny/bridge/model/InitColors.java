package life.genny.bridge.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * A class used in sending init data to alyson. Since colors must be global in alyson for efficiency reasons, we send them on init.
 *
 * @author Jasper Robison
 */
@RegisterForReflection
public class InitColors {

	private String primary;
	private String secondary;

	public InitColors() {
    }

    public InitColors(String primary, String secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public String getSecondary() {
        return secondary;
    }

    public void setSecondary(String secondary) {
        this.secondary = secondary;
    }

}
