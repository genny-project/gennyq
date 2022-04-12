package life.genny.qwandaq.message;

public class QEventDropdownMessage extends QEventMessage {
	
	private Integer pageIndex = 0;
	private Integer pageSize = 20;
	
	private static final long serialVersionUID = 1L;

	private static final String EVENT_TYPE_DROPDOWN = "DD";

	public QEventDropdownMessage() {
		super(EVENT_TYPE_DROPDOWN);
	}

	public QEventDropdownMessage(String fieldCode) {
		super(EVENT_TYPE_DROPDOWN, fieldCode);
	}

	@Override
	public String toString() {
		return "QEventDropdownMessage [" + (pageIndex != null ? "pageIndex=" + pageIndex + ", " : "")
				+ (pageSize != null ? "pageSize=" + pageSize + ", " : "") + (data != null ? "data=" + data : "") + "]";
	}

	/**
	 * @return the pageIndex
	 */
	public Integer getPageIndex() {
		return pageIndex;
	}

	/**
	 * @param pageIndex the pageIndex to set
	 */
	public void setPageIndex(Integer pageIndex) {
		this.pageIndex = pageIndex;
	}

	/**
	 * @return the pageSize
	 */
	public Integer getPageSize() {
		return pageSize;
	}

	/**
	 * @param pageSize the pageSize to set
	 */
	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}
	
}
