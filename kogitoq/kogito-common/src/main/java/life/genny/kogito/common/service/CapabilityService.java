package life.genny.kogito.common.service;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.utils.CommonUtils;

@ApplicationScoped
public class CapabilityService extends KogitoService {
    
    @Inject
    Logger log;

    private static final String LOG_PREPEND = "[!] Kogito Capabilities: ";

    @Inject
    CapabilitiesManager capabilities;

    public Set<Capability> getUserCapabilities() {
        return capabilities.getUserCapabilities();
    }

    /**
     * Add a capability to the session-based User base entity
     * @param capability - Capability code to add (Starting with {@link CAP_})
     * @param capabilityString - String representation of the nodes to set
     * <p><h4>Example</h4>
     * <pre>
     *  addCapabilityToUser("CAP_BANANA_BREAD", "[\"V:A\",\"D:A\"]");
     * </pre>
     * will add the capability <b>CAP_BANANA_BREAD</b> with the nodes <b>VIEW:ALL</b> and <b>DELETE:ALL to the user's base entity specifically</b>
     * </p>
     * <p>
     * perhaps this means the user can now view and eat all the banana bread?
     * </p>
     */
    public void addCapabilityToUser(String capability, String capabilityString) {
        log.info(LOG_PREPEND + "Updating user capability: " + capability + " = " + capabilityString);
        CapabilityNode[] capabilityNodes = CommonUtils.getArrayFromString(capabilityString, CapabilityNode.class, CapabilityNode::parseNode);
        capabilities.addCapabilityToBaseEntity(userToken.getProductCode(), userToken.getCode(), capability, capabilityNodes);
    }

    /**
     * Add a capability to the session-based User base entity
     * @param capability - Capability code to remove (Starting with {@link CAP_})
     * <p><h4>Example</h4>
     * <pre>
     *  removeCapabilityFromUser("CAP_BANANA_BREAD");
     * </pre>
     * will remove any nodes set for the user for the capability: <b>CAP_BANANA_BREAD</b>
     * </p>
     * <p>
     * perhaps this means the user ate too much banana bread and has now been banned from it?
     * </p>
     */
	public void removeCapabilityFromUser(String capability) {
        log.info(LOG_PREPEND + "Removing user capability: " + capability);
		BaseEntity userBe = beUtils.getUserBaseEntity();
        capabilities.removeCapabilityFromBaseEntity(userToken.getProductCode(), userBe, capability);
	}
}
