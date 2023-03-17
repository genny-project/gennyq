package life.genny.qwandaq.attribute;

import java.lang.invoke.MethodHandles;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.math.BigDecimal;
import java.io.StringReader;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.converter.CapabilityConverter;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.entity.HBaseEntity;
import life.genny.qwandaq.intf.ICapabilityHiddenFilterable;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import life.genny.qwandaq.serialization.entityattribute.EntityAttribute;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import org.javamoney.moneta.Money;
import javax.money.CurrencyUnit;
import javax.money.Monetary;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.JsonObject;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;

import com.fasterxml.jackson.annotation.JsonIgnore;

import life.genny.qwandaq.converter.MoneyConverter;
import life.genny.qwandaq.entity.BaseEntity;

@Entity
@Table(name = "baseentity_attribute", indexes = {
		@Index(columnList = "baseEntityCode", name = "ba_idx"),
		@Index(columnList = "attributeCode", name = "ba_idx"),
		@Index(columnList = "valueString", name = "ba_idx"),
		@Index(columnList = "valueBoolean", name = "ba_idx")
}, uniqueConstraints = @UniqueConstraint(columnNames = { "attributeCode", "baseEntityCode", "realm" }))
@AssociationOverrides({ @AssociationOverride(name = "pk.baseEntity", joinColumns = @JoinColumn(name = "BASEENTITY_ID")),
		@AssociationOverride(name = "pk.attribute", joinColumns = @JoinColumn(name = "ATTRIBUTE_ID")) })
@RegisterForReflection
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class HEntityAttribute implements CoreEntityPersistable, Comparable<Object>, ICapabilityHiddenFilterable {

	private static final Logger log = Logger.getLogger(HEntityAttribute.class);

	private static final long serialVersionUID = 1L;

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
	@XmlTransient
	@Column
	private Integer index = 0; // used to assist with ordering

	@Transient
	private String feedback = null;

	@EmbeddedId
	@Column
	public HEntityAttributeId pk = new HEntityAttributeId();

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

	// // please do not send this out to frontend no matter what
	// @JsonbTransient
	// @JsonIgnore
	@Column(name = "capreqs")
	@Convert(converter = CapabilityConverter.class)
	private Set<Capability> capabilityRequirements;

	@Enumerated(EnumType.ORDINAL)
	private EEntityStatus status = EEntityStatus.ACTIVE;

	public HEntityAttribute() {
	}

	/**
	 * Constructor.
	 *
	 * @param baseEntity
	 *                   the entity that needs to contain attributes
	 * @param attribute
	 *                   the associated Attribute
	 * @param weight
	 *                   the weighted importance of this attribute (relative to the
	 *                   other
	 *                   attributes)
	 */
	public HEntityAttribute(final HBaseEntity baseEntity, final HAttribute attribute, Double weight) {
		autocreateCreated();
		setBaseEntity(baseEntity);
		setAttribute(attribute);
		if (weight == null) {
			weight = 0.0; // This permits ease of adding attributes and hides
			// attribute from scoring.
		}
		setWeight(weight);
		setReadonly(false);
	}

	/**
	 * Constructor.
	 *
	 * @param baseEntity
	 *                   the entity that needs to contain attributes
	 * @param attribute
	 *                   the associated Attribute
	 * @param weight
	 *                   the weighted importance of this attribute (relative to the
	 *                   other
	 *                   attributes)
	 * @param value
	 *                   the value associated with this attribute
	 */
	public HEntityAttribute(final HBaseEntity baseEntity, final HAttribute attribute, Double weight, final Object value) {
		autocreateCreated();
		setBaseEntity(baseEntity);
		setAttribute(attribute);
		this.setPrivacyFlag(attribute.getDefaultPrivacyFlag());
		if (weight == null) {
			weight = 0.0; // This permits ease of adding attributes and hides
			// attribute from scoring.
			weight = 0.0; // This permits ease of adding attributes and hides attribute from scoring.
		}
		setWeight(weight);
		// Assume that Attribute Validation has been performed
		if (value != null) {
			setValue(value);
		}
	}

	public HEntityAttribute(Double weight, final Object value) {
		autocreateCreated();
		if (weight == null) {
			weight = 0.0; // This permits ease of adding attributes and hides attribute from scoring.
		}
		setWeight(weight);
		// Assume that Attribute Validation has been performed
		if (value != null) {
			setValue(value);
		}
	}

	@JsonbTransient
	@JsonIgnore
	public Set<Capability> getCapabilityRequirements() {
		return this.capabilityRequirements;
	}

	/**
	 * @return HEntityAttributeId
	 */
	@JsonIgnore
	@JsonbTransient
	public HEntityAttributeId getPk() {
		return pk;
	}

	/**
	 * @return the baseEntityCode
	 */
	public String getBaseEntityCode() {
		return baseEntityCode;
	}

	/**
	 * @param baseEntityCode
	 *                       the baseEntityCode to set
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
	public void setPk(final HEntityAttributeId pk) {
		this.pk = pk;
	}

	public void setBaseEntity(final HBaseEntity baseEntity) {
		getPk().setBaseEntity(baseEntity);
		this.baseEntityCode = baseEntity.getCode();
		this.realm = baseEntity.getRealm();
	}

	@Transient
	@JsonbTransient
	public HBaseEntity getBaseEntity() {
		return getPk().getBaseEntity();
	}

	/**
	 * @return Attribute
	 */
	@Transient
	// @JsonIgnore
	public HAttribute getAttribute() {
		return getPk().getAttribute();
	}

	/**
	 * @param attribute the attribute to set
	 */
	public void setAttribute(final HAttribute attribute) {
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

	// /**
	// * @return the valueDateRange
	// */
	// public Range<LocalDate> getValueDateRange() {
	// return valueDateRange;
	// }
	//
	// /**
	// * @param valueDateRange the valueDateRange to set
	// */
	// public void setValueDateRange(Range<LocalDate> valueDateRange) {
	// this.valueDateRange = valueDateRange;
	// }

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
	@JsonIgnore
	@JsonbTransient
	public Date getCreatedDate() {

		final Date out = Date.from(created.atZone(ZoneId.systemDefault()).toInstant());

		return out;
	}

	/**
	 * @return the status
	 */
	public EEntityStatus getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(EEntityStatus status) {
		this.status = status;
	}

	/**
	 * @return Date
	 */
	@Transient
	@JsonIgnore
	@JsonbTransient
	public Date getUpdatedDate() {

		if (updated == null)
			return null;
		final Date out = Date.from(updated.atZone(ZoneId.systemDefault()).toInstant());

		return out;
	}

	/**
	 * Get the value of the HEntityAttribute.
	 *
	 * @param <T> the type to return
	 * @return T
	 */
	@SuppressWarnings("unchecked")
	@JsonIgnore
	@Transient
	@XmlTransient
	@JsonbProperty(nillable = true)
	public <T> T getValue() {
		if ((getPk() == null) || (getPk().attribute == null)) {
			return getLoopValue();
		}
		final String dataType = getPk().getAttribute().getDataType().getClassName();
		switch (dataType) {
			case "java.lang.Integer":
			case "Integer":
				return (T) getValueInteger();
			case "java.time.LocalDateTime":
			case "LocalDateTime":
				return (T) getValueDateTime();
			case "java.time.LocalTime":
			case "LocalTime":
				return (T) getValueTime();
			case "java.lang.Long":
			case "Long":
				return (T) getValueLong();
			case "java.lang.Double":
			case "Double":
				return (T) getValueDouble();
			case "java.lang.Boolean":
			case "Boolean":
				return (T) getValueBoolean();
			case "java.time.LocalDate":
			case "LocalDate":
				return (T) getValueDate();
			case "org.javamoney.moneta.Money":
			case "Money":
				return (T) getValueMoney();
			case "java.lang.String":
			default:
				return (T) getValueString();
		}

	}

	/**
	 * Set the value
	 *
	 * @param <T>   the Type
	 * @param value the value to set
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public <T> void setValue(final Object value) {
		setValue(value, false);
	}

	/**
	 * Set the value, specifying a lock status
	 *
	 * @param <T>   the Type
	 * @param value the value to set
	 * @param lock  should lock
	 */
	@SuppressWarnings("unchecked")
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public <T> void setValue(final Object value, final Boolean lock) {

		if (this.getReadonly()) {
			log.error("Trying to set the value of a readonly HEntityAttribute! " + this.getBaseEntityCode() + ":"
					+ this.attributeCode);
			return;
		}

		if (getAttribute() == null) {
			setLoopValue(value);
			return;
		}

		if (value instanceof String) {
			String result = (String) value;
			try {
				if (getAttribute().getDataType().getClassName().equalsIgnoreCase(String.class.getCanonicalName())) {
					setValueString(result);
				} else if (getAttribute().getDataType().getClassName()
						.equalsIgnoreCase(LocalDateTime.class.getCanonicalName())) {
					List<String> formatStrings = Arrays.asList("yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss",
							"yyyy-MM-dd HH:mm:ss",
							"yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd HH:mm:ss.SSSZ");
					for (String formatString : formatStrings) {
						try {
							Date olddate = new SimpleDateFormat(formatString).parse(result);
							final LocalDateTime dateTime = olddate.toInstant().atZone(ZoneId.systemDefault())
									.toLocalDateTime();
							setValueDateTime(dateTime);
							break;

						} catch (ParseException e) {
						}

					}

				} else if (getAttribute().getDataType().getClassName()
						.equalsIgnoreCase(LocalDate.class.getCanonicalName())) {
					Date olddate = null;
					try {
						olddate = DateUtils.parseDate(result, "M/y", "yyyy-MM-dd", "yyyy/MM/dd",
								"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
					} catch (java.text.ParseException e) {
						olddate = DateUtils.parseDate(result, "yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss",
								"yyyy-MM-dd HH:mm:ss",
								"yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd HH:mm:ss.SSSZ");
					}
					final LocalDate date = olddate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
					setValueDate(date);
				} else if (getAttribute().getDataType().getClassName()
						.equalsIgnoreCase(LocalTime.class.getCanonicalName())) {
					final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
					final LocalTime date = LocalTime.parse(result, formatter);
					setValueTime(date);
				} else if (getAttribute().getDataType().getClassName()
						.equalsIgnoreCase(Money.class.getCanonicalName())) {
					JsonReader reader = Json.createReader(new StringReader(result));
					JsonObject obj = reader.readObject();

					CurrencyUnit currency = Monetary.getCurrency(obj.getString("currency"));
					Double amount = Double.valueOf(obj.getString("amount"));

					Money money = Money.of(amount, currency);
					setValueMoney(money);
				} else if (getAttribute().getDataType().getClassName()
						.equalsIgnoreCase(Integer.class.getCanonicalName())) {
					final Integer integer = Integer.parseInt(result);
					setValueInteger(integer);
				} else if (getAttribute().getDataType().getClassName()
						.equalsIgnoreCase(Double.class.getCanonicalName())) {
					final Double d = Double.parseDouble(result);
					setValueDouble(d);
				} else if (getAttribute().getDataType().getClassName()
						.equalsIgnoreCase(Long.class.getCanonicalName())) {
					final Long l = Long.parseLong(result);
					setValueLong(l);
				} else if (getAttribute().getDataType().getClassName()
						.equalsIgnoreCase(Boolean.class.getCanonicalName())) {
					final Boolean b = Boolean.parseBoolean(result);
					setValueBoolean(b);
				} else {
					setValueString(result);
				}
			} catch (Exception e) {
				log.error("Conversion Error :" + value + " for attribute " + getAttribute() + " and SourceCode:"
						+ this.baseEntityCode);
			}
		} else {

			switch (this.getAttribute().getDataType().getClassName()) {

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
				default:
					if (value instanceof Boolean) {
						log.error("Value is boolean being saved to String. DataType = "
								+ this.getAttribute().getDataType().getClassName() + " and attributecode="
								+ this.getAttributeCode());
						setValueBoolean((Boolean) value);
					} else {
						setValueString((String) value);
					}
					break;
			}
		}

		// if the lock is set then 'Lock it in Eddie!'.
		if (lock) {
			this.setReadonly(true);
		}
	}

	/**
	 * Set the loop value
	 *
	 * @param <T>   the Type
	 * @param value the value to set
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public <T> void setLoopValue(final Object value) {
		setLoopValue(value, false);
	}

	/**
	 * Set the loop value, specifying a lock status
	 *
	 * @param <T>   the Type
	 * @param value the value to set
	 * @param lock  should lock
	 */
	@SuppressWarnings("unchecked")
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public <T> void setLoopValue(final Object value, final Boolean lock) {

		if (this.getReadonly()) {
			log.error("Trying to set the value of a readonly HEntityAttribute! " + this.getBaseEntityCode() + ":"
					+ this.attributeCode);
			return;
		}

		if (value instanceof Money)
			setValueMoney((Money) value);
		else if (value instanceof Integer)
			setValueInteger((Integer) value);
		else if (value instanceof LocalDateTime)
			setValueDateTime((LocalDateTime) value);
		else if (value instanceof LocalDate)
			setValueDate((LocalDate) value);
		else if (value instanceof Long)
			setValueLong((Long) value);
		else if (value instanceof LocalTime)
			setValueTime((LocalTime) value);
		else if (value instanceof Double)
			setValueDouble((Double) value);
		else if (value instanceof Boolean)
			setValueBoolean((Boolean) value);
		else if (value instanceof BigDecimal)
			// NOTE: This assumes at least one will not be null and defaults to int
			// otherwise
			// This could cause issues with deserialisation.
			if (this.getValueDouble() != null) {
				setValueDouble(((BigDecimal) value).doubleValue());
			} else if (this.getValueLong() != null) {
				setValueLong(((BigDecimal) value).longValue());
			} else {
				setValueInteger(((BigDecimal) value).intValue());
			}
		else
			setValueString((String) value);

		if (lock) {
			this.setReadonly(true);
		}
	}

	/**
	 * @return String
	 */
	@JsonIgnore
	@Transient
	@XmlTransient
	@JsonbTransient
	public String getAsString() {
		if ((getPk() == null) || (getPk().attribute == null)) {
			return getAsLoopString();
		}

		if (getValue() == null) {
			return null;
		}
		final String dataType = getPk().getAttribute().getDataType().getClassName();
		switch (dataType) {
			case "java.lang.Integer":
			case "Integer":
				return "" + getValueInteger();
			case "java.time.LocalDateTime":
			case "LocalDateTime":
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
				Date datetime = Date.from(getValueDateTime().atZone(ZoneId.systemDefault()).toInstant());
				String dout = df.format(datetime);
				return dout;
			case "java.lang.Long":
			case "Long":
				return "" + getValueLong();
			case "java.time.LocalTime":
			case "LocalTime":
				DateFormat df2 = new SimpleDateFormat("HH:mm");
				String dout2 = df2.format(getValueTime());
				return dout2;
			case "org.javamoney.moneta.Money":
			case "Money":
				DecimalFormat decimalFormat = new DecimalFormat("###############0.00");
				String amount = decimalFormat.format(getValueMoney().getNumber().doubleValue());
				return "{\"amount\":" + amount + ",\"currency\":\"" + getValueMoney().getCurrency().getCurrencyCode()
						+ "\"}";
			case "java.lang.Double":
			case "Double":
				return getValueDouble().toString();
			case "java.lang.Boolean":
			case "Boolean":
				return getValueBoolean() ? "TRUE" : "FALSE";
			case "java.time.LocalDate":
			case "LocalDate":
				df2 = new SimpleDateFormat("yyyy-MM-dd");
				Date date = Date.from(getValueDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
				dout2 = df2.format(date);
				return dout2;

			case "java.lang.String":
			default:
				return getValueString();
		}

	}

	/**
	 * @return String
	 */
	@JsonIgnore
	@Transient
	@XmlTransient
	@JsonbTransient
	public String getAsLoopString() {

		String ret = "";
		if (getValueString() != null) {
			return getValueString();
		}
		if (getValueInteger() != null) {
			return getValueInteger().toString();
		}
		if (getValueDateTime() != null) {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
			Date datetime = Date.from(getValueDateTime().atZone(ZoneId.systemDefault()).toInstant());
			String dout = df.format(datetime);
			return dout;
		}
		if (getValueDate() != null) {
			DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
			Date date = Date.from(getValueDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
			String dout2 = df2.format(date);
			return dout2;
		}
		if (getValueTime() != null) {
			DateFormat df2 = new SimpleDateFormat("HH:mm");
			String dout2 = df2.format(getValueTime());
			return dout2;
		}
		if (getValueLong() != null) {
			return getValueLong().toString();
		}
		if (getValueDouble() != null) {
			return getValueDouble().toString();
		}
		if (getValueBoolean() != null) {
			return getValueBoolean() ? "TRUE" : "FALSE";
		}

		return ret;
	}

	/**
	 * Get the loop value
	 *
	 * @param <T> the Type to return
	 * @return T
	 */
	@SuppressWarnings("unchecked")
	@JsonIgnore
	@Transient
	@XmlTransient
	@JsonbTransient
	public <T> T getLoopValue() {

		if (getValueString() != null) {
			return (T) getValueString();
		} else if (getValueBoolean() != null) {
			return (T) getValueBoolean();
		} else if (getValueDateTime() != null) {
			return (T) getValueDateTime();
		} else if (getValueDouble() != null) {
			return (T) getValueDouble();
		} else if (getValueInteger() != null) {
			return (T) getValueInteger();
		} else if (getValueDate() != null) {
			return (T) getValueDate();
		} else if (getValueTime() != null) {
			return (T) getValueTime();
		} else if (getValueLong() != null) {
			return (T) getValueLong();
		}

		return null;
	}

	/**
	 * @return int
	 */
	@Override
	public int hashCode() {

		HashCodeBuilder hcb = new HashCodeBuilder();
		hcb.append(baseEntityCode);
		hcb.append(attributeCode);
		return hcb.toHashCode();
	}

	/**
	 * @param obj the object to compare to
	 * @return boolean
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}
		if (!(obj instanceof HEntityAttribute)) {
			return false;
		}
		HEntityAttribute that = (HEntityAttribute) obj;
		EqualsBuilder eb = new EqualsBuilder();
		eb.append(baseEntityCode, that.baseEntityCode);
		eb.append(attributeCode, that.attributeCode);

		return eb.isEquals();
	}

	/**
	 * Compare to an object
	 *
	 * @param obj object to compare to
	 * @return int
	 */
	public int compareTo(Object obj) {

		HEntityAttribute myClass = (HEntityAttribute) obj;
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
		return "attributeCode=" + attributeCode + ", value="
				+ getObjectAsString() + ", weight=" + weight + ", inferred=" + inferred + "] be="
				+ this.getBaseEntityCode();
	}

	/**
	 * Get the object
	 *
	 * @param <T> the Type to return
	 * @return T
	 */
	@SuppressWarnings("unchecked")
	@JsonIgnore
	@Transient
	@XmlTransient
	@JsonbTransient
	public <T> T getObject() {

		if (getValueInteger() != null) {
			return (T) getValueInteger();
		}
		if (getValueDateTime() != null) {
			return (T) getValueDateTime();
		}
		if (getValueLong() != null) {
			return (T) getValueLong();
		}
		if (getValueDouble() != null) {
			return (T) getValueDouble();
		}
		if (getValueBoolean() != null) {
			return (T) getValueBoolean();
		}
		if (getValueDate() != null) {
			return (T) getValueDate();
		}
		if (getValueTime() != null) {
			return (T) getValueTime();
		}
		if (getValueString() != null) {
			return (T) getValueString();
		}

		return (T) getValueString();
	}

	@JsonIgnore
	@Transient
	@XmlTransient
	@JsonbTransient
	public String getObjectAsString() {

		if (getValueInteger() != null) {
			return "" + getValueInteger();
		}

		if (getValueDateTime() != null) {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
			Date datetime = Date.from(getValueDateTime().atZone(ZoneId.systemDefault()).toInstant());
			String dout = df.format(datetime);
			return dout;
		}

		if (getValueLong() != null) {
			return "" + getValueLong();
		}

		if (getValueDouble() != null) {
			return getValueDouble().toString();
		}

		if (getValueBoolean() != null) {
			return getValueBoolean() ? "TRUE" : "FALSE";
		}

		if (getValueDate() != null) {
			DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
			Date date = Date.from(getValueDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
			String dout2 = df2.format(date);
			return dout2;
		}
		if (getValueTime() != null) {

			String dout2 = getValueTime().toString();
			return dout2;
		}

		if (getValueString() != null) {
			return getValueString();
		}

		return getValueString();
	}

	/**
	 * @return the attributeName
	 */
	public String getAttributeName() {
		return attributeName;
	}

	/**
	 * @param attributeName the attributeName to set
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	/**
	 * @return the index
	 */
	public Integer getIndex() {
		return index;
	}

	/**
	 * @param index the index to set
	 */
	public void setIndex(Integer index) {
		this.index = index;
	}

	/**
	 * @return the realm
	 */
	public String getRealm() {
		return realm;
	}

	/**
	 * @param realm the realm to set
	 */
	public void setRealm(String realm) {
		this.realm = realm;
	}

	/**
	 * @return Boolean
	 */
	public Boolean getConfirmationFlag() {
		return confirmationFlag;
	}

	/**
	 * @param confirmationFlag the confirmationFlag to set
	 */
	public void setConfirmationFlag(Boolean confirmationFlag) {
		this.confirmationFlag = confirmationFlag;
	}

	public boolean isLocked() {
		return capabilityRequirements != null;
	}

	@Override
	@JsonbTransient
	@JsonIgnore
	public void setCapabilityRequirements(Set<Capability> requirements) {
		this.capabilityRequirements = requirements;
	}

	@Override
	public CoreEntitySerializable toSerializableCoreEntity() {
		EntityAttribute serializableEntityAttribute = new EntityAttribute();
		serializableEntityAttribute.setRealm(getRealm());
		serializableEntityAttribute.setBaseEntityCode(getBaseEntityCode());
		serializableEntityAttribute.setAttributeCode(getAttributeCode());
		serializableEntityAttribute.setCreated(getCreated());
		serializableEntityAttribute.setReadonly(getReadonly());
		serializableEntityAttribute.setUpdated(getUpdated());
		serializableEntityAttribute.setValueBoolean(getValueBoolean());
		serializableEntityAttribute.setValueDate(getValueDate());
		serializableEntityAttribute.setValueDateTime(getValueDateTime());
		serializableEntityAttribute.setValueDouble(getValueDouble());
		serializableEntityAttribute.setValueInteger(getValueInteger());
		serializableEntityAttribute.setValueLong(getValueLong());
		serializableEntityAttribute.setMoney(getValueMoney());
		serializableEntityAttribute.setValueString(getValueString());
		serializableEntityAttribute.setUpdated(getUpdated());
		serializableEntityAttribute.setWeight(getWeight());
		serializableEntityAttribute.setInferred(getInferred());
		serializableEntityAttribute.setPrivacyFlag(getPrivacyFlag());
		serializableEntityAttribute.setConfirmationFlag(getConfirmationFlag());
		serializableEntityAttribute.setCapreqs(CapabilityConverter.convertToDBColumn(getCapabilityRequirements()));
		serializableEntityAttribute.setStatus(getStatus());
		return serializableEntityAttribute;
	}
}

