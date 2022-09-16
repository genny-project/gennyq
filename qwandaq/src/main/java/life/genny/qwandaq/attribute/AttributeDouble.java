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

import java.io.Serializable;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import life.genny.qwandaq.datatype.DataType;

/**
 * AttributeDouble class handles LocalDouble based attributes.
 * This information adds:
 * <ul>
 * <li>The AnswerTypeDouble is the Type for the Attribute class
 * </ul>
 * <p>
 * AttributeDouble represent the major way of specifying the Double data type
 * about a target
 * from sources.
 * <p>
 * 
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */
@Entity
@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("double")
public class AttributeDouble extends Attribute {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public AttributeDouble() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param aCode The unique code for this Attribute
	 * @param aName The name for this Attribute
	 */
	public AttributeDouble(String aCode, String aName) {
		super(aCode, aName, new DataType(Double.class));
	}

}
