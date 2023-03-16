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
	private LocalDateTime created;

	@ProtoField(2)
	private String name;

	@ProtoField(3)
	private String realm;

	@ProtoField(3)
	private LocalDateTime updated;

	@ProtoField(4)
	private String code;

	@ProtoField(5)
	private Integer status;

	@ProtoField(6)
	private String attributeCode;

	@ProtoField(7)
	private String directions;

	@ProtoField(8)
	private String helper = "";

	@ProtoField(9)
	private String html;

	@ProtoField(10)
	private String icon;

	@ProtoField(11)
	private Boolean mandatory;

	@ProtoField(12)
	private Boolean oneshot;

	@ProtoField(13)
	private String placeholder;

	@ProtoField(14)
	private Boolean readonly;

	@ProtoField(15)
	private String capreqs;

	@ProtoFactory
	public Question(String dtype, Long id, LocalDateTime created, String name, String realm, LocalDateTime updated,
			String code, Integer status) {
		super();
		this.created = created;
		this.name = name;
		this.realm = realm;
		this.updated = updated;
		this.code = code;
		this.status = status;
	}

	public Question() {
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
