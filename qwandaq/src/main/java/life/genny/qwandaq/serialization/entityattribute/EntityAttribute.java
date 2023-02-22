package life.genny.qwandaq.serialization.entityattribute;

import java.io.StringReader;
import java.math.BigDecimal;
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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.converter.CapabilityConverter;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.handler.AttributeMinIOHandler;
import org.apache.commons.lang3.time.DateUtils;
import org.javamoney.moneta.Money;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import org.jboss.logging.Logger;


public class EntityAttribute implements CoreEntitySerializable {

	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(EntityAttribute.class);

	private String baseEntityCode;

	private String attributeCode;

	private LocalDateTime created;

	private Boolean inferred;

	private Boolean privacyFlag;

	private Boolean readonly;

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

	@Transient
	private String capreqs;

	public EntityAttribute() {
	}

	public EntityAttribute(String baseEntityCode, String attributeCode, LocalDateTime created, Boolean inferred,
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

	public EntityAttribute(BaseEntity processEntity, Attribute attribute, Double value, Object o) {
		autocreateCreated();
		this.baseEntityCode = baseEntity.getCode();
		setAttribute(attribute);
		if (weight == null) {
			weight = 0.0;
		}
		setWeight(weight);
		if (value != null) {
			setValue(value);
		}
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

	@Override
	public String toString() {
		return super.toString();
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

	public String getCapreqs() {
		return capreqs;
	}

	public void setCapreqs(String capreqs) {
		this.capreqs = capreqs;
	}

	@Override
	public CoreEntityPersistable toPersistableCoreEntity() {
		life.genny.qwandaq.attribute.EntityAttribute ea = new life.genny.qwandaq.attribute.EntityAttribute();
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
		ea.setAttributeId(getAttributeId());
		ea.setBaseEntityId(getBaseEntityId());
		// bea.setIcon(geticon);
		ea.setConfirmationFlag(getConfirmationFlag());
		ea.setCapabilityRequirements(CapabilityConverter.convertToEA(getCapreqs()));
		return ea;
	}

	public void autocreateCreated() {
		if (getCreated() == null)
			setCreated(LocalDateTime.now(ZoneId.of("Z")));

		if (getValueString() != null) {
			this.valueString = AttributeMinIOHandler.convertToMinIOObject(valueString,baseEntityCode,attributeCode);
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
						setMoney(money);
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

					case GennyConstants.JAVA_LANG_INTEGER, GennyConstants.INTEGER -> {
						if (value instanceof BigDecimal)
							setValueInteger(((BigDecimal) value).intValue());
						else
							setValueInteger((Integer) value);
					}

					case GennyConstants.JAVA_TIME_LOCAL_DATE_TIME, GennyConstants.LOCAL_DATE_TIME -> {
						setValueDateTime((LocalDateTime) value);
					}

					case GennyConstants.JAVA_TIME_LOCAL_DATE, GennyConstants.LOCAL_DATE -> {
						setValueDate((LocalDate) value);
					}

					case GennyConstants.JAVA_LANG_LONG, GennyConstants.LONG -> {
						if (value instanceof BigDecimal)
							setValueLong(((BigDecimal) value).longValue());
						else
							setValueLong((Long) value);
					}

					case GennyConstants.JAVA_TIME_LOCAL_TIME, GennyConstants.LOCAL_TIME -> {
						setValueTime((LocalTime) value);
					}

					case GennyConstants.ORG_JAVAMONEY_MONETA_MONEY, GennyConstants.MONEY -> {
						setMoney((Money) value);
					}

					case GennyConstants.JAVA_LANG_DOUBLE, GennyConstants.DOUBLE -> {
						if (value instanceof BigDecimal)
							setValueDouble(((BigDecimal) value).doubleValue());
						else
							setValueDouble((Double) value);
					}

					case GennyConstants.JAVA_LANG_BOOLEAN, GennyConstants.BOOLEAN -> {
						setValueBoolean((Boolean) value);
					}

					default -> {
						if (value instanceof Boolean) {
							setValueBoolean((Boolean) value);
						} else {
							setValueString((String) value);
						}
					}
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

		if (value instanceof Money)
			setMoney((Money) value);
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
				case GennyConstants.ORG_JAVAMONEY_MONETA_MONEY, GennyConstants.MONEY -> { return (T) getMoney(); }
			}
		}
		return (T) getValueString();
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
}
