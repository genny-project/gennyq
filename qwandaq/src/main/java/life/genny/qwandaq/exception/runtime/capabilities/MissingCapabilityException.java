package life.genny.qwandaq.exception.runtime.capabilities;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.exception.GennyRuntimeException;

public class MissingCapabilityException extends GennyRuntimeException {


    public MissingCapabilityException(Capability capability, CapabilitySet userCapabilities) {
        super(new StringBuilder("User: ")
            .append(userCapabilities.getEntityCode())
            .append(" Missing Capability: ")
            .append(capability)
            .toString());
    }

}
