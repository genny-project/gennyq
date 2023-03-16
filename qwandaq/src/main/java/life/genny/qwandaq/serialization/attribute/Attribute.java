package life.genny.qwandaq.serialization.attribute;

import java.time.LocalDateTime;

import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.serialization.CoreEntitySerializable;

/*
 * A representation of BaseEntity in the cache
 * 
 * @author Varun Shastry
 */
public class Attribute implements CoreEntitySerializable {
	
	private LocalDateTime created;
	
	private String name;
	
	private String realm;
	
	private LocalDateTime updated;
	
	private String code;

	private String dttCode;

	private Boolean defaultPrivacyFlag;

	private String defaultValue;

	private String description;

	private String help;

	private String placeholder;

	private String icon;

	public String getDttCode() {
		return dttCode;
	}

	public void setDttCode(String dttCode) {
		this.dttCode = dttCode;
	}

	public Boolean getDefaultPrivacyFlag() {
		return defaultPrivacyFlag;
	}

	public void setDefaultPrivacyFlag(Boolean defaultPrivacyFlag) {
		this.defaultPrivacyFlag = defaultPrivacyFlag;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getHelp() {
		return help;
	}

	public void setHelp(String help) {
		this.help = help;
	}

	public String getPlaceholder() {
		return placeholder;
	}

	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	private Integer status;

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
		life.genny.qwandaq.attribute.Attribute attribute = new life.genny.qwandaq.attribute.Attribute();
		attribute.setCode(getCode());
		attribute.setCreated(getCreated());
		attribute.setName(getName());
		attribute.setRealm(getRealm());
		attribute.setStatus(EEntityStatus.valueOf(getStatus()));
		attribute.setUpdated(getUpdated());
		attribute.setDefaultPrivacyFlag(getDefaultPrivacyFlag());
		DataType dataType = new DataType();
		dataType.setCode(getDttCode());
		attribute.setDataType(dataType);
		return attribute;
	}

}
