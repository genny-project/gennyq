package life.genny.qwandaq.serialization.questionquestion;

import life.genny.qwandaq.serialization.common.CoreEntityKey;

public class QuestionQuestionKey implements CoreEntityKey {
	public static final String QQ_KEY_DELIMITER = "|";

	private String realm;
	private String parentCode;

	private String childCode;

	// No-arg constructor
	public QuestionQuestionKey() { }

	public QuestionQuestionKey(String realm, String parentCode, String childCode) {
		super(); // TODO: Why call java.lang.Object constructor?
		this.realm = realm;
		this.parentCode = parentCode;
		this.childCode = childCode;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getParentCode() {
		return parentCode;
	}

	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}

	public String getChildCode() {
		return childCode;
	}

	public void setChildCode(String childCode) {
		this.childCode = childCode;
	}

	@Override
	public String toString() {
		return getKeyString();
	}

	// Core Entity Key Overrides

	@Override
	public String getDelimiter() {
		return QQ_KEY_DELIMITER;
	}

	@Override
	public QuestionQuestionKey fromKey(String key) {
		String[] args = key.split(getDelimiter());
		return new QuestionQuestionKey(args[0], args[1], args[2]);
	}
	@Override
	public String getKeyString() {
		return realm + getDelimiter() + parentCode + getDelimiter() + childCode;
	}

	@Override
	public String getEntityCode() {
		return parentCode + getDelimiter() + childCode;
	}

	@Override
	public int hashCode() {
		return getKeyString().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if(!(other instanceof QuestionQuestionKey)) {
			return false;
		}
		QuestionQuestionKey otherQQ = (QuestionQuestionKey) other;
		return getKeyString().equals(otherQQ.getKeyString());
	}
}
