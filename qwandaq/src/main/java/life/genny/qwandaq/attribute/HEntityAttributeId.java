package life.genny.qwandaq.attribute;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.HBaseEntity;
import org.apache.commons.lang3.builder.EqualsBuilder;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

@Embeddable
public class HEntityAttributeId implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	@ManyToOne()
	// @JsonBackReference(value="entityAttribute")
        @JsonManagedReference(value = "entityAttribute")
        @JsonIgnoreProperties("baseEntityAttributes")
        @JsonbTransient
	public HBaseEntity baseEntity;

	@ManyToOne
	@JsonBackReference(value = "attribute")
	// @JsonIgnore
	public HAttribute attribute;

	/**
	 * @return BaseEntity
	 */
	public HBaseEntity getBaseEntity() {
		return baseEntity;
	}

	/**
	 * @param baseEntity the baseentity to set
	 */
	public void setBaseEntity(final HBaseEntity baseEntity) {
		this.baseEntity = baseEntity;
	}

	/** 
	 * @return Attribute
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

	/**
	 * @return int
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
		result = prime * result + ((baseEntity == null) ? 0 : baseEntity.hashCode());
		return result;
	}

	/** 
	 * Chek equality
	 *
	 * @param obj the object to compare to
	 * @return boolean
	 */
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HEntityAttributeId other = (HEntityAttributeId) obj;
		EqualsBuilder equalsBuilder = new EqualsBuilder();
		equalsBuilder.append(baseEntity, other.baseEntity);
		equalsBuilder.append(attribute, other.attribute);
		return equalsBuilder.isEquals();
	}

}
