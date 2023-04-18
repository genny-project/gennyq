package life.genny.qwandaq.intf;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.json.bind.annotation.JsonbTransient;

import org.jboss.logging.Logger;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;

import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.callbacks.FILogCallback;

public interface ICapabilityFilterable {
    static Logger log = Logger.getLogger(ICapabilityFilterable.class);

    public static Logger getLogger() {
        return log;
    }
    
    public Set<Capability> getCapabilityRequirements();

    @JsonbTransient
    public default boolean hasCapabilityRequirements() {
        return getCapabilityRequirements() != null && !getCapabilityRequirements().isEmpty();
    }

    public default void setCapabilityRequirements(Capability... requirements) {
        setCapabilityRequirements(new HashSet<>(Arrays.asList(requirements)));
    }

    public void setCapabilityRequirements(Set<Capability> requirements);

    public default boolean requirementsMet(CapabilitySet userCapabilities) {
        return requirementsMet(userCapabilities, new ReqConfig());
    }

    public default boolean requirementsMet(CapabilitySet userCapabilities, ReqConfig requirementsConfig) {
        return requirementsMetImpl(userCapabilities, getCapabilityRequirements(), requirementsConfig);
    }

    public static boolean requirementsMetImpl(CapabilitySet userCapabilities, Set<Capability> capabilityRequirements, ReqConfig requirementsConfig) {
        if(capabilityRequirements == null || capabilityRequirements.isEmpty()) {
            log.debug("No capabilityRequirements found!");
            return true;
        }

        boolean requiresAllCaps = requirementsConfig.needsAllCaps();

        log.debug("Testing Capability Config: " + requirementsConfig);

        // TODO: Can optimize this into two separate loops if necessary, to save on
        // if checks
        for(Capability reqCap : capabilityRequirements) {
            Optional<Capability> optCap = userCapabilities.parallelStream()
                .filter(cap -> cap.code.equals(reqCap.code)).findFirst();
            if(!optCap.isPresent()) {
                log.warn("Could not find cap in user caps: " + reqCap.code);
                return false;
            }

            // a set of user capabilities should only have 1 entry per capability code
            Capability cap = optCap.get();

            boolean passesCheck = requirementsConfig.checkCapability(cap.nodes, 
                reqCap.nodes.toArray(new CapabilityNode[0]));
            
            // negate test
            // if reqCap has negate we have success if passesCheck is false
            // if reqCap doesn't have negate, then false (don't check pass)
            

            if(!passesCheck) {
                if(requiresAllCaps) {
                    log.warn("Missing cap permissions " + reqCap);
                    log.debug("User perms: " + cap);
                    log.debug("ReqConfig: " + requirementsConfig);
                    return false;
                }
            } else {
                if(!requiresAllCaps)
                    return true;
            }
        }

        return requiresAllCaps;
    }

    /**
     * Print capability requirements (one per line) using log.debug
     */
    public default void printRequirements() {
        printRequirements(log::debug);
    }

    /**
     * Print capability requirements (one per line) using a given log function
     * @param logLevel - log level / function to use (e.g log::debug or System.out::println)
     */
    public default void printRequirements(FILogCallback logLevel) {
        if(getCapabilityRequirements() == null || getCapabilityRequirements().size() == 0) {
            logLevel.log("No requirements found to print!");
            return;
        }
        CommonUtils.printCollection(getCapabilityRequirements(), logLevel, (req) -> {
            return new StringBuilder("  - ")
                .append(req.code)
                .append(" = ")
                .append(CommonUtils.getArrayString(req.nodes))
                .toString();
        });
    }

}
