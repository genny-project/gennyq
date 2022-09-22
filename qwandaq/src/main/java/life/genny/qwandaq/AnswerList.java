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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.querydsl.core.annotations.QueryExclude;


/**
 * AnswerList represents a set of Answers in the Qwanda library. The answers in
 * the list can be
 * applied to a passed value.
 * <ul>
 * <li>List of Answers
 * </ul>
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */

@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)
@Embeddable
public class AnswerList implements Serializable {

	private static final long serialVersionUID = 1L;

    /**
     * A fieldlist that stores the answers for this object.
     */
    // @JsonIgnore
    @XmlTransient

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@JoinColumn(name = "answerlist_id", referencedColumnName = "id")
	private List<AnswerLink> answerList = new CopyOnWriteArrayList<>();

	/**
	 * Constructor.
	 */
	public AnswerList() {
	}

	/**
	 * Constructor.
	 *
	 * @param answers the List AnswerLink objects to set
	 */
	public AnswerList(final List<AnswerLink> answers) {
		this.answerList = answers;
	}

	/**
	 * @return the answerList
	 */
	public List<AnswerLink> getAnswerList() {
		return answerList;
	}

	/**
	 * @param answerList the answerList to set
	 */
	public void setAnswerList(final List<AnswerLink> answerList) {
		this.answerList = answerList;
	}

}
