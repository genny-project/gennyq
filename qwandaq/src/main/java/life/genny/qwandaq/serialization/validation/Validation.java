package life.genny.qwandaq.serialization.validation;

import java.time.LocalDateTime;

import life.genny.qwandaq.CoreEntity;
import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.converter.ValidationListConverter;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/*
 * A representation of Validation in the cache
 * 
 * @author Varun Shastry
 */
public class Validation implements CoreEntitySerializable {

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

	@ProtoField(9)
	private String errorMsg;

	@ProtoField(10)
	private Boolean multiAllowed;

	@ProtoField(11)
	private String options;

	@ProtoField(12)
	private Boolean recursiveGroup;

	@ProtoField(13)
	private String regex;

	@ProtoField(11)
	private String selectionGroup;

	public Validation() {
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

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public Boolean getMultiAllowed() {
		return multiAllowed;
	}

	public void setMultiAllowed(Boolean multiAllowed) {
		this.multiAllowed = multiAllowed;
	}

	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

	public Boolean getRecursiveGroup() {
		return recursiveGroup;
	}

	public void setRecursiveGroup(Boolean recursiveGroup) {
		this.recursiveGroup = recursiveGroup;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public String getSelectionGroup() {
		return selectionGroup;
	}

	public void setSelectionGroup(String selectionGroup) {
		this.selectionGroup = selectionGroup;
	}

	@Override
	public CoreEntityPersistable toPersistableCoreEntity() {
		life.genny.qwandaq.validation.Validation validation = new life.genny.qwandaq.validation.Validation();
		validation.setErrormsg(getErrorMsg());
		validation.setOptions(getOptions());
		validation.setMultiAllowed(getMultiAllowed());
		validation.setRegex(getRegex());
		validation.setCode(getCode());
		validation.setRecursiveGroup(getRecursiveGroup());
		validation.setCreated(getCreated());
		validation.setUpdated(getUpdated());
		validation.setStatus(EEntityStatus.valueOf(getStatus()));
		validation.setRealm(getRealm());
		validation.setName(getName());
		ValidationListConverter validationListConverter = new ValidationListConverter();
		validation.setSelectionBaseEntityGroupList(validationListConverter.convertFromStringToStringList(getSelectionGroup()));
		return validation;
	}

}
