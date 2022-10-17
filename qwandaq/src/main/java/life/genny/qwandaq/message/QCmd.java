package life.genny.qwandaq.message;

public class QCmd extends QMessage {

	private static final String MSG_TYPE = "CMD";

	public QCmd() {
		super(MSG_TYPE);
	}

	public QCmd(String code) {
		this();
		setCode(code);
	}
	
}
