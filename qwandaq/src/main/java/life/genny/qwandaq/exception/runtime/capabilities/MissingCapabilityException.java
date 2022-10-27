package life.genny.qwandaq.exception.runtime.capabilities;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.exception.GennyRuntimeException;

public class MissingCapabilityException extends GennyRuntimeException {

    public MissingCapabilityException(String message, ReqConfig requirementsConfig) {
        super(message);
    }

    public MissingCapabilityException(Capability capability, ReqConfig requirementsConfig) {
        this(new StringBuilder("User: ")
            .append(requirementsConfig.userCapabilities.getEntityCode())
            .append(" Missing Capability: ")
            .append(capability)
            .append("\nRequirements Config: ")
            .append(requirementsConfig)
            .toString(), requirementsConfig);
    }

}
