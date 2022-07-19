package life.genny.qwandaq.message;

public class QCommandMessage extends QMessage {

	private static final String MESSAGE_TYPE = "CMD_MSG";
	private String cmdType;
	private String code;
	private String message;

	public QCommandMessage() {
		super(MESSAGE_TYPE);
	}

	public QCommandMessage(String cmdType, String code) {
		super(MESSAGE_TYPE);
		this.code = code;
		this.cmdType = cmdType;
	}

    public static String getMessageType() {
        return MESSAGE_TYPE;
    }

    public String getCmdType() {
        return cmdType;
    }

    public void setCmdType(String cmdType) {
        this.cmdType = cmdType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
