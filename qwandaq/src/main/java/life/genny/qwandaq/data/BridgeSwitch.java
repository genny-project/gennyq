package life.genny.qwandaq.data;

import java.util.HashSet;
import java.util.Set;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.serialization.common.key.cache.CacheKey;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.constants.CacheName;
import life.genny.qwandaq.data.BridgeInfo;

/**
 * A Bridge ID management class for data message route selection.
 * 
 * @author Byron Aguirre
 * @author Jasper Robison
 */
public class BridgeSwitch {

	static final Logger log = Logger.getLogger(BridgeSwitch.class);

	static Jsonb jsonb = JsonbBuilder.create();

	public static String BRIDGE_INFO_PREFIX = "BIF";
	public static String BRIDGE_SWITCH_KEY = "ACTIVE_BRIDGE_IDS";

	/**
	* Cache active Bridge Ids
	*
	* @param gennyToken The users gennyToken
	* @param bridgeId The id to add to active ids
	 */
	public static void addActiveBridgeId(GennyToken gennyToken, String bridgeId) {

		String productCode = gennyToken.getProductCode();
		CacheKey key = new CacheKey(productCode, BRIDGE_SWITCH_KEY);
		Set<String> activeBridgeIds = CacheUtils.getObject(CacheName.BASEENTITY, key, Set.class);

		if (activeBridgeIds == null) {
			activeBridgeIds = new HashSet<String>();
		}

		activeBridgeIds.add(bridgeId);

		CacheUtils.putObject(CacheName.BASEENTITY, key, activeBridgeIds);
	}

	/**
	* Find an active bridge ID
	*
	* @param gennyToken Used to find the realm
	* @return String An active Bridge ID
	 */
	public static String findActiveBridgeId(GennyToken gennyToken) {

		String productCode = gennyToken.getProductCode();

		CacheKey key = new CacheKey(productCode, BRIDGE_SWITCH_KEY);
		Set<String> activeBridgeIds = CacheUtils.getObject(CacheName.BASEENTITY, key, Set.class);

		// null check
		if (activeBridgeIds == null) {
			return null;
		}

		// find first
		if (activeBridgeIds.iterator().hasNext()) {
			return activeBridgeIds.iterator().next();
		}

		return null;
	}

	/**
	* Put an entry into the users BridgeInfo item in the cache
	*
	* @param gennyToken The users GennyToken
	* @param bridgeId The ID of the bridge used in communication
	 */
	public static void put(GennyToken gennyToken, String bridgeId) {

		String productCode = gennyToken.getProductCode();
		CacheKey key = new CacheKey(productCode, BRIDGE_INFO_PREFIX + "_" + gennyToken.getUserCode());

		log.debug("Adding Switch to Cache --- " + key + " :: " + bridgeId);
		
		// grab from cache or create if null
		BridgeInfo info = CacheUtils.getObject(CacheName.BASEENTITY, key, BridgeInfo.class);
		
		if (info == null) {
			info = new BridgeInfo();
		}

		// add entry for jti and update in cache
		String jti = gennyToken.getJTI();
		info.mappings.put(jti, bridgeId);

		CacheUtils.putObject(CacheName.BASEENTITY, key, info);
	}

	/**
	* Get the corresponding bridgeId from the users BridgeInfo 
	* object in the cache.
	*
	* @param gennyToken The users GennyToken
	* @return String The corresponding bridgeId
	 */
	public static String get(GennyToken gennyToken) {

		String productCode = gennyToken.getProductCode();
		CacheKey key = new CacheKey(productCode, BRIDGE_INFO_PREFIX + "_" + gennyToken.getUserCode());
		
		// grab from cache
		BridgeInfo info = CacheUtils.getObject(CacheName.BASEENTITY, key, BridgeInfo.class);
		
		if (info == null) {
			log.debug("No BridgeInfo object found for user " + gennyToken.getUserCode());
			return null;
		}

		// grab entry for jti
		String jti = gennyToken.getJTI();
		String bridgeId = info.mappings.get(jti);

		log.debug("Found Switch --- " + key + " :: " + bridgeId);

		return bridgeId;
	}

}
