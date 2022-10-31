package life.genny.qwandaq.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.entity.BaseEntity;

@RegisterForReflection
public class QBulkMessage extends QMessage {

	private static final long serialVersionUID = 1L;

	private List<BaseEntity> entities = new ArrayList<>();
	private List<Ask> asks = new ArrayList<>();

	public QBulkMessage() {
		super();
	}

	public QBulkMessage(List<BaseEntity> entities, List<Ask> asks) {
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

	public List<Ask> getAsks() {
		return asks;
	}

	public void setAsks(List<Ask> asks) {
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
