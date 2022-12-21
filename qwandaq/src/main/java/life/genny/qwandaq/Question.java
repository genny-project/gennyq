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

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.intf.ICapabilityHiddenFilterable;

/**
 * Question is the abstract base class for all questions managed in the Qwanda
 * library. A Question object is used as a means of requesting information from
 * a source about a target attribute. This question information includes:
 * <ul>
 * <li>The Human Readable name for this question (used for summary lists)
 * <li>A title for the question
 * <li>The text that presents the default question to the source
 * <li>The attribute that the question serves to fill
 * <li>The contexts that are mandatory for this question
 * <li>The default expiry duration that should be required to answer.
 * <li>The default media used to ask this question.
 * </ul>
 * <p>
 * Questions represent the major way of retrieving facts about a target from
 * sources. Each question is associated with an attribute which represents a
 * distinct fact about a target.
 * </p>
 * 
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */

@RegisterForReflection
public class Question extends CodedEntity implements ICapabilityHiddenFilterable {

	private static final Logger log = Logger.getLogger(Question.class);

	private static final long serialVersionUID = 1L;

	// core
	public static final String QUE_SUBMIT = "QUE_SUBMIT";
	public static final String QUE_CANCEL = "QUE_CANCEL";
	public static final String QUE_RESET = "QUE_RESET";

	public static final String QUE_UPDATE = "QUE_UPDATE";
	public static final String QUE_UNDO = "QUE_UNDO";
	public static final String QUE_REDO = "QUE_REDO";

	public static final String QUE_NEXT = "QUE_NEXT";
	public static final String QUE_PREVIOUS = "QUE_PREVIOUS";

	public static final String QUE_EVENTS = "QUE_EVENTS";

	// entity
	public static final String QUE_NAME = "QUE_NAME";
	public static final String QUE_EMAIL = "QUE_EMAIL";
	public static final String QUE_MOBILE = "QUE_MOBILE";

	// navigation
	public static final String QUE_PROCESS = "QUE_PROCESS";
	public static final String QUE_DASHBOARD = "QUE_DASHBOARD";

	@JsonManagedReference(value = "questionQuestion")
	@JsonbTransient
	private Set<QuestionQuestion> childQuestions = new HashSet<>(0);

	private Set<String> childQuestionCodes = new HashSet<>(0);

	private Attribute attribute;

	private Set<Capability> capabilityRequirements;

	private String attributeCode;

	private Boolean mandatory = false;

	private Boolean readonly = false;

	private Boolean oneshot = false;

	private String placeholder = "";

	private String directions = "";

	@Type(type = "text")
	private String html;

	private String helper = "";

	private String icon;

	/**
	 * @return String
	 */
	public String getHelper() {
		return helper;
	}

	/**
	 * @param helper the helper to set
	 */
	public void setHelper(String helper) {
		this.helper = helper;
	}

	/**
	 * Constructor.
	 */
	public Question() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param code      The unique code for this Question
	 * @param name      The human readable summary name
	 * @param attribute The associated attribute
	 */
	public Question(final String code, final String name, final Attribute attribute) {
		this(code, name);
		this.attribute = attribute;
		this.attributeCode = attribute.getCode();
	}

	/**
	 * Constructor.
	 * 
	 * @param code The unique code for this empty Question Group
	 * @param name The human readable summary name
	 */
	public Question(final String code, final String name) {
		super(code, name);
		if (childQuestions == null) {
			throw new InvalidParameterException("QuestionList must not be null");
		}
		this.attribute = null;
		this.attributeCode = Attribute.QQQ_QUESTION_GROUP;
	}

	/**
	 * @param childQuestions the List of child Questions to initialize with
	 */
	@Transient
	public void initialiseChildQuestions(List<Question> childQuestions) {

		// Assume the list of Questions represents the order
		Double sortPriority = 10.0;
        this.setChildQuestions(new HashSet<>(0));

		for (Question childQuestion : childQuestions) {
			QuestionQuestion qq = new QuestionQuestion(this, childQuestion, sortPriority);
			this.getChildQuestions().add(qq);
			sortPriority += 10.0;
		}

	}

	/**
	 * addTarget This links this question to a target question and associated weight
	 * to the question. It auto creates the QuestionQuestion object and sets itself
	 * to be the source. For efficiency we assume the link does not already exist
	 * 
	 * @param target the target to add
	 * @param weight the weight
	 * @return QuestionQuestion
	 * @throws BadDataException if target could not be added
	 */
	public QuestionQuestion addTarget(final Question target, final Double weight) throws BadDataException {
		if (target == null)
			throw new BadDataException("missing Target Entity");
		if (weight == null)
			throw new BadDataException("missing weight");

		final QuestionQuestion qq = new QuestionQuestion(this, target, weight);
		getChildQuestions().add(qq);
		return qq;
	}


	public Set<Capability> getCapabilityRequirements() {
		return this.capabilityRequirements;
	}

	public void setCapabilityRequirements(Set<Capability> requirements) {
		this.capabilityRequirements = requirements;
	}

	/**
	 * @return the attribute
	 */
	public Attribute getAttribute() {
		return attribute;
	}

	/**
	 * @param attribute the attribute to set
	 */
	public void setAttribute(final Attribute attribute) {
		this.attribute = attribute;
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
	 * @return the html
	 */
	public String getHtml() {
		return html;
	}

	/**
	 * @param html the html to set
	 */
	public void setHtml(String html) {
		this.html = html;
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
	 * @return the placeholder
	 */
	public String getPlaceholder() {
		return placeholder;
	}

	/**
	 * @param placeholder the placeholder to set
	 */
	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}

	/**
	 * @param mandatory the mandatory to set
	 */
	public void setMandatory(Boolean mandatory) {
		this.mandatory = mandatory;
	}

	/**
	 * @return the directions
	 */
	public String getDirections() {
		return directions;
	}

	/**
	 * @param directions the directions to set
	 */
	public void setDirections(String directions) {
		this.directions = directions;
	}

	/**
	 * @return the childQuestions
	 */
	public Set<QuestionQuestion> getChildQuestions() {
		return childQuestions;
	}

	/**
	 * @param childQuestions the childQuestions to set
	 */
	public void setChildQuestions(Set<QuestionQuestion> childQuestions) {
		this.childQuestions = childQuestions;
	}

	/**
	 * @param childQuestions the childQuestions to set
	 */
	public void setChildQuestions(ArrayList<QuestionQuestion> childQuestions) {
        this.childQuestions = new HashSet<QuestionQuestion>(childQuestions);
    }

	/**
	 * removeChildQuestion This removes a child Question from the question group.
	 * For efficiency we assume the child question exists
	 *
	 * @param childQuestionCode the code of the child Question used to remove the
	 *                          child Question
	 * @return <b>true</b> if child question was present, <b>false</b> otherwise
	 */
	public boolean removeChildQuestion(final String childQuestionCode) {
		final Optional<QuestionQuestion> optQuestionQuestion = findQuestionLink(childQuestionCode);
		boolean isPresent = optQuestionQuestion.isPresent();
		if(isPresent)
			getChildQuestions().remove(optQuestionQuestion.get());
		return isPresent;
	}

	/**
	 * findChildQuestion This returns an QuestionLink if it exists in the question
	 * group.
	 *
	 * @param childQuestionCode the code of the child Question used to find the
	 *                          Question Link
	 * @return Optional&lt;QuestionQuestion&gt;
	 */
	public Optional<QuestionQuestion> findQuestionLink(final String childQuestionCode) {
		final Optional<QuestionQuestion> foundEntity = Optional.of(getChildQuestions().parallelStream()
				.filter(x -> (x.getChildCode().equals(childQuestionCode))).findFirst().get());

		return foundEntity;
	}

	/**
	 * findQuestionQuestion This returns an question link if it exists in the
	 * question group. Could be more efficient in retrival (ACC: test)
	 *
	 * @param childQuestion the code of the child Question used to find the
	 *                      QuestionQuestion
	 * @return QuestionQuestion
	 */
	public QuestionQuestion findQuestionQuestion(final Question childQuestion) {
		final QuestionQuestion foundEntity = getChildQuestions().parallelStream()
				.filter(x -> (x.getChildCode().equals(childQuestion.getCode()))).findFirst().get();

		return foundEntity;
	}

	/**
	 * @return String
	 */
	@Override
	public String toString() {
		return this.getCode() + ":" + getChildQuestionCodes();
	}

	/**
	 * @return String
	 */
	@Transient
	@JsonIgnore
	private String getChildQuestionCodes() {
		List<QuestionQuestion> qqList = new CopyOnWriteArrayList<QuestionQuestion>(getChildQuestions());
		Collections.sort(qqList);
		String ret = "";
		if (getAttributeCode().equals(Attribute.QQQ_QUESTION_GROUP)) {
			for (QuestionQuestion childQuestion : qqList) {
				ret += childQuestion.getChildCode() + ",";
			}
		} else {
			ret = getCode();
		}
		return ret;
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
	 * @return the icon
	 */
	public String getIcon() {
		return icon;
	}

	/**
	 * @param icon the icon to set
	 */
	public void setIcon(String icon) {
		this.icon = icon;
	}

	public Set<String> getChildQuestionCodesAsStrings() {
		return childQuestionCodes;
	}

	public void setChildQuestionCodes(Set<String> childQuestionCodes) {
		this.childQuestionCodes = childQuestionCodes;
	}
}
