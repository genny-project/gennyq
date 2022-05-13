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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.builder.CompareToBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import life.genny.qwandaq.exception.BadDataException;

import com.querydsl.core.annotations.QueryExclude;

/**
 * Ask represents the presentation of a Question to a source entity. A Question
 * object is refered to as a means of requesting information from a source about
 * a target attribute. This ask information includes:
 * <ul>
 * <li>The source of the answer (Who is being asked the question?)
 * <li>The target of the answer (To whom does the answer refer to?)
 * <li>The text that presents the question to the source
 * <li>The context entities that relate to the question
 * <li>The associated Question object
 * <li>The expiry duration that should be required to answer.
 * <li>The media used to ask this question.
 * <li>The associated answers List
 * </ul>
 * <p>
 * Asks represent the major way of retrieving facts (answers) about a target
 * from sources. Each ask is associated with an question which represents one or
 * more distinct fact about a target.
 * <p>
 * 
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */
@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)
@Table(name = "ask", indexes = { @Index(columnList = "id", name = "code_idx"),
		@Index(columnList = "realm", name = "code_idx") }, uniqueConstraints = @UniqueConstraint(columnNames = { "id",
				"realm" }))
@Entity
@QueryExclude
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@Inheritance(strategy = InheritanceType.JOINED)
public class Ask extends CoreEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@XmlTransient
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "question_id", nullable = false)
	private Question question;

	private String sourceCode;
	private String targetCode;
	private String questionCode;
	private String attributeCode;

	private Boolean mandatory = false;
	private Boolean oneshot = false;

	private Boolean disabled = false;
	private Boolean hidden = false;

	private Boolean readonly = false;
	private Double weight = 0.0;

	private Long parentId = 0L;

	private Boolean formTrigger = false;

	private Boolean createOnTrigger = false;

	@Transient
	private transient Ask[] childAsks;

	// @Embedded
	// @Valid
	// @JsonInclude(Include.NON_NULL)
	// private AnswerList answerList;

	@Embedded
	@Valid
	@JsonInclude(Include.NON_NULL)
	private ContextList contextList;

	@Transient
	private String processId = "no-idq";

	/**
	 * Constructor.
	 * dummy for hibernate
	 */
	@SuppressWarnings("unused")
	public Ask() {
	}

	/**
	 * Constructor.
	 * 
	 * @param aQuestion The associated Question
	 */
	public Ask(final Question aQuestion) {
		super(aQuestion.getName());
		setQuestion(aQuestion);
		contextList = new ContextList(new CopyOnWriteArrayList<Context>());
		this.disabled = false;
		this.hidden = false;
	}

	/**
	 * Constructor.
	 * 
	 * @param aAttributeCode The associated Attribute
	 * @param aSourceCode    The person answering the question
	 * @param aTargetCode    The BaseEntity that the question is about
	 * @param name           The name of the Ask
	 */
	public Ask(final String aAttributeCode, final String aSourceCode, final String aTargetCode, final String name) {
		super(name);

		this.sourceCode = aSourceCode;
		this.targetCode = aTargetCode;
		this.attributeCode = aAttributeCode;
		contextList = new ContextList(new CopyOnWriteArrayList<Context>());
		this.disabled = false;
		this.hidden = false;
		this.readonly = false;
	}

	/**
	 * Constructor.
	 * 
	 * @param aQuestion   The associated Question
	 * @param aSourceCode The person answering the question
	 * @param aTargetCode The BaseEntity that the question is about
	 */
	public Ask(final Question aQuestion, final String aSourceCode, final String aTargetCode) {
		this(aQuestion, aSourceCode, aTargetCode, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param aQuestion   The associated Question
	 * @param aSourceCode The person answering the question
	 * @param aTargetCode The BaseEntity that the question is about
	 * @param aMandatory  the mandatory status
	 */
	public Ask(final Question aQuestion, final String aSourceCode, final String aTargetCode, final Boolean aMandatory) {
		this(aQuestion, aSourceCode, aTargetCode, aMandatory, 0.0);
	}

	/**
	 * Constructor.
	 * 
	 * @param aQuestion   The associated Question
	 * @param aSourceCode The person answering the question
	 * @param aTargetCode The BaseEntity that the question is about
	 * @param aMandatory  the mandatory status
	 * @param weight      the weight
	 */
	public Ask(final Question aQuestion, final String aSourceCode, final String aTargetCode, final Boolean aMandatory,
			final Double weight) {
		this(aQuestion, aSourceCode, aTargetCode, aMandatory, weight, false, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param aQuestion   The associated Question
	 * @param aSourceCode The person answering the question
	 * @param aTargetCode The BaseEntity that the question is about
	 * @param aMandatory  the mandatory status
	 * @param weight      the weight
	 * @param disabled    the disabled status
	 * @param hidden      the hidden status
	 */
	public Ask(final Question aQuestion, final String aSourceCode, final String aTargetCode, final Boolean aMandatory,
			final Double weight, final Boolean disabled, final Boolean hidden) {

		this(aQuestion, aSourceCode, aTargetCode, aMandatory, weight, disabled, hidden, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param aQuestion   The associated Question
	 * @param aSourceCode The source answering the question
	 * @param aTargetCode The BaseEntity that the question is about
	 * @param aMandatory  the mandatory status
	 * @param weight      the weight
	 * @param disabled    the disabled status
	 * @param hidden      the hidden status
	 * @param readonly    the readonly status
	 */
	public Ask(final Question aQuestion, final String aSourceCode, final String aTargetCode, final Boolean aMandatory,
			final Double weight, final Boolean disabled, final Boolean hidden, final Boolean readonly) {
		super(aQuestion.getName());
		setQuestion(aQuestion);

		this.sourceCode = aSourceCode;
		this.targetCode = aTargetCode;
		this.attributeCode = aQuestion.getAttributeCode();
		contextList = new ContextList(new CopyOnWriteArrayList<Context>());
		this.mandatory = aMandatory;
		this.weight = weight;
		this.disabled = disabled;
		this.hidden = hidden;
		this.readonly = readonly;
	}

	/**
	 * @return the question
	 */
	public Question getQuestion() {
		return question;
	}

	/**
	 * @param question the question to set
	 */
	public void setQuestion(final Question question) {
		this.question = question;
		this.questionCode = question.getCode();
		this.attributeCode = question.getAttributeCode(); // .getAttribute().getCode();
	}

	// /**
	// * @return the answerList
	// */
	// public AnswerList getAnswerList() {
	// return answerList;
	// }

	// /**
	// * @param answerList the answerList to set
	// */
	// public void setAnswerList(final AnswerList answerList) {
	// this.answerList = answerList;
	// }

	/**
	 * @return the contextList
	 */
	public ContextList getContextList() {
		return contextList;
	}

	/**
	 * @param contextList the contextList to set
	 */
	public void setContextList(final ContextList contextList) {
		this.contextList = contextList;
	}

	/**
	 * @return the mandatory
	 */
	public Boolean getMandatory() {
		return mandatory;
	}

	/**
	 * @return the mandatory
	 */
	public Boolean isMandatory() {
		return getMandatory();
	}

	/**
	 * @param mandatory the mandatory to set
	 */
	public void setMandatory(Boolean mandatory) {
		this.mandatory = mandatory;
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
	public void setWeight(Double weight) {
		this.weight = weight;
	}

	/**
	 * @return the sourceCode
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
	 * @return the targetCode
	 */
	public String getTargetCode() {
		return targetCode;
	}

	/**
	 * @return the questionCode
	 */
	public String getQuestionCode() {
		return questionCode;
	}

	/**
	 * @param questionCode the questionCode to set
	 */
	public void setQuestionCode(final String questionCode) {
		this.questionCode = questionCode;
	}

	/**
	 * @return the disabled
	 */
	public Boolean getDisabled() {
		return disabled;
	}

	/**
	 * @return the disabled
	 */
	public Boolean isDisabled() {
		return getDisabled();
	}

	/**
	 * @param disabled the disabled to set
	 */
	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * @return the hidden
	 */
	public Boolean getHidden() {
		return hidden;
	}

	/**
	 * @return the hidden
	 */
	public Boolean isHidden() {
		return getHidden();
	}

	/**
	 * @param hidden the hidden to set
	 */
	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	/**
	 * @return the parentId
	 */
	public Long getParentId() {
		return parentId;
	}

	/**
	 * @param parentId the parentId to set
	 */
	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	/**
	 * @return the attributeCode
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
	 * @param targetCode the targetCode to set
	 */
	public void setTargetCode(final String targetCode) {
		this.targetCode = targetCode;
	}

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	/**
	 * Add an Answer to an entity.
	 *
	 * @param answer the Answer to add to the entity
	 * @throws BadDataException exception thrown if sourceCode, targetCode or
	 *                          attributeCode don't match
	 */
	public void add(final Answer answer) throws BadDataException {
		if ((answer.getSourceCode().equals(sourceCode)) && (answer.getTargetCode().equals(targetCode))
				&& (answer.getAttributeCode().equals(attributeCode))) {
			// getAnswerList().getAnswerList().add(new AnswerLink(source, target, answer));
		} else {
			throw new BadDataException("Source / Target ids do not match Ask");
		}

	}

	/**
	 * Compare to an object
	 *
	 * @param o the object to compare to
	 * @return int
	 */
	@Override
	public int compareTo(Object o) {
		Ask myClass = (Ask) o;
		return new CompareToBuilder().append(questionCode, myClass.getQuestionCode())
				.append(targetCode, myClass.getTargetCode()).toComparison();
	}

	/**
	 * @return the childAsks
	 */
	@Transient
	public Ask[] getChildAsks() {
		return childAsks;
	}

	/**
	 * @param childAsks the childAsks to set
	 */
	public void setChildAsks(Ask[] childAsks) {
		this.childAsks = childAsks;
	}

	/**
	 * @return the oneshot
	 */
	public Boolean getOneshot() {
		return oneshot;
	}

	/**
	 * @return the oneshot
	 */
	public Boolean isOneshot() {
		return getOneshot();
	}

	/**
	 * @param oneshot the oneshot to set
	 */
	public void setOneshot(Boolean oneshot) {
		this.oneshot = oneshot;
	}

	/**
	 * @return the readonly
	 */
	public Boolean getReadonly() {
		return readonly;
	}

	/**
	 * @return the readonly
	 */
	public Boolean isReadonly() {
		return getReadonly();
	}

	/**
	 * @param readonly the readonly to set
	 */
	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	/**
	 * @return the formTrigger
	 */
	public Boolean getFormTrigger() {
		return formTrigger;
	}

	/**
	 * @return the formTrigger
	 */
	public Boolean isFormTrigger() {
		return getFormTrigger();
	}

	/**
	 * @param formTrigger the formTrigger to set
	 */
	public void setFormTrigger(Boolean formTrigger) {
		this.formTrigger = formTrigger;
	}

	/**
	 * @return the createOnTrigger
	 */
	public Boolean getCreateOnTrigger() {
		return createOnTrigger;
	}

	/**
	 * @return the createOnTrigger
	 */
	public Boolean isCreateOnTrigger() {
		return getCreateOnTrigger();
	}

	/**
	 * @param createOnTrigger the createOnTrigger to set
	 */
	public void setCreateOnTrigger(Boolean createOnTrigger) {
		this.createOnTrigger = createOnTrigger;
	}

	/**
	 * @return Boolean
	 */
	@XmlTransient
	@Transient
	@JsonbTransient
	@JsonIgnore
	public Boolean hasTriggerQuestion() {
		// recurse through the childAsks
		// this is used to tell if intermediate BaseEntity is to be created and then
		// copied in upon a trigger question
		if (this.formTrigger) {
			return true;
		} else {
			if ((this.childAsks != null) && (this.childAsks.length > 0)) {
				for (Ask childAsk : this.childAsks) {
					return childAsk.hasTriggerQuestion();
				}
			}
		}
		return false;
	}

	/**
	 * Clone an Ask
	 *
	 * @param ask the Ask to clone
	 * @return Ask
	 */
	public static Ask clone(Ask ask) {
		Ask newAsk = new Ask();
		newAsk.sourceCode = ask.getSourceCode();
		newAsk.targetCode = ask.getTargetCode();
		newAsk.questionCode = ask.getQuestionCode();
		newAsk.question = ask.getQuestion();
		newAsk.attributeCode = ask.getAttributeCode();
		newAsk.mandatory = ask.getMandatory();
		newAsk.oneshot = ask.getOneshot();
		newAsk.disabled = ask.getDisabled();
		newAsk.readonly = ask.getReadonly();
		newAsk.weight = ask.getWeight();
		newAsk.parentId = ask.getParentId();
		newAsk.formTrigger = ask.getFormTrigger();
		newAsk.createOnTrigger = ask.getCreateOnTrigger();
		if (ask.getChildAsks() != null && ask.getChildAsks().length > 0) {
			newAsk.childAsks = ask.getChildAsks();
		}
		if (ask.getContextList() != null) {
			newAsk.contextList = ask.getContextList();
		}
		return newAsk;
	}

	/**
	* Add a child to childAsks
	*
	* @param child The child ask to add
	 */
	public void addChildAsk(Ask child) {

		List<Ask> children = Arrays.asList(getChildAsks());
		children.add(child);
		setChildAsks(children.toArray(new Ask[children.size()]));
	}

}
