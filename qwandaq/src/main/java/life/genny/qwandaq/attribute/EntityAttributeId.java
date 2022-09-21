package life.genny.qwandaq.attribute;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import life.genny.qwandaq.entity.BaseEntity;

@Embeddable
public class EntityAttributeId implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

<<<<<<< HEAD
	@ManyToOne()
	// @JsonBackReference(value="entityAttribute")
	@JsonManagedReference(value = "entityAttribute")
=======
	@ManyToOne ( )
	//	@JsonBackReference(value="entityAttribute")
	@JsonManagedReference(value="entityAttribute")
>>>>>>> @{-1}
	@JsonIgnoreProperties("baseEntityAttributes")
	@JsonbTransient
	public BaseEntity baseEntity;

	@ManyToOne
<<<<<<< HEAD
	@JsonBackReference(value = "attribute")
	// @JsonIgnore
	public Attribute attribute;

	/**
=======
	@JsonBackReference(value="attribute")
	//	@JsonIgnore
	public Attribute attribute;

	/** 
>>>>>>> @{-1}
	 * @return BaseEntity
	 */
	public BaseEntity getBaseEntity() {
		return baseEntity;
	}

<<<<<<< HEAD
	/**
=======
	/** 
>>>>>>> @{-1}
	 * @param baseEntity the baseentity to set
	 */
	public void setBaseEntity(final BaseEntity baseEntity) {
		this.baseEntity = baseEntity;
	}

<<<<<<< HEAD
	/**
=======
	/** 
>>>>>>> @{-1}
	 * @return Attribute
	 */
	public Attribute getAttribute() {
		return attribute;
	}

<<<<<<< HEAD
	/**
=======
	/** 
>>>>>>> @{-1}
	 * @param attribute the attribute to set
	 */
	public void setAttribute(final Attribute attribute) {
		this.attribute = attribute;
	}

<<<<<<< HEAD
	/**
	 * @return int
	 */
	/*
	 * (non-Javadoc)
	 * 
=======
	/** 
	 * @return int
	 */
	/* (non-Javadoc)
>>>>>>> @{-1}
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

<<<<<<< HEAD
	/**
=======
	/** 
>>>>>>> @{-1}
	 * Chek equality
	 *
	 * @param obj the object to compare to
	 * @return boolean
	 */
<<<<<<< HEAD
	/*
	 * (non-Javadoc)
	 * 
=======
	/* (non-Javadoc)
>>>>>>> @{-1}
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
		EntityAttributeId other = (EntityAttributeId) obj;
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
