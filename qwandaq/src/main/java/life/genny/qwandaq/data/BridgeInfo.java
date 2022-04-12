package life.genny.qwandaq.data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * A Bridge Info storage class used with BridgeSwitch to map individual user bridge Ids
 * 
 * @author Jasper Robison
 */
@RegisterForReflection
public class BridgeInfo {

	public ConcurrentMap<String, String> mappings = new ConcurrentHashMap<>();

	public BridgeInfo() {}
}
