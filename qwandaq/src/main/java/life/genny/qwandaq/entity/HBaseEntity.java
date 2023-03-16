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

package life.genny.qwandaq.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.CodedEntity;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.HEntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import org.hibernate.annotations.*;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.jboss.logging.Logger;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * BaseEntity represents a base entity that contains many attributes. It is the
 * base parent for many Qwanda classes and serves to establish Hibernate
 * compatibility and datetime stamping. BaseEntity objects may be scored against
 * each other. BaseEntity objects may not have a deterministic code Examples of
 * derivative entities may be Person, Company, Event, Product, TradeService.
 * This attribute information includes:
 * <ul>
 * <li>The List of attributes
 * </ul>
 *
 *
 *
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */

@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)
@Table(name = "baseentity", indexes = { @Index(columnList = "code", name = "code_idx"),
		@Index(columnList = "realm", name = "code_idx") }, uniqueConstraints = @UniqueConstraint(columnNames = {
		"code",
		"realm"
}))
@Entity
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@FilterDefs({
		@FilterDef(name = "filterAttribute", defaultCondition = "attributeCode in (:attributeCodes)", parameters = {
				@ParamDef(name = "attributeCodes", type = "string")
		}),
		@FilterDef(name = "filterAttribute2", defaultCondition = "attributeCode =:attributeCode", parameters = {
				@ParamDef(name = "attributeCode", type = "string")
		})
})
@Cacheable
@RegisterForReflection
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class HBaseEntity extends CodedEntity implements CoreEntityPersistable, BaseEntityIntf {

	@Transient
	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(HBaseEntity.class);

	private static final String DEFAULT_CODE_PREFIX = "BAS_";

	public static final String PRI_NAME = "PRI_NAME";
	public static final String PRI_IMAGE_URL = "PRI_IMAGE_URL";
	public static final String PRI_PHONE = "PRI_PHONE";
	public static final String PRI_ADDRESS_FULL = "PRI_ADDRESS_FULL";
	public static final String PRI_EMAIL = "PRI_EMAIL";

	@XmlTransient
	@OneToMany(fetch = FetchType.EAGER, mappedBy = "pk.baseEntity", cascade = CascadeType.ALL)
	@JsonBackReference(value = "entityAttribute")
	// @Cascade({CascadeType.MERGE, CascadeType.REMOVE})
	@Filters({
			@org.hibernate.annotations.Filter(name = "filterAttribute", condition = "attributeCode in (:attributeCodes)"),
			@org.hibernate.annotations.Filter(name = "filterAttribute2", condition = "attributeCode =:attributeCode")
	})
	private Set<HEntityAttribute> baseEntityAttributes = new HashSet<HEntityAttribute>(0);

	@XmlTransient
	@Transient
	private Boolean fromCache = false;

	@JsonIgnore
	@JsonbTransient
	@XmlTransient
	@Transient
	private transient Map<String, HEntityAttribute> attributeMap = null;

	/**
	 * @return Map&lt;String, HEntityAttribute&gt; the attributeMap
	 */
	public Map<String, HEntityAttribute> getAttributeMap() {
		return attributeMap;
	}

	/**
	 * @param attributeMap the attributeMap to set
	 */
	public void setAttributeMap(Map<String, HEntityAttribute> attributeMap) {
		this.attributeMap = attributeMap;
	}

	/**
	 * Constructor.
	 */
	public HBaseEntity() {
		// super();
		// dummy
	}

	/**
	 * Constructor.
	 *
	 * @param aName the summary name of the core entity
	 */
	public HBaseEntity(final String aName) {
		super(getDefaultCodePrefix() + UUID.randomUUID().toString(), aName);
	}

	/**
	 * Constructor.
	 *
	 * @param aCode the unique code of the core entity
	 * @param aName the summary name of the core entity
	 */
	@ProtoFactory
	public HBaseEntity(final String aCode, final String aName) {
		super(aCode, aName);
	}

	/**
	 * @return the baseEntityAttributes
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Set<HEntityAttribute> getBaseEntityAttributes() {
		return baseEntityAttributes;
	}

	/**
	 * @param baseEntityAttributes the baseEntityAttributes to set
	 */
	public void setBaseEntityAttributes(final Set<HEntityAttribute> baseEntityAttributes) {
		this.baseEntityAttributes = baseEntityAttributes;
	}

	/**
	 * @param baseEntityAttributes the baseEntityAttributes to set
	 */
	public void setBaseEntityAttributes(final List<HEntityAttribute> baseEntityAttributes) {
		this.baseEntityAttributes.addAll(baseEntityAttributes);
	}

	/**
	 * getDefaultCodePrefix This method is expected to be overridden in specialised
	 * child classes.
	 *
	 * @return the default Code prefix for this class.
	 */

	static public String getDefaultCodePrefix() {
		return DEFAULT_CODE_PREFIX;
	}

	/**
	 * containsEntityAttribute This checks if an attribute exists in the baseEntity.
	 *
	 * @param attributeCode the attributeCode to check
	 * @return boolean
	 */
	public boolean containsEntityAttribute(final String attributeCode) {
		boolean ret = false;

		if (attributeMap != null) {
			return attributeMap.containsKey(attributeCode);
		}
		// Check if this code exists in the baseEntityAttributes
		if (getBaseEntityAttributes().parallelStream().anyMatch(ti -> ti.getAttributeCode().equals(attributeCode))) {
			ret = true;
		}
		return ret;
	}

	/**
	 * findEntityAttribute This returns an attributeEntity if it exists in the
	 * baseEntity.
	 *
	 * @param attributeCode the attributeCode to find with
	 * @return Optional
	 */
	public Optional<HEntityAttribute> findEntityAttribute(final String attributeCode) {

		if (attributeMap != null) {
			return Optional.ofNullable(attributeMap.get(attributeCode));
		}
		Optional<HEntityAttribute> foundEntity = Optional.empty();

		try {
			foundEntity = getBaseEntityAttributes().stream()
					.filter(x -> (x.getAttributeCode().equals(attributeCode)))
					.findFirst();
		} catch (Exception e) {
			log.error("Error in fetching attribute value: " + attributeCode);
		}

		return foundEntity;
	}

	/**
	 * findEntityAttribute This returns an attributeEntity if it exists in the
	 * baseEntity. Could be more efficient in retrival (ACC: test)
	 *
	 * @param attributePrefix the attributePrefix to find with
	 * @return HEntityAttribute
	 */
	public List<HEntityAttribute> findPrefixEntityAttributes(final String attributePrefix) {
		List<HEntityAttribute> foundEntitys = getBaseEntityAttributes().stream()
				.filter(x -> (x.getAttributeCode().startsWith(attributePrefix))).collect(Collectors.toList());

		return foundEntitys;
	}

	/**
	 * findEntityAttributes This returns attributeEntitys if it exists in the
	 * baseEntity. Could be more efficient in retrival (ACC: test)
	 *
	 * @param attribute the attribute to find
	 * @return HEntityAttribute
	 */
	public Optional<HEntityAttribute> findEntityAttribute(final Attribute attribute) {
		final Optional<HEntityAttribute> foundEntityOpt = getBaseEntityAttributes().stream()
				.filter(x -> (x.getAttributeCode().equals(attribute.getCode()))).findFirst();

		return foundEntityOpt;
	}

	/**
	 * addAttribute This adds an attribute with default weight of 0.0 to the
	 * baseEntity. It auto creates the HEntityAttribute object. For efficiency we
	 * assume the attribute does not already exist
	 *
	 * @param ea the ea to add
	 * @return HEntityAttribute
	 * @throws BadDataException if the attribute could not be added
	 */
	public HEntityAttribute addAttribute(final HEntityAttribute ea) throws BadDataException {

		if (ea == null) {
			throw new BadDataException("missing Attribute");
		}

		return addAttribute(ea.getAttribute().toAttribute(), ea.getWeight(), ea.getValue());
	}

	/**
	 * addAttribute This adds an attribute and associated weight to the baseEntity.
	 * It auto creates the HEntityAttribute object. For efficiency we assume the
	 * attribute does not already exist
	 *
	 * @param attribute tha Attribute to add
	 * @throws BadDataException if attribute could not be added
	 * @return HEntityAttribute
	 */
	public HEntityAttribute addAttribute(final Attribute attribute) throws BadDataException {

		return addAttribute(attribute, 1.0);
	}

	/**
	 * addAttribute This adds an attribute and associated weight to the baseEntity.
	 * It auto creates the HEntityAttribute object. For efficiency we assume the
	 * attribute does not already exist
	 *
	 * @param attribute tha Attribute to add
	 * @param weight    the weight
	 * @throws BadDataException if attribute could not be added
	 * @return HEntityAttribute
	 */
	public HEntityAttribute addAttribute(final Attribute attribute, final Double weight) throws BadDataException {

		return addAttribute(attribute, weight, null);
	}

	/**
	 * addAttribute This adds an attribute and associated weight to the baseEntity.
	 * It auto creates the HEntityAttribute object. For efficiency we assume the
	 * attribute does not already exist
	 *
	 * @param attribute tha Attribute to add
	 * @param weight    the weight
	 * @param value     of type String, LocalDateTime, Long, Integer, Boolean
	 * @throws BadDataException if attribute could not be added
	 * @return HEntityAttribute
	 */
	public HEntityAttribute addAttribute(final Attribute attribute, final Double weight, final Object value)
			throws BadDataException {

		if (attribute == null)
			throw new BadDataException("missing Attribute");
		if (weight == null)
			throw new BadDataException("missing weight");

		final HEntityAttribute entityAttribute = new HEntityAttribute(this, attribute.toHAttribute(), weight, value);
		Optional<HEntityAttribute> existing = findEntityAttribute(attribute.getCode());
		if (existing.isPresent()) {
			if (value != null)
				existing.get().setValue(value);
			existing.get().setWeight(weight);
			// removeAttribute(existing.get().getAttributeCode());
		} else {
			this.getBaseEntityAttributes().add(entityAttribute);
		}
		return updateEntityAttributePk(entityAttribute, attribute);
	}

	private HEntityAttribute updateEntityAttributePk(HEntityAttribute entityAttribute, Attribute attribute) {
		entityAttribute.setBaseEntity(this);
		entityAttribute.setAttribute(attribute.toHAttribute());

		return entityAttribute;
	}

	/**
	 * addAttributeOmitCheck This adds an attribute and associated weight to the
	 * baseEntity. This method will NOT check and update any existing attributes.
	 * Use with Caution.
	 *
	 * @param attribute tha Attribute to add the omit check to
	 * @param weight    the weight of the omit check
	 * @param value     of type String, LocalDateTime, Long, Integer, Boolean
	 * @throws BadDataException if omit check could not be added
	 * @return HEntityAttribute
	 */
	public HEntityAttribute addAttributeOmitCheck(final Attribute attribute, final Double weight, final Object value)
			throws BadDataException {
		if (attribute == null)
			throw new BadDataException("missing Attribute");
		if (weight == null)
			throw new BadDataException("missing weight");

		final HEntityAttribute entityAttribute = new HEntityAttribute(this, attribute.toHAttribute(), weight, value);
		getBaseEntityAttributes().add(entityAttribute);

		return entityAttribute;
	}

	/**
	 * removeAttribute This removes an attribute and associated weight from the
	 * baseEntity. For efficiency we assume the attribute exists
	 *
	 * @param attributeCode the code of the Attribute to remove
	 * @return Boolean
	 */
	public Boolean removeAttribute(final String attributeCode) {
		Boolean removed = false;

		Iterator<HEntityAttribute> i = this.baseEntityAttributes.iterator();
		while (i.hasNext()) {
			HEntityAttribute ea = i.next();
			if (ea.getAttributeCode().equals(attributeCode)) {
				i.remove();
				removed = true;
				break;
			}
		}

		if (attributeMap != null) {
			attributeMap.remove(attributeCode);
		}

		return removed;
	}

	/**
	 * Merge a HBaseEntity.
	 *
	 * @param entity the entity to merge
	 * @return Set
	 */
	@Transient
	@XmlTransient
	@JsonIgnore
	@JsonbTransient
	public Set<HEntityAttribute> merge(final HBaseEntity entity) {
		final Set<HEntityAttribute> changes = new HashSet<HEntityAttribute>();

		// go through the attributes in the entity and check if already existing , if so
		// then check the
		// value and override, else add new attribute

		for (final HEntityAttribute ea : entity.getBaseEntityAttributes()) {
			final Attribute attribute = ea.getAttribute().toAttribute();
			if (this.containsEntityAttribute(attribute.getCode())) {
				// check for update value
				final Object oldValue = this.getValue(attribute);
				final Object newValue = this.getValue(ea);
				if (newValue != null) {
					if (!newValue.equals(oldValue)) {
						// override the old value // TODO allow versioning
						try {
							this.setValue(attribute, this.getValue(ea), ea.getValueDouble());
						} catch (BadDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			} else {
				// add this new entityAttribute
				try {
					addAttribute(ea);
					changes.add(ea);
				} catch (final BadDataException e) {
					// TODO - log error and continue
				}
			}
		}

		return changes;
	}

	/**
	 * @param <T>       The Type to return
	 * @param attribute
	 * @return T
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	private <T> T getValue(final Attribute attribute) {
		// TODO Dumb find for attribute. needs a hashMap

		for (final HEntityAttribute ea : this.getBaseEntityAttributes()) {
			if (ea.getAttribute().getCode().equalsIgnoreCase(attribute.getCode())) {
				return getValue(ea);
			}
		}
		return null;
	}

	/**
	 * @param <T> The Type to return
	 * @param ea  the ea to get
	 * @return T
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	private <T> T getValue(final HEntityAttribute ea) {
		return ea.getValue();

	}

	/**
	 * @param <T>           The Type to return
	 * @param attributeCode the attributeCode to get with
	 * @return Optional
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public <T> Optional<T> getValue(final String attributeCode) {

		Optional<HEntityAttribute> ea = this.findEntityAttribute(attributeCode);

		Optional<T> result = Optional.empty();
		if (ea.isPresent()) {
			if (ea.get() != null) {
				if (ea.get().getValue() != null) {
					result = Optional.of(ea.get().getValue());
				}
			}
		}
		return result;
	}

	/**
	 * @param <T>           the Type to return
	 * @param attributeCode the code of the attribute value to get
	 * @return Optional
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public <T> Optional<T> getLoopValue(final String attributeCode) {

		Optional<HEntityAttribute> ea = this.findEntityAttribute(attributeCode);

		Optional<T> result = Optional.empty();
		if (ea.isPresent()) {
			result = Optional.of(ea.get().getLoopValue());
		}
		return result;
	}

	/**
	 * @param attributeCode the code of the attribute value to get
	 * @return String
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public String getValueAsString(final String attributeCode) {

		Optional<HEntityAttribute> ea = this.findEntityAttribute(attributeCode);
		String result = null;
		if (ea.isPresent()) {
			if (ea.get() != null) {
				if (ea.get().getValue() != null) {
					result = ea.get().getAsString();
				}
			}
		}
		return result;
	}

	/**
	 * Get the value
	 *
	 * @param attributeCode the attribute code
	 * @param defaultValue  the default value
	 * @param <T>           The Type to return
	 * @return T
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public <T> T getValue(final String attributeCode, T defaultValue) {

		Optional<T> result = getValue(attributeCode);
		if (result.isPresent()) {
			if (!result.equals(Optional.empty())) {
				return result.get();
			}
		}
		return defaultValue;
	}

	/**
	 * Get the loop value
	 *
	 * @param attributeCode the attribute code
	 * @param defaultValue  the default value
	 * @param <T>           The Type to return
	 * @return T
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public <T> T getLoopValue(final String attributeCode, T defaultValue) {

		Optional<T> result = getLoopValue(attributeCode);
		if (result.isPresent()) {
			if (!result.equals(Optional.empty())) {
				return result.get();
			}
		}
		return defaultValue;
	}

	/**
	 * @param attributeCode the attribute code
	 * @return Boolean
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public Boolean is(final String attributeCode) {

		Optional<HEntityAttribute> ea = this.findEntityAttribute(attributeCode);
		Boolean result = false;

		if (ea.isPresent()) {
			result = ea.get().getValueBoolean();
			if (result == null) {
				return false;
			}
		}
		return result;

	}

	/**
	 * Set the value
	 *
	 * @param attribute the attribute
	 * @param value     the value to set
	 * @param weight    the weight
	 * @param <T>       The Type to return
	 * @return Optional
	 * @throws BadDataException if value cannot be set
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public <T> Optional<T> setValue(final Attribute attribute, T value, Double weight) throws BadDataException {

		Optional<HEntityAttribute> oldValue = this.findEntityAttribute(attribute.getCode());

		Optional<T> result = Optional.empty();
		if (oldValue.isPresent()) {
			if (oldValue.get().getLoopValue() != null) {
				result = Optional.of(oldValue.get().getLoopValue());
			}
			HEntityAttribute ea = oldValue.get();
			ea.setValue(value);
			ea.setWeight(weight);
		} else {
			this.addAttribute(attribute, weight, value);
		}
		return result;
	}

	/**
	 * Set the value
	 *
	 * @param attribute the attribute
	 * @param value     the value to set
	 * @param <T>       The Type to return
	 * @return Optional
	 * @throws BadDataException if value cannot be set
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public <T> Optional<T> setValue(final Attribute attribute, T value) throws BadDataException {
		return setValue(attribute, value, 0.0);
	}

	/**
	 * Set the value
	 *
	 * @param attributeCode the attribute code
	 * @param value         the value to set
	 * @param <T>           The Type to return
	 * @return Optional
	 * @throws BadDataException if value cannot be set
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public <T> Optional<T> setValue(final String attributeCode, T value) throws BadDataException {
		return setValue(attributeCode, value, 0.0);
	}

	/**
	 * Set the value
	 *
	 * @param attributeCode the attribute code
	 * @param value         the value to set
	 * @param weight        the weight
	 * @param <T>           The Type to return
	 * @return Optional
	 * @throws BadDataException if value cannot be set
	 */
	@JsonIgnore
	@JsonbTransient
	@Transient
	@XmlTransient
	public <T> Optional<T> setValue(final String attributeCode, T value, Double weight) throws BadDataException {
		Optional<HEntityAttribute> oldValue = this.findEntityAttribute(attributeCode);

		Optional<T> result = Optional.empty();
		if (oldValue.isPresent()) {
			if (oldValue.get().getLoopValue() != null) {
				result = Optional.of(oldValue.get().getLoopValue());
			}
			HEntityAttribute ea = oldValue.get();
			ea.setValue(value);
			ea.setWeight(weight);
		}
		return result;
	}

	/**
	 * Force private
	 *
	 * @param attribute the attribute to force
	 * @param state     should force
	 */
	@Transient
	public void forcePrivate(final Attribute attribute, final Boolean state) {
		forcePrivate(attribute.getCode(), state);
	}

	/**
	 * Force inferred
	 *
	 * @param attribute the attribute to force
	 * @param state     should force
	 */
	@Transient
	public void forceInferred(final Attribute attribute, final Boolean state) {
		forceInferred(attribute.getCode(), state);
	}

	/**
	 * Force readonly
	 *
	 * @param attribute the attribute to force
	 * @param state     should force
	 */
	@Transient
	public void forceReadonly(final Attribute attribute, final Boolean state) {
		forceReadonly(attribute.getCode(), state);
	}

	/**
	 * Force private
	 *
	 * @param attributeCode the code of the attribute to force
	 * @param state         should force
	 */
	@Transient
	public void forcePrivate(String attributeCode, Boolean state) {
		Optional<HEntityAttribute> optEa = this.findEntityAttribute(attributeCode);
		if (optEa.isPresent()) {
			HEntityAttribute ea = optEa.get();
			ea.setPrivacyFlag(state);
		}
	}

	/**
	 * Force inferred
	 *
	 * @param attributeCode the code of the attribute to force
	 * @param state         should force
	 */
	@Transient
	public void forceInferred(final String attributeCode, final Boolean state) {
		Optional<HEntityAttribute> optEa = this.findEntityAttribute(attributeCode);
		if (optEa.isPresent()) {
			HEntityAttribute ea = optEa.get();
			ea.setInferred(state);
		}
	}

	/**
	 * Force readonly
	 *
	 * @param attributeCode the code of the attribute to force
	 * @param state         should force
	 */
	@Transient
	public void forceReadonly(final String attributeCode, final Boolean state) {
		Optional<HEntityAttribute> optEa = this.findEntityAttribute(attributeCode);
		if (optEa.isPresent()) {
			HEntityAttribute ea = optEa.get();
			ea.setReadonly(state);
		}
	}

	/**
	 * @return the fromCache
	 */
	public Boolean getFromCache() {
		return fromCache;
	}

	/**
	 * @return Boolean
	 */
	public Boolean isFromCache() {
		return getFromCache();
	}

	/**
	 * @param fromCache the fromCache to set
	 */
	public void setFromCache(Boolean fromCache) {
		this.fromCache = fromCache;
	}

	/**
	 * @return String[]
	 */
	@Transient
	@JsonbTransient
	public String[] getPushCodes() {
		return getPushCodes(new String[0]);
	}

	/**
	 * @param initialCodes the initialCodes to set
	 * @return String[]
	 */
	@Transient
	@JsonbTransient
	public String[] getPushCodes(String... initialCodes) {
		// go through all the links
		Set<String> codes = new HashSet<String>();
		codes.addAll(new HashSet<>(Arrays.asList(initialCodes)));
		if ((this.baseEntityAttributes != null) && (!this.baseEntityAttributes.isEmpty())) {
			for (HEntityAttribute ea : this.baseEntityAttributes) {
				// if (ea.getAttributeCode().startsWith("LNK_")) {
				String value = ea.getValueString();
				if (value != null) {
					if (value.startsWith("[") && !value.equals("[]")) {
						value = value.substring(2, value.length() - 2);
					}
					if (value.startsWith("PER") || (value.startsWith("CPY"))) {
						codes.add(value);
					}
				}
			}
			// }
			if (this.getCode().startsWith("PER") || (this.getCode().startsWith("CPY"))) {
				codes.add(this.getCode());
			}
		}

		return codes.toArray(new String[0]);
	}

	/**
	 * @param prefix the prefix to set
	 * @return Optional&lt;HEntityAttribute&gt;
	 */
	@Transient
	@JsonbTransient
	public Optional<HEntityAttribute> getHighestEA(final String prefix) {
		// go through all the EA
		Optional<HEntityAttribute> highest = Optional.empty();
		Double weight = -1000.0;

		if ((this.baseEntityAttributes != null) && (!this.baseEntityAttributes.isEmpty())) {
			for (HEntityAttribute ea : this.baseEntityAttributes) {
				if (ea.getAttributeCode().startsWith(prefix)) {
					if (ea.getWeight() > weight) {
						highest = Optional.of(ea);
						weight = ea.getWeight();
					}

				}
			}

		}

		return highest;
	}

	/**
	 * @param fastMode the fastMode to set
	 */
	@Transient
	@JsonbTransient
	public void setFastAttributes(Boolean fastMode) {
		if (fastMode) {
			attributeMap = new ConcurrentHashMap<String, HEntityAttribute>();
			// Grab all the entityAttributes and create a fast HashMap lookup
			for (HEntityAttribute ea : this.baseEntityAttributes) {
				attributeMap.put(ea.getAttributeCode(), ea);
			}
		} else {
			attributeMap = null;
		}

	}

	@JsonbTransient
	public boolean isPerson() {
		return getCode().startsWith(Prefix.PER_);
	}

	@Override
	public CoreEntitySerializable toSerializableCoreEntity() {
		life.genny.qwandaq.serialization.baseentity.BaseEntity baseEntitySerializable = new life.genny.qwandaq.serialization.baseentity.BaseEntity();
		baseEntitySerializable.setCode(getCode());
		baseEntitySerializable.setCreated(getCreated());
		baseEntitySerializable.setName(getName());
		baseEntitySerializable.setRealm(getRealm());
		baseEntitySerializable.setUpdated(getUpdated());
		return baseEntitySerializable;
	}

	@Override
	public int hashCode() {
		return (this.getRealm()+this.getCode()).hashCode();
	}

	@Override
	public boolean equals(Object otherObject) {
		return this.getRealm().equals(((BaseEntity) otherObject).getRealm())
				&& this.getCode().equals(((BaseEntity) otherObject).getCode());
	}

	public BaseEntity toBaseEntity() {
		BaseEntity baseEntity = new BaseEntity();
		baseEntity.setCode(getCode());
		baseEntity.setCreated(getCreated());
		// baseEntity.setDtype();
		baseEntity.setId(getId());
		baseEntity.setName(getName());
		baseEntity.setRealm(getRealm());
		baseEntity.setStatus(getStatus());
		baseEntity.setUpdated(getUpdated());
		return baseEntity;
	}
}
