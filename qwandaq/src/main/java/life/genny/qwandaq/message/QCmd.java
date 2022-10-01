package life.genny.qwandaq.message;

public class QCmd extends QMessage {

	private static final String TYPE = "CMD_MSG";

	public QCmd() {
		super(TYPE);
	}

	public QCmd(String code) {
		super(TYPE, code);
	}

}
