package life.genny.qwandaq.handlers;

import java.util.Map;

import org.jboss.logging.Logger;

import life.genny.qwandaq.utils.BaseEntityUtils;

public class RuleFlowGroupWorkItemHandler {

	static final Logger log = Logger.getLogger(RuleFlowGroupWorkItemHandler.class);

	public RuleFlowGroupWorkItemHandler() {
		log.error("RuleFlowGroup handler is incomplete!!!!");
	}
	
	/** 
	 * @param beUtils the be utility instance
	 * @param items the items to use
	 * @param ruleFlowGroup the rulegroup to run
	 * @param callingWorkflow the workflow calling this method
	 * @return Map&lt;String, Object&gt;
	 */
	public Map<String, Object> executeRules(BaseEntityUtils beUtils, Map<String, Object> items,
			String ruleFlowGroup, String callingWorkflow) {
		log.error("RuleFlowGroup executeRules function is incomplete!!!!");
		return null;
	}
}
