package life.genny.qwandaq.message;

import java.io.Serializable;


public class QEventMessage extends QMessage implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private static final String MESSAGE_TYPE = "EVT_MSG";
	private String event_type;
	public MessageData data;

	public QEventMessage()
        {
	   super();
        }
	
	/** 
	 * @return String
	 */
	@Override
	public String toString() {
		return "QEventMessage [event_type=" + event_type + ", data=" + data + "]";
	}
	
	/** 
	 * @return String
	 */
	public String getEvent_type() {
		return event_type;
	}
	
	/** 
	 * @param event_type the event type to set
	 */
	public void setEvent_type(String event_type) {
		this.event_type = event_type;
	}
	
	/** 
	 * @return MessageData
	 */
	public MessageData getData() {
		return data;
	}
	
	/** 
	 * @param data the message data to set
	 */
	public void setData(MessageData data) {
		this.data = data;
	}

	public QEventMessage(String eventType) {
		super(MESSAGE_TYPE);
	}
	
	public QEventMessage(String eventType, String code) {
		super(MESSAGE_TYPE);
		this.event_type = eventType;
		this.data = new MessageData(code);
	}
	
}
