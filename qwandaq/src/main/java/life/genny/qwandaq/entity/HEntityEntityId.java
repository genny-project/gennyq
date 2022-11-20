package life.genny.qwandaq.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.HAttribute;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Embeddable
@RegisterForReflection
public class HEntityEntityId implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	@ManyToOne()
	@JsonManagedReference(value = "entityEntity")
	@JsonIgnoreProperties("links")
	private HBaseEntity source;

	private String targetCode;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	private HAttribute attribute;

	public HEntityEntityId() {
	}

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

	/**
	 * @return the source
	 */
	public HBaseEntity getSource() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(final HBaseEntity source) {
		this.source = source;
	}

	/**
	 * @return the attribute
	 */
	public HAttribute getAttribute() {
		return attribute;
	}

	/**
	 * @param attribute the attribute to set
	 */
	public void setAttribute(final HAttribute attribute) {
		this.attribute = attribute;
	}

	@Override
	public int hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder();
		hcb.append(attribute);
		hcb.append(source.getCode());
		hcb.append(targetCode);
		return hcb.toHashCode();
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
		if (!(obj instanceof HEntityEntityId)) {
			return false;
		}
		HEntityEntityId that = (HEntityEntityId) obj;
		EqualsBuilder eb = new EqualsBuilder();
		eb.append(attribute, that.attribute);
		eb.append(source.getCode(), that.source.getCode());
		eb.append(targetCode, that.targetCode);
		return eb.isEquals();
	}

}
