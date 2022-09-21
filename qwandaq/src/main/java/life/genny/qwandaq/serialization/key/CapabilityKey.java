package life.genny.qwandaq.serialization.key;

import life.genny.qwandaq.serialization.common.CoreEntityKey;
import life.genny.qwandaq.utils.capabilities.CapabilityUtils;

public class CapabilityKey implements CoreEntityKey {

    private String roleCode;
    private String capabilityCode;

    // no-arg constructor
    public CapabilityKey() { }

    public CapabilityKey(String roleCode, String capabilityCode) {
        setRoleCode(roleCode);
        setCapabilityCode(capabilityCode);
    }

    @Override
    public String getKeyString() {
        return null;
    }

    @Override
    public CoreEntityKey fromKey(String key) {
        return null;
    }

    @Override
    public String getDelimiter() {
        return null;
    }

    @Override
    public String getEntityCode() {
        return getComponents()[0];
    }

    @Override
    public String toString() {
        return getKeyString();
    }
    
    // Getters and Setters

    public String getRoleCode() {
        return this.roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public void setCapabilityCode(String capabilityCode) {
        this.capabilityCode = CapabilityUtils.cleanCapabilityCode(capabilityCode);
    }

    public String getCapabilityCode() {
        return this.capabilityCode;
    }
}
