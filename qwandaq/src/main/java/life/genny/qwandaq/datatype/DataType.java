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
 * Contributors:
 * 	Adam Crow 
 *	Byron Aguirre
 *	Jasper Robison
 */

package life.genny.qwandaq.datatype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.json.bind.annotation.JsonbTransient;
import javax.money.Monetary;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import life.genny.qwandaq.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.javamoney.moneta.Money;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.converter.ValidationListConverter;
import life.genny.qwandaq.validation.Validation;

/**
 * DataType represents a distinct abstract Data Representation in the Qwanda
 * library. The data types express the format and the validations required for
 * values collected. In addition to the extended CoreEntity this information
 * includes:
 * <ul>
 * <li>The code type of the base data e.g. Text, Integer, etc.
 * <li>The List of default Validation items
 * <li>The default mask used for data entry
 * </ul>
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 * @author Jasper Robison
 * @version %I%, %G%
 * @since 1.0
 */

@Embeddable
@RegisterForReflection
public class DataType implements CoreEntityPersistable {

	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(DataType.class);

	public static final String DTT_LINK = "LNK_ATTRIBUTE"; // This datatype classname indicates the datatype belongs to
															// the BaseEntity set with parent

	@NotNull
	@Size(max = 120)
	private String realm;

	@NotNull
	@Size(max = 120)
	private String dttCode; // e.g. java.util.String

	@NotNull
	@Size(max = 120)
	private String className; // e.g. java.util.String

	@NotNull
	@Size(max = 120)
	// @JsonIgnore
	private String typeName; // e.g. TEXT

	private String inputmask;

	private String validationCodes;

	/**
	 * @return String
	 */
	public String getComponent() {
		return component;
	}

	/**
	 * @param component the component to set
	 */
	public void setComponent(String component) {
		this.component = component;
	}

	private String component;

	/**
	 * A fieldlist that stores the validations for this object.
	 * Note that this is stored into a single object
	 */

	@Column(name = "validation_list", length = 512)
	@Convert(converter = ValidationListConverter.class)
	private List<Validation> validationList = new CopyOnWriteArrayList<Validation>();

	/**
	 * Constructor.
	 */
	@SuppressWarnings("unused")
	public DataType() {
		// super();
		// dummy for hibernate
	}

	public DataType(final Class clazz) {
		this(clazz, new ArrayList<>(0));
	}

	public DataType(final String className) {
		this(className, new ArrayList<>(0));
	}

	public DataType(final String className, final List<Validation> aValidationList, final String name,
			final String inputmask) {
		setDttCodeFromClassName(className);
		setClassName(className);
		setValidationList(aValidationList);
		setTypeName(name);
		setInputmask(inputmask);
	}

	public DataType(final String className, final List<Validation> aValidationList, final String name,
			final String inputmask, final String component) {
		setDttCodeFromClassName(className);
		setClassName(className);
		setValidationList(aValidationList);
		setTypeName(name);
		setInputmask(inputmask);
		setComponent(component);
	}

	public DataType(final String className, final List<Validation> aValidationList, final String name) {
		this(className, aValidationList, name, "");
	}

	/**
	 * @param str the className string used to set the Dtt
	 */
	public void setDttCodeFromClassName(String str) {
		String[] strs = str.split("\\.");
		String type;

		if (strs.length > 1) {
			type = strs[strs.length - 1];
		} else {
			type = strs[0];
		}
		if (str.contains("DTT")) {
			setDttCode(str);
		} else {
			setDttCode("DTT_" + type.toUpperCase());
		}
	}

	public DataType(final String className, final List<Validation> aValidationList) {
		this(className, aValidationList, className);
	}

	public DataType(final Class clazz, final List<Validation> aValidationList) {
		this(clazz.getCanonicalName(), aValidationList);
	}

	/**
	 * @return the validationList
	 */
	public List<Validation> getValidationList() {
		return validationList;
	}

	/**
	 * @param validationList
	 *                       the validationList to set
	 */
	public void setValidationList(final List<Validation> validationList) {
		this.validationList = validationList;
	}

	/**
	 * @return the realm
	 */
	public String getRealm() {
		return realm;
	}

	/**
	 * @param realm
	 *                  the realm to set
	 */
	public void setRealm(final String realm) {
		this.realm = realm;
	}

	/**
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @param className
	 *                  the className to set
	 */
	public void setClassName(final String className) {
		this.className = className;
	}

	/**
	 * @return the name
	 */
	public String getTypeName() {
		return typeName;
	}

	/**
	 * @param name
	 *             the name to set
	 */
	public void setTypeName(String name) {
		this.typeName = name;
	}

	/**
	 * @return the name
	 */
	public String getDttCode() {
		return this.dttCode;
	}

	/**
	 * @param code
	 *             the name to set
	 */
	public void setDttCode(String code) {
		this.dttCode = code;
	}

	/**
	 * @return the inputmask
	 */
	public String getInputmask() {
		return inputmask;
	}

	/**
	 * @param inputmask
	 *                  the inputmask to set
	 */
	public void setInputmask(String inputmask) {
		this.inputmask = inputmask;
	}

	/**
	 * @return the validationCodes
	 */
	public String getValidationCodes() {
		return validationCodes;
	}

	/**
	 * @param validationCodes
	 *                  the validationCodes to set
	 */
	public void setValidationCodes(String validationCodes) {
		this.validationCodes = validationCodes;
	}

	/**
	 * @param c the class to set
	 */
	@JsonIgnore
	@Transient
	@XmlTransient
	public void setClass(final Class c) {
		final String simpleClassName = c.getCanonicalName();
		setClassName(simpleClassName);
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
		return "DataType(" + className + " component: " + this.component + ", inputmask:" + this.inputmask + ")";
	}

	/**
	 * Get an Instance of a class
	 *
	 * @param className the className of the instance to get
	 * @return DataType
	 */
	static public DataType getInstance(final String className) {
		final List<Validation> validationList = new CopyOnWriteArrayList<Validation>();
		DataType dataTypeInstance = new DataType(className, validationList);
		return dataTypeInstance;
	}

	/**
	 * Is Datatype summable
	 *
	 * @param dtype the DataType to check
	 * @return boolean
	 */
	static public boolean summable(DataType dtype) {
		switch (dtype.getClassName()) {
			case "java.lang.Integer":
			case "Integer":
			case "java.lang.Long":
			case "Long":
			case "java.lang.Double":
			case "Double":
			case "org.javamoney.moneta.Money":
			case "Money":
				return true;
			default:
				return false;
		}
	}

	/**
	 * Return a zero item
	 *
	 * @param dtype the DataType of the return item
	 * @return Object
	 */
	static public Object Zero(DataType dtype) {
		switch (dtype.getClassName()) {
			case "java.lang.Integer", "Integer" -> { return Integer.valueOf(0); }
			case "java.lang.Long", "Long" -> { return Long.valueOf(0); }
			case "java.lang.Double","Double" -> { return Double.valueOf(0.0); }
			case "javax.money.CurrencyUnit", "org.javamoney.moneta.Money", "Money" -> { return Money.zero(Monetary.getCurrency("AUD")); }
		}
		return null;
	}

	/**
	 * Add two items together
	 *
	 * @param dtype the DataType of the items
	 * @param x     item one
	 * @param y     item two
	 * @return Object
	 */
	static public Object add(DataType dtype, Object x, Object y) {
		switch (dtype.getClassName()) {
			case "java.lang.Integer", "Integer" -> { return ((Integer) x) + ((Integer) y); }
			case "java.lang.Long", "Long" -> { return ((Long) x) + ((Long) y); }
			case "java.lang.Double", "Double" -> { return ((Double) x) + ((Double) y); }
			case "org.javamoney.moneta.Money", "Money" -> {
				Money m1 = (Money) x;
				Money m2 = (Money) y;
				Money sum = m1.add(m2);
			}
		}
		return null;
	}

	@Override
	public CoreEntitySerializable toSerializableCoreEntity() {
		life.genny.qwandaq.serialization.datatype.DataType dataType = new life.genny.qwandaq.serialization.datatype.DataType();
		dataType.setRealm(getRealm());
		dataType.setDttCode(getDttCode());
		dataType.setClassName(getClassName());
		dataType.setComponent(getComponent());
		dataType.setInputMask(getInputmask());
		dataType.setValidationCodes(getValidationCodes());
		return dataType;
	}
}
