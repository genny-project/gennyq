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

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Embeddable;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * A Link object
 * is used as a means of storing information from a source to a target attribute. This link
 * information includes:
 * <ul>
 * <li>The SourceCode
 * <li>The TargetCode
 * <li> The LinkCode
 * </ul>
 * <p>
 * Link class represents a simple pojo about baseentity links are stored. 
 * <p>
 * 
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */
@Embeddable
@RegisterForReflection
public class Link implements Serializable {

	/**
	 * A field that stores the human readable attributecode associated with this link.
	 */
	public String attributeCode;

	/**
	 * A field that stores the human readable targetcode associated with this link.
	 */
	public String targetCode;

	/**
	 * A field that stores the human readable sourcecode associated with this link.
	 */
	public String sourceCode;

	/**
	 * A field that stores the human readable link Value associated with this link.
	 */
	public String linkValue;

	public Double weight;

	/**
	 * Constructor.
	 */
	public Link() {
	}

	/**
	 * Constructor.
	 * 
	 * @param source The source associated with this Answer
	 * @param target The target associated with this Answer
	 * @param linkCode The linkCode associated with this Answer
	 */
	public Link(final String source, final String target, final String linkCode) {
		this(source, target, linkCode, "LINK");  
	}

	/**
	 * Constructor.
	 * 
	 * @param source The source associated with this Answer
	 * @param target The target associated with this Answer
	 * @param linkCode The linkCode associated with this Answer
	 * @param linkValue The linkValue associated with this Answer
	 */
	public Link(final String source, final String target, final String linkCode, final String linkValue) {
		this(source,target,linkCode,linkValue,0.0);
	}

	/**
	 * Constructor.
	 * 
	 * @param source The source associated with this Answer
	 * @param target The target associated with this Answer
	 * @param linkCode The linkCode associated with this Answer
	 * @param linkValue The linkValue associated with this Answer
	 * @param weight The weight of the link
	 */
	public Link(final String source, final String target, final String linkCode, final String linkValue, final Double weight) {
		this.sourceCode = source;
		this.targetCode = target;
		this.attributeCode = linkCode;
		this.linkValue = linkValue;
		this.weight = weight;
	}
	/**
	 * @return the attributeCode
	 */
	public String getAttributeCode() {
		return attributeCode;
	}

	/**
	 * @param attributeCode the attributeCode to set
	 */
	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
	}

	/**
	 * @return the targetCode
	 */
	public String getTargetCode() {
		return targetCode;
	}

	/**
	 * @param targetCode the targetCode to set
	 */
	public void setTargetCode(String targetCode) {
		this.targetCode = targetCode;
	}

	/**
	 * @return the sourceCode
	 */
	public String getSourceCode() {
		return sourceCode;
	}

	/**
	 * @param sourceCode the sourceCode to set
	 */
	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	/**
	 * @return thelinkValue 
	 */
	public String getLinkValue() {
		return linkValue;
	}

	/**
	 * @param linkValue the linkValue to set
	 */
	public void setLinkValue(String linkValue) {
		this.linkValue = linkValue;
	}

	/**
	 * @return the weight
	 */
	public Double getWeight() {
		return weight;
	}

	/**
	 * @param weight the weight to set
	 */
	public void setWeight(Double weight) {
		this.weight = weight;
	}

	/** 
	 * @return int
	 */
	@Override
	public int hashCode() {
		return Objects.hash(attributeCode, linkValue, sourceCode, targetCode, weight);
	}

	/** 
	 * Check equality.
	 *
	 * @param obj t=the object to check
	 * @return boolean
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Link)) {
			return false;
		}
		Link other = (Link) obj;
		return Objects.equals(attributeCode, other.attributeCode)
			&& Objects.equals(linkValue, other.linkValue)
			&& Objects.equals(sourceCode, other.sourceCode)
			&& Objects.equals(targetCode, other.targetCode)
			&& Objects.equals(weight, other.weight);
	}

	/** 
	 * @return String
	 */
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Link [" + (sourceCode != null ? "sourceCode=" + sourceCode + ", " : "")
			+ (targetCode != null ? "targetCode=" + targetCode + ", " : "")
			+ (attributeCode != null ? "attributeCode=" + attributeCode + ", " : "")
			+ (linkValue != null ? "linkValue=" + linkValue + ", " : "")
			+ (weight != null ? "weight=" + weight + ", " : "")
			+ "]";
	}

}
