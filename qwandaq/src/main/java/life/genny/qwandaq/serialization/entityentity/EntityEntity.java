package life.genny.qwandaq.serialization.entityentity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.persistence.Transient;

import life.genny.qwandaq.Link;
import org.javamoney.moneta.Money;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.serialization.CoreEntitySerializable;


public class EntityEntity implements CoreEntitySerializable {

	private static final long serialVersionUID = 1L;

	private String targetCode;

	private LocalDateTime created;

	private String linkCode;

	private String childColor;

	private String linkValue;

	private String parentColor;

	private String rule;

	private String sourceCode;

	private String target_code;

	private Double linkWeight;

	private String realm;

	private LocalDateTime updated;;

	private Boolean valueBoolean;

	private LocalDate valueDate;

	private LocalDateTime valueDateTime;

	private Double valueDouble;

	private Integer valueInteger;

	private Long valueLong;

	private Money money;

	private String valueString;

	private LocalTime valueTime;

	private Long version;

	private Double weight;
	
	private Long attributeId;

	private Long sourceId;

	@Transient
	private BaseEntity baseEntity;

	@Transient
	private Attribute attribute;

	public String getTargetCode() {
		return targetCode;
	}

	public void setTargetCode(String targetCode) {
		this.targetCode = targetCode;
	}

	public LocalDateTime getCreated() {
		return created;
	}

	public void setCreated(LocalDateTime created) {
		this.created = created;
	}

	public String getLinkCode() {
		return linkCode;
	}

	public void setLinkCode(String linkCode) {
		this.linkCode = linkCode;
	}

	public String getChildColor() {
		return childColor;
	}

	public void setChildColor(String childColor) {
		this.childColor = childColor;
	}

	public String getLinkValue() {
		return linkValue;
	}

	public void setLinkValue(String linkValue) {
		this.linkValue = linkValue;
	}

	public String getParentColor() {
		return parentColor;
	}

	public void setParentColor(String parentColor) {
		this.parentColor = parentColor;
	}

	public String getRule() {
		return rule;
	}

	public void setRule(String rule) {
		this.rule = rule;
	}

	public String getSourceCode() {
		return sourceCode;
	}

	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	public String getTarget_code() {
		return target_code;
	}

	public void setTarget_code(String target_code) {
		this.target_code = target_code;
	}

	public Double getLinkWeight() {
		return linkWeight;
	}

	public void setLinkWeight(Double linkWeight) {
		this.linkWeight = linkWeight;
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

	public Boolean getValueBoolean() {
		return valueBoolean;
	}

	public void setValueBoolean(Boolean valueBoolean) {
		this.valueBoolean = valueBoolean;
	}

	public LocalDate getValueDate() {
		return valueDate;
	}

	public void setValueDate(LocalDate valueDate) {
		this.valueDate = valueDate;
	}

	public LocalDateTime getValueDateTime() {
		return valueDateTime;
	}

	public void setValueDateTime(LocalDateTime valueDateTime) {
		this.valueDateTime = valueDateTime;
	}

	public Double getValueDouble() {
		return valueDouble;
	}

	public void setValueDouble(Double valueDouble) {
		this.valueDouble = valueDouble;
	}

	public Integer getValueInteger() {
		return valueInteger;
	}

	public void setValueInteger(Integer valueInteger) {
		this.valueInteger = valueInteger;
	}

	public Long getValueLong() {
		return valueLong;
	}

	public void setValueLong(Long valueLong) {
		this.valueLong = valueLong;
	}

	public Money getMoney() {
		return money;
	}

	public void setMoney(Money money) {
		this.money = money;
	}

	public String getValueString() {
		return valueString;
	}

	public void setValueString(String valueString) {
		this.valueString = valueString;
	}

	public LocalTime getValueTime() {
		return valueTime;
	}

	public void setValueTime(LocalTime valueTime) {
		this.valueTime = valueTime;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public Long getAttributeId() {
		return attributeId;
	}

	public void setAttributeId(Long attributeId) {
		this.attributeId = attributeId;
	}

	public Long getSourceId() {
		return sourceId;
	}

	public void setSourceId(Long sourceId) {
		this.sourceId = sourceId;
	}

	public BaseEntity getBaseEntity() {
		return baseEntity;
	}

	public void setBaseEntity(BaseEntity baseEntity) {
		this.baseEntity = baseEntity;
	}

	public Attribute getAttribute() {
		return attribute;
	}

	public void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}

	@Override
	public CoreEntityPersistable toPersistableCoreEntity() {
		life.genny.qwandaq.entity.EntityEntity ee = new life.genny.qwandaq.entity.EntityEntity();
		ee.setTargetCode(getTargetCode());
		ee.setCreated(getCreated());
		// ee.setLinkCode();
		Link link = new Link();
		link.setAttributeCode(getAttribute().getCode());
		link.setChildColor(getChildColor());
		link.setLinkValue(getLinkValue());
		link.setParentColor(getParentColor());
		link.setRule(getRule());
		link.setSourceCode(getSourceCode());
		link.setTargetCode(getTargetCode());
		link.setWeight(getLinkWeight());
		ee.setLink(link);
		ee.setSourceCode(getSourceCode());
		ee.setTargetCode(getTarget_code());
		ee.setRealm(getRealm());
		ee.setUpdated(getUpdated());
		ee.setValueBoolean(getValueBoolean());
		ee.setValueDate(getValueDate());
		ee.setValueDateTime(getValueDateTime());
		ee.setValueDouble(getValueDouble());
		ee.setValueInteger(getValueInteger());
		ee.setValueLong(getValueLong());
		ee.setValueMoney(getMoney());
		ee.setValueString(getValueString());
		ee.setValueTime(getValueTime());
		ee.setVersion(getVersion());
		ee.setWeight(getWeight());
		return ee;
	}
}
