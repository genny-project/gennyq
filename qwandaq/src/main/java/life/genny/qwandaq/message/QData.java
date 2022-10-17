package life.genny.qwandaq.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class QData<T> extends QMessage {

	private static final String MSG_TYPE = "DATA";
	private Boolean replace = false;
	private String aliasCode;

	private List<T> items = new ArrayList<>();

	public QData() {
		super(MSG_TYPE);
	}

	public QData(List<T> items) {
		this();
		this.items = items;
	}

	public void add(T item) {
		this.items.add(item);
	}

	public void add(Collection<T> items) {
		this.items.addAll(items);
	}

	/**
	* @return String
	*/
	@Override
	public String toString() {
		return "QData []";
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

	public List<T> getItems() {
		return items;
	}

	public void setItems(List<T> items) {
		this.items = items;
	}

}
