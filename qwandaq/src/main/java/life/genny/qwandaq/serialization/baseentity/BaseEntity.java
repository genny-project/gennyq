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
	private String dtype;

	@ProtoField(2)
	private Long id;

	@ProtoField(3)
	private LocalDateTime created;

	@ProtoField(4)
	private String name;

	@ProtoField(5)
	private String realm;

	@ProtoField(6)
	private LocalDateTime updated;

	@ProtoField(7)
	private String code;

	@ProtoField(8)
	private Integer status;

	@ProtoFactory
	public BaseEntity(String dtype, Long id, LocalDateTime created, String name, String realm, LocalDateTime updated,
			String code, Integer status) {
		super();
		this.dtype = dtype;
		this.id = id;
		this.created = created;
		this.name = name;
		this.realm = realm;
		this.updated = updated;
		this.code = code;
		this.status = status;
	}

	public BaseEntity() {
	}

	public String getDtype() {
		return dtype;
	}

	public void setDtype(String dtype) {
		this.dtype = dtype;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	@Override
	public CoreEntityPersistable toPersistableCoreEntity() {
		life.genny.qwandaq.entity.BaseEntity baseEntity = new life.genny.qwandaq.entity.BaseEntity();
		baseEntity.setCode(getCode());
		baseEntity.setCreated(getCreated());
		// baseEntity.setDtype();
		baseEntity.setId(getId());
		baseEntity.setName(getName());
		baseEntity.setRealm(getRealm());
		baseEntity.setStatus(EEntityStatus.valueOf(getStatus()));
		baseEntity.setUpdated(getUpdated());
		return baseEntity;
	}

}
