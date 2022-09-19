/*
 * (C) Copyright 2017 GADA Technology (http://www.outcome-hub.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 * Contributors: Adam Crow Byron Aguirre
 */

package life.genny.qwandaq;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.annotations.QueryExclude;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.datatype.LocalDateTimeAdapter;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.BadDataException;

/**
 * Answer is the abstract base class for all answers managed in the Qwanda
 * library. An Answer object is used as a means of storing information from a
 * source about a target attribute. This answer information includes:
 * <ul>
 * <li>The Associated Ask
 * <li>The time at which the answer was created
 * <li>The status of the answer e.g Expired, Refused, Answered
 * </ul>
 * <p>
 * Answers represent the manner in which facts about a target from sources are
 * stored. Each Answer is associated with an attribute.
 * </p>
 * 
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */
@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)
@Table(name = "answer", indexes = {
		@Index(columnList = "targetcode", name = "code_idx"),
		@Index(columnList = "attributecode", name = "code_idx"),
		@Index(columnList = "realm", name = "code_idx")
})
@Entity
@QueryExclude
@Immutable
@RegisterForReflection
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
public class Answer {

	private static final long serialVersionUID = 1L;

	/**
	 * Stores the hibernate generated Id value for this object
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
	@GenericGenerator(name = "native", strategy = "native")

	@Basic(optional = false)
	@Column(name = "id", updatable = false, nullable = false)
	private Long id;

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
	 * A field that stores the human readable value of the answer.
	 */
	@NotNull
	@Type(type = "text")
	@Column(name = "value", updatable = true, nullable = false)
	private String value;

	/**
	 * A field that stores the human readable attributecode associated with this
	 * answer.
	 */
	@NotNull
	@Size(max = 250)
	@Column(name = "attributecode", updatable = true, nullable = false)
	private String attributeCode;

	@JsonIgnore
	@NotNull
	@XmlTransient
	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "attribute_id", nullable = false)
	private Attribute attribute;

	/**
	 * Store the askId (if present)
	 */
	private Long askId;

	/**
	 * Store the processId (if present)
	 */
	private String processId;

	/**
	 * A field that stores the human readable targetcode associated with this
	 * answer.
	 */
	@NotNull
	@Size(max = 64)
	@Column(name = "targetcode", updatable = true, nullable = true)
	private String targetCode;

	/**
	 * A field that stores the human readable sourcecode associated with this
	 * answer.
	 */
	@NotNull
	@Size(max = 64)
	@Column(name = "sourcecode", updatable = true, nullable = true)
	private String sourceCode;

	private String code;

	/**
	 * Store the Expired boolean value of the attribute for the baseEntity
	 */
	private Boolean expired = false;

	/**
	 * Store the Refused boolean value of the attribute for the baseEntity
	 */
	private Boolean refused = false;

	/**
	 * Store the relative importance of the attribute for the baseEntity
	 */
	private Double weight = 0.0;

	/**
	 * Store whether this answer was inferred
	 */
	private Boolean inferred = false;

	private Boolean changeEvent = false;

	// Provide a clue to any new attribute type that may be needed if the attribute
	// does not exist yet, e.g. java.util.Double
	@Transient
	private String dataType = null;

	private String realm;

	/**
	 * Constructor.
	 */
	@SuppressWarnings("unused")
	public Answer() {
	}

	/**
	 * Constructor.
	 * 
	 * @param source
	 *                  The source associated with this Answer
	 * @param target
	 *                  The target associated with this Answer
	 * @param attribute
	 *                  The attribute associated with this Answer
	 * @param value
	 *                  The associated String value
	 */
	public Answer(final BaseEntity source, final BaseEntity target, final Attribute attribute, final String value) {
		this.sourceCode = source.getCode();
		this.targetCode = target.getCode();
		this.attributeCode = attribute.getCode();
		this.attribute = attribute;
		this.setValue(value);
		autocreateCreated();
		checkInputs();
	}

	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *                      The sourceCode associated with this Answer
	 * @param targetCode
	 *                      The targetCode associated with this Answer
	 * @param attributeCode
	 *                      The attributeCode associated with this Answer
	 * @param value
	 *                      The associated String value
	 */
	public Answer(final String sourceCode, final String targetCode, final String attributeCode, final String value) {
		this.sourceCode = sourceCode;
		this.targetCode = targetCode;
		this.attributeCode = attributeCode;
		this.setValue(value);
		autocreateCreated();
		checkInputs();
	}

	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *                      The sourceCode associated with this Answer
	 * @param targetCode
	 *                      The targetCode associated with this Answer
	 * @param attributeCode
	 *                      The attributeCode associated with this Answer
	 * @param value
	 *                      The associated Double value
	 */
	public Answer(final String sourceCode, final String targetCode, final String attributeCode, final Double value) {
		this(sourceCode, targetCode, attributeCode, value + "");
	}

	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *                      The sourceCode associated with this Answer
	 * @param targetCode
	 *                      The targetCode associated with this Answer
	 * @param attributeCode
	 *                      The attributeCode associated with this Answer
	 * @param value
	 *                      The associated String value
	 * @param changeEvent
	 *                      The changeEvent status
	 * @param inferred
	 *                      The inferred status
	 */
	public Answer(final String sourceCode, final String targetCode, final String attributeCode, final Double value,
			final Boolean changeEvent, final Boolean inferred) {
		this(sourceCode, targetCode, attributeCode, value + "");
		this.changeEvent = changeEvent;
		this.inferred = inferred;
	}

	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *                      The sourceCode associated with this Answer
	 * @param targetCode
	 *                      The targetCode associated with this Answer
	 * @param attributeCode
	 *                      The attributeCode associated with this Answer
	 * @param value
	 *                      The associated Long value
	 */
	public Answer(final String sourceCode, final String targetCode, final String attributeCode, final Long value) {
		this(sourceCode, targetCode, attributeCode, value + "");
	}

	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *                      The sourceCode associated with this Answer
	 * @param targetCode
	 *                      The targetCode associated with this Answer
	 * @param attributeCode
	 *                      The attributeCode associated with this Answer
	 * @param value
	 *                      The associated LocalDateTime value
	 */
	public Answer(final String sourceCode, final String targetCode, final String attributeCode,
			final LocalDateTime value) {
		this(sourceCode, targetCode, attributeCode, value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
	}

	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *                      The sourceCode associated with this Answer
	 * @param targetCode
	 *                      The targetCode associated with this Answer
	 * @param attributeCode
	 *                      The attributeCode associated with this Answer
	 * @param value
	 *                      The associated LocalDate value
	 */
	public Answer(final String sourceCode, final String targetCode, final String attributeCode, final LocalDate value) {
		this(sourceCode, targetCode, attributeCode, value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
	}

	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *                      The sourceCode associated with this Answer
	 * @param targetCode
	 *                      The targetCode associated with this Answer
	 * @param attributeCode
	 *                      The attributeCode associated with this Answer
	 * @param value
	 *                      The associated LocalTime value
	 */
	public Answer(final String sourceCode, final String targetCode, final String attributeCode, final LocalTime value) {
		this(sourceCode, targetCode, attributeCode, value.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
	}

	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *                      The sourceCode associated with this Answer
	 * @param targetCode
	 *                      The targetCode associated with this Answer
	 * @param attributeCode
	 *                      The attributeCode associated with this Answer
	 * @param value
	 *                      The associated Integer value
	 */
	public Answer(final String sourceCode, final String targetCode, final String attributeCode, final Integer value) {
		this(sourceCode, targetCode, attributeCode, value + "");
	}

	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *                      The sourceCode associated with this Answer
	 * @param targetCode
	 *                      The targetCode associated with this Answer
	 * @param attributeCode
	 *                      The attributeCode associated with this Answer
	 * @param value
	 *                      The associated Boolean value
	 */
	public Answer(final String sourceCode, final String targetCode, final String attributeCode, final Boolean value) {
		this(sourceCode, targetCode, attributeCode, value ? "TRUE" : "FALSE");

	}

	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *                      The sourceCode associated with this Answer
	 * @param targetCode
	 *                      The targetCode associated with this Answer
	 * @param attributeCode
	 *                      The attributeCode associated with this Answer
	 * @param value
	 *                      The associated String value
	 * @param changeEvent
	 *                      The changeEvent status
	 * @param inferred
	 *                      The inferred status
	 */
	public Answer(final String sourceCode, final String targetCode, final String attributeCode, final String value,
			final Boolean changeEvent, final Boolean inferred) {
		this.sourceCode = sourceCode;
		this.targetCode = targetCode;
		this.attributeCode = attributeCode;
		this.setValue(value);
		autocreateCreated();
		checkInputs();
		this.changeEvent = changeEvent;
		this.inferred = inferred;
	}

	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *                      The sourceCode associated with this Answer
	 * @param targetCode
	 *                      The targetCode associated with this Answer
	 * @param attributeCode
	 *                      The attributeCode associated with this Answer
	 * @param value
	 *                      The associated String value
	 * @param changeEvent
	 *                      The changeEvent status
	 */
	public Answer(final String sourceCode, final String targetCode, final String attributeCode, final String value,
			final Boolean changeEvent) {
		this(sourceCode, targetCode, attributeCode, value, changeEvent, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param source
	 *                      The source BE associated with this Answer
	 * @param target
	 *                      The target BE associated with this Answer
	 * @param attributeCode
	 *                      The attributeCode associated with this Answer
	 * @param value
	 *                      The associated String value
	 */
	public Answer(final BaseEntity source, final BaseEntity target, final String attributeCode, final String value) {
		this.sourceCode = source.getCode();
		this.targetCode = target.getCode();
		this.attributeCode = attributeCode;
		this.setValue(value);
		autocreateCreated();
		checkInputs();
	}

	/**
	 * Constructor.
	 * 
	 * @param aAsk
	 *              The ask that created this answer
	 * @param value
	 *              The associated String value
	 * @throws BadDataException if Answer could not be constructed
	 */
	public Answer(final Ask aAsk, final String value) throws BadDataException {
		this.askId = aAsk.getId();
		this.attributeCode = aAsk.getQuestion().getAttribute().getCode();
		this.attribute = aAsk.getQuestion().getAttribute();
		this.sourceCode = aAsk.getSourceCode();
		this.targetCode = aAsk.getTargetCode();
		this.setValue(value);
		autocreateCreated();
		checkInputs();
		// this.ask.add(this);
	}

	/**
	 * Constructor.
	 * 
	 * @param aAsk
	 *                The ask that created this answer
	 * @param expired
	 *                did this ask expire?
	 * @param refused
	 *                did the user refuse this question?
	 * @throws BadDataException if Answer could not be constructed
	 */
	public Answer(final Ask aAsk, final Boolean expired, final Boolean refused) throws BadDataException {
		// this.ask = aAsk;
		// this.attributeCode = this.ask.getQuestion().getAttribute().getCode();
		// this.attribute = this.ask.getQuestion().getAttribute();
		// this.sourceCode = this.ask.getSource().getCode();
		// this.targetCode = this.ask.getTarget().getCode();

		this.setRefused(refused);
		this.setExpired(expired);
		autocreateCreated();
		checkInputs();
		// this.ask.add(this);
	}

	/**
	 * @return the created
	 */
	@XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
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

	@PreUpdate
	/**
	 * Set Updated to current UTC Time
	 */
	public void autocreateUpdate() {
		setUpdated(LocalDateTime.now(ZoneId.of("Z")));
	}

	@PrePersist
	/**
	 * set created to the current UTC time
	 */
	public void autocreateCreated() {
		if (getCreated() == null)
			setCreated(LocalDateTime.now(ZoneId.of("Z")));
	}

	/**
	 * @return Date
	 */
	@JsonbTransient
	@JsonIgnore
	public Date getCreatedDate() {
		final Date out = Date.from(created.atZone(ZoneId.systemDefault()).toInstant());
		return out;
	}

	/**
	 * @return Date
	 */
	@JsonbTransient
	@JsonIgnore
	public Date getUpdatedDate() {
		final Date out = Date.from(updated.atZone(ZoneId.systemDefault()).toInstant());
		return out;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 *           the id to set
	 */
	public void setId(final Long id) {
		this.id = id;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value
	 *              the value to set
	 */
	public void setValue(final String value) {
		if (value != null) {
			this.value = value.trim();
		} else {
			this.value = "";
		}
	}

	/**
	 * @return the expired
	 */
	public Boolean getExpired() {
		return expired;
	}

	/**
	 * @param expired
	 *                the expired to set
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
	 * @param refused
	 *                the refused to set
	 */
	public void setRefused(final Boolean refused) {
		this.refused = refused;
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

	// /**
	// * @return the ask
	// */
	// public Ask getAsk() {
	// return ask;
	// }
	//
	// /**
	// * @param ask the ask to set
	// */
	// public void setAsk(final Ask ask) {
	// this.ask = ask;
	// }

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
	 * @return the askId
	 */
	public Long getAskId() {
		return askId;
	}

	/**
	 * @param askId
	 *              the askId to set
	 */
	public void setAskId(final Long askId) {
		this.askId = askId;
	}

	/*
	 * @return the current kogito workflow process Id
	 */
	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
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
	 * @return the targetCode
	 */
	public String getTargetCode() {
		return targetCode;
	}

	/**
	 * @param targetCode
	 *                   the targetCode to set
	 */
	public void setTargetCode(final String targetCode) {
		this.targetCode = targetCode;
	}

	/**
	 * @return the sourceCode
	 */
	public String getSourceCode() {
		return sourceCode;
	}

	/**
	 * @param sourceCode
	 *                   the sourceCode to set
	 */
	public void setSourceCode(final String sourceCode) {
		this.sourceCode = sourceCode;
	}

	/**
	 * @return the attribute
	 */
	public Attribute getAttribute() {
		return attribute;
	}

	/**
	 * @param attribute
	 *                  the attribute to set
	 */
	public void setAttribute(final Attribute attribute) {
		this.attribute = attribute;
		if (this.dataType == null) {
			setDataType(attribute.getDataType().getClassName());
		}
	}

	/**
	 * @return the changeEvent
	 */
	public Boolean getChangeEvent() {
		return changeEvent;
	}

	/**
	 * @param changeEvent
	 *                    the changeEvent to set
	 */
	public void setChangeEvent(Boolean changeEvent) {
		this.changeEvent = changeEvent;
	}

	/**
	 * @return the dataType
	 */
	public String getDataType() {
		return dataType;
	}

	/**
	 * @param dataType the dataType to set
	 */
	public void setDataType(String dataType) {
		this.dataType = dataType;
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
	@JsonbTransient
	@JsonIgnore
	public String getUniqueCode() {
		return getSourceCode() + ":" + getTargetCode() + ":" + getAttributeCode();
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
		return "Answer [" + realm + "," + (created != null ? "created=" + created + ", " : "")
				+ (sourceCode != null ? "sourceCode=" + sourceCode + ", " : "")
				+ (targetCode != null ? "targetCode=" + targetCode + ", " : "")
				+ (attributeCode != null ? "attributeCode=" + attributeCode + ", " : "")
				+ (value != null ? "value=" + value + ", " : "") + (askId != null ? "askId=" + askId + ", " : "")
				+ (expired != null ? "expired=" + expired + ", " : "")
				+ (refused != null ? "refused=" + refused + ", " : "")
				+ (weight != null ? "weight=" + weight + ", " : "") + (inferred != null ? "inferred=" + inferred : "")
				+ "]";
	}

	private void checkInputs() {
		if (this.sourceCode == null)
			throw new NullPointerException("SourceCode cannot be null");
		if (this.targetCode == null)
			throw new NullPointerException("targetCode cannot be null");
		if (this.attributeCode == null)
			throw new NullPointerException("attributeCode cannot be null");
	}

	/**
	 * @return changeEvent
	 */
	public Boolean isChangeEvent() {
		return this.changeEvent;
	}

	/**
	 * 
	 * @return expired
	 */
	public Boolean isExpired() {
		return this.expired;
	}

	/**
	 * 
	 * @return inferred
	 */
	public Boolean isInferred() {
		return this.inferred;
	}

	public Boolean isRefused() {
		return this.refused;
	}

	/**
	 * @return relevant question code
	 */
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}
