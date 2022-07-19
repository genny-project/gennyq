package life.genny.qwandaq.message;

import java.io.Serializable;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.entity.BaseEntity;

@RegisterForReflection
public class QSearchBeResult implements Serializable {

	private static final long serialVersionUID = 1L;
	Long total;
	List<String> codes;
	List<BaseEntity> entities;

	public QSearchBeResult() {}

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<String> getCodes() {
        return codes;
    }

    public void setCodes(List<String> codes) {
        this.codes = codes;
    }

    public List<BaseEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<BaseEntity> entities) {
        this.entities = entities;
    }
	
}
