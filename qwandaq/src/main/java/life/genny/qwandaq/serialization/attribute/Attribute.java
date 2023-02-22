package life.genny.qwandaq.serialization.attribute;

import java.time.LocalDateTime;
import java.util.List;
import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.converter.ValidationListConverter;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import life.genny.qwandaq.validation.Validation;

/*
 * A representation of Attribute in the cache
 * 
 * @author Varun Shastry
 */
public class Attribute implements CoreEntitySerializable {
	
	private String dtype;
	
	private Long id;
	
	private LocalDateTime created;
	
	private String name;
	
	private String realm;
	
	private LocalDateTime updated;
	
	private String code;

	private String className;

	private String dttCode;

	private String inputMask;

	private String typeName;

	private String validationList;

	private Boolean defaultPrivacyFlag;

	private String defaultValue;

	private String description;

	private String help;

	private String placeholder;

	private String component;

	private String icon;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getDttCode() {
		return dttCode;
	}

	public void setDttCode(String dttCode) {
		this.dttCode = dttCode;
	}

	public String getInputMask() {
		return inputMask;
	}

	public void setInputMask(String inputMask) {
		this.inputMask = inputMask;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public String getValidationList() {
		return validationList;
	}

	public void setValidationList(String validationList) {
		this.validationList = validationList;
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

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	private Integer status;

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
		life.genny.qwandaq.attribute.Attribute attribute = new life.genny.qwandaq.attribute.Attribute();
		attribute.setCode(getCode());
		attribute.setCreated(getCreated());
		attribute.setId(getId());
		attribute.setName(getName());
		attribute.setRealm(getRealm());
		attribute.setStatus(EEntityStatus.valueOf(getStatus()));
		attribute.setUpdated(getUpdated());
		attribute.setDefaultPrivacyFlag(getDefaultPrivacyFlag());
		DataType dataType = new DataType();
		dataType.setDttCode(getDttCode());
		dataType.setClassName(getClassName());
		dataType.setComponent(getComponent());
		dataType.setTypeName(getTypeName());
		List<Validation> validations = new ValidationListConverter().convertToEntityAttribute(getValidationList());
		dataType.setValidationList(validations);
		attribute.setDataType(dataType);
		return attribute;
	}

}
