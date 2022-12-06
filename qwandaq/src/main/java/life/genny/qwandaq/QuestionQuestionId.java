package life.genny.qwandaq;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import io.quarkus.runtime.annotations.RegisterForReflection;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.json.bind.annotation.JsonbTransient;

@Embeddable
@RegisterForReflection
public class QuestionQuestionId implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JsonBackReference(value = "questionQuestion")
	@JsonIgnore
	@JsonbTransient
	private Question source;

	private String targetCode;
	private String sourceCode;

	/**
	 * @return the targetCode
	 */
	public String getTargetCode() {
		return targetCode;
	}

	/**
	 * @param targetCode the targetCode to set
	 */
	public void setTargetCode(String targetCode) {
		this.targetCode = targetCode;
	}

	//
	/**
	 * @return the source
	 */
	@JsonIgnore
	public Question getSource() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(final Question source) {
		this.source = source;
		this.sourceCode = source.getCode();
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
	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	/**
	 * @return int
	 */
	@Override
	public int hashCode() {
		// int result;
		HashCodeBuilder hcb = new HashCodeBuilder();
		hcb.append(sourceCode);
		hcb.append(targetCode);
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
        if (obj instanceof QuestionQuestionId that) {
            EqualsBuilder eb = new EqualsBuilder();
            eb.append(sourceCode, that.sourceCode);
            eb.append(targetCode, that.targetCode);
            return eb.isEquals();
        } else {
            return false;
        }
    }

}
