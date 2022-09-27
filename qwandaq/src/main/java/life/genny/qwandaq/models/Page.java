package life.genny.qwandaq.models;

import java.util.List;

import life.genny.qwandaq.entity.BaseEntity;

/**
 * Potion
 */
public class Page {

	List<String> codes;
	List<BaseEntity> items;
	Long total;

	Integer pageNumber;
	Integer pageSize;
	Long pageStart;

	public Page() {
	}

	public List<String> getCodes() {
		return codes;
	}

	public void setCodes(List<String> codes) {
		this.codes = codes;
	}

	public List<BaseEntity> getItems() {
		return items;
	}

	public void setItems(List<BaseEntity> items) {
		this.items = items;
	}

	public Long getTotal() {
		return total;
	}

	public void setTotal(Long total) {
		this.total = total;
	}

	public Integer getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public Long getPageStart() {
		return pageStart;
	}

	public void setPageStart(Long pageStart) {
		this.pageStart = pageStart;
	}

}
