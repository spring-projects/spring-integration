/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jpa.test.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;


/**
 * The JPA Entity for the Student class
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 *
 * @since 2.2
 *
 */
@Entity(name = "Student")
@Table(name = "Student")
@NamedQueries({
	@NamedQuery(name = "selectAllStudents", query = "select s from Student s"),
	@NamedQuery(name = "selectStudent", query = "select s from Student s where s.lastName = 'Last One'"),
	@NamedQuery(name = "updateStudent", query = "update Student s set s.lastName = :lastName, s.lastUpdated = :lastUpdated where s.rollNumber in (select max(a.rollNumber) from Student a)")
})
@NamedNativeQuery(resultClass = StudentDomain.class, name = "updateStudentNativeQuery", query = "update Student s set s.lastName = :lastName, lastUpdated = :lastUpdated where s.rollNumber in (select max(a.rollNumber) from Student a)")
public class StudentDomain {

	@Id
	@Column(name = "rollNumber")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long rollNumber;

	@Column(name = "firstName")
	private String firstName;

	@Column(name = "lastName")
	private String lastName;

	@Column(name = "gender")
	private String gender;

	@Column(name = "dateOfBirth")
	@Temporal(TemporalType.DATE)
	private Date dateOfBirth;

	@Column(name = "lastUpdated")
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastUpdated;

	public Long getRollNumber() {
		return rollNumber;
	}

	public void setRollNumber(Long rollNumber) {
		this.rollNumber = rollNumber;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Gender getGender() {
		return Gender.getGenderFromIdentifier(gender);
	}

	public void setGender(Gender gender) {
		this.gender = gender.getIdentifier();
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}


	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	//Convenience methods for chaining method calls

	public StudentDomain withRollNumber(Long rollNumber) {
		setRollNumber(rollNumber);
		return this;
	}

	public StudentDomain withFirstName(String firstName) {
		setFirstName(firstName);
		return this;
	}

	public StudentDomain withLastName(String lastName) {
		setLastName(lastName);
		return this;
	}

	public StudentDomain withGender(Gender gender) {
		setGender(gender);
		return this;
	}

	public StudentDomain withDateOfBirth(Date dateOfBirth) {
		setDateOfBirth(dateOfBirth);
		return this;
	}

	public StudentDomain withLastUpdated(Date lastUpdated) {
		setLastUpdated(lastUpdated);
		return this;
	}
}
