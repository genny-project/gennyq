package life.genny.qwandaq.attribute;

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
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.javamoney.moneta.Money;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.CodedEntity;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.converter.CapabilityConverter;
import life.genny.qwandaq.converter.MoneyConverter;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import life.genny.qwandaq.serialization.baseentityattribute.BaseEntityAttribute;
import life.genny.qwandaq.intf.ICapabilityFilterable;
import life.genny.qwandaq.intf.ICapabilityHiddenFilterable;

public class EntityAttribute implements CoreEntityPersistable, ICapabilityHiddenFilterable, Comparable<Object> {

	private static final Logger log = Logger.getLogger(EntityAttribute.class);

	private static final long serialVersionUID = 1L;

	private String baseEntityCode;

	private String attributeCode;

	private String attributeName;

	private Boolean readonly = false;

	private String realm;

	@Transient
	@XmlTransient
	private Integer index = 0; // used to assist with ordering

	@Transient
	private String feedback = null;

	/**
	 * Stores the Created UMT DateTime that this object was created
	 */
	private LocalDateTime created;

	/**
	 * Stores the Last Modified UMT DateTime that this object was last updated
	 */
	private LocalDateTime updated;

	/**
	 * Store the Double value of the attribute for the baseEntity
	 */
	private Double valueDouble;

	/**
	 * Store the Boolean value of the attribute for the baseEntity
	 */
	//@Column
	private Boolean valueBoolean;
	/**
	 * Store the Integer value of the attribute for the baseEntity
	 */
	//@Column
	private Integer valueInteger;

	/**
	 * Store the Long value of the attribute for the baseEntity
	 */
	//@Column
	private Long valueLong;

	/**
	 * Store the LocalDateTime value of the attribute for the baseEntity
	 */
	private LocalTime valueTime;

	/**
	 * Store the LocalDateTime value of the attribute for the baseEntity
	 */
	private LocalDateTime valueDateTime;

	/**
	 * Store the LocalDate value of the attribute for the baseEntity
	 */
	private LocalDate valueDate;

	/**
	 * Store the String value of the attribute for the baseEntity
	 */
	private String valueString;

	//@Column(name = "money", length = 128)
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

	private Attribute attribute;

	private Set<Capability> capabilityRequirements;

	public EntityAttribute() {
	}

	/**
	 * Constructor.
	 *
	 * @param weight
	 *                   the weighted importance of this attribute (relative to the
	 *                   other
	 *                   attributes)
	 */
	public EntityAttribute(Double weight) {
		autocreateCreated();
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
	 * @param weight
	 *                   the weighted importance of this attribute (relative to the
	 *                   other
	 *                   attributes)
	 * @param value
	 *                   the value associated with this attribute
	 */
	public EntityAttribute(Double weight, final Object value) {
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

	/**
	 * Constructor.
	 *
	 * @param weight
	 *                   the weighted importance of this attribute (relative to the
	 *                   other
	 *                   attributes)
	 * @param value
	 *                   the value associated with this attribute
	 * @param privacyFlag
	 *                   the value for privacy of this attribute
	 */
	public EntityAttribute(Double weight, final Object value, final boolean privacyFlag) {
		autocreateCreated();
		this.setPrivacyFlag(privacyFlag);
		if (weight == null) {
			weight = 0.0; // This permits ease of adding attributes and hides attribute from scoring.
		}
		setWeight(weight);
		// Assume that Attribute Validation has been performed
		if (value != null) {
			setValue(value);
		}
		setReadonly(false);
	}

    @JsonbTransient
    @JsonIgnore
    public Set<Capability> getCapabilityRequirements() {
		return this.capabilityRequirements;
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

	public void autocreateUpdate() {
		setUpdated(LocalDateTime.now(ZoneId.of("Z")));
	}

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

	public Attribute getAttribute() {
		return attribute;
	}

	public void setAttribute(Attribute attribute) {
		this.attribute = attribute;
		this.attributeCode = attribute.getCode();
		this.attributeName = attribute.getName();
	}

	/**
	 * Get the value of the EntityAttribute.
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
		if (attribute == null) {
			return getLoopValue();
		}
		final String dataType = attribute.getDataType().getClassName();
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
			log.error("Trying to set the value of a readonly EntityAttribute! " + this.getBaseEntityCode() + ":"
					+ this.attributeCode);
			return;
		}

		if (attribute == null) {
			setLoopValue(value);
			return;
		}

		if (value instanceof String) {
			String result = (String) value;
			try {
				if (attribute.getDataType().getClassName().equalsIgnoreCase(String.class.getCanonicalName())) {
					setValueString(result);
				} else if (attribute.getDataType().getClassName()
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

				} else if (attribute.getDataType().getClassName()
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
				} else if (attribute.getDataType().getClassName()
						.equalsIgnoreCase(LocalTime.class.getCanonicalName())) {
					final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
					final LocalTime date = LocalTime.parse(result, formatter);
					setValueTime(date);
				} else if (attribute.getDataType().getClassName()
						.equalsIgnoreCase(Money.class.getCanonicalName())) {
					JsonReader reader = Json.createReader(new StringReader(result));
					JsonObject obj = reader.readObject();

					CurrencyUnit currency = Monetary.getCurrency(obj.getString("currency"));
					Double amount = Double.valueOf(obj.getString("amount"));

					Money money = Money.of(amount, currency);
					setValueMoney(money);
				} else if (attribute.getDataType().getClassName()
						.equalsIgnoreCase(Integer.class.getCanonicalName())) {
					final Integer integer = Integer.parseInt(result);
					setValueInteger(integer);
				} else if (attribute.getDataType().getClassName()
						.equalsIgnoreCase(Double.class.getCanonicalName())) {
					final Double d = Double.parseDouble(result);
					setValueDouble(d);
				} else if (attribute.getDataType().getClassName()
						.equalsIgnoreCase(Long.class.getCanonicalName())) {
					final Long l = Long.parseLong(result);
					setValueLong(l);
				} else if (attribute.getDataType().getClassName()
						.equalsIgnoreCase(Boolean.class.getCanonicalName())) {
					final Boolean b = Boolean.parseBoolean(result);
					setValueBoolean(b);
				} else {
					setValueString(result);
				}
			} catch (Exception e) {
				log.error("Conversion Error :" + value + " for attribute " + attribute + " and SourceCode:"
						+ this.baseEntityCode);
			}
		} else {

			switch (this.attribute.getDataType().getClassName()) {

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
								+ this.attribute.getDataType().getClassName() + " and attributecode="
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
			log.error("Trying to set the value of a readonly EntityAttribute! " + this.getBaseEntityCode() + ":"
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
		if (attribute == null) {
			return getAsLoopString();
		}

		if (getValue() == null) {
			return null;
		}
		final String dataType = attribute.getDataType().getClassName();
		switch (dataType) {
			case "java.lang.Integer":
			case "Integer":
				return "" + getValueInteger();
			case "java.time.LocalDateTime":
			case "LocalDateTime":
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
				Date datetime = Date.from(getValueDateTime().atZone(ZoneId.systemDefault()).toInstant());
				return df.format(datetime);
			case "java.lang.Long":
			case "Long":
				return "" + getValueLong();
			case "java.time.LocalTime":
			case "LocalTime":
				DateFormat df2 = new SimpleDateFormat("HH:mm");
				return df2.format(getValueTime());
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
				return df2.format(date);

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
			return df.format(datetime);
		}
		if (getValueDate() != null) {
			DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
			Date date = Date.from(getValueDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
			return df2.format(date);
		}
		if (getValueTime() != null) {
			DateFormat df2 = new SimpleDateFormat("HH:mm");
			return df2.format(getValueTime());
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

	private Object getValueAsObject() {
		return (Object)getValue();
	}

	/**
	 * @return int
	 */
	@Override
	public int hashCode() {

		HashCodeBuilder hcb = new HashCodeBuilder();
		hcb.append(realm);
		hcb.append(baseEntityCode);
		hcb.append(attributeCode);
		hcb.append(getValueAsObject());
		return hcb.toHashCode();
	}

	/**
	 * @param obj the object to compare to
	 * @return boolean
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EntityAttribute)) {
			return false;
		}
		EntityAttribute that = (EntityAttribute) obj;
		EqualsBuilder eb = new EqualsBuilder();
		eb.append(realm, that.realm);
		eb.append(baseEntityCode, that.baseEntityCode);
		eb.append(attributeCode, that.attributeCode);
		eb.append(getValueAsObject(), getValueAsObject());
		return eb.isEquals();
	}

	/**
	 * Compare to an object
	 *
	 * @param obj object to compare to
	 * @return int
	 */
	public int compareTo(Object obj) {

		EntityAttribute myClass = (EntityAttribute) obj;
		final String dataType = attribute.getDataType().getClassName();

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
			return df.format(datetime);
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
			return df2.format(date);
		}
		if (getValueTime() != null) {

			return getValueTime().toString();
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

	@Override
	public CoreEntitySerializable toSerializableCoreEntity() {
		BaseEntityAttribute bea = new BaseEntityAttribute();
		bea.setRealm(getRealm());
		bea.setBaseEntityCode(getBaseEntityCode());
		bea.setAttributeCode(getAttributeCode());
		bea.setCreated(getCreated());
		bea.setInferred(getInferred());
		bea.setPrivacyFlag(getPrivacyFlag());
		bea.setReadonly(getReadonly());
		bea.setUpdated(getUpdated());
		bea.setValueBoolean(getValueBoolean());
		bea.setValueDate(getValueDate());
		bea.setValueDateTime(getValueDateTime());
		bea.setValueDouble(getValueDouble());
		bea.setValueInteger(getValueInteger());
		bea.setValueLong(getValueLong());
		bea.setMoney(getValueMoney());
		bea.setValueString(getValueString());
		bea.setUpdated(getUpdated());
		bea.setWeight(getWeight());
		// bea.setIcon(geticon);
		bea.setConfirmationFlag(getConfirmationFlag());
		return bea;
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
}
