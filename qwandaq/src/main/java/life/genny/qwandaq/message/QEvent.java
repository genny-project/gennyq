package life.genny.qwandaq.message;

public class QEvent extends QMessage {
	
	private static final String MSG_TYPE = "EVENT";

	public QEvent() {
		super(MSG_TYPE);
	}

	public QEvent(String code) {
		this();
		setCode(code);
	}

	/** 
	 * @return String
	 */
	@Override
	public String toString() {
		return "QEvent [msgType=" + getMsgType() + ", code=" + getCode() + "]";
	}
}
