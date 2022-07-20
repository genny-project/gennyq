package life.genny.qwandaq.attribute;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.json.Json;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.javamoney.moneta.Money;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.converter.MoneyConverter;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.DebugException;
import life.genny.qwandaq.exception.NullParameterException;

@Entity
@Table(name = "baseentity_attribute", indexes = {
		@Index(columnList = "baseEntityCode", name = "ba_idx"),
		@Index(columnList = "attributeCode", name = "ba_idx"),
		@Index(columnList = "valueString", name = "ba_idx"),
		@Index(columnList = "valueBoolean", name = "ba_idx")
}, uniqueConstraints = @UniqueConstraint(columnNames = { "attributeCode", "baseEntityCode", "realm" }))
@AssociationOverrides({
	@AssociationOverride(name = "pk.baseEntity", joinColumns = @JoinColumn(name = "BASEENTITY_ID")),
	@AssociationOverride(name = "pk.attribute", joinColumns = @JoinColumn(name = "ATTRIBUTE_ID")) 
})
@RegisterForReflection
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class EntityAttribute implements Serializable, Comparable<Object> {

	private static final Logger log = Logger.getLogger(EntityAttribute.class);

	private static final long serialVersionUID = 1L;

	final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:SS");
	final static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	final static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

	@Column
	private String baseEntityCode;

	@Column
	private String attributeCode;

	@Transient
	@Column
	private String attributeName;

	@Column
	private Boolean readonly = false;

	private String realm;

	@Transient
	private String feedback = null;

	@EmbeddedId
	@Column
	private EntityAttributeId pk = new EntityAttributeId();

	/**
	 * Stores the Created UMT DateTime that this object was created
	 */
	@Column(name = "created")
	private LocalDateTime created;

	/**
	 * Stores the Last Modified UMT DateTime that this object was last updated
	 */
	@Column(name = "updated")
	private LocalDateTime updated;

	/**
	 * Store the Double value of the attribute for the baseEntity
	 */
	@Column
	private Double valueDouble;

	/**
	 * Store the Boolean value of the attribute for the baseEntity
	 */
	@Column
	private Boolean valueBoolean;
	/**
	 * Store the Integer value of the attribute for the baseEntity
	 */
	@Column
	private Integer valueInteger;

	/**
	 * Store the Long value of the attribute for the baseEntity
	 */
	@Column
	private Long valueLong;

	/**
	 * Store the LocalDateTime value of the attribute for the baseEntity
	 */
	@Column
	private LocalTime valueTime;

	/**
	 * Store the LocalDateTime value of the attribute for the baseEntity
	 */
	@Column
	private LocalDateTime valueDateTime;

	/**
	 * Store the LocalDate value of the attribute for the baseEntity
	 */
	@Column
	private LocalDate valueDate;

	/**
	 * Store the String value of the attribute for the baseEntity
	 */
	@Type(type = "text")
	@Column
	private String valueString;

	@Column(name = "money", length = 128)
	@Convert(converter = MoneyConverter.class)
	Money valueMoney;

	/**
	 * Store the relative importance of the attribute for the baseEntity
	 */
	@Column
	private Double weight;

	/**
	 * Store the relative importance of the attribute for the baseEntity
	 */
	private Boolean inferred = false;

	/**
	 * Store the privacy of this attribute , i.e. Don't display
	 */
	private Boolean privacyFlag = false;

	/**
	 * Store the confirmation
	 */
	private Boolean confirmationFlag = false;

	public EntityAttribute() {
	}

	/**
	 * Constructor.
	 * 
	 * @param baseEntity the entity that needs to contain attributes
	 * @param attribute the associated Attribute
	 * @param weight the weighted importance of this attribute (relative to the other attributes)
	 * @param value the value associated with this attribute
	 */
	public EntityAttribute(final BaseEntity baseEntity, final Attribute attribute, Double weight, final Object value) {
		autocreateCreated();
		setBaseEntity(baseEntity);
		setAttribute(attribute);
		this.setPrivacyFlag(attribute.getDefaultPrivacyFlag());
		setWeight(weight != null ? weight : 0.0);
		setValue(value);
	}

	/**
	 * @return EntityAttributeId
	 */
	@JsonbTransient
	public EntityAttributeId getPk() {
		return pk;
	}

	/**
	 * @return the baseEntityCode
	 */
	public String getBaseEntityCode() {
		return baseEntityCode;
	}

	/**
	 * @param baseEntityCode the baseEntityCode to set
	 */
	public void setBaseEntityCode(final String baseEntityCode) {
		this.baseEntityCode = baseEntityCode;
	}

	/**
	 * @return the attributeCode
	 */
	public String getAttributeCode() {
		return attributeCode;
	}

	/**
	 * @param attributeCode
	 *                      the attributeCode to set
	 */
	public void setAttributeCode(final String attributeCode) {
		this.attributeCode = attributeCode;
	}

	/**
	 * @param pk the pk to set
	 */
	public void setPk(final EntityAttributeId pk) {
		this.pk = pk;
	}

	public void setBaseEntity(final BaseEntity baseEntity) {
		getPk().setBaseEntity(baseEntity);
		this.baseEntityCode = baseEntity.getCode();
		this.realm = baseEntity.getRealm();
	}

	/**
	 * @return Attribute
	 */
	@Transient
	public Attribute getAttribute() {
		return getPk().getAttribute();
	}

	/**
	 * @param attribute the attribute to set
	 */
	public void setAttribute(final Attribute attribute) {
		getPk().setAttribute(attribute);
		this.attributeCode = attribute.getCode();
		this.attributeName = attribute.getName();
	}

	/**
	 * @return Boolean
	 */
	public Boolean isConfirmationFlag() {
		return getConfirmationFlag();
	}

	/**
	 * @return Boolean
	 */
	public Boolean isInferred() {
		return getInferred();
	}

	/**
	 * @return Boolean
	 */
	public Boolean isPrivacyFlag() {
		return getPrivacyFlag();
	}

	/**
	 * @return Boolean
	 */
	public Boolean isReadonly() {
		return getReadonly();
	}

	/**
	 * @return Boolean
	 */
	public Boolean isValueBoolean() {
		return getValueBoolean();
	}

	/**
	 * @return the created
	 */
	@JsonbTransient
	public LocalDateTime getCreated() {
		return created;
	}

	/**
	 * @param created
	 *                the created to set
	 */
	public void setCreated(final LocalDateTime created) {
		this.created = created;
	}

	/**
	 * @return the updated
	 */
	// @JsonbTransient
	public LocalDateTime getUpdated() {
		return updated;
	}

	/**
	 * @param updated
	 *                the updated to set
	 */
	public void setUpdated(final LocalDateTime updated) {
		this.updated = updated;
	}

	/**
	 * @return the weight
	 */
	public Double getWeight() {
		return weight;
	}

	/**
	 * @param weight
	 *               the weight to set
	 */
	public void setWeight(final Double weight) {
		this.weight = weight;
	}

	/**
	 * @return the valueDouble
	 */
	public Double getValueDouble() {
		return valueDouble;
	}

	/**
	 * @param valueDouble
	 *                    the valueDouble to set
	 */
	public void setValueDouble(final Double valueDouble) {
		this.valueDouble = valueDouble;
	}

	/**
	 * @return the valueInteger
	 */
	public Integer getValueInteger() {
		return valueInteger;
	}

	/**
	 * @param valueInteger
	 *                     the valueInteger to set
	 */
	public void setValueInteger(final Integer valueInteger) {
		this.valueInteger = valueInteger;
	}

	/**
	 * @return the valueLong
	 */
	public Long getValueLong() {
		return valueLong;
	}

	/**
	 * @param valueLong
	 *                  the valueLong to set
	 */
	public void setValueLong(final Long valueLong) {
		this.valueLong = valueLong;
	}

	/**
	 * @return LocalDate
	 */
	public LocalDate getValueDate() {
		return valueDate;
	}

	/**
	 * @param valueDate the valueDate to set
	 */
	public void setValueDate(LocalDate valueDate) {
		this.valueDate = valueDate;
	}

	/**
	 * @return the valueDateTime
	 */
	public LocalDateTime getValueDateTime() {
		return valueDateTime;
	}

	/**
	 * @param valueDateTime
	 *                      the valueDateTime to set
	 */
	public void setValueDateTime(final LocalDateTime valueDateTime) {
		this.valueDateTime = valueDateTime;
	}

	/**
	 * @return the valueTime
	 */
	public LocalTime getValueTime() {
		return valueTime;
	}

	/**
	 * @param valueTime
	 *                  the valueTime to set
	 */
	public void setValueTime(LocalTime valueTime) {
		this.valueTime = valueTime;
	}

	/**
	 * @return the valueString
	 */
	public String getValueString() {
		return valueString;
	}

	/**
	 * @param valueString
	 *                    the valueString to set
	 */
	public void setValueString(final String valueString) {
		this.valueString = valueString;
	}

	/**
	 * @return Boolean
	 */
	public Boolean getValueBoolean() {
		return valueBoolean;
	}

	/**
	 * @param valueBoolean the valueBoolean to set
	 */
	public void setValueBoolean(Boolean valueBoolean) {
		this.valueBoolean = valueBoolean;
	}

	/**
	 * @return the valueMoney
	 */
	public Money getValueMoney() {
		return valueMoney;
	}

	/**
	 * @param valueMoney the valueMoney to set
	 */
	public void setValueMoney(Money valueMoney) {
		this.valueMoney = valueMoney;
	}

	/**
	 * @return the privacyFlag
	 */
	public Boolean getPrivacyFlag() {
		return privacyFlag;
	}

	/**
	 * @param privacyFlag
	 *                    the privacyFlag to set
	 */
	public void setPrivacyFlag(Boolean privacyFlag) {
		this.privacyFlag = privacyFlag;
	}

	/**
	 * @return the inferred
	 */
	public Boolean getInferred() {
		return inferred;
	}

	/**
	 * @param inferred
	 *                 the inferred to set
	 */
	public void setInferred(Boolean inferred) {
		this.inferred = inferred;
	}

	/**
	 * @return the readonly
	 */
	public Boolean getReadonly() {
		return readonly;
	}

	/**
	 * @param readonly the readonly to set
	 */
	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public static Logger getLog() {
        return log;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Boolean getConfirmationFlag() {
        return confirmationFlag;
    }

    public void setConfirmationFlag(Boolean confirmationFlag) {
        this.confirmationFlag = confirmationFlag;
    }

    /**
	 * @return the feedback
	 */
	public String getFeedback() {
		return feedback;
	}

	/**
	 * @param feedback the feedback to set
	 */
	public void setFeedback(String feedback) {
		this.feedback = feedback;
	}

	@PreUpdate
	public void autocreateUpdate() {
		setUpdated(LocalDateTime.now(ZoneId.of("Z")));
	}

	@PrePersist
	public void autocreateCreated() {
		if (getCreated() == null)
			setCreated(LocalDateTime.now(ZoneId.of("Z")));
	}

	/**
	 * @return Date
	 */
	@Transient
	@JsonbTransient
	public Date getCreatedDate() {

		final Date out = Date.from(created.atZone(ZoneId.systemDefault()).toInstant());

		return out;
	}

	/**
	 * @return Date
	 */
	@Transient
	@JsonbTransient
	public Date getUpdatedDate() {

		if (updated == null)
			return null;
		final Date out = Date.from(updated.atZone(ZoneId.systemDefault()).toInstant());

		return out;
	}

	/**
	 * Get the value of the EntityAttribute.
	 *
	 * @param <T> the type to return
	 * @return T
	 */
	@SuppressWarnings("unchecked")
	@Transient
	@JsonbProperty(nillable = true)
	public <T> T getValue() {

		Attribute attribute = pk.getAttribute();
		DataType dataType = attribute.getDataType();

		switch (dataType.getClassName()) {
			case "java.lang.Integer":
			case "Integer":
				return (T) valueInteger;
			case "java.time.LocalDateTime":
			case "LocalDateTime":
				return (T) valueDateTime;
			case "java.time.LocalTime":
			case "LocalTime":
				return (T) valueTime;
			case "java.lang.Long":
			case "Long":
				return (T) valueLong;
			case "java.lang.Double":
			case "Double":
				return (T) valueDouble;
			case "java.lang.Boolean":
			case "Boolean":
				return (T) valueBoolean;
			case "java.time.LocalDate":
			case "LocalDate":
				return (T) valueDate;
			case "org.javamoney.moneta.Money":
			case "Money":
				return (T) valueMoney;
			case "java.lang.String":
			default:
				return (T) valueString;
		}

	}

	/**
	 * Set the value
	 *
	 * @param <T>   the Type
	 * @param value the value to set
	 */
	@JsonbTransient
	@Transient
	public <T> void setValue(final Object value) {

		if (value == null)
			throw new NullParameterException("value");
		if (readonly)
			throw new DebugException("Cannot set value of readonly EntityAttribute: " + attributeCode);

		Attribute attribute = pk.getAttribute();
		DataType dataType = attribute.getDataType();

		switch (dataType.getClassName()) {

			case "java.lang.Integer":
			case "Integer":
				if (value instanceof BigDecimal)
					setValueInteger(((BigDecimal) value).intValue());
				else
					setValueInteger((Integer) value);
				break;

			case "java.time.LocalDateTime":
			case "LocalDateTime":
				setValueDateTime((LocalDateTime) value);
				break;

			case "java.time.LocalDate":
			case "LocalDate":
				setValueDate((LocalDate) value);
				break;

			case "java.lang.Long":
			case "Long":
				if (value instanceof BigDecimal)
					setValueLong(((BigDecimal) value).longValue());
				else
					setValueLong((Long) value);
				break;

			case "java.time.LocalTime":
			case "LocalTime":
				setValueTime((LocalTime) value);
				break;

			case "org.javamoney.moneta.Money":
			case "Money":
				setValueMoney((Money) value);
				break;

			case "java.lang.Double":
			case "Double":
				if (value instanceof BigDecimal)
					setValueDouble(((BigDecimal) value).doubleValue());
				else
					setValueDouble((Double) value);
				break;

			case "java.lang.Boolean":
			case "Boolean":
				setValueBoolean((Boolean) value);
				break;

			case "java.lang.String":
			case "String":
			default:
				setValueString((String) value);
				break;
		}
	}

	/**
	 * @return String
	 */
	@Transient
	@JsonbTransient
	public String getValueAsString() {

		Attribute attribute = pk.getAttribute();
		DataType dataType = attribute.getDataType();

		switch (dataType.getClassName()) {
			case "java.lang.Integer":
			case "Integer":
				return String.valueOf(valueInteger);

			case "java.time.LocalDateTime":
			case "LocalDateTime":
				return valueDateTime.format(dateTimeFormatter);

			case "java.time.LocalDate":
			case "LocalDate":
				return valueDate.format(dateFormatter);

			case "java.lang.Long":
			case "Long":
				return String.valueOf(valueLong);

			case "java.time.LocalTime":
			case "LocalTime":
				return valueTime.format(timeFormatter);

			case "org.javamoney.moneta.Money":
			case "Money":
				String amount = new DecimalFormat("###############0.00").format(getValueMoney().getNumber().doubleValue());
				return Json.createObjectBuilder()
					.add("amount", amount)
					.add("currency", getValueMoney().getCurrency().getCurrencyCode())
					.build()
					.toString();

			case "java.lang.Double":
			case "Double":
				return String.valueOf(valueDouble);

			case "java.lang.Boolean":
			case "Boolean":
				return valueBoolean ? "TRUE" : "FALSE";

			case "java.lang.String":
			default:
				return valueString;
		}
	}

	/**
	 * @return int
	 */
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(baseEntityCode).append(attributeCode).toHashCode();
	}

	/**
	 * @param obj the object to compare to
	 * @return boolean
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj)
			return true;
		if (!(obj instanceof EntityAttribute))
			return false;

		EntityAttribute that = (EntityAttribute) obj;
		return new EqualsBuilder()
			.append(baseEntityCode, that.baseEntityCode)
			.append(attributeCode, that.attributeCode)
			.isEquals();
	}

	/**
	 * Compare to an object
	 *
	 * @param obj object to compare to
	 * @return int
	 */
	public int compareTo(Object obj) {

		EntityAttribute myClass = (EntityAttribute) obj;
		final String dataType = getPk().getAttribute().getDataType().getClassName();

		switch (dataType) {

			case "java.lang.Integer":
			case "Integer":
				return new CompareToBuilder().append(this.getValueInteger(), myClass.getValueInteger()).toComparison();

			case "java.time.LocalDateTime":
			case "LocalDateTime":
				return new CompareToBuilder().append(this.getValueDateTime(), myClass.getValueDateTime())
						.toComparison();

			case "java.time.LocalTime":
			case "LocalTime":
				return new CompareToBuilder().append(this.getValueTime(), myClass.getValueTime()).toComparison();

			case "java.lang.Long":
			case "Long":
				return new CompareToBuilder().append(this.getValueLong(), myClass.getValueLong()).toComparison();

			case "java.lang.Double":
			case "Double":
				return new CompareToBuilder().append(this.getValueDouble(), myClass.getValueDouble()).toComparison();

			case "java.lang.Boolean":
			case "Boolean":
				return new CompareToBuilder().append(this.getValueBoolean(), myClass.getValueBoolean()).toComparison();

			case "java.time.LocalDate":
			case "LocalDate":
				return new CompareToBuilder().append(this.getValueDate(), myClass.getValueDate()).toComparison();

			case "org.javamoney.moneta.Money":
			case "Money":
				return new CompareToBuilder().append(this.getValueMoney(), myClass.getValueMoney()).toComparison();

			case "java.lang.String":
			default:
				return new CompareToBuilder().append(this.getValueString(), myClass.getValueString()).toComparison();

		}
	}

	@Override
	public String toString() {
		return "attributeCode=" + attributeCode + ", value=" + getValueAsString() + ", weight=" + weight + ", inferred=" + inferred + "] be=" + this.getBaseEntityCode();
	}

}
