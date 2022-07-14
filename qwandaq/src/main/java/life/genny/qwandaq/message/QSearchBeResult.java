package life.genny.qwandaq.message;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.runtime.annotations.RegisterForReflection;

import life.genny.qwandaq.entity.BaseEntity;

@RegisterForReflection
public class QSearchBeResult implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String[] codes;
	Long total;

	// Added 19/08/2021
	// Users may want to just return the Entities instead of 
	// A list of codes
	BaseEntity[] entities;

	public QSearchBeResult() {}
	
	public QSearchBeResult(List<String> codes, Long total) {
		this.codes = codes.toArray(new String[0]);
		this.total = total;
	}

	public QSearchBeResult(BaseEntity[] entities, Long total) {
		this.codes = Stream.of(entities).map(x -> x.getCode()).collect(Collectors.toList()).toArray(String[]::new);
		this.entities = entities;
		this.total = total;
	}

	/**
	 * @param total the total to set
	 */
	public QSearchBeResult(Long total) {
		this.total = total;
	}
	/**
	 * @return the codes
	 */
	public String[] getCodes() {
		return codes;
	}
	/**
	 * @param codes the codes to set
	 */
	public void setCodes(List<String> codes) {
		this.codes = codes.toArray(new String[0]);
	}
	/**
	 * @return the total
	 */
	public Long getTotal() {
		return total;
	}
	/**
	 * @param total the total to set
	 */
	public void setTotal(Long total) {
		this.total = total;
	}

	
	/** 
	 * @param entities the entities to set
	 */
	public void setEntities(BaseEntity[] entities) {
		this.entities = entities;
	}

	
	/** 
	 * @return BaseEntity[]
	 */
	public BaseEntity[] getEntities() {
		return entities;
	}
	
	
}
