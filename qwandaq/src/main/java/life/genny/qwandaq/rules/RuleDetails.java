package life.genny.qwandaq.rules;

import java.io.Serializable;

import org.jboss.logging.Logger;

import org.apache.commons.lang3.StringUtils;

public class RuleDetails implements Serializable {

	static Logger log = Logger.getLogger(RuleDetails.class);

	String callingWorkflow;
	String ruleGroup;
	String note;
	
	public RuleDetails(String callingWorkflow, String ruleGroup, String note) {
		super();
		this.callingWorkflow = callingWorkflow;
		this.ruleGroup = ruleGroup;
		this.note = note;
	}
	
	public RuleDetails(String callingWorkflow, String ruleGroup) {
		this(callingWorkflow, ruleGroup,null);
	}
	
	public RuleDetails(String callingWorkflow) {
		this(callingWorkflow, null,null);
	}


	/**
	 * @return the callingWorkflow
	 */
	public String getCallingWorkflow() {
		return callingWorkflow;
	}

	/**
	 * @param callingWorkflow the callingWorkflow to set
	 */
	public void setCallingWorkflow(String callingWorkflow) {
		this.callingWorkflow = callingWorkflow;
	}

	/**
	 * @return the ruleGroup
	 */
	public String getRuleGroup() {
		return ruleGroup;
	}

	/**
	 * @param ruleGroup the ruleGroup to set
	 */
	public void setRuleGroup(String ruleGroup) {
		this.ruleGroup = ruleGroup;
	}

	/**
	 * @return the note
	 */
	public String getNote() {
		return note;
	}

	/**
	 * @param note the note to set
	 */
	public void setNote(String note) {
		this.note = note;
	}

	
	/** 
	 * @return String
	 */
	@Override
	public String toString() {
		String cw = callingWorkflow==null?"":callingWorkflow;
		String rg = ruleGroup==null?"":("-"+ruleGroup);
		String n = note==null?"":("-"+note);
		String op = cw+rg+n;
		if (StringUtils.isBlank(op)) {
			
		} else {
			op = op+":";
		}
		return op;
	}
	
	
}
