package life.genny.qwandaq.serialization.baseentity;

import java.time.LocalDateTime;

import life.genny.qwandaq.CoreEntity;
import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/*
 * A representation of BaseEntity in the cache
 * 
 * @author Varun Shastry
 */
public class BaseEntity implements CoreEntitySerializable {

	@ProtoField(1)
	private LocalDateTime created;

	@ProtoField(2)
	private String name;

	@ProtoField(3)
	private String realm;

	@ProtoField(4)
	private LocalDateTime updated;

	@ProtoField(5)
	private String code;

	@ProtoField(6)
	private String capreqs;

	@ProtoFactory
	public BaseEntity(LocalDateTime created, String name, String realm, LocalDateTime updated,
			String code, String capreqs) {
		super();
		this.created = created;
		this.name = name;
		this.realm = realm;
		this.updated = updated;
		this.code = code;
		this.capreqs = capreqs;
	}

	public BaseEntity() {
	}

	public LocalDateTime getCreated() {
		return created;
	}

	public void setCreated(LocalDateTime created) {
		this.created = created;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public LocalDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(LocalDateTime updated) {
		this.updated = updated;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getCapreqs() {
		return capreqs;
	}

	public void setCapreqs(String capreqs) {
		this.capreqs = capreqs;
	}

	@Override
	public CoreEntityPersistable toPersistableCoreEntity() {
		life.genny.qwandaq.entity.BaseEntity baseEntity = new life.genny.qwandaq.entity.BaseEntity();
		baseEntity.setCode(getCode());
		baseEntity.setCreated(getCreated());
		baseEntity.setName(getName());
		baseEntity.setRealm(getRealm());
		baseEntity.setUpdated(getUpdated());
		baseEntity.setCapabilityRequirements();
		return baseEntity;
	}

}
