package life.genny.qwandaq.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Link;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.converter.MoneyConverter;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.annotations.Type;
import org.javamoney.moneta.Money;
import org.jboss.logging.Logger;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.text.DateFormat;
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
import java.util.Objects;








@Entity
@Table(name = "baseentity_baseentity")

@AssociationOverrides({
    @AssociationOverride(name = "pk.source", joinColumns = @JoinColumn(name = "SOURCE_ID"))
})

@RegisterForReflection
public class EntityEntity implements java.io.Serializable, Comparable<Object> {

	private static final Logger log = Logger.getLogger(EntityEntity.class);

	@AttributeOverrides({
		@AttributeOverride(name = "sourceCode", column = @Column(name = "SOURCE_CODE", nullable = false)),
		@AttributeOverride(name = "targetCode", column = @Column(name = "TARGET_CODE", nullable = false)),
		@AttributeOverride(name = "attributeCode", column = @Column(name = "LINK_CODE", nullable = false)),
		@AttributeOverride(name = "weight", column = @Column(name = "LINK_WEIGHT", nullable = false)),
		@AttributeOverride(name = "parentColour", column = @Column(name = "PARENT_COL", nullable = true)),
		@AttributeOverride(name = "childColour", column = @Column(name = "CHILD_COL", nullable = true)),
		@AttributeOverride(name = "rule", column = @Column(name = "RULE", nullable = true))
	})
	@Column
	private Link link;

	private String realm;

	private static final long serialVersionUID = 1L;

	@EmbeddedId
	@Column
	@JsonbTransient
	private EntityEntityId pk = new EntityEntityId();

	/**
	 * @return the link
	 */
	public Link getLink() {
		return link;

	}

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
	//  @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
	private LocalDateTime valueDateTime;

	/**
	 * Store the LocalDate value of the attribute for the baseEntity
	 */
	//  @XmlJavaTypeAdapter(LocalDateAdapter.class)
	private LocalDate valueDate;

	/**
	 * Store the LocalTime value of the attribute for the baseEntity
	 */
	//  @XmlJavaTypeAdapter(LocalTimeAdapter.class)
	private LocalTime valueTime;

	@Column(name = "money", length = 128)
	@Convert(converter = MoneyConverter.class)
	Money valueMoney;

	/**
	 * Store the String value of the attribute for the baseEntity
	 */
	@Type(type="text")
	private String valueString;

	/**
	 * Store the relative importance of the attribute for the baseEntity
	 */
	private Double weight;

	private Long version = 1L;


	public EntityEntity() {}

	/**
	 * Constructor.
	 * 
	 * @param source the source baseEntity
	 * @param target the target entity that is linked to
	 * @param attribute the associated attribute
	 * @param weight the weighted importance of this attribute (relative to the other attributes)
	 */
	public EntityEntity(final BaseEntity source, final BaseEntity target,
			final Attribute attribute, Double weight) {
		this(source,target,attribute, "DUMMY",weight);
		this.getLink().setLinkValue(null);
		this.setValueString(null);
	}

	/**
	 * Constructor.
	 * 
	 * @param source the source baseEntity
	 * @param target the target entity that is linked to
	 * @param attribute the associated attribute
	 * @param value the associated value
	 * @param weight the weighted importance of this attribute (relative to the other attributes)
	 */
	public EntityEntity(final BaseEntity source, final BaseEntity target,
			final Attribute attribute, final Object value, Double weight) {
		autocreateCreated();
		getPk().setSource(source);
		//    getPk().setTarget(target);
		getPk().setAttribute(attribute);
		this.setRealm(target.getRealm());
		//    this.pk.setSourceCode(source.getCode());
		this.pk.setTargetCode(target.getCode());
		link = new Link(source.getCode(),target.getCode(),attribute.getCode(),null);

		if (value != null) {
			setValue(value);
		}
		if (weight == null) {
			weight = 0.0; // This permits ease of adding attributes and hides
			// attribute from scoring.
		}
		setWeight(weight);
	}

	/**
	 * Constructor.
	 *
	 * @param source the source baseEntity
	 * @param target the target entity that is linked to
	 * @param attribute the associated attribute
	 * @param value the associated value
	 * @param weight the weighted importance of this attribute (relative to the other attributes)
	 */
	public EntityEntity(final BaseEntity source, final BaseEntity target,
			final Attribute attribute, Double weight, final Object value) {
		autocreateCreated();

		this.pk.setSource(source);
		//   this.pk.setTarget(target);
		this.pk.setTargetCode(target.getCode());
		this.pk.setAttribute(attribute);

		link = new Link(source.getCode(),target.getCode(),attribute.getCode());

		if (weight == null) {
			weight = 0.0; // This permits ease of adding attributes and hides
			// attribute from scoring.
		}
		setWeight(weight);
		if (value != null) {
			setValue(value);
		}
	}


	/** 
	 * @return EntityEntityId
	 */
	@JsonIgnore
	@JsonbTransient
	public EntityEntityId getPk() {
		return pk;
	}


	/** 
	 * @param pk the pk to set
	 */
	public void setPk(final EntityEntityId pk) {
		this.pk = pk;
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
		this.link.setWeight(weight);
	}

	/**
	 * @return the version
	 */
	public Long getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(final Long version) {
		this.version = version;
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
	 * @param link the link to set
	 */
	public void setLink(Link link) {
		this.link = link;
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
		return Date.from(created.atZone(ZoneId.systemDefault()).toInstant());
	}

	/** 
	 * @return Date
	 */
	@Transient
	@JsonbTransient
	public Date getUpdatedDate() {
		if (updated!=null) {
			return Date.from(updated.atZone(ZoneId.systemDefault()).toInstant());
		} else {
			return null;
		}
	}

	/** 
	 * @param o the object to compare to
	 * @return int
	 */
 @Override
	public int compareTo(Object o) {

		EntityEntity myClass = (EntityEntity) o;
		return new CompareToBuilder()
			//	       .appendSuper(super.compareTo(o)
			.append(this.weight, myClass.weight)
			.toComparison();
	}

	/** 
	 * @return int
	 */
	@Override
	public int hashCode() {
		return Objects.hash(link, realm, valueBoolean, valueDate, valueDateTime, valueDouble, valueInteger,
				valueLong, valueMoney, valueString, valueTime, weight);
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
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof EntityEntity other)) {
			return false;
		}
		return  Objects.equals(link, other.link)
			&& Objects.equals(realm, other.realm) && Objects.equals(valueBoolean, other.valueBoolean)
			&& Objects.equals(valueDate, other.valueDate) && Objects.equals(valueDateTime, other.valueDateTime)
			&& Objects.equals(valueDouble, other.valueDouble) && Objects.equals(valueInteger, other.valueInteger)
			&& Objects.equals(valueLong, other.valueLong) && Objects.equals(valueMoney, other.valueMoney)
			&& Objects.equals(valueString, other.valueString) && Objects.equals(valueTime, other.valueTime)
			&& Objects.equals(weight, other.weight);
	}

	/** 
	 * @param <T> the Type to return
	 * @return T
	 */
	@SuppressWarnings("unchecked")
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public <T> T getValue() {
		final String dataType = getPk().getAttribute().getDataType().getClassName();
		return switch (dataType) {
			case "java.lang.Integer" -> (T) getValueInteger();
			case "java.time.LocalDateTime" -> (T) getValueDateTime();
			case "java.time.LocalTime" -> (T) getValueTime();
			case "java.lang.Long" -> (T) getValueLong();
			case "java.lang.Double" -> (T) getValueDouble();
			case "java.lang.Boolean" -> (T) getValueBoolean();
			case "java.time.LocalDate" -> (T) getValueDate();

//			case "java.lang.String" ->
			default -> (T) getValueString();
		};

	}

	/**
	 * @return the valueBoolean
	 */
	public Boolean getValueBoolean() {
		return valueBoolean;
	}


	/** 
	 * @return Boolean
	 */
	public Boolean isValueBoolean() {
		return getValueBoolean();
	}

	/**
	 * @param valueBoolean the valueBoolean to set
	 */
	public void setValueBoolean(Boolean valueBoolean) {
		this.valueBoolean = valueBoolean;
	}

	/**
	 * @return the valueDate
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
	 * @param <T> the type to return
	 * @param value the value to set
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public <T> void setValue(final Object value) {

		if (value instanceof String result) {
			try {
				if (getPk().getAttribute().getDataType().getClassName().equalsIgnoreCase(String.class.getCanonicalName())) {
					setValueString(result);
				} else if (getPk().getAttribute().getDataType().getClassName()
						.equalsIgnoreCase(LocalDateTime.class.getCanonicalName())) {
					List<String> formatStrings = Arrays.asList("yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss",
							"yyyy-MM-dd'T'HH:mm:ss.SSSZ");
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
					// Date olddate = null;
					// olddate = DateTimeUtils.parseDateTime(result,
					// "yyyy-MM-dd","yyyy-MM-dd'T'HH:mm:ss","yyyy-MM-dd'T'HH:mm:ss.SSSZ");
					// final LocalDateTime dateTime =
					// olddate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
					// setValueDateTime(dateTime);
				} else if (getPk().getAttribute().getDataType().getClassName()
						.equalsIgnoreCase(LocalDate.class.getCanonicalName())) {
					Date olddate = null;
					try {
						olddate = DateUtils.parseDate(result, "M/y", "yyyy-MM-dd", "yyyy/MM/dd",
								"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
					} catch (java.text.ParseException e) {
						olddate = DateUtils.parseDate(result, "yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss",
								"yyyy-MM-dd'T'HH:mm:ss.SSSZ");
					}
					final LocalDate date = olddate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
					setValueDate(date);
				} else if (getPk().getAttribute().getDataType().getClassName()
						.equalsIgnoreCase(LocalTime.class.getCanonicalName())) {
					final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
					final LocalTime date = LocalTime.parse(result, formatter);
					setValueTime(date);
					// } else if (getPk().getAttribute().getDataType().getClassName()
					// 		.equalsIgnoreCase(Money.class.getCanonicalName())) {
					// 	GsonBuilder gsonBuilder = new GsonBuilder().registerTypeAdapter(Money.class, new MoneyDeserializer());
					// 	Gson gson = gsonBuilder.create();
					// 	Money money = gson.fromJson(result, Money.class);
					// 	setValueMoney(money);
			} else if (getPk().getAttribute().getDataType().getClassName()
					.equalsIgnoreCase(Integer.class.getCanonicalName())) {
				final Integer integer = Integer.parseInt(result);
				setValueInteger(integer);
			} else if (getPk().getAttribute().getDataType().getClassName()
					.equalsIgnoreCase(Double.class.getCanonicalName())) {
				final Double d = Double.parseDouble(result);
				setValueDouble(d);
			} else if (getPk().getAttribute().getDataType().getClassName()
					.equalsIgnoreCase(Long.class.getCanonicalName())) {
				final Long l = Long.parseLong(result);
				setValueLong(l);
			} else if (getPk().getAttribute().getDataType().getClassName()
					.equalsIgnoreCase(Boolean.class.getCanonicalName())) {
				final Boolean b = Boolean.parseBoolean(result);
				setValueBoolean(b);
			} else {
				setValueString(result);
			}
			} catch (Exception e) {
				log.error("Conversion Error :" + value + " for attribute " + getPk().getAttribute() + " and SourceCode:"
						+ this.getPk().getSource().getCode());
			}
		} else {

			switch (this.getPk().getAttribute().getDataType().getClassName()) {
				case "java.lang.Integer", "Integer" -> setValueInteger((Integer) value);
				case "java.time.LocalDateTime", "LocalDateTime" -> setValueDateTime((LocalDateTime) value);
				case "java.time.LocalDate", "LocalDate" -> setValueDate((LocalDate) value);
				case "java.lang.Long", "Long" -> setValueLong((Long) value);
				case "java.time.LocalTime", "LocalTime" -> setValueTime((LocalTime) value);
				case "org.javamoney.moneta.Money", "Money" -> setValueMoney((Money) value);
				case "java.lang.Double", "Double" -> setValueDouble((Double) value);
				case "java.lang.Boolean", "Boolean" -> setValueBoolean((Boolean) value);
//				case "java.lang.String" ->
				default -> setValueString((String) value);
			}
		}

		this.link.setLinkValue(getObjectAsString(getValue()));
	}

	/** 
	 * @return String
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public String getAsString() {
		String dataType = "";
		try {
			dataType = getPk().getAttribute().getDataType().getClassName();
		} catch (Exception e) {
		}
		return switch (dataType) {
			case "java.lang.Integer" -> "" + getValueInteger();
			case "java.time.LocalDateTime" -> {
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
				Date datetime = Date.from(getValueDateTime().atZone(ZoneId.systemDefault()).toInstant());
				yield df.format(datetime);
			}
			case "java.lang.Long" -> "" + getValueLong();
			case "java.time.LocalTime" -> getValueTime().toString();
			case "org.javamoney.moneta.Money" -> getValueMoney().toString();
			case "java.lang.Double" -> getValueDouble().toString();
			case "java.lang.Boolean" -> Boolean.TRUE.equals(getValueBoolean()) ? "TRUE" : "FALSE";
			case "java.time.LocalDate" -> {
				DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
				Date date = Date.from(getValueDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
				yield df2.format(date);
			}
//			case "java.lang.String" ->
			default -> getValueString();
		};
	}

	/** 
	 * @param value the value to get
	 * @return String
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public String getObjectAsString(Object value) {
		if (value instanceof Integer)
			return "" + value;
		if (value instanceof LocalDateTime val) {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
			Date datetime = Date.from(val.atZone(ZoneId.systemDefault()).toInstant());
			return df.format(datetime);
		}
		if (value instanceof Long)
			return "" + value;
		if (value instanceof Double val) {
			return val.toString();
		}
		if (value instanceof Boolean val) {
			return Boolean.TRUE.equals(val) ? "TRUE" : "FALSE";
		}
		if (value instanceof LocalDate val) {
			DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
			Date date = Date.from(val.atStartOfDay(ZoneId.systemDefault()).toInstant());
			return df2.format(date);
		}

		if (value instanceof Money val) {
			return val.toString();
		}
		if (value instanceof LocalTime val) {
			return val.toString();
		}
		return (String) value;
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
	 * @return String
	 */
	@Override
	public String toString() {
		return this.realm+":"+this.link;
	}
}
