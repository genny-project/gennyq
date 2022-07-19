package life.genny.qwandaq.message;

public class QEventMessage extends QMessage {
	
	private static final long serialVersionUID = 1L;
	private static final String MESSAGE_TYPE = "EVT_MSG";
    private String eventType;
	private String code;

	public QEventMessage() {
		super();
	}

	public QEventMessage(String eventType) {
		super(MESSAGE_TYPE);
	}

    public QEventMessage(String eventType, String code) {
		super(MESSAGE_TYPE);
		setEventType(eventType);
		setCode(code);
	}

	public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public static String getMessageType() {
        return MESSAGE_TYPE;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
}
