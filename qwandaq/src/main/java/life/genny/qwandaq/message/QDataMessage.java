package life.genny.qwandaq.message;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class QDataMessage <T> extends QMessage {

    private static final String MESSAGE_TYPE = "DATA_MSG";
	private String dataType;
	private Boolean replace = false;
	private String aliasCode;
	private List<T> items = new ArrayList<>();
	private Long total;

    public QDataMessage() {
		super(MESSAGE_TYPE);
	}

	public QDataMessage(T item) {
		super(MESSAGE_TYPE);
		add(item);
		updateDataType();
	}

	public QDataMessage(List<T> list) {
		super(MESSAGE_TYPE);
		add(list);
		updateDataType();
	}

	public static String getMessageType() {
		return MESSAGE_TYPE;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
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
		this.total = Long.valueOf(items.size());
    }

	public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public void add(T item) {
		this.items.add(item);
		this.total = Long.valueOf(items.size());
	}

	public void add(List<T> list) {
		this.items.addAll(list);
		this.total = Long.valueOf(items.size());
	}

	public void updateDataType() {
		if (items.isEmpty())
			return;
		setDataType(items.get(0).getClass().getName());
	}

}
