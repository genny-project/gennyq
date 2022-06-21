package life.genny.qwandaq.models;

// import javax.json.JsonObject;

public class Service2ServiceData {

	private String processId;
	// private JsonObject payload;

	public Service2ServiceData() { }

	public Service2ServiceData(String processId) {
		this.processId = processId;
	}

	public String getProcessId() {
		return this.processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	// public JsonObject getPayload() {
	// 	return this.payload;
	// }

	// public void setPayload(JsonObject payload) {
	// 	this.payload = payload;
	// }
}
