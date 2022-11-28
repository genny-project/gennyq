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

package life.genny.qwandaq.attribute;

import javax.persistence.Cacheable;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.annotations.CacheConcurrencyStrategy;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.CodedEntity;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.utils.CommonUtils;

/**
 * Attribute represents a distinct abstract Fact about a target entity
 * managed in the Qwanda library.
 * An attribute may be used directly in processing meaning for a target
 * entity. Such processing may be in relation to a comparison score against
 * another target entity, or to generate more attribute information via
 * inference and induction This
 * attribute information includes:
 * <ul>
 * <li>The Human Readable name for this attibute (used for summary lists)
 * <li>The unique code for the attribute
 * <li>The description of the attribute
 * <li>The answerType that represents the format of the attribute
 * </ul>
 * <p>
 * Attributes represent facts about a target.
 * </p>
 * 
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */

@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)

@Table(name = "attribute", indexes = {
		@Index(columnList = "code", name = "code_idx"),
		@Index(columnList = "realm", name = "code_idx")
}, uniqueConstraints = @UniqueConstraint(columnNames = { "code", "realm" }))
@Entity
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@RegisterForReflection
public class Attribute extends CodedEntity {

	private static final long serialVersionUID = 1L;

	// core
	public static final String QQQ_QUESTION_GROUP = "QQQ_QUESTION_GROUP";
	public static final String PRI_NAME = "PRI_NAME";
	public static final String PRI_CODE = "PRI_CODE";
	public static final String PRI_CREATED = "PRI_CREATED";
	public static final String PRI_UPDATED = "PRI_UPDATED";
	public static final String PRI_UUID = "PRI_UUID";
	public static final String PRI_USERNAME = "PRI_USERNAME";

	public static final String PRI_EVENT = "PRI_EVENT";
	public static final String PRI_SUBMIT = "PRI_SUBMIT";

	// display
	public static final String PRI_IMAGE_URL = "PRI_IMAGE_URL";
	public static final String PRI_IMAGE = "PRI_IMAGE";
	public static final String PRI_IMAGES = "PRI_IMAGES";
	public static final String PRI_DESCRIPTION = "PRI_DESCRIPTION";

	// links
	public static final String LNK_CORE = "LNK_CORE";
	public static final String LNK_ITEMS = "LNK_ITEMS";
	public static final String LNK_AUTHOR = "LNK_AUTHOR";
	public static final String LNK_SUMMARY = "LNK_SUMMARY";

	// definition
	public static final String LNK_DEF = "LNK_DEF";
	public static final String PRI_PREFIX = "PRI_PREFIX";

	// roles
	public static final String LNK_ROLE = "LNK_ROLE";
	public static final String LNK_CHILDREN = "LNK_CHILDREN";

	// pcm
	public static final String PRI_LOC = "PRI_LOC";
	public static final String PRI_TEMPLATE_CODE = "PRI_TEMPLATE_CODE";
	public static final String PRI_QUESTION_CODE = "PRI_QUESTION_CODE";
	public static final String PRI_TARGET_CODE = "PRI_TARGET_CODE";

	// events
	public static final String EVT_SUBMIT = "EVT_SUBMIT";
	public static final String EVT_UPDATE = "EVT_UPDATE";
	public static final String EVT_CANCEL = "EVT_CANCEL";
	public static final String EVT_UNDO = "EVT_UNDO";
	public static final String EVT_REDO = "EVT_REDO";
	public static final String EVT_RESET = "EVT_RESET";

	public static final String EVT_NEXT = "EVT_NEXT";
	public static final String EVT_PREVIOUS = "EVT_PREVIOUS";

	// person
	public static final String PRI_FIRSTNAME = "PRI_FIRSTNAME";
	public static final String PRI_LASTNAME = "PRI_LASTNAME";

	// contact
	public static final String PRI_MOBILE = "PRI_MOBILE";
	public static final String PRI_EMAIL = "PRI_EMAIL";
	public static final String PRI_TIMEZONE_ID = "PRI_TIMEZONE_ID";
	public static final String PRI_ADDRESS = "PRI_ADDRESS";

	// search
	public static final String PRI_SEARCH_TEXT = "PRI_SEARCH_TEXT";
	public static final String PRI_TOTAL_RESULTS = "PRI_TOTAL_RESULTS";
	public static final String PRI_INDEX = "PRI_INDEX";

	@Embedded
	@NotNull
	public DataType dataType;

	private Boolean defaultPrivacyFlag = false;

	private String description;

	private String help;

	private String placeholder;

	private String defaultValue;

	private String icon;

	/**
	 * Constructor.
	 */
	public Attribute() {
	}

	public Attribute(String code, String name, DataType dataType) {
		super(code, name);
		setDataType(dataType);
	}

	/**
	 * @return the dataType
	 */
	public DataType getDataType() {
		return dataType;
	}

	/**
	 * @param dataType the dataType to set
	 */
	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	/**
	 * @return String
	 */
	@Override
	public String toString() {
		return getCode() + ",dataType=" + dataType;
	}

	/**
	 * @return the defaultPrivacyFlag
	 */
	public Boolean getDefaultPrivacyFlag() {
		return defaultPrivacyFlag;
	}

	/**
	 * @return Boolean
	 */
	public Boolean isDefaultPrivacyFlag() {
		return getDefaultPrivacyFlag();
	}

	/**
	 * @param defaultPrivacyFlag the defaultPrivacyFlag to set
	 */
	public void setDefaultPrivacyFlag(Boolean defaultPrivacyFlag) {
		this.defaultPrivacyFlag = defaultPrivacyFlag;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the help
	 */
	public String getHelp() {
		return help;
	}

	/**
	 * @param help the help to set
	 */
	public void setHelp(String help) {
		this.help = help;
	}

	/**
	 * @return the placeholder
	 */
	public String getPlaceholder() {
		return placeholder;
	}

	/**
	 * @param placeholder the placeholder to set
	 */
	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}

	/**
	 * @return the defaultValue
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @param defaultValue the defaultValue to set
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	/**
	 * @return String
	 */
	public String getIcon() {
		return this.icon;
	}

	/**
	 * Deep-compare two attributes
	 * 
	 * @param other attribute to compare against
	 * @return true if all fields are the same. False if one is different
	 */
	public boolean equals(Attribute other) {
		return equals(other, false);
	}

	/**
	 * Deep-compare two attributes
	 * 
	 * @param other   attribute to compare against
	 * @param checkId whether to check the id or not (database equality) (default:
	 *                false)
	 * @return true if all fields are the same. False if one is different
	 */
	public boolean equals(Attribute other, boolean checkId) {
		boolean sameDesc = CommonUtils.compare(description, other.description);
		if (!sameDesc)
			return false;

		boolean samePrivacy = (defaultPrivacyFlag == other.defaultPrivacyFlag);
		if (!samePrivacy)
			return false;

		boolean sameDTT = CommonUtils.compare(dataType, other.dataType);
		if (!sameDTT)
			return false;

		boolean sameHelp = CommonUtils.compare(help, other.help);
		if (!sameHelp)
			return false;

		boolean samePlaceholder = CommonUtils.compare(placeholder, other.placeholder);
		if (!samePlaceholder)
			return false;

		boolean sameDefault = CommonUtils.compare(defaultValue, other.defaultValue);
		if (!sameDefault)
			return false;

		boolean sameIcon = CommonUtils.compare(icon, other.icon);
		if (!sameIcon)
			return false;

		// Check the id if necessary
		return checkId ? (other.getId() == getId()) : true;
	}

}
