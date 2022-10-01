package life.genny.qwandaq.message;

public class QEvent extends QMessage {
	
	private static final long serialVersionUID = 1L;
	private static final String TYPE = "EVT_MSG";

	public QEvent() {
		super(TYPE);
	}
	
	public QEvent(String code) {
		super(TYPE, code);
	}
	
	/** 
	 * @return String
	 */
	@Override
	public String toString() {
		return "QEvent[code=" + getCode() + "]";
	}
}
