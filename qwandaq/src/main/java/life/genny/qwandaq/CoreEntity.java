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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.cdi.crud.infra.model.CoreEntityInterface;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.datatype.LocalDateTimeAdapter;

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
public abstract class CoreEntity implements CoreEntityInterface, CreatedIntf, Comparable<Object> {

	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(CoreEntity.class);

	static public final String REGEX_NAME = "[\\pL0-9/\\:\\ \\_\\.\\,\\?\\>\\<\\%\\$\\&\\!\\*" + ""
			+ "\\[\\]\\'\\-\\@\\(\\)]+.?";
	static public final String REGEX_REALM = "[a-zA-Z0-9]+";
	static public final String DEFAULT_REALM = "genny";

	/**
	 * Stores the Created UMT DateTime that this object was created
	 */
	// @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
	@Column(name = "created")
	private LocalDateTime created;

	/**
	 * Stores the Last Modified UMT DateTime that this object was last updated
	 */
	// @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
	@Column(name = "updated")
	private LocalDateTime updated;

	/**
	 * Stores the hibernate generated Id value for this object
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Basic(optional = false)
	@Column(name = "id", updatable = false, nullable = false)
	private Long id;

	/**
	 * A field that stores the human readable summary name of the attribute.
	 * Note that this field is in English.
	 */
	@NotNull
	@Size(max = 128)
	@Pattern(regexp = REGEX_NAME, message = "Must contain valid characters for name")
	@Column(name = "name", updatable = true, nullable = true)
	private String name;

	/**
	 * A field that stores the human readable realm of this entity.
	 * <p>
	 * Note that this field is in English.
	 */
	@NotNull
	@Size(max = 48)
	@Pattern(regexp = REGEX_REALM, message = "Must contain valid characters for realm")
	@Column(name = "realm", updatable = true, nullable = false)
	private String realm = DEFAULT_REALM;

	/**
	 * Constructor.
	 */
	public CoreEntity() {
		autocreateCreated();
	}

	/**
	 * Constructor.
	 * 
	 * @param realm
	 *              the security realm of the core entity
	 * @param name
	 *              the name of the core entity
	 */
	public CoreEntity(final String realm, final String name) {
		super();
		this.realm = realm;
		this.name = name;
		autocreateCreated();
	}

	/**
	 * Constructor.
	 * 
	 * @param name
	 *             the summary name of the core entity
	 */
	public CoreEntity(final String name) {
		super();
		this.realm = DEFAULT_REALM;
		this.name = name;
		autocreateCreated();
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 *           the id to set
	 */
	public void setId(final Long id) {
		this.id = id;
	}

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @param aName
	 *              human readable text representing the question
	 */
	public void setName(final String aName) {
		this.name = aName;
	}

	/**
	 * @return the created
	 */
	@XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
	@JsonbTransient
	public LocalDateTime getCreated() {
		return created;
	}

	/**
	 * @param created
	 *                the created to set
	 */
	@Override
	public void setCreated(final LocalDateTime created) {
		this.created = created;
	}

	/**
	 * @return the updated
	 */
	@JsonbTransient
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
	 * @return the realm
	 */
	public String getRealm() {
		return realm;
	}

	/**
	 * @param realm the realm to set
	 * @return self
	 */
	public CoreEntity setRealm(final String realm) {
		this.realm = realm;
		return this;
	}

	@PreUpdate
	public void autocreateUpdate() {
		setUpdated(LocalDateTime.now(ZoneId.of("Z")));
	}

	@PrePersist
	public void autocreateCreated() {
		if (getCreated() == null)
			setCreated(LocalDateTime.now(ZoneId.of("Z")));
		autocreateUpdate();
	}

	/**
	 * @return Date
	 */
	@Transient
	@JsonIgnore
	@JsonbTransient
	public Date getCreatedDate() {
		final Date out = Date.from(created.atZone(ZoneId.systemDefault()).toInstant());
		return out;
	}

	/**
	 * @return Date
	 */
	@Transient
	@JsonIgnore
	@JsonbTransient
	public Date getUpdatedDate() {
		if (updated != null) {
			final Date out = Date.from(updated.atZone(ZoneId.systemDefault()).toInstant());
			return out;
		} else
			return null;
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
		return "[id=" + id + ", created=" + created + ", updated=" + updated + ", name=" + name + "]";
	}

	/**
	 * @return boolean
	 */
	public boolean hasName() {
		return name != null && !"".equals(name.trim());
	}
}
