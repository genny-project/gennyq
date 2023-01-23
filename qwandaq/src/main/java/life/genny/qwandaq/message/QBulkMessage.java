package life.genny.qwandaq.message;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.entity.BaseEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RegisterForReflection
public class QBulkMessage extends QMessage {

	private static final long serialVersionUID = 1L;

	private List<BaseEntity> entities = new ArrayList<>();
	private Set<Ask> asks = new HashSet<>();

	public QBulkMessage() {
		super();
	}

	public QBulkMessage(List<BaseEntity> entities, Set<Ask> asks) {
		super();
		this.entities = entities;
		this.asks = asks;
	}

	public void add(BaseEntity baseEntity) {
		this.entities.add(baseEntity);
	}

	public void add(Ask ask) {
		this.asks.add(ask);
	}

	public List<BaseEntity> getEntities() {
		return entities;
	}

	public void setEntities(List<BaseEntity> entities) {
		this.entities = entities;
	}

	public Set<Ask> getAsks() {
		return asks;
	}

	public void setAsks(Set<Ask> asks) {
		this.asks = asks;
	}

	/** 
	 * @return String
	 */
	@Override
	public String toString() {
		int entityCount = (entities == null) ? 0 : entities.size();
		int asksCount = (asks == null) ? 0 : asks.size();
		return "QBulkMessage [QDataBaseEntityMsgs= " + entityCount + " AskMsgs=" + asksCount + "]";
	}

}
