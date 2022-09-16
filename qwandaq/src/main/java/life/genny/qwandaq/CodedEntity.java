/*
 * (C) Copyright 2017 GADA Technology (http://www.outcome-hub.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Adam Crow
 *     Byron Aguirre
 */

package life.genny.qwandaq;

import java.lang.invoke.MethodHandles;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * CoreEntity represents a base level core set of class attributes. It is the
 * base parent for many Qwanda classes and serves to establish Hibernate
 * compatibility and datetime stamping. This attribute information includes:
 * <ul>
 * <li>The Human Readable name for this class (used for summary lists)
 * <li>The unique code for the class object
 * <li>The description of the class object
 * <li>The created date time
 * <li>The last modified date time for the object
 * </ul>
 *
 * 
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */

@MappedSuperclass
@RegisterForReflection
public abstract class CodedEntity extends CoreEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Stores logger object.
	 */
	private static final Logger log = Logger.getLogger(CodedEntity.class);

	static public final String REGEX_CODE = "[A-Z]{3}\\_[A-Z0-9\\.\\-\\@\\_]*";

	/**
	 * A field that stores the unique code name of the entity.
	 * <p>
	 * p Note that the prefix of the attribute can specify the source. e.g.
	 * FBK_BIRTHDATE indicates that the attribute represents the facebook value
	 */
	@NotNull
	@Size(max = 64)
	@Pattern(regexp = REGEX_CODE, message = "Must be valid Code!")
	@Column(name = "code", updatable = false, nullable = false)
	private String code;

	@Transient
	private Integer index;

	// TODO, this probably should not be exposed once we have hibernate/infinispan
	// in place
	@Enumerated(EnumType.ORDINAL)
	private EEntityStatus status = EEntityStatus.ACTIVE;

	/**
	 * Constructor.
	 */
	protected CodedEntity() {
		// dummy
		// super();
		// setIndex(0);
	}

	/**
	 * Constructor.
	 * 
	 * @param name
	 *             the summary name of the coded entity
	 * @param code
	 *             the unique code of the coded entity
	 */
	public CodedEntity(String code, String name) {
		super(name);
		setCode(code);
		setIndex(0);
	}

	/**
	 * @return code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code the unique code of the coded entity
	 */
	public void setCode(String code) {
		if (code == null) {
			log.error("Null Code passed. Will result in error if saved");
		} else {
			this.code = code.toUpperCase();
		}
	}

	/**
	 * @return String
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return code + ":" + super.toString();
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
		HashCodeBuilder hcb = new HashCodeBuilder();
		hcb.append(code);
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
		if (!(obj instanceof CodedEntity)) {
			return false;
		}
		CodedEntity that = (CodedEntity) obj;
		EqualsBuilder eb = new EqualsBuilder();
		eb.append(code, that.getCode());
		return eb.isEquals();
	}

	/**
	 * Compare to an object.
	 *
	 * @param o the object to compare to
	 * @return int
	 */
	@Override
	public int compareTo(Object o) {
		CodedEntity myClass = (CodedEntity) o;
		return new CompareToBuilder().append(code, myClass.getCode())
				// .append(this.weight, myClass.weight)
				.toComparison();
	}

	/**
	 * @return the index
	 */
	public Integer getIndex() {
		return index;
	}

	/**
	 * @param index
	 *              the index to set
	 */
	public void setIndex(Integer index) {
		this.index = index;
	}

	/**
	 * @return the status
	 */
	public EEntityStatus getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(EEntityStatus status) {
		this.status = status;
	}

	/**
	 * @return boolean
	 */
	public boolean hasCode() {
		return code != null && !"".equals(code.trim());
	}
}
