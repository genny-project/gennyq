package life.genny.qwandaq.message;

public class QDropdownMessage extends QEventMessage {
	
	private static final long serialVersionUID = 1L;

	private Integer pageIndex = 0;
    private Integer pageSize = 20;

	private static final String EVENT_TYPE = "DROPDOWN";

	public QDropdownMessage() {
		super(EVENT_TYPE);
	}

	public QDropdownMessage(String code) {
		super(EVENT_TYPE, code);
	}
	
	public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public Integer getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(Integer pageIndex) {
        this.pageIndex = pageIndex;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

}
