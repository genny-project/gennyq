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
import javax.persistence.Convert;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.converter.CapabilityConverter;
import life.genny.qwandaq.datatype.DataType;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.javamoney.moneta.Money;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.annotation.JsonIgnore;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.converter.MoneyConverter;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.handler.AttributeMinIOHandler;
import life.genny.qwandaq.intf.ICapabilityHiddenFilterable;
import life.genny.qwandaq.serialization.CoreEntitySerializable;

public class EntityAttribute implements CoreEntityPersistable, ICapabilityHiddenFilterable, Comparable<Object> {

	public static final String DOUBLE = "Double";
	public static final String MONEY = "Money";
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
	private Boolean valueBoolean;
	/**
	 * Store the Integer value of the attribute for the baseEntity
	 */
	private Integer valueInteger;

	/**
	 * Store the Long value of the attribute for the baseEntity
	 */
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

	private Long baseEntityId;

	private Long attributeId;

	private Set<Capability> capabilityRequirements;

	public EntityAttribute() {
	}

	public EntityAttribute(BaseEntity baseEntity, Attribute attribute) {
		this.realm = baseEntity.getRealm();
		setBaseEntity(baseEntity);
		setAttribute(attribute);
	}

	/**
	 * @param baseEntity
	 * @param attribute
	 * @param weight
	 * @param value
	 */
	public EntityAttribute(BaseEntity baseEntity, Attribute attribute, Double weight, final Object value) {
		autocreateCreated();
		this.realm = baseEntity.getRealm();
		setBaseEntity(baseEntity);
		setAttribute(attribute);
		if (weight == null) {
			weight = 0.0; // This permits ease of adding attributes and hides attribute from scoring.
		}
		setWeight(weight);
		if (value != null) {
			setValue(value);
		}
	}

	public void setBaseEntity(BaseEntity baseEntity) {
		setBaseEntityCode(baseEntity.getCode());
		setBaseEntityId(baseEntity.getId());
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

	public Long getBaseEntityId() {
		return baseEntityId;
	}

	public void setBaseEntityId(Long baseEntityId) {
		this.baseEntityId = baseEntityId;
	}

	public Long getAttributeId() {
		return attributeId;
	}

	public void setAttributeId(Long attributeId) {
		this.attributeId = attributeId;
	}

	public void autocreateUpdate() {
		if (valueString != null && valueString.length() > 0) {
            this.valueString = AttributeMinIOHandler.convertToMinIOObject(valueString,baseEntityCode,attributeCode);
		}
		setUpdated(LocalDateTime.now(ZoneId.of("Z")));
	}

	public void autocreateCreated() {
		if (getCreated() == null)
			setCreated(LocalDateTime.now(ZoneId.of("Z")));

		if (valueString != null && valueString.length() > 0) {
			this.valueString = AttributeMinIOHandler.convertToMinIOObject(valueString,baseEntityCode,attributeCode);
		}

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
		setAttributeId(attribute.getId());
		setAttributeCode(attribute.getCode());
		setAttributeName(attribute.getName());
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
		DataType dataType = attribute.getDataType();
		if (dataType != null) {
			final String dataTypeClassName = dataType.getClassName();
			switch (dataTypeClassName) {
				case GennyConstants.JAVA_LANG_INTEGER, GennyConstants.INTEGER -> { return (T) getValueInteger(); }
				case GennyConstants.JAVA_TIME_LOCAL_DATE_TIME, GennyConstants.LOCAL_DATE_TIME -> { return (T) getValueDateTime(); }
				case GennyConstants.JAVA_TIME_LOCAL_TIME, GennyConstants.LOCAL_TIME -> { return (T) getValueTime(); }
				case GennyConstants.JAVA_LANG_LONG, GennyConstants.LONG -> { return (T) getValueLong(); }
				case GennyConstants.JAVA_LANG_DOUBLE, GennyConstants.DOUBLE -> { return (T) getValueDouble(); }
				case GennyConstants.JAVA_LANG_BOOLEAN, GennyConstants.BOOLEAN -> { return (T) getValueBoolean(); }
				case GennyConstants.JAVA_TIME_LOCAL_DATE, GennyConstants.LOCAL_DATE -> { return (T) getValueDate(); }
				case GennyConstants.ORG_JAVAMONEY_MONETA_MONEY, GennyConstants.MONEY -> { return (T) getValueMoney(); }
			}
		}
		return (T) getValueString();
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
		if (this.getReadonly()) {
			log.error("Trying to set the value of a readonly EntityAttribute! " + this.getBaseEntityCode() + ":"
					+ this.attributeCode);
			return;
		}

		if (attribute == null) {
			setLoopValue(value);
			return;
		}

		DataType dataType = attribute.getDataType();
		if(dataType!=null) {
			String className = dataType.getClassName();
			if (value instanceof String) {
				String result = (String) value;
				try {
					if (className.equalsIgnoreCase(String.class.getCanonicalName())) {
						setValueString(result);
					} else if (className
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

					} else if (className
							.equalsIgnoreCase(LocalDate.class.getCanonicalName())) {
						DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
						Date olddate = format.parse(result);
						final LocalDate date = olddate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
						setValueDate(date);
					} else if (className
							.equalsIgnoreCase(LocalTime.class.getCanonicalName())) {
						final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
						final LocalTime date = LocalTime.parse(result, formatter);
						setValueTime(date);
					} else if (className
							.equalsIgnoreCase(Money.class.getCanonicalName())) {
						JsonReader reader = Json.createReader(new StringReader(result));
						JsonObject obj = reader.readObject();

						CurrencyUnit currency = Monetary.getCurrency(obj.getString("currency"));
						Double amount = Double.valueOf(obj.getString("amount"));

						Money money = Money.of(amount, currency);
						setValueMoney(money);
					} else if (className
							.equalsIgnoreCase(Integer.class.getCanonicalName())) {
						final Integer integer = Integer.parseInt(result);
						setValueInteger(integer);
					} else if (className
							.equalsIgnoreCase(Double.class.getCanonicalName())) {
						final Double d = Double.parseDouble(result);
						setValueDouble(d);
					} else if (className
							.equalsIgnoreCase(Long.class.getCanonicalName())) {
						final Long l = Long.parseLong(result);
						setValueLong(l);
					} else if (className
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

				switch (className) {

					case GennyConstants.JAVA_LANG_INTEGER:
					case GennyConstants.INTEGER:
						if (value instanceof BigDecimal)
							setValueInteger(((BigDecimal) value).intValue());
						else
							setValueInteger((Integer) value);
						break;

					case GennyConstants.JAVA_TIME_LOCAL_DATE_TIME:
					case GennyConstants.LOCAL_DATE_TIME:
						setValueDateTime((LocalDateTime) value);
						break;

					case GennyConstants.JAVA_TIME_LOCAL_DATE:
					case GennyConstants.LOCAL_DATE:
						setValueDate((LocalDate) value);
						break;

					case GennyConstants.JAVA_LANG_LONG:
					case GennyConstants.LONG:
						if (value instanceof BigDecimal)
							setValueLong(((BigDecimal) value).longValue());
						else
							setValueLong((Long) value);
						break;

					case GennyConstants.JAVA_TIME_LOCAL_TIME:
					case GennyConstants.LOCAL_TIME:
						setValueTime((LocalTime) value);
						break;

					case GennyConstants.ORG_JAVAMONEY_MONETA_MONEY:
					case GennyConstants.MONEY:
						setValueMoney((Money) value);
						break;

					case GennyConstants.JAVA_LANG_DOUBLE:
					case GennyConstants.DOUBLE:
						if (value instanceof BigDecimal)
							setValueDouble(((BigDecimal) value).doubleValue());
						else
							setValueDouble((Double) value);
						break;

					case GennyConstants.JAVA_LANG_BOOLEAN:
					case GennyConstants.BOOLEAN:
						setValueBoolean((Boolean) value);
						break;

					case GennyConstants.JAVA_LANG_STRING:
					default:
						if (value instanceof Boolean) {
							log.error("Value is boolean being saved to String. DataType = "
									+ className + " and attributecode="
									+ this.getAttributeCode());
							setValueBoolean((Boolean) value);
						} else {
							setValueString((String) value);
						}
						break;
				}
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
			case GennyConstants.JAVA_LANG_INTEGER, GennyConstants.INTEGER -> { return "" + getValueInteger(); }
			case GennyConstants.JAVA_TIME_LOCAL_DATE_TIME, GennyConstants.LOCAL_DATE_TIME -> {
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
				Date datetime = Date.from(getValueDateTime().atZone(ZoneId.systemDefault()).toInstant());
				return df.format(datetime);
			}
			case GennyConstants.JAVA_LANG_LONG, GennyConstants.LONG -> { return "" + getValueLong(); }
			case GennyConstants.JAVA_TIME_LOCAL_TIME, GennyConstants.LOCAL_TIME -> {
				DateFormat df2 = new SimpleDateFormat("HH:mm");
				return df2.format(getValueTime());
			}
			case GennyConstants.ORG_JAVAMONEY_MONETA_MONEY, GennyConstants.MONEY -> {
				DecimalFormat decimalFormat = new DecimalFormat("###############0.00");
				String amount = decimalFormat.format(getValueMoney().getNumber().doubleValue());
				return "{\"amount\":" + amount + ",\"currency\":\"" + getValueMoney().getCurrency().getCurrencyCode()
						+ "\"}";
			}
			case GennyConstants.JAVA_LANG_DOUBLE, GennyConstants.DOUBLE -> { return getValueDouble().toString(); }
			case GennyConstants.JAVA_LANG_BOOLEAN, GennyConstants.BOOLEAN -> { return getValueBoolean() ? "TRUE" : "FALSE"; }
			case GennyConstants.JAVA_TIME_LOCAL_DATE, GennyConstants.LOCAL_DATE -> {
				DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
				Date date = Date.from(getValueDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
				return df2.format(date);
			}
		}
		return getValueString();
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
			case GennyConstants.JAVA_LANG_INTEGER, GennyConstants.INTEGER -> {
				return new CompareToBuilder().append(this.getValueInteger(), myClass.getValueInteger()).toComparison();
			}
			case GennyConstants.JAVA_TIME_LOCAL_DATE_TIME, GennyConstants.LOCAL_DATE_TIME -> {
				return new CompareToBuilder().append(this.getValueDateTime(), myClass.getValueDateTime())
						.toComparison();
			}
			case GennyConstants.JAVA_TIME_LOCAL_TIME, GennyConstants.LOCAL_TIME -> {
				return new CompareToBuilder().append(this.getValueTime(), myClass.getValueTime()).toComparison();
			}
			case GennyConstants.JAVA_LANG_LONG, GennyConstants.LONG -> {
				return new CompareToBuilder().append(this.getValueLong(), myClass.getValueLong()).toComparison();
			}
			case GennyConstants.JAVA_LANG_DOUBLE, GennyConstants.DOUBLE -> {
				return new CompareToBuilder().append(this.getValueDouble(), myClass.getValueDouble()).toComparison();
			}
			case GennyConstants.JAVA_LANG_BOOLEAN, GennyConstants.BOOLEAN -> {
				return new CompareToBuilder().append(this.getValueBoolean(), myClass.getValueBoolean()).toComparison();
			}
			case GennyConstants.JAVA_TIME_LOCAL_DATE, GennyConstants.LOCAL_DATE -> {
				return new CompareToBuilder().append(this.getValueDate(), myClass.getValueDate()).toComparison();
			}
			case GennyConstants.ORG_JAVAMONEY_MONETA_MONEY, GennyConstants.MONEY -> {
				return new CompareToBuilder().append(this.getValueMoney(), myClass.getValueMoney()).toComparison();
			}
		}
		return new CompareToBuilder().append(this.getValueString(), myClass.getValueString()).toComparison();
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
		life.genny.qwandaq.serialization.entityattribute.EntityAttribute bea = new life.genny.qwandaq.serialization.entityattribute.EntityAttribute();
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
		bea.setAttributeId(getAttributeId());
		bea.setBaseEntityId(getBaseEntityId());
		// bea.setIcon(geticon);
		bea.setConfirmationFlag(getConfirmationFlag());
		bea.setCapreqs(CapabilityConverter.convertToDBColumn(getCapabilityRequirements()));
		return bea;
	}


	public boolean isLocked() {
		return capabilityRequirements != null;
	}

	@Override
	public void setCapabilityRequirements(Set<Capability> requirements) {
		this.capabilityRequirements = requirements;
	}

	@Override
	public EntityAttribute clone() {
		EntityAttribute clone = new EntityAttribute();
		clone.setRealm(getRealm());
		clone.setBaseEntityCode(getBaseEntityCode());
		clone.setAttributeCode(getAttributeCode());
		clone.setCreated(getCreated());
		clone.setInferred(getInferred());
		clone.setPrivacyFlag(getPrivacyFlag());
		clone.setReadonly(getReadonly());
		clone.setUpdated(getUpdated());
		clone.setValueBoolean(getValueBoolean());
		clone.setValueDate(getValueDate());
		clone.setValueDateTime(getValueDateTime());
		clone.setValueDouble(getValueDouble());
		clone.setValueInteger(getValueInteger());
		clone.setValueLong(getValueLong());
		clone.setValueMoney(getValueMoney());
		clone.setValueString(getValueString());
		clone.setUpdated(getUpdated());
		clone.setWeight(getWeight());
		clone.setConfirmationFlag(getConfirmationFlag());
		clone.setAttributeId(getAttributeId());
		clone.setBaseEntityId(getBaseEntityId());
		clone.setCapabilityRequirements(getCapabilityRequirements());
		return clone;
	}

	public HEntityAttribute toHEntityAttribute() {
		HEntityAttribute hEntityAttribute = new HEntityAttribute();
		hEntityAttribute.setRealm(getRealm());
		hEntityAttribute.setBaseEntityCode(getBaseEntityCode());
		hEntityAttribute.setAttributeCode(getAttributeCode());
		hEntityAttribute.setCreated(getCreated());
		hEntityAttribute.setReadonly(getReadonly());
		hEntityAttribute.setUpdated(getUpdated());
		hEntityAttribute.setValueBoolean(getValueBoolean());
		hEntityAttribute.setValueDate(getValueDate());
		hEntityAttribute.setValueDateTime(getValueDateTime());
		hEntityAttribute.setValueDouble(getValueDouble());
		hEntityAttribute.setValueInteger(getValueInteger());
		hEntityAttribute.setValueLong(getValueLong());
		hEntityAttribute.setValueMoney(getValueMoney());
		hEntityAttribute.setValueString(getValueString());
		hEntityAttribute.setUpdated(getUpdated());
		hEntityAttribute.setWeight(getWeight());
		hEntityAttribute.setInferred(getInferred());
		hEntityAttribute.setPrivacyFlag(getPrivacyFlag());
		hEntityAttribute.setConfirmationFlag(getConfirmationFlag());
		hEntityAttribute.setCapabilityRequirements(getCapabilityRequirements());
		return hEntityAttribute;
	}
}
