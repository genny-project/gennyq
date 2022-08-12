package life.genny.qwandaq.session.stack;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.CacheUtils;

/**
 * A stack management class for stacking redirect codes.
 * 
 * @author Jasper Robison
 */
public class StackCache {

	static final Logger log = Logger.getLogger(StackCache.class);

	public static String USER_STACK_PREFIX = "USK";

	/**
	* Put an entry into the users UserStack item in the cache
	*
	* @param gennyToken The users GennyToken
	* @param code The ID of the bridge used in communication
	 */
	public static void put(GennyToken gennyToken, String code) {

		String product = gennyToken.getProductCode();
		String key = USER_STACK_PREFIX + "_" + gennyToken.getUserCode();

		log.debug("Adding Switch to Cache --- " + key + " :: " + code);
		
		// grab from cache or create if null
		UserStack userStack = CacheUtils.getObject(product, key, UserStack.class);
		
		if (userStack == null) {
			userStack = new UserStack();
		}

		String jti = gennyToken.getJTI();
		List<String> stack = userStack.mappings.get(jti);
		if (stack == null) {
			stack = new ArrayList<String>();
		}

		stack.add(code);

		// add entry for jti and update in cache
		userStack.mappings.put(jti, stack);

		CacheUtils.putObject(product, key, stack);
	}

	/**
	* Get the corresponding bridgeId from the users UserStack 
	* object in the cache.
	*
	* @param gennyToken The users GennyToken
	* @return String The corresponding bridgeId
	 */
	public static String get(GennyToken gennyToken) {

		String product = gennyToken.getProductCode();
		String key = USER_STACK_PREFIX + "_" + gennyToken.getUserCode();
		
		// grab from cache
		UserStack userStack = CacheUtils.getObject(product, key, UserStack.class);
		
		if (userStack == null) {
			log.debug("No UserStack object found for user " + gennyToken.getUserCode());
		}

		log.debug("Found UserStack --- " + key);

		// grab entry for jti
		String jti = gennyToken.getJTI();

		List<String> stack = userStack.mappings.get(jti);
		Integer index = stack.size() - 1;

		String code = stack.get(index);
		stack.remove(index);

		return code;
	}

}
