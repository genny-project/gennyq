package life.genny.qwandaq.serialization.question;

import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.converter.CapabilityConverter;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.time.LocalDateTime;

/*
 * A representation of Question in the cache
 * 
 * @author Varun Shastry
 */
public class Question implements CoreEntitySerializable {

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
	private String attributeCode;

	@ProtoField(10)
	private String directions;

	@ProtoField(11)
	private String helper = "";

	@ProtoField(12)
	private String html;

	@ProtoField(13)
	private String icon;

	@ProtoField(14)
	private Boolean mandatory;

	@ProtoField(15)
	private Boolean oneshot;

	@ProtoField(16)
	private String placeholder;

	@ProtoField(17)
	private Boolean readonly;

	@ProtoField(18)
	private Long attributeId;

	@ProtoField(19)
	private String capreqs;

	@ProtoFactory
	public Question(String dtype, Long id, LocalDateTime created, String name, String realm, LocalDateTime updated,
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

	public Question() {
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

	public String getAttributeCode() {
		return attributeCode;
	}

	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
	}

	public String getDirections() {
		return directions;
	}

	public void setDirections(String directions) {
		this.directions = directions;
	}

	public String getHelper() {
		return helper;
	}

	public void setHelper(String helper) {
		this.helper = helper;
	}

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public Boolean getMandatory() {
		return mandatory;
	}

	public void setMandatory(Boolean mandatory) {
		this.mandatory = mandatory;
	}

	public Boolean getOneshot() {
		return oneshot;
	}

	public void setOneshot(Boolean oneshot) {
		this.oneshot = oneshot;
	}

	public String getPlaceholder() {
		return placeholder;
	}

	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}

	public Boolean getReadonly() {
		return readonly;
	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Long getAttributeId() {
		return attributeId;
	}

	public void setAttributeId(Long attributeId) {
		this.attributeId = attributeId;
	}

	public String getCapreqs() {
		return capreqs;
	}

	public void setCapreqs(String capreqs) {
		this.capreqs = capreqs;
	}

	@Override
	public CoreEntityPersistable toPersistableCoreEntity() {
		life.genny.qwandaq.Question question = new life.genny.qwandaq.Question();
		question.setCode(getCode());
		question.setCreated(getCreated());
		// question.setDtype();
		question.setId(getId());
		question.setName(getName());
		question.setRealm(getRealm());
		question.setStatus(EEntityStatus.valueOf(getStatus()));
		question.setUpdated(getUpdated());
		question.setAttributeCode(getAttributeCode());
		question.setDirections(getDirections());
		question.setHelper(getHelper());
		question.setHtml(getHtml());
		question.setIcon(getIcon());
		question.setMandatory(getMandatory());
		question.setOneshot(getOneshot());
		question.setPlaceholder(getPlaceholder());
		question.setReadonly(getReadonly());
		question.setCapabilityRequirements(CapabilityConverter.convertToEA(getCapreqs()));
		return question;
	}

}
