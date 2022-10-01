package life.genny.qwandaq.message;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public abstract class QData<T> extends QMessage {

	private static final String TYPE = "DATA_MSG";

	private List<T> items = new ArrayList<T>();

	private Boolean delete = false;
	private Boolean replace = false;
	private String aliasCode;

	public QData() {
		super(TYPE);
	}

	public QData(List<T> items) {
		super(TYPE);
		this.items = items;
	}

	/**
    * @return String
    */
	@Override
	public String toString() {
		return "QDataMessage []";
	}

	public List<T> getItems() {
		return items;
	}

	public void setItems(List<T> items) {
		this.items = items;
	}

	public Boolean getDelete() {
		return delete;
	}

	public void setDelete(Boolean delete) {
		this.delete = delete;
	}

	public Boolean getReplace() {
		return replace;
	}

	public void setReplace(Boolean replace) {
		this.replace = replace;
	}

	public String getAliasCode() {
		return aliasCode;
	}

	public void setAliasCode(String aliasCode) {
		this.aliasCode = aliasCode;
	}

}
