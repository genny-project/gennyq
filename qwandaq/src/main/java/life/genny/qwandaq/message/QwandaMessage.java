package life.genny.qwandaq.message;

import java.io.Serializable;

 

public class QwandaMessage extends  QCmdMessage implements Serializable {
		
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public QDataAskMessage asks;
	public QBulkMessage askData;
	
	private static final String CMD_TYPE = "CMD_BULKASK";
	private static final String CODE = "QWANDAMESSAGE";

	
	public QwandaMessage() {
		super(CMD_TYPE, CODE);
		
	}

	
}