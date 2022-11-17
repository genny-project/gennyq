package life.genny.qwandaq.message;

import java.io.Serializable;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.entity.search.SearchEntity;

@RegisterForReflection
public class QSearchMessage extends QMessage implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private static final String MESSAGE_TYPE = "SCH_MSG";
	private SearchEntity searchEntity;
	private String destination;

	public QSearchMessage() {
		super(MESSAGE_TYPE);
	}

	public QSearchMessage(SearchEntity searchEntity) {
		super(MESSAGE_TYPE);
		this.searchEntity = searchEntity;
	}

	/** 
	 * @return String
	 */
	@Override
	public String toString() {
		return "QSearchMessage [searchEntity=" + searchEntity.getCode() + "]";
	}

	
	/** 
	 * @param searchEntity the searchEntity to set
	 */
	public void setSearchEntity(SearchEntity searchEntity) {
		this.searchEntity = searchEntity;
	}

	
	/** 
	 * @return SearchEntity
	 */
	public SearchEntity getSearchEntity() {
		return this.searchEntity;
	}
	
	
	/** 
	 * @param destination the destination to set
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}

	
	/** 
	 * @return String
	 */
	public String getDestination() {
		return this.destination;
	}
}
