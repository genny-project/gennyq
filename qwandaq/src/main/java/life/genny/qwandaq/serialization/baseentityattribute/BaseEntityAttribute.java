package life.genny.qwandaq.serialization.baseentityattribute;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.persistence.Transient;

import org.javamoney.moneta.Money;

import life.genny.qwandaq.CoreEntity;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.serialization.CoreEntitySerializable;


public class BaseEntityAttribute implements CoreEntitySerializable {

	private static final long serialVersionUID = 1L;

	private String baseEntityCode;

	private String attributeCode;

	private LocalDateTime created;

	private Boolean inferred = false;

	private Boolean privacyFlag = false;

	private Boolean readonly = false;

	private String realm;

	private LocalDateTime updated;

	private Double valueDouble;

	private Boolean valueBoolean;

	private Integer valueInteger;

	private Long valueLong;

	private LocalTime valueTime;

	private LocalDateTime valueDateTime;

	private LocalDate valueDate;

	private String valueString;

	private Money money;

	private Double weight;
	
	private Long attributeId;
	
	private Long baseEntityId;
	
	private String icon;

	private Boolean confirmationFlag = false;

	@Transient
	private BaseEntity baseEntity;

	@Transient
	private Attribute attribute;

	public BaseEntityAttribute() {
	}

	public BaseEntityAttribute(String baseEntityCode, String attributeCode, LocalDateTime created, Boolean inferred,
			Boolean privacyFlag, Boolean readonly, String realm, LocalDateTime updated, Double valueDouble,
			Boolean valueBoolean, Integer valueInteger, Long valueLong, LocalTime valueTime,
			LocalDateTime valueDateTime, LocalDate valueDate, String valueString, Money valueMoney, Double weight,
			Attribute attribute, BaseEntity baseEntity, String icon, Boolean confirmationFlag) {
		super();
		this.baseEntityCode = baseEntityCode;
		this.attributeCode = attributeCode;
		this.created = created;
		this.inferred = inferred;
		this.privacyFlag = privacyFlag;
		this.readonly = readonly;
		this.realm = realm;
		this.updated = updated;
		this.valueDouble = valueDouble;
		this.valueBoolean = valueBoolean;
		this.valueInteger = valueInteger;
		this.valueLong = valueLong;
		this.valueTime = valueTime;
		this.valueDateTime = valueDateTime;
		this.valueDate = valueDate;
		this.valueString = valueString;
		this.money = valueMoney;
		this.weight = weight;
		this.attribute = attribute;
		this.baseEntity = baseEntity;
		this.icon = icon;
		this.confirmationFlag = confirmationFlag;
	}

	public String getBaseEntityCode() {
		return baseEntityCode;
	}

	public void setBaseEntityCode(String baseEntityCode) {
		this.baseEntityCode = baseEntityCode;
	}

	public String getAttributeCode() {
		return attributeCode;
	}

	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
	}

	public LocalDateTime getCreated() {
		return created;
	}

	public void setCreated(LocalDateTime created) {
		this.created = created;
	}

	public Boolean getInferred() {
		return inferred;
	}

	public void setInferred(Boolean inferred) {
		this.inferred = inferred;
	}

	public Boolean getPrivacyFlag() {
		return privacyFlag;
	}

	public void setPrivacyFlag(Boolean privacyFlag) {
		this.privacyFlag = privacyFlag;
	}

	public Boolean getReadonly() {
		return readonly;
	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
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

	public Double getValueDouble() {
		return valueDouble;
	}

	public void setValueDouble(Double valueDouble) {
		this.valueDouble = valueDouble;
	}

	public Boolean getValueBoolean() {
		return valueBoolean;
	}

	public void setValueBoolean(Boolean valueBoolean) {
		this.valueBoolean = valueBoolean;
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

	public LocalTime getValueTime() {
		return valueTime;
	}

	public void setValueTime(LocalTime valueTime) {
		this.valueTime = valueTime;
	}

	public LocalDateTime getValueDateTime() {
		return valueDateTime;
	}

	public void setValueDateTime(LocalDateTime valueDateTime) {
		this.valueDateTime = valueDateTime;
	}

	public LocalDate getValueDate() {
		return valueDate;
	}

	public void setValueDate(LocalDate valueDate) {
		this.valueDate = valueDate;
	}

	public String getValueString() {
		return valueString;
	}

	public void setValueString(String valueString) {
		this.valueString = valueString;
	}

	public Money getMoney() {
		return money;
	}

	public void setMoney(Money valueMoney) {
		this.money = valueMoney;
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

	public Long getBaseEntityId() {
		return baseEntityId;
	}

	public void setBaseEntityId(Long baseEntityId) {
		this.baseEntityId = baseEntityId;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public Boolean getConfirmationFlag() {
		return confirmationFlag;
	}

	public void setConfirmationFlag(Boolean confirmationFlag) {
		this.confirmationFlag = confirmationFlag;
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
		EntityAttribute ea = new EntityAttribute();
		ea.setRealm(getRealm());
		ea.setBaseEntityCode(getBaseEntityCode());
		ea.setAttributeCode(getAttributeCode());
		ea.setCreated(getCreated());
		ea.setInferred(getInferred());
		ea.setPrivacyFlag(getPrivacyFlag());
		ea.setReadonly(getReadonly());
		ea.setUpdated(getUpdated());
		ea.setValueBoolean(getValueBoolean());
		ea.setValueDate(getValueDate());
		ea.setValueDateTime(getValueDateTime());
		ea.setValueDouble(getValueDouble());
		ea.setValueInteger(getValueInteger());
		ea.setValueLong(getValueLong());
		ea.setValueMoney(getMoney());
		ea.setValueString(getValueString());
		ea.setUpdated(getUpdated());
		ea.setWeight(getWeight());
		ea.setAttribute(getAttribute());
		ea.setBaseEntity(getBaseEntity());
		// bea.setIcon(geticon);
		ea.setConfirmationFlag(getConfirmationFlag());
		return ea;
	}
}
