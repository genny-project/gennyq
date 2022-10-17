package life.genny.qwandaq.message;

public class QCmd extends QMessage {

	private static final String MSG_TYPE = "CMD";

	private String command;

	public QCmd() {
		super(MSG_TYPE);
	}

	public QCmd(String command, String code) {
		this();
		this.command = command;
		setCode(code);
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}
	
}
