package life.genny.qwandaq.attribute;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import life.genny.qwandaq.entity.BaseEntity;

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
	public BaseEntity baseEntity;

	@ManyToOne
	@JsonBackReference(value = "attribute")
	// @JsonIgnore
	public Attribute attribute;

	/**
	 * @return BaseEntity
	 */
	public BaseEntity getBaseEntity() {
		return baseEntity;
	}

	/**
	 * @param baseEntity the baseentity to set
	 */
	public void setBaseEntity(final BaseEntity baseEntity) {
		this.baseEntity = baseEntity;
	}

	/** 
	 * @return Attribute
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
		if (attribute == null) {
			if (other.attribute != null)
				return false;
		} else if (!attribute.equals(other.attribute))
			return false;
		if (baseEntity == null) {
			if (other.baseEntity != null)
				return false;
		} else if (!baseEntity.equals(other.baseEntity))
			return false;
		return true;
	}

}
