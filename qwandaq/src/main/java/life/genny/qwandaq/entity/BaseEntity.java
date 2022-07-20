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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.AnswerLink;
import life.genny.qwandaq.CodedEntity;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.exception.ItemNotFoundException;

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

@Table(name = "baseentity", indexes = { @Index(columnList = "code", name = "code_idx"),
	@Index(columnList = "realm", name = "code_idx") }, uniqueConstraints = @UniqueConstraint(columnNames = { 
	"code",
	"realm" 
	}
))
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
public class BaseEntity extends CodedEntity implements BaseEntityIntf {

	@Transient
	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(BaseEntity.class);

	private static final String DEFAULT_CODE_PREFIX = "BAS_";

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "pk.baseEntity", cascade=CascadeType.ALL)
	@Filters({
			@org.hibernate.annotations.Filter(name = "filterAttribute", condition = "attributeCode in (:attributeCodes)"),
			@org.hibernate.annotations.Filter(name = "filterAttribute2", condition = "attributeCode =:attributeCode")
	})
	private Set<EntityAttribute> baseEntityAttributes = new HashSet<EntityAttribute>(0);

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "pk.source")
	private Set<EntityEntity> links = new LinkedHashSet<>();

	@Transient
	@JsonbTransient
	private Set<EntityQuestion> questions = new HashSet<EntityQuestion>(0);

	private transient Set<AnswerLink> answers = new HashSet<AnswerLink>(0);

	@JsonbTransient
	@Transient
	private transient Map<String, EntityAttribute> attributeMap = null;

	/**
	 * Constructor.
	 */
	public BaseEntity() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param aName the summary name of the core entity
	 */
	public BaseEntity(final String aName) {
		super(getDefaultCodePrefix() + UUID.randomUUID().toString(), aName);

	}

	/**
	 * Constructor.
	 * 
	 * @param aCode the unique code of the core entity
	 * @param aName the summary name of the core entity
	 */
	@ProtoFactory
	public BaseEntity(final String aCode, final String aName) {
		super(aCode, aName);

	}

	/**
	 * @return Set The Answers.
	 */
	public Set<AnswerLink> getAnswers() {
		return answers;
	}

	/**
	 * @param answers the answers to set
	 */
	public void setAnswers(final Set<AnswerLink> answers) {
		this.answers = answers;
	}

	/**
	 * @param answers the answers to set
	 */
	public void setAnswers(final List<AnswerLink> answers) {
		this.answers.addAll(answers);
	}

	/**
	 * @return the baseEntityAttributes
	 */
	public Set<EntityAttribute> getBaseEntityAttributes() {
		return baseEntityAttributes;
	}

	/**
	 * @param baseEntityAttributes the baseEntityAttributes to set
	 */
	public void setBaseEntityAttributes(final Set<EntityAttribute> baseEntityAttributes) {
		this.baseEntityAttributes = baseEntityAttributes;
	}

	/**
	 * @param baseEntityAttributes the baseEntityAttributes to set
	 */
	public void setBaseEntityAttributes(final List<EntityAttribute> baseEntityAttributes) {
		this.baseEntityAttributes.addAll(baseEntityAttributes);
	}

	/**
	 * @return the links
	 */
	public Set<EntityEntity> getLinks() {
		return links;
	}

	/**
	 * Sets the Links of the BaseEntity with another BaseEntity
	 * 
	 * @param links the links to set
	 */
	public void setLinks(final Set<EntityEntity> links) {
		this.links = links;
	}

	/**
	 * @param links the links to set
	 */
	public void setLinks(final List<EntityEntity> links) {
		this.links.addAll(links);
	}

	/**
	 * @return the questions
	 */
	public Set<EntityQuestion> getQuestions() {
		return this.questions;
	}

	/**
	 * Sets the Questions of the BaseEntity with another BaseEntity
	 * 
	 * @param questions the questions to set
	 */
	public void setQuestions(final Set<EntityQuestion> questions) {
		this.questions = questions;
	}

	/**
	 * @param questions the questions to set
	 */
	@JsonbTransient
	public void setQuestions(final List<EntityQuestion> questions) {
		this.questions.addAll(questions);
	}

    public Map<String, EntityAttribute> getAttributeMap() {
        return attributeMap;
    }

    public void setAttributeMap(Map<String, EntityAttribute> attributeMap) {
        this.attributeMap = attributeMap;
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
	 * containsLink This checks if an attribute link code is linked to the
	 * baseEntity.
	 * 
	 * @param linkAttributeCode the linkAttributeCode to check
	 * @return boolean
	 */
	public boolean containsLink(final String linkAttributeCode) {
		boolean ret = false;

		// Check if this code exists in the baseEntityAttributes
		if (getLinks().parallelStream().anyMatch(ti -> ti.getPk().getAttribute().getCode().equals(linkAttributeCode))) {
			ret = true;
		}
		return ret;
	}

	/**
	 * containsTarget This checks if another baseEntity is linked to the baseEntity.
	 * 
	 * @param targetCode        the targetCode to check
	 * @param linkAttributeCode the linkAttributeCode to check
	 * @return boolean
	 */
	public boolean containsTarget(final String targetCode, final String linkAttributeCode) {
		boolean ret = false;

		// Check if this code exists in the baseEntityAttributes
		if (getLinks().parallelStream().anyMatch(ti -> (ti.getLink().getAttributeCode().equals(linkAttributeCode)
				&& (ti.getLink().getTargetCode().equals(targetCode))))) {
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
	public Optional<EntityAttribute> findEntityAttribute(final String attributeCode) {

		if (attributeMap != null) {
			return Optional.ofNullable(attributeMap.get(attributeCode));
		}
		Optional<EntityAttribute> foundEntity = Optional.empty();

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
	 * @return EntityAttribute
	 */
	public List<EntityAttribute> findPrefixEntityAttributes(final String attributePrefix) {
		List<EntityAttribute> foundEntitys = getBaseEntityAttributes().stream()
				.filter(x -> (x.getAttributeCode().startsWith(attributePrefix))).collect(Collectors.toList());

		return foundEntitys;
	}

	/**
	 * findEntityAttributes This returns attributeEntitys if it exists in the
	 * baseEntity. Could be more efficient in retrival (ACC: test)
	 * 
	 * @param attribute the attribute to find
	 * @return EntityAttribute
	 */
	public EntityAttribute findEntityAttribute(final Attribute attribute) {
		final EntityAttribute foundEntity = getBaseEntityAttributes().stream()
				.filter(x -> (x.getAttributeCode().equals(attribute.getCode()))).findFirst().get();

		return foundEntity;
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

		final EntityAttribute entityAttribute = new EntityAttribute(this, attribute, weight, value);
		Optional<EntityAttribute> existing = findEntityAttribute(attribute.getCode());
		if (existing.isPresent()) {
			existing.get().setValue(value);
			existing.get().setWeight(weight);
		} else {
			this.getBaseEntityAttributes().add(entityAttribute);
		}
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

		Iterator<EntityAttribute> i = this.baseEntityAttributes.iterator();
		while (i.hasNext()) {
			EntityAttribute ea = i.next();
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
	 * addTarget This links this baseEntity to a target BaseEntity and associated
	 * weight,value to the baseEntity. It auto creates the EntityEntity object and
	 * sets itself to be the source. For efficiency we assume the link does not
	 * already exist
	 * 
	 * @param target        the target to add
	 * @param linkAttribute the attribute link
	 * @param weight        the weight of the target
	 * @return EntityEntity
	 * @throws BadDataException if the target could not be added
	 */
	public EntityEntity addTarget(final BaseEntity target, final Attribute linkAttribute, final Double weight)
			throws BadDataException {
		return addTarget(target, linkAttribute, weight, null);
	}

	/**
	 * addTarget This links this baseEntity to a target BaseEntity and associated
	 * weight,value to the baseEntity. It auto creates the EntityEntity object and
	 * sets itself to be the source. For efficiency we assume the link does not
	 * already exist
	 * 
	 * @param target        the target to add
	 * @param linkAttribute the attribute link
	 * @param weight        the weight of the target
	 * @param value         the value of the target
	 * @return EntityEntity
	 * @throws BadDataException if the target could not be added
	 */
	public EntityEntity addTarget(final BaseEntity target, final Attribute linkAttribute, final Double weight,
			final Object value) throws BadDataException {
		if (target == null)
			throw new BadDataException("missing Target Entity");
		if (linkAttribute == null)
			throw new BadDataException("missing Link Attribute");
		if (weight == null)
			throw new BadDataException("missing weight");

		final EntityEntity entityEntity = new EntityEntity(this, target, linkAttribute, value, weight);
		getLinks().add(entityEntity);
		return entityEntity;
	}

	/**
	 * addAnswer This links this baseEntity to a target BaseEntity and associated
	 * Answer. It auto creates the AnswerLink object and sets itself to be the
	 * source and assumes itself to be the target. For efficiency we assume the link
	 * does not already exist and weight = 0
	 * 
	 * @param answer the answer to add
	 * @return AnswerLink
	 * @throws BadDataException if answer could not be added
	 */
	public AnswerLink addAnswer(final Answer answer) throws BadDataException {
		return addAnswer(this, answer, 0.0);
	}

	/**
	 * addAnswer This links this baseEntity to a target BaseEntity and associated
	 * Answer. It auto creates the AnswerLink object and sets itself to be the
	 * source and assumes itself to be the target. For efficiency we assume the link
	 * does not already exist
	 * 
	 * @param answer the answer to add
	 * @param weight the weight of the answer
	 * @return AnswerLink
	 * @throws BadDataException if answer could not be added
	 */
	public AnswerLink addAnswer(final Answer answer, final Double weight) throws BadDataException {
		return addAnswer(this, answer, weight);
	}

	/**
	 * addAnswer This links this baseEntity to a target BaseEntity and associated
	 * Answer. It auto creates the AnswerLink object and sets itself to be the
	 * source. For efficiency we assume the link does not already exist
	 * 
	 * @param source the source entity
	 * @param answer the answer to add
	 * @param weight the weight of the answer
	 * @return AnswerLink
	 * @throws BadDataException if answer could not be added
	 */
	public AnswerLink addAnswer(final BaseEntity source, final Answer answer, final Double weight)
			throws BadDataException {
		if (source == null)
			throw new BadDataException("missing Target Entity");
		if (answer == null)
			throw new BadDataException("missing Answer");
		if (weight == null)
			throw new BadDataException("missing weight");

		final AnswerLink answerLink = new AnswerLink(source, this, answer, weight);
		if (answer.getAskId() != null) {
			answerLink.setAskId(answer.getAskId());
		}

		// Update the EntityAttribute
		Optional<EntityAttribute> ea = findEntityAttribute(answer.getAttributeCode());
		if (ea.isPresent()) {
			// modify
			ea.get().setValue(answerLink.getValue());
			ea.get().setInferred(answer.getInferred());
			ea.get().setWeight(answer.getWeight());
			ea.get().setBaseEntity(this);
		} else {
			EntityAttribute newEA = new EntityAttribute(this, answerLink.getAttribute(), weight, answerLink.getValue());
			newEA.setInferred(answerLink.getInferred());
			this.baseEntityAttributes.add(newEA);
		}

		return answerLink;
	}

	/**
	 * Merge a BaseEntity.
	 *
	 * @param entity the entity to merge
	 * @return Set
	 */
	@Transient
	@JsonbTransient
	public Set<EntityAttribute> merge(final BaseEntity entity) {
		final Set<EntityAttribute> changes = new HashSet<EntityAttribute>();

		for (final EntityAttribute ea : entity.getBaseEntityAttributes()) {
			final Attribute attribute = ea.getAttribute();
			if (this.containsEntityAttribute(attribute.getCode())) {
				// check for update value
				final Object oldValue = this.getValue(attribute);
				final Object newValue = this.getValue(ea);
				if (newValue != null) {
					if (!newValue.equals(oldValue)) {
						this.setValue(attribute, this.getValue(ea), ea.getValueDouble());
					}
				}
			} else {
				addAttribute(ea);
				changes.add(ea);
			}
		}

		return changes;
	}

	/**
	 * @param <T>       The Type to return
	 * @param attribute
	 * @return T
	 */
	@JsonbTransient
	@Transient
	private <T> T getValue(final Attribute attribute) {
		// TODO Dumb find for attribute. needs a hashMap

		for (final EntityAttribute ea : this.getBaseEntityAttributes()) {
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
	@JsonbTransient
	@Transient
	public <T> Optional<T> getValue(final String attributeCode) {

		Optional<EntityAttribute> ea = this.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return Optional.ofNullable(ea.get().getValue());
		}

		return Optional.empty();
	}

	/**
	 * @param attributeCode the code of the attribute value to get
	 * @return String
	 */
	@JsonbTransient
	@Transient
	public String getValueAsString(final String attributeCode) {

		Optional<EntityAttribute> ea = this.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getValueAsString();
		}
		return null;
	}

	/**
	 * Get the value
	 *
	 * @param attributeCode the attribute code
	 * @param defaultValue  the default value
	 * @param <T>           The Type to return
	 * @return T
	 */
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
	 * @param attributeCode the attribute code
	 * @return Boolean
	 */
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
	 * @param <T>       The Type to return
	 * @return Optional
	 */
	@JsonbTransient
	@Transient
	public <T> Optional<T> setValue(final Attribute attribute, T value) {
		return setValue(attribute, value, 0.0);
	}

	/**
	 * Set the value
	 *
	 * @param attributeCode the attribute code
	 * @param value         the value to set
	 * @param <T>           The Type to return
	 * @return Optional
	 */
	@JsonbTransient
	@Transient
	public <T> Optional<T> setValue(final String attributeCode, T value) {
		return setValue(attributeCode, value, 0.0);
	}

	/**
	 * Set the value
	 *
	 * @param attribute the attribute
	 * @param value     the value to set
	 * @param weight    the weight
	 * @param <T>       The Type to return
	 * @return Optional
	 */
	@JsonbTransient
	@Transient
	public <T> Optional<T> setValue(final Attribute attribute, T value, Double weight) {
		return setValue(attribute.getCode(), value, weight);
	}

	/**
	 * Set the value
	 *
	 * @param attributeCode the attribute code
	 * @param value         the value to set
	 * @param weight        the weight
	 * @param <T>           The Type to return
	 * @return Optional
	 */
	@JsonbTransient
	@Transient
	public <T> Optional<T> setValue(final String attributeCode, T value, Double weight) {

		Optional<EntityAttribute> existing = findEntityAttribute(attributeCode);
		if (existing.isPresent()) {
			existing.get().setValue(value);
			existing.get().setWeight(weight);
			return Optional.ofNullable(existing.get().getValue());
		}

		throw new ItemNotFoundException(getCode()+":"+attributeCode);
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
			for (EntityAttribute ea : this.baseEntityAttributes) {
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
			attributeMap = new ConcurrentHashMap<String, EntityAttribute>();
			// Grab all the entityAttributes and create a fast HashMap lookup
			for (EntityAttribute ea : this.baseEntityAttributes) {
				attributeMap.put(ea.getAttributeCode(), ea);
			}
		} else {
			attributeMap = null;
		}
	}

}
