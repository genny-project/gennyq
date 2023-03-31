package life.genny.kogito.common.service;

import java.util.Set;

import javax.inject.Inject;

import org.jboss.logging.Logger;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.utils.CommonUtils;

public class CapabilityService extends KogitoService {
    
    @Inject
    Logger log;

    @Inject
    CapabilitiesManager capabilities;

    public Set<Capability> getUserCapabilities() {
        return capabilities.getUserCapabilities();
    }

    public void addCapabilityToUser(String capability, String capabilityString) {
        CapabilityNode[] capabilityNodes = CommonUtils.getArrayFromString(capabilityString, CapabilityNode.class, CapabilityNode::parseNode);
        capabilities.addCapabilityToBaseEntity(userToken.getProductCode(), userToken.getCode(), capability, capabilityNodes);
    }

	public void removeCapabilityFromUser(String capability) {
		BaseEntity userBe = beUtils.getUserBaseEntity();
        capabilities.removeCapabilityFromBaseEntity(userToken.getProductCode(), userBe, capability);
	}
}
