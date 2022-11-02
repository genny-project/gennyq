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

import java.util.ArrayList;
import java.util.Arrays;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.List;

import javax.json.bind.annotation.JsonbProperty;

import org.apache.commons.lang3.builder.CompareToBuilder;

import life.genny.qwandaq.exception.runtime.BadDataException;

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
 * </p>
 * 
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */
public class Ask extends CoreEntity {

	private static final long serialVersionUID = 1L;

	@XmlTransient
	/*@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "question_id", nullable = false)*/
	private Question question;
	private String questionCode;
	private String attributeCode;

	private String processId = "no-idq";
	private String sourceCode;
	private String targetCode;

	private Boolean mandatory = false;
	private Boolean oneshot = false;
	private Boolean disabled = false;
	private Boolean hidden = false;
	private Boolean readonly = false;

	private Double weight = 0.0;

	private List<Ask> childAsks;

	/**
	 * Default Constructor.
	 */
	public Ask() {
		super();
	}

	/**
	 * @param question
	 * @param sourceCode
	 * @param targetCode
	 */
	public Ask(Question question, String sourceCode, String targetCode) {
		super(question.getName());
		setQuestion(question);
		this.sourceCode = sourceCode;
		this.targetCode = targetCode;
	}

	/**
	 * Clone an Ask
	 *
	 * @param ask the Ask to clone
	 * @return Ask
	 */
	public Ask clone() {

		Ask ask = new Ask(question, sourceCode, targetCode);
		ask.mandatory = this.getMandatory();
		ask.oneshot = this.getOneshot();
		ask.disabled = this.getDisabled();
		ask.readonly = this.getReadonly();
		ask.weight = this.getWeight();

		if (this.hasChildren()) {
			ask.childAsks = this.childAsks;
		}
		return ask;
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
		return new CompareToBuilder()
				.append(question.getCode(), myClass.getQuestion().getCode())
				.append(sourceCode, myClass.getSourceCode())
				.append(targetCode, myClass.getTargetCode())
				.toComparison();
	}

	/**
	 * @return
	 */
	public boolean hasChildren() {
		return !(this.childAsks == null || this.childAsks.isEmpty());
	}

	/**
	 * Add a child to childAsks
	 *
	 * @param child The child ask to add
	 */
	public void add(Ask child) {
		if (this.childAsks == null)
			this.childAsks = new ArrayList<Ask>();
		this.childAsks.add(child);
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Question getQuestion() {
		return question;
	}

	public void setQuestion(Question question) {
		this.question = question;
		this.questionCode = question.getCode();
		this.attributeCode = question.getAttribute().getCode();
	}

	public String getQuestionCode() {
		return questionCode;
	}

	public void setQuestionCode(String questionCode) {
		this.questionCode = questionCode;
	}

	public String getAttributeCode() {
		return attributeCode;
	}

	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
	}

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	public String getSourceCode() {
		return sourceCode;
	}

	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	public String getTargetCode() {
		return targetCode;
	}

	public void setTargetCode(String targetCode) {
		this.targetCode = targetCode;
	}

	public Boolean getMandatory() {
		return mandatory;
	}

	public void setMandatory(Boolean mandatory) {
		this.mandatory = mandatory;
	}

	public Boolean getOneshot() {
		return oneshot;
	}

	public void setOneshot(Boolean oneshot) {
		this.oneshot = oneshot;
	}

	public Boolean getDisabled() {
		return disabled;
	}

	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public Boolean getReadonly() {
		return readonly;
	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public List<Ask> getChildAsks() {
		return childAsks;
	}

	public void setChildAsks(Ask[] children) {
		this.setChildAsks(Arrays.asList(children));
	}

	public void setChildAsks(List<Ask> children) {
		this.childAsks = children;
	}
}
