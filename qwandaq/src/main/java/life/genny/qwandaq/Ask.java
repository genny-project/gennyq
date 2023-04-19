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

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;

import javax.xml.bind.annotation.XmlTransient;
import java.util.LinkedHashSet;
import java.util.Set;

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
	private String testTargetCode;

	private Boolean mandatory = false;
	private Boolean oneshot = false;
	private Boolean disabled = false;
	private Boolean hidden = false;
	private Boolean readonly = false;

	private Double weight = 0.0;

	private LinkedHashSet<Ask> childAsks;

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
	public Ask(Question question, String sourceCode, String targetCode, double weight) {
		super(question.getName());
		setQuestion(question);
		this.sourceCode = sourceCode;
		this.targetCode = targetCode;
		this.weight = weight;
	}

	/**
	 * Clone an Ask
	 *
	 * @param ask the Ask to clone
	 * @return Ask
	 */
	public Ask clone() {

		Ask ask = new Ask(question, sourceCode, targetCode, weight);
		ask.mandatory = this.getMandatory();
		ask.oneshot = this.getOneshot();
		ask.disabled = this.getDisabled();
		ask.readonly = this.getReadonly();
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
				.append(question.getRealm(), myClass.getRealm())
				.append(question.getCode(), myClass.getQuestion().getCode())
				.append(sourceCode, myClass.getSourceCode())
				.append(targetCode, myClass.getTargetCode())
				.append(weight, myClass.getWeight())
				.toComparison();
	}

	@Override
	public int hashCode() {
		return (question.getRealm() + questionCode + sourceCode + targetCode).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof Ask)) {
			return false;
		}
		Ask otherAsk = (Ask) obj;
		EqualsBuilder equalsBuilder = new EqualsBuilder();
		equalsBuilder.append(this.question.getRealm(), otherAsk.question.getRealm());
		equalsBuilder.append(this.questionCode, otherAsk.questionCode);
		equalsBuilder.append(this.sourceCode, otherAsk.sourceCode);
		equalsBuilder.append(this.targetCode, otherAsk.targetCode);
		return equalsBuilder.isEquals();
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
			this.childAsks = new LinkedHashSet<Ask>();
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
		this.attributeCode = question.getAttributeCode();
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

	public String getTestTargetCode() {
		return testTargetCode;
	}

	public void setTestTargetCode(String testTargetCode) {
		this.testTargetCode = testTargetCode;
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

	public boolean getReadonly() {
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

	public Set<Ask> getChildAsks() {
		return childAsks;
	}

	public void setChildAsks(LinkedHashSet<Ask> children) {
		this.childAsks = children;
	}
}
