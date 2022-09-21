package life.genny.qwandaq;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import io.quarkus.runtime.annotations.RegisterForReflection;

import org.apache.commons.lang3.builder.CompareToBuilder;

/*@Entity
@Table(name = "question_question", uniqueConstraints = @UniqueConstraint(columnNames = { "sourceCode", "targetCode",
		"realm" }), indexes = {
				@Index(columnList = "sourceCode", name = "source_idx"),
				@Index(columnList = "realm", name = "code_idx")
		})
@AssociationOverrides({ @AssociationOverride(name = "pk.source", joinColumns = @JoinColumn(name = "SOURCE_ID"))
})
@Cacheable*/
@RegisterForReflection
public class QuestionQuestion implements java.io.Serializable, Comparable<Object> {

	private static final long serialVersionUID = 1L;

	private String sourceCode;

	private String targetCode;

	private LocalDateTime created;

	/**
	 * Stores the Last Modified UMT DateTime that this object was last updated
	 */
	private LocalDateTime updated;

	/**
	 * Store the relative importance of this question link
	 */
	private Double weight;

	private Long version = 1L;

	Boolean mandatory = false;

	// If this is set to true then attribute needs to be set to readonly after value
	// set.
	Boolean oneshot = false;

	private Boolean disabled = false;
	private Boolean hidden = false;

	private Boolean readonly = false;

	private String realm;

	private Boolean formTrigger;

	private Boolean createOnTrigger;

	private String dependency;

	private String icon;

	private Set<String> parentQuestionCodes = new HashSet<>(0);

	private Set<String> childQuestionCodes = new HashSet<>(0);

	private Set<QuestionQuestion> parentQuestionQuestions = new HashSet<>(0);

	private Set<QuestionQuestion> childQuestionQuestions = new HashSet<>(0);

	public QuestionQuestion() {
	}

	/**
	 * Constructor.
	 * 
	 * @param source
	 *                   the source baseEntity
	 * @param targetCode
	 *                   the target code of the entity that is linked to
	 * @param weight
	 *                   the associated weight
	 * @param mandatory
	 *                   Is the question mandatory
	 * @param disabled
	 *                   Is the question read only
	 * @param readonly
	 *                   Is the question readonly
	 * @param hidden
	 *                   Is the question hidden * @param Weight
	 *                   the weighted importance of this attribute (relative to the
	 *                   other
	 *                   attributes)
	 */
	public QuestionQuestion(final Question source, final String targetCode, Double weight, boolean mandatory,
			boolean disabled, boolean hidden, boolean readonly) {
		autocreateCreated();
		setSourceCode(source.getCode());
		setTargetCode(targetCode);
		setMandatory(mandatory);
		setDisabled(disabled);
		setHidden(hidden);
		setReadonly(readonly);
		if (weight == null) {
			// This permits ease of adding attributes and hides attribute from scoring.
			weight = 0.0;
		}
		setWeight(weight);
	}

	/**
	 * Constructor.
	 * 
	 * @param source
	 *                   the source baseEntity
	 * @param targetCode
	 *                   the target code of the entity that is linked to
	 * @param weight
	 *                   the weighted importance of this attribute (relative to the
	 *                   other
	 *                   attributes)
	 * @param mandatory
	 *                   Is the question mandatory
	 * @param disabled
	 *                   Is the question read only
	 * @param hidden
	 *                   Is the question hidden
	 */
	public QuestionQuestion(final Question source, final String targetCode, Double weight, boolean mandatory,
			boolean disabled, boolean hidden) {
		this(source, targetCode, weight, mandatory, disabled, hidden, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param source
	 *                   the source baseEntity
	 * @param targetCode
	 *                   the target code of the entity that is linked to
	 * @param weight
	 *                   the weighted importance of this attribute (relative to the
	 *                   other
	 *                   attributes)
	 * @param mandatory
	 *                   Is the question mandatory
	 * @param disabled
	 *                   Is the question read only
	 */
	public QuestionQuestion(final Question source, final String targetCode, Double weight, boolean mandatory,
			boolean disabled) {
		this(source, targetCode, weight, mandatory, disabled, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param source
	 *                   the source baseEntity
	 * @param targetCode
	 *                   the target code of the entity that is linked to
	 * @param weight
	 *                   the weighted importance of this attribute (relative to the
	 *                   other
	 *                   attributes)
	 * @param mandatory
	 *                   The mandatory status of this QuestionQuestion
	 */
	public QuestionQuestion(final Question source, final String targetCode, Double weight, boolean mandatory) {
		this(source, targetCode, weight, mandatory, false);
	}

	/**
	 * Constructor.
	 *
	 * @param source
	 *               The Source Question.
	 * @param target
	 *               The Target Question.
	 * @param weight
	 *               the weighted importance of this attribute (relative to the
	 *               other
	 *               attributes)
	 */
	public QuestionQuestion(final Question source, final Question target, Double weight) {
		autocreateCreated();

		this.setSourceCode(source.getCode());

		this.setTargetCode(target.getCode());

		if (weight == null) {
			weight = 0.0; // This permits ease of adding attributes and hides
							// attribute from scoring.
		}
		setWeight(weight);

	}

	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	public void setTargetCode(String targetCode) {
		this.targetCode = targetCode;
	}

	/**
	 * @return the created
	 */
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
	 * @return the version
	 */
	public Long getVersion() {
		return version;
	}

	/**
	 * @param version
	 *                the version to set
	 */
	public void setVersion(final Long version) {
		this.version = version;
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
	 * @param mandatory
	 *                  the mandatory to set
	 */
	public void setMandatory(Boolean mandatory) {
		this.mandatory = mandatory;
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
	 * @param oneshot , if true then attribute must be set to readonly
	 */
	public void setOneshot(Boolean oneshot) {
		this.oneshot = oneshot;
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
	public Date getCreatedDate() {
		final Date out = Date.from(created.atZone(ZoneId.systemDefault()).toInstant());
		return out;
	}

	/**
	 * @return Date
	 */
	@Transient
	public Date getUpdatedDate() {
		if (updated != null) {
			final Date out = Date.from(updated.atZone(ZoneId.systemDefault()).toInstant());
			return out;
		} else {
			return null;
		}
	}

	/**
	 * @return int
	 */
	@Override
	public int hashCode() {

		HashCodeBuilder hcb = new HashCodeBuilder();
		hcb.append(getSourceCode());
		hcb.append(getTargetCode());
		hcb.append(getRealm());
		return hcb.toHashCode();
	}

	/**
	 * Check equality
	 *
	 * @param obj the object to compare to
	 * @return boolean
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof QuestionQuestion)) {
			return false;
		}
		QuestionQuestion that = (QuestionQuestion) obj;
		EqualsBuilder eb = new EqualsBuilder();
		eb.append(getSourceCode(), that.getSourceCode());
		eb.append(getTargetCode(), that.getTargetCode());
		eb.append(getRealm(), that.getRealm());
		return eb.isEquals();
	}

	/**
	 * Compare to an object
	 *
	 * @param o the object to compare to
	 * @return int
	 */
	public int compareTo(Object o) {
		QuestionQuestion myClass = (QuestionQuestion) o;
		return new CompareToBuilder()
				// .appendSuper(super.compareTo(o)
				.append(this.weight, myClass.weight)
				.toComparison();
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
	 * @return String
	 */
	@Override
	public String toString() {
		return "SRC:" + getSourceCode() + " - " + getTargetCode() + " "
				+ (this.getMandatory() ? "MANDATORY" : "OPTIONAL") + " " + (this.getReadonly() ? "RO" : "RW") + " "
				+ (this.getFormTrigger() ? "FT" : "NFT") + " " + (this.getCreateOnTrigger() ? "COT" : "NCOT");
	}

	/**
	 * @return String
	 */
	public String getSourceCode() {
		return getSourceCode();
	}

	/**
	 * @return String
	 */
	public String getTargetCode() {
		return getTargetCode();
	}

	/**
	 * @return String
	 */
	public String getDependency() {
		return dependency;
	}

	/**
	 * @param dependency the dependency to set
	 */
	public void setDependency(String dependency) {
		this.dependency = dependency;
	}

	/**
	 * @param icon the icon to set
	 */
	public void setIcon(String icon) {
		this.icon = icon;
	}

	/**
	 * @return String
	 */
	public String getIcon() {
		return this.icon;
	}

	public Set<String> getParentQuestionCodes() {
		return parentQuestionCodes;
	}

	public void setParentQuestionCodes(Set<String> parentQuestionCodes) {
		this.parentQuestionCodes = parentQuestionCodes;
	}

	public Set<String> getChildQuestionCodes() {
		return childQuestionCodes;
	}

	public void setChildQuestionCodes(Set<String> childQuestionCodes) {
		this.childQuestionCodes = childQuestionCodes;
	}

	public Set<QuestionQuestion> getParentQuestionQuestions() {
		return parentQuestionQuestions;
	}

	public void setParentQuestionQuestions(Set<QuestionQuestion> parentQuestionQuestions) {
		this.parentQuestionQuestions = parentQuestionQuestions;
	}

	public Set<QuestionQuestion> getChildQuestionQuestions() {
		return childQuestionQuestions;
	}

	public void setChildQuestionQuestions(Set<QuestionQuestion> childQuestionQuestions) {
		this.childQuestionQuestions = childQuestionQuestions;
	}
}
