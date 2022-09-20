package life.genny.qwandaq;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.StringReader;

import javax.persistence.Convert;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;

import org.hibernate.annotations.Type;

import org.javamoney.moneta.Money;
import javax.money.CurrencyUnit;
import javax.money.Monetary;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonObject;

import com.fasterxml.jackson.annotation.JsonIgnore;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.converter.MoneyConverter;
import life.genny.qwandaq.converter.StringListConverter;
import life.genny.qwandaq.entity.BaseEntity;

/**
 * AnswerLink represents a link between BaseEntitys for Answer objects.
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 */
@RegisterForReflection
public class AnswerLink implements java.io.Serializable {

	private static final Logger log = Logger.getLogger(AnswerLink.class);

	private static final long serialVersionUID = 1L;

	/**
	 * Stores the Created UMT DateTime that this object was created
	 */

	private LocalDateTime created;

	/**
	 * Stores the Last Modified UMT DateTime that this object was last updated
	 */
	private LocalDateTime updated;

	/**
	 * The following fields can be subclassed for better abstraction
	 */

	/**
	 * Store the Double value of the attribute for the baseEntity
	 */
	private Double valueDouble;

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
	private LocalDateTime valueDateTime;

	/**
	 * Store the LocalDate value of the attribute for the baseEntity
	 */
	private LocalDate valueDate;

	/**
	 * Store the LocalTime value of the attribute for the baseEntity
	 */
	private LocalTime valueTime;
	/**
	 * Store the String value of the attribute for the baseEntity
	 */
	@Type(type = "text")
	private String valueString;

	/**
	 * Store the Boolean value of the attribute for the baseEntity
	 */
	private Boolean valueBoolean;

	/**
	 * Stores the sale value in local currency.
	 */

	// @Column(name = "money", length = 128)
	@Convert(converter = MoneyConverter.class)
	Money valueMoney;

	/**
	 * Store the BaseEntity Code value of the attribute for the baseEntity
	 */
	@Convert(converter = StringListConverter.class)
	private List<String> ValueBaseEntityCodeList;

	/**
	 * Store the Expired boolean value of the attribute for the baseEntity
	 */
	private Boolean expired = false;

	/**
	 * Store the Refused boolean value of the attribute for the baseEntity
	 */
	private Boolean refused = false;

	/**
	 * Store wther this answer is inferred
	 */
	private Boolean inferred = false;

	/**
	 * Store the relative importance of the attribute for the baseEntity
	 */
	private Double weight;

	// @Version
	private Long version = 1L;

	private String targetCode;
	private String sourceCode;
	private Long askId;
	private String attributeCode;

	private String attributeDataType;

	public AnswerLink() {
	}

	/**
	 * Constructor.
	 * 
	 * @param source    the source entity
	 * @param target    the target entity
	 * @param answer 	the answer
	 */
	public AnswerLink(final BaseEntity source, final BaseEntity target, final Answer answer) {

		this(source, target, answer, 0.0); // make zero so to not impact scoring
		this.attributeCode = answer.getAttributeCode();
		this.attributeDataType = answer.getAttribute().getDataType().getClassName();
		this.setSourceCode(answer.getSourceCode());
		this.setTargetCode(answer.getTargetCode());
		this.setAskId(answer.getAskId());
	}

	/**
	 * Constructor.
	 * 
	 * @param source    the source entity
	 * @param target    the target entity
	 * @param answer 	the answer
	 * @param weight    the weighted importance
	 */
	public AnswerLink(final BaseEntity source, final BaseEntity target, final Answer answer, Double weight) {
		autocreateCreated();
		setSourceCode(source.getCode());
		setTargetCode(target.getCode());
		setAttributeCode(answer.getAttributeCode());
		this.attributeDataType = answer.getAttribute().getDataType().getClassName();

		// This permits ease of adding attributes and hides attribute from scoring.
		if (weight == null) {
			weight = 0.0;
		}
		setWeight(weight);
		setAnswer(answer);

	}
	
	/** 
	 * @param answer the answer to set
	 */
	@JsonIgnore
	public void setAnswer(final Answer answer) {
		this.setCreated(answer.getCreated());
		this.setExpired(answer.getExpired());
		this.setRefused(answer.getRefused());
		this.setInferred(answer.getInferred());
		
		List<String> formatStrings = null;

		switch (attributeDataType) {
			case "life.genny.qwandaq.entity":
				List<String> beCodeList = new CopyOnWriteArrayList<String>();
				beCodeList.add(answer.getValue());
				setValueBaseEntityCodeList(beCodeList);
				break;
			case "java.lang.Integer":
			case "Integer":
				String result = answer.getValue();
				if (!StringUtils.isBlank(result)) {

					final Integer integer = Integer.parseInt(result);
					setValueInteger(integer);
				} else {
					setValueInteger(0);

				}

				break;
			case "java.time.LocalDateTime":
			case "LocalDateTime":
				result = answer.getValue();
				if (!StringUtils.isBlank(result)) {

					formatStrings = Arrays.asList("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm",
							"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd HH:mm:ss.SSSZ");
					for (String formatString : formatStrings) {
						try {
							Date olddate = new SimpleDateFormat(formatString).parse(result);
							final LocalDateTime dateTime = olddate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
							setValueDateTime(dateTime);
							break;
						} catch (java.text.ParseException e) {
							continue;
						}

					}

				} 

				break;
			case "java.time.LocalTime":
			case "LocalTime":
				result = answer.getValue();
				formatStrings = Arrays.asList("HH:mm", "HH:mm:ss", "HH:mm:ss.SSSZ");
				for (String formatString : formatStrings) {
					Date olddate;
					try {
						olddate = new SimpleDateFormat(formatString).parse(result);
						final LocalTime dateTime = olddate.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
						setValueTime(dateTime);
					} catch (java.text.ParseException e) {
						continue;
					}

					break;

				}

				break;

			case "java.time.LocalDate":
			case "LocalDate":
				result = answer.getValue();
				if (!StringUtils.isBlank(result)) {
					formatStrings = Arrays.asList("yyyy-MM-dd", "M/y", "yyyy/MM/dd", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss",
							"yyyy-MM-dd'T'HH:mm:ss.SSSZ","yyyy-MM-dd HH:mm:ss.SSSZ");
					for (String formatString : formatStrings) {
						Date olddate;
						try {
							olddate = new SimpleDateFormat(formatString).parse(result);
							final LocalDate dateTime = olddate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
							setValueDate(dateTime);
						} catch (java.text.ParseException e) {
							continue;
						}

						break;

					}
				}

				break;
			case "java.lang.Long":
			case "Long":
				result = answer.getValue();
				if (!StringUtils.isBlank(result)) {

					final Long l = Long.parseLong(result);
					setValueLong(l);
				} else {
					setValueLong(0L);
				}

				break;
			case "java.lang.Double":
			case "Double":
				result = answer.getValue();
				if (!StringUtils.isBlank(result)) {

					Double d = null;
					try {
						d = Double.parseDouble(result);
					} catch (NumberFormatException e) {
						log.error("Bad double coversion for "+answer.getAttributeCode()+" for value="+answer.getValue());
						d = 0.0;
					}
					setValueDouble(d);
				} else {
					setValueDouble(0.0);
				}

				break;
			case "java.lang.Boolean":
			case "Boolean":
				result = answer.getValue();
				if (!StringUtils.isBlank(result)) {

					final Boolean b = Boolean.parseBoolean(result);
					setValueBoolean(b);

				} 

				break;
			case "org.javamoney.moneta.Money":
			case "Money":
				result = answer.getValue();
				if (!StringUtils.isBlank(result)) {
					JsonReader reader = Json.createReader(new StringReader(result));
					JsonObject obj = reader.readObject();

					CurrencyUnit currency = Monetary.getCurrency(obj.getString("currency"));
					Double amount = Double.valueOf(obj.getString("amount"));

					Money money = Money.of(amount, currency);
					setValueMoney(money);
				} else {
					setValueMoney(Money.zero(null));
				}

				break;
			case "java.lang.String":
			default:
				setValueString(answer.getValue());
				break;
		}

	}

	/**
	 * @return the askId
	 */
	public Long getAskId() {
		return askId;
	}

	/**
	 * @param askId the askId to set
	 */
	public void setAskId(final Long askId) {
		this.askId = askId;
	}

	/**
	 * @return the created
	 */
	public LocalDateTime getCreated() {
		return created;
	}

	/**
	 * @param created the created to set
	 */
	public void setCreated(final LocalDateTime created) {
		this.created = created;
	}

	/**
	 * @return the updated
	 */
	public LocalDateTime getUpdated() {
		return updated;
	}

	/**
	 * @param updated the updated to set
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
	 * @param weight the weight to set
	 */
	public void setWeight(final Double weight) {
		this.weight = weight;
	}

	/**
	 * @return the version
	 */
	public Long getVersion() {
		return version;
	}

	
	/** 
	 * @return Boolean
	 */
	public Boolean isExpired() {
		return getExpired();
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
	public Boolean isRefused() {
		return getRefused();
	}

	
	/** 
	 * @return Boolean
	 */
	public Boolean isValueBoolean() {
		return getValueBoolean();
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(final Long version) {
		this.version = version;
	}

	/**
	 * @return the inferred
	 */
	public Boolean getInferred() {
		return inferred;
	}

	/**
	 * @param inferred the inferred to set
	 */
	public void setInferred(Boolean inferred) {
		this.inferred = inferred;
	}

	/**
	 * @return the valueDouble
	 */
	public Double getValueDouble() {
		return valueDouble;
	}

	/**
	 * @param valueDouble the valueDouble to set
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
	 * @param valueInteger the valueInteger to set
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
	 * @param valueLong the valueLong to set
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
	 * @return the valueTime
	 */
	public LocalTime getValueTime() {
		return valueTime;
	}

	/**
	 * @param valueTime the valueTime to set
	 */
	public void setValueTime(LocalTime valueTime) {
		this.valueTime = valueTime;
	}

	/**
	 * @return the valueDateTime
	 */
	public LocalDateTime getValueDateTime() {
		return valueDateTime;
	}

	/**
	 * @param valueDateTime the valueDateTime to set
	 */
	public void setValueDateTime(final LocalDateTime valueDateTime) {
		this.valueDateTime = valueDateTime;
	}

	/**
	 * @return the valueString
	 */
	public String getValueString() {
		return valueString;
	}

	/**
	 * @param valueString the valueString to set
	 */
	public void setValueString(final String valueString) {
		this.valueString = valueString;
	}

	/**
	 * @return the valueBoolean
	 */
	public Boolean getValueBoolean() {
		return valueBoolean;
	}

	/**
	 * @param valueBoolean the valueBoolean to set
	 */
	public void setValueBoolean(final Boolean valueBoolean) {
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
	 * @return the valueBaseEntityCode
	 */
	public List<String> getValueBaseEntityCodeList() {
		return ValueBaseEntityCodeList;
	}

	/**
	 * @param valueBaseEntityCode the valueBaseEntityCode to set
	 */
	public void setValueBaseEntityCodeList(List<String> valueBaseEntityCode) {
		this.ValueBaseEntityCodeList = valueBaseEntityCode;
	}

	/**
	 * @return the expired
	 */
	public Boolean getExpired() {
		return expired;
	}

	/**
	 * @param expired the expired to set
	 */
	public void setExpired(final Boolean expired) {
		this.expired = expired;
	}

	/**
	 * @return the refused
	 */
	public Boolean getRefused() {
		return refused;
	}

	/**
	 * @param refused the refused to set
	 */
	public void setRefused(final Boolean refused) {
		this.refused = refused;
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
	public Date getCreatedDate() {
		final Date out = Date.from(created.atZone(ZoneId.systemDefault()).toInstant());
		return out;
	}

	/** 
	 * @return Date
	 */
	@Transient
	public Date getUpdatedDate() {
		Date out = null;
		if (updated != null) {
			out = Date.from(updated.atZone(ZoneId.systemDefault()).toInstant());
		}
		return out;
	}

	/** 
	 * @return String
	 */
	public String getTargetCode() {
		return targetCode;
	}

	/** 
	 * @param targetCode the targetCode to set
	 */
	public void setTargetCode(final String targetCode) {
		this.targetCode = targetCode;
	}

	/** 
	 * @return String
	 */
	public String getSourceCode() {
		return sourceCode;
	}

	/** 
	 * @param sourceCode the sourceCode to set
	 */
	public void setSourceCode(final String sourceCode) {
		this.sourceCode = sourceCode;
	}

	/** 
	 * @return String
	 */
	public String getAttributeCode() {
		return attributeCode;
	}
	
	/** 
	 * @param attributeCode the attributeCode to set
	 */
	public void setAttributeCode(final String attributeCode) {
		this.attributeCode = attributeCode;
	}
	
	/** 
	 * Check equality
	 *
	 * @param o the object to compare to
	 * @return boolean
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		final AnswerLink that = (AnswerLink) o;

		EqualsBuilder equalsBuilder = new EqualsBuilder();
		equalsBuilder.append(this.attributeCode, that.attributeCode);
		equalsBuilder.append(this.sourceCode, that.sourceCode);
		equalsBuilder.append(this.targetCode, this.targetCode);
		return equalsBuilder.isEquals();
	}
	
	/** 
	 * @return int
	 */
	@Override
	public int hashCode() {
		return (this.attributeCode + this.sourceCode + this.targetCode).hashCode();
	}
	
	/** 
	 * @return String
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "EE[" + getTargetCode() + ":" + created + ", linkType=" + getAttributeCode() + ",weight="
				+ weight + ", value=" + getValue() + ", v=" + version + "]";
	}

	/**
	* Get the value
	*
	* @param <T> the type to return
	* @return T
	 */
	@SuppressWarnings("unchecked")
	@JsonIgnore
	@Transient
	@XmlTransient
	public <T> T getValue() {
		switch (attributeDataType) {
		case "life.genny.qwandaq.entity":
			return (T) getValueBaseEntityCodeList();
		case "java.lang.Integer":
		case "Integer":
			return (T) getValueInteger();
		case "java.time.LocalDateTime":
		case "LocalDateTime":
			return (T) getValueDateTime();
		case "java.time.LocalDate":
		case "LocalDate":
			return (T) getValueDate();
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
	* @param <T> the type to return
	* @param value the object to set as the value
	 */
	@SuppressWarnings("unchecked")
	@JsonIgnore
	@Transient
	@XmlTransient
	public <T> void setValue(final Object value) {
		switch (attributeDataType) {
		case "life.genny.qwandaq.entity":
			setValueBaseEntityCodeList((List<String>) value);
			break;
		case "java.lang.Integer":
		case "Integer":
			Integer i = null;
			if (value instanceof String) {
				log.info("ANSWERLINK["+((String)value)+"]");
				i = Integer.parseInt((String)value);
				setValueInteger(i);
			} else {
				setValueInteger((Integer) value);
			}
			break;
		case "java.time.LocalDateTime":
		case "LocalDateTime":
			setValueDateTime((LocalDateTime) value);
			break;
		case "java.time.LocalTime":
		case "LocalTime":
			setValueTime((LocalTime) value);
			break;

		case "java.time.LocalDate":
		case "LocalDate":
			setValueDate((LocalDate) value);
			break;
		case "java.lang.Long":
		case "Long":
			setValueLong((Long) value);
			break;
		case "java.lang.Double":
		case "Double":
			setValueDouble((Double) value);
			break;
		case "java.lang.Boolean":
		case "Boolean":
			setValueBoolean((Boolean) value);
			break;
		case "org.javamoney.moneta.Money":
		case "Money":
			setValueMoney((Money) value);
			break;
		case "java.lang.String":
		default:
			setValueString((String) value);
			break;
		}

	}
}

