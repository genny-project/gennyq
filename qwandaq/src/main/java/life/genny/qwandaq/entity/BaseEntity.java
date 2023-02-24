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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.CodedEntity;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.converter.CapabilityConverter;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.intf.ICapabilityHiddenFilterable;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.jboss.logging.Logger;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Transient;
import java.util.*;
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
@RegisterForReflection
public class BaseEntity extends CodedEntity implements CoreEntityPersistable, BaseEntityIntf, ICapabilityHiddenFilterable {

	@Transient
	private static final long serialVersionUID = 1L;

	public static final String PRI_NAME = "PRI_NAME";
	public static final String PRI_IMAGE_URL = "PRI_IMAGE_URL";
	public static final String PRI_PHONE = "PRI_PHONE";
	public static final String PRI_ADDRESS_FULL = "PRI_ADDRESS_FULL";
	public static final String PRI_EMAIL = "PRI_EMAIL";

	private Map<String, EntityAttribute> baseEntityAttributes = new HashMap<>(0);

	@Transient
	private Boolean fromCache = false;

	private Set<Capability> capabilityRequirements;

	/**
	 * Constructor.
	 */
	public BaseEntity() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param code the unique code of the core entity
	 */
	@ProtoFactory
	public BaseEntity(final String code) {
		super(code, code);
	}

	/**
	 * Constructor.
	 * 
	 * @param code the unique code of the core entity
	 * @param name the summary name of the core entity
	 */
	@ProtoFactory
	public BaseEntity(final String code, final String name) {
		super(code, name);
	}

	@Override
    public Set<Capability> getCapabilityRequirements() {
		return this.capabilityRequirements;
	}

	@Override
	public void setCapabilityRequirements(Set<Capability> requirements) {
		this.capabilityRequirements = requirements;
	}

	public Collection<EntityAttribute> getBaseEntityAttributes() {
		return baseEntityAttributes.values();
	}

	@JsonbTransient
	public Map<String, EntityAttribute> getBaseEntityAttributesMap() {
		return baseEntityAttributes;
	}

	@JsonbTransient
	public void setBaseEntityAttributes(final Map<String, EntityAttribute> baseEntityAttributesMap) {
		this.baseEntityAttributes = baseEntityAttributesMap;
	}

	@JsonbTransient
	public void setBaseEntityAttributes(final Set<EntityAttribute> baseEntityAttributes) {
		baseEntityAttributes.forEach(bea -> this.baseEntityAttributes.put(bea.getAttributeCode(), bea));
	}

	@JsonbTransient
	public void setBaseEntityAttributes(final Collection<EntityAttribute> baseEntityAttributes) {
		baseEntityAttributes.forEach(bea -> this.baseEntityAttributes.put(bea.getAttributeCode(), bea));
	}

	/**
	 * containsEntityAttribute This checks if an attribute exists in the baseEntity.
	 * 
	 * @param attributeCode the attributeCode to check
	 * @return boolean
	 */
	public boolean containsEntityAttribute(final String attributeCode) {
		return getBaseEntityAttributesMap().containsKey(attributeCode);
	}

	/**
	 * findEntityAttribute This returns an attributeEntity if it exists in the
	 * baseEntity.
	 * 
	 * @param attributeCode the attributeCode to find with
	 * @return Optional
	 * 
	 */
	@Deprecated
	public Optional<EntityAttribute> findEntityAttribute(final String attributeCode) {
		return Optional.ofNullable(this.baseEntityAttributes.get(attributeCode));
	}

	/**
	 * findEntityAttribute This returns an attributeEntity if it exists in the
	 * baseEntity. Could be more efficient in retrival (ACC: test)
	 * 
	 * @param attributePrefix the attributePrefix to find with
	 * @return EntityAttribute
	 */
	public List<EntityAttribute> findPrefixEntityAttributes(final String attributePrefix) {
		return getBaseEntityAttributes().stream()
				.filter(x -> (x.getAttributeCode().startsWith(attributePrefix)))
				.collect(Collectors.toList());
	}

	/**
	 * findEntityAttributes This returns attributeEntitys if it exists in the
	 * baseEntity. Could be more efficient in retrival (ACC: test)
	 * 
	 * @param attribute the attribute to find
	 * @return EntityAttribute
	 */
	public Optional<EntityAttribute> findEntityAttribute(final Attribute attribute) {
		return Optional.ofNullable(getBaseEntityAttributesMap().get(attribute.getCode()));
	}

	/**
	 * addAttribute This adds an attribute with default weight of 0.0 to the
	 * baseEntity. It auto creates the EntityAttribute object. For efficiency we
	 * assume the attribute does not already exist
	 * 
	 * @param ea the ea to add
	 * @return EntityAttribute
	 * @throws BadDataException if the attribute could not be added
	 */
	public EntityAttribute addAttribute(final EntityAttribute ea) throws BadDataException {
		if (ea == null) {
			throw new BadDataException("missing Attribute");
		}
		return addAttribute(ea.getAttribute(), ea.getWeight(), ea.getValue());
	}

	/**
	 * addAttribute This adds an attribute and associated weight to the baseEntity.
	 * It auto creates the EntityAttribute object. For efficiency we assume the
	 * attribute does not already exist
	 * 
	 * @param attribute tha Attribute to add
	 * @throws BadDataException if attribute could not be added
	 * @return EntityAttribute
	 */
	public EntityAttribute addAttribute(final Attribute attribute) throws BadDataException {
		return addAttribute(attribute, 1.0);
	}

	/**
	 * addAttribute This adds an attribute and associated weight to the baseEntity.
	 * It auto creates the EntityAttribute object. For efficiency we assume the
	 * attribute does not already exist
	 * 
	 * @param attribute tha Attribute to add
	 * @param weight    the weight
	 * @throws BadDataException if attribute could not be added
	 * @return EntityAttribute
	 */
	public EntityAttribute addAttribute(final Attribute attribute, final Double weight) throws BadDataException {
		return addAttribute(attribute, weight, null);
	}

	/**
	 * addAttribute This adds an attribute and associated weight to the baseEntity.
	 * It auto creates the EntityAttribute object. For efficiency we assume the
	 * attribute does not already exist
	 * 
	 * @param attribute tha Attribute to add
	 * @param weight    the weight
	 * @param value     of type String, LocalDateTime, Long, Integer, Boolean
	 * @throws BadDataException if attribute could not be added
	 * @return EntityAttribute
	 */
	public EntityAttribute addAttribute(final Attribute attribute, final Double weight, final Object value)
			throws BadDataException {
		if (attribute == null)
			throw new BadDataException("missing Attribute");
		if (weight == null)
			throw new BadDataException("missing weight");
		EntityAttribute entityAttribute = this.baseEntityAttributes.get(attribute.getCode());
		if (entityAttribute == null) {
			entityAttribute = new EntityAttribute(this, attribute, weight, value);
		}
		if (value != null) {
			entityAttribute.setAttribute(attribute);
			entityAttribute.setValue(value);
		}
		entityAttribute.setWeight(weight);
		this.baseEntityAttributes.put(attribute.getCode(), entityAttribute);
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
	 * @return EntityAttribute
	 */
	public EntityAttribute addAttributeOmitCheck(final Attribute attribute, final Double weight, final Object value)
			throws BadDataException {
		if (attribute == null)
			throw new BadDataException("missing Attribute");
		if (weight == null)
			throw new BadDataException("missing weight");

		final EntityAttribute entityAttribute = new EntityAttribute(this, attribute, weight, value);
		entityAttribute.setRealm(getRealm());
		entityAttribute.setBaseEntityCode(getCode());
		entityAttribute.setBaseEntityId(getId());
		entityAttribute.setAttribute(attribute);
		entityAttribute.setAttributeId(attribute.getId());
		this.baseEntityAttributes.put(attribute.getCode(), entityAttribute);

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
		return this.getBaseEntityAttributesMap().remove(attributeCode) != null ? true : false;
	}

	/**
	 * Add or update an EntityAttribute for this base entity
	 * @param attribute - attribute to attach to base entity
	 * @param weight - weight of the entity attribute
	 * @param inferred - whether or not the value of this EntityAttribute is inferred
	 * @param value - the value of this EntityAttribute
	 * @return - the new (or existing) EntityAttribute
	 * 
	 * @see {@link EntityAttribute}
	 */
	public EntityAttribute addEntityAttribute(Attribute attribute, double weight, boolean inferred, Object value) {

		Optional<EntityAttribute> eaOpt = findEntityAttribute(attribute);
		EntityAttribute ea;
		if (eaOpt.isPresent()) {
			ea = eaOpt.get();
			// modify
			ea.setAttribute(attribute);
			ea.setValue(value);
			ea.setInferred(inferred);
			ea.setWeight(weight);
			ea.setRealm(getRealm());
			ea.setBaseEntityCode(getCode());
			ea.setBaseEntityId(getId());
		} else {
			ea = new EntityAttribute(this, attribute, weight, value);
			ea.setRealm(getRealm());
			ea.setBaseEntityCode(getCode());
			ea.setAttributeCode(attribute.getCode());
			ea.setInferred(inferred);
			this.baseEntityAttributes.put(attribute.getCode(), ea);
		}

		return ea;
	}

	/**
	 * Merge a BaseEntity.
	 *
	 * @param entity the entity to merge
	 * @return Set
	 */
	@Transient
	@JsonIgnore
	@JsonbTransient
	public Set<EntityAttribute> merge(final BaseEntity entity) {
		final Set<EntityAttribute> changes = new HashSet<>();

		// go through the attributes in the entity and check if already existing , if so
		// then check the
		// value and override, else add new attribute

		for (final EntityAttribute ea : entity.getBaseEntityAttributes()) {
			final Attribute attribute = ea.getAttribute();
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
	private <T> T getValue(final Attribute attribute) {
		EntityAttribute entityAttribute = this.baseEntityAttributes.get(attribute.getCode());
		if (entityAttribute != null) {
			return getValue(entityAttribute);
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
	private <T> T getValue(final EntityAttribute ea) {
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
	public <T> Optional<T> getValue(final String attributeCode) {

		Optional<EntityAttribute> ea = this.findEntityAttribute(attributeCode);

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
	public <T> Optional<T> getLoopValue(final String attributeCode) {

		Optional<EntityAttribute> ea = this.findEntityAttribute(attributeCode);

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
	public String getValueAsString(final String attributeCode) {

		Optional<EntityAttribute> ea = this.findEntityAttribute(attributeCode);
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
	public Boolean is(final String attributeCode) {

		Optional<EntityAttribute> ea = this.findEntityAttribute(attributeCode);
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
	public <T> Optional<T> setValue(final Attribute attribute, T value, Double weight) throws BadDataException {

		Optional<EntityAttribute> oldValue = this.findEntityAttribute(attribute.getCode());

		Optional<T> result = Optional.empty();
		if (oldValue.isPresent()) {
			if (oldValue.get().getLoopValue() != null) {
				result = Optional.of(oldValue.get().getLoopValue());
			}
			EntityAttribute ea = oldValue.get();
			ea.setAttribute(attribute);
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
	public <T> Optional<T> setValue(final String attributeCode, T value, Double weight) throws BadDataException {
		Optional<EntityAttribute> oldValue = this.findEntityAttribute(attributeCode);

		Optional<T> result = Optional.empty();
		if (oldValue.isPresent()) {
			if (oldValue.get().getLoopValue() != null) {
				result = Optional.of(oldValue.get().getLoopValue());
			}
			EntityAttribute ea = oldValue.get();
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
		Set<String> codes = new HashSet<>();
		codes.addAll(new HashSet<>(Arrays.asList(initialCodes)));
		if ((this.baseEntityAttributes != null) && (!this.baseEntityAttributes.isEmpty())) {
			for (EntityAttribute ea : getBaseEntityAttributes()) {
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
	 * Force private
	 *
	 * @param attributeCode the code of the attribute to force
	 * @param state         should force
	 */
	@Transient
	public void forcePrivate(String attributeCode, Boolean state) {
		Optional<EntityAttribute> optEa = this.findEntityAttribute(attributeCode);
		if (optEa.isPresent()) {
			EntityAttribute ea = optEa.get();
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
		Optional<EntityAttribute> optEa = this.findEntityAttribute(attributeCode);
		if (optEa.isPresent()) {
			EntityAttribute ea = optEa.get();
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
		Optional<EntityAttribute> optEa = this.findEntityAttribute(attributeCode);
		if (optEa.isPresent()) {
			EntityAttribute ea = optEa.get();
			ea.setReadonly(state);
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
		// baseEntitySerializable.setDtype();
		baseEntitySerializable.setName(getName());
		baseEntitySerializable.setRealm(getRealm());
		baseEntitySerializable.setStatus(getStatus().ordinal());
		baseEntitySerializable.setUpdated(getUpdated());
		baseEntitySerializable.setCapreqs(CapabilityConverter.convertToDBColumn(getCapabilityRequirements()));
		return baseEntitySerializable;
	}

	/**
	 * @param prefix the prefix to set
	 * @return Optional&lt;EntityAttribute&gt;
	 */
	@Transient
	@JsonbTransient
	public Optional<EntityAttribute> getHighestEA(final String prefix) {
		// go through all the EA
		Optional<EntityAttribute> highest = Optional.empty();
		Double weight = -1000.0;

		if ((this.baseEntityAttributes != null) && (!this.baseEntityAttributes.isEmpty())) {
			for (EntityAttribute ea : getBaseEntityAttributes()) {
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

	@Override
	public int hashCode() {
		return (this.getRealm()+this.getCode()).hashCode();
	}

	@Override
	public boolean equals(Object otherObject) {
		return this.getRealm().equals(((BaseEntity) otherObject).getRealm())
				&& this.getCode().equals(((BaseEntity) otherObject).getCode());
	}

	public HBaseEntity toHBaseEntity() {
		HBaseEntity hBaseEntity = new HBaseEntity();
		hBaseEntity.setCode(getCode());
		hBaseEntity.setCreated(getCreated());
		// hBaseEntity.setDtype();
		hBaseEntity.setName(getName());
		hBaseEntity.setRealm(getRealm());
		hBaseEntity.setStatus(getStatus());
		hBaseEntity.setUpdated(getUpdated());
		return hBaseEntity;
	}

	public BaseEntity clone(boolean includeAttributes) {
		BaseEntity clone = new BaseEntity();
		clone.setCode(getCode());
		clone.setCreated(getCreated());
		clone.setName(getName());
		clone.setRealm(getRealm());
		clone.setStatus(getStatus());
		clone.setUpdated(getUpdated());
		clone.setCapabilityRequirements(getCapabilityRequirements());
		if(includeAttributes) {
			Map<String, EntityAttribute> baseEntityAttributesMap = getBaseEntityAttributesMap();
			Map<String, EntityAttribute> clonedBaseEntityAttributesMap = new HashMap<>(baseEntityAttributesMap.size());
			clone.setBaseEntityAttributes(clonedBaseEntityAttributesMap);
			for(Map.Entry<String, EntityAttribute> entry : baseEntityAttributesMap.entrySet()) {
				clonedBaseEntityAttributesMap.put(entry.getKey(), entry.getValue().clone());
			}
		}
		return clone;
	}

	/**
	 * Copy across all metadata about this base entity to another base entity of variable type
	 * @param other
	 */
	public void decorate(BaseEntity other) {
		other.setCapabilityRequirements(getCapabilityRequirements());
		other.setRealm(getRealm());
		other.setBaseEntityAttributes(getBaseEntityAttributes());
	}
}
