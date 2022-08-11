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

package life.genny.qwandaq.validation;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.PatternSyntaxException;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.annotations.Type;

import life.genny.qwandaq.CodedEntity;
import life.genny.qwandaq.converter.StringListConverter;
import life.genny.qwandaq.serialization.CoreEntitySerializable;

/**
 * Validation represents a distinct abstract Validation Representation in the Qwanda library.
 * The validations are applied to values.
 * In addition to the extended CoreEntity this information includes:
 * <ul>
 * <li>Regex 
 * </ul>
 * 
 * @author      Adam Crow
 * @author      Byron Aguirre
 * @version     %I%, %G%
 * @since       1.0
 */


@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)
@Table(name = "validation", 
indexes = {
        @Index(columnList = "code", name =  "code_idx"),
        @Index(columnList = "realm", name = "code_idx")
    },
uniqueConstraints = @UniqueConstraint(columnNames = {"code", "realm"}))
@Entity
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)

public class Validation extends CodedEntity implements Serializable {

	/** 
	 * @return String
	 */
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Validation [regex=" + regex + "]";
	}

	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_CODE_PREFIX = "VLD_";
	
	private static final String DEFAULT_REGEX = ".*";

	/**
	 * A field that stores the validation regex.
	 * Note that this regex needs to be applied to the complete value (Not partial).
	 */
	@NotNull
	@Type(type = "text")
	@Column(name = "regex", updatable = true, nullable = false)	
	private String regex;
	
	
	@Column(name = "selection_grp",  length = 512,updatable = true, nullable = true)	
	  @Convert(converter = StringListConverter.class)	
	private List<String> selectionBaseEntityGroupList;
	
	private Boolean recursiveGroup = false;
	
	private Boolean multiAllowed = false;

	@Column(name = "options", length = 2048, updatable = true, nullable = true)
	private String options;
	
	@Column(name = "errormsg", length = 280, updatable = true, nullable = true)
	private String errormsg;
	
	
	/**
	 * @return the options
	 */
	public String getOptions() {
		return options;
	}

	/**
	 * @param options the options to set
	 */
	public void setOptions(String options) {
		this.options = options;
	}
	
	
	
	/** 
	 * @return String
	 */
	public String getErrormsg() {
		return errormsg;
	}

	
	/** 
	 * @param errormsg the error msg to set
	 */
	public void setErrormsg(String errormsg) {
		this.errormsg = errormsg;
	}

	/**
	 * Constructor.
	 */
	@SuppressWarnings("unused")
	public Validation()
	{
		super();
	}
	
	public Validation(String aCode, String aName, String aRegex) throws PatternSyntaxException
	{
		super(aCode, aName);
		setRegex(aRegex);
	}
	
	public Validation(String aCode, String aName, String aRegex,String aOptions) throws PatternSyntaxException
	{
		super(aCode, aName);
		setRegex(aRegex);
		setOptions(aOptions);
	}

	
	public Validation(String aCode, String aName, String aSelectionBaseEntityGroup, Boolean recursive, Boolean multiAllowed) throws PatternSyntaxException
	{
		super(aCode, aName);
		setRegex(DEFAULT_REGEX);
		List<String> aSelectionBaseEntityGroupList = new CopyOnWriteArrayList<String>();
		aSelectionBaseEntityGroupList.add(aSelectionBaseEntityGroup);
		setSelectionBaseEntityGroupList(aSelectionBaseEntityGroupList);
		setMultiAllowed(multiAllowed);
	}

	public Validation(String aCode, String aName, String aSelectionBaseEntityGroup, Boolean recursive, Boolean multiAllowed,String aOptions) throws PatternSyntaxException
	{
		super(aCode, aName);
		setRegex(DEFAULT_REGEX);
		List<String> aSelectionBaseEntityGroupList = new CopyOnWriteArrayList<String>();
		aSelectionBaseEntityGroupList.add(aSelectionBaseEntityGroup);
		setSelectionBaseEntityGroupList(aSelectionBaseEntityGroupList);
		setMultiAllowed(multiAllowed);
		setOptions(aOptions);
	}
	
	public Validation(String aCode, String aName, List<String> aSelectionBaseEntityGroupList, Boolean recursive, Boolean multiAllowed) throws PatternSyntaxException
	{
		super(aCode, aName);
		setRegex(DEFAULT_REGEX);
		setSelectionBaseEntityGroupList(aSelectionBaseEntityGroupList);
		setMultiAllowed(multiAllowed);
	}
	
	public Validation(String aCode, String aName, List<String> aSelectionBaseEntityGroupList, Boolean recursive, Boolean multiAllowed,String aOptions) throws PatternSyntaxException
	{
		super(aCode, aName);
		setRegex(DEFAULT_REGEX);
		setSelectionBaseEntityGroupList(aSelectionBaseEntityGroupList);
		setMultiAllowed(multiAllowed);
		setOptions(aOptions);
	}

	/**
	 * @return the regex
	 */
	public String getRegex() {
		return regex;
	}

	/**
	 * @param regex the regex to set
	 */
	public void setRegex(String regex) throws PatternSyntaxException {
		if (regex!=null) {
			validateRegex(regex);  // confirm the regex is valid, if invalid throws PatternSyntaxException
		} else {
			regex = ".*";
		}
		this.regex = regex;
	}

	
	
	/**
	 * @return the selectionBaseEntityGroup
	 */
	public List<String> getSelectionBaseEntityGroupList() {
		return selectionBaseEntityGroupList;
	}

	/**
	 * @param selectionBaseEntityGroup the selectionBaseEntityGroup to set
	 */
	public void setSelectionBaseEntityGroupList(List<String> selectionBaseEntityGroup) {
		this.selectionBaseEntityGroupList = selectionBaseEntityGroup;
	}

	/**
	 * @return the recursiveGroup
	 */
	public Boolean getRecursiveGroup() {
		return recursiveGroup;
	}

	/**
	 * @param recursiveGroup the recursiveGroup to set
	 */
	public void setRecursiveGroup(Boolean recursiveGroup) {
		this.recursiveGroup = recursiveGroup;
	}

	
	/**
	 * @return the multiAllowed
	 */
	public Boolean getMultiAllowed() {
		return multiAllowed;
	}

	
	/** 
	 * @return Boolean
	 */
	public Boolean isMultiAllowed() {
		return getMultiAllowed();
	}

	
	/** 
	 * @return Boolean
	 */
	public Boolean isRecursiveGroup() {
		return getRecursiveGroup();
	}

	/**
	 * @param multiAllowed the multiAllowed to set
	 */
	public void setMultiAllowed(Boolean multiAllowed) {
		this.multiAllowed = multiAllowed;
	}

	/**
	 * @param regex the regex to validate
	 */
	static public void validateRegex(String regex) {
		java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex);
	}

	/**
	 * getDefaultCodePrefix This method is overrides the Base class
	 * 
	 * @return the default Code prefix for this class.
	 */
	static public String getDefaultCodePrefix() {
		return DEFAULT_CODE_PREFIX;
	}

	@Override
	public CoreEntitySerializable getCoreEntitySerializable() {
		// TODO Auto-generated method stub
		return null;
	}
}
