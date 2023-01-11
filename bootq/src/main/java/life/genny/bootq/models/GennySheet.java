package life.genny.bootq.models;

import java.util.Map;

public abstract class GennySheet {

	private String sheetId;
	private String name;
	private String productCode;

    public GennySheet(Map<String, String> map) {
		this.sheetId = map.get("sheetId");
		this.name = map.get("name");
		this.productCode = map.get("code");
	}

	public String getSheetId() {
		return sheetId;
	}

	public void setSheetId(String sheetId) {
		this.sheetId = sheetId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String realm) {
		this.productCode = realm;
	}

}
