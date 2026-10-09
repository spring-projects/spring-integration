/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.test.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * The JPA Entity for the Student class
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 * @author Artem Bilan
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
@NamedNativeQuery(resultClass = StudentDomain.class, name = "updateStudentNativeQuery",
		query = "update Student s set s.lastName = :lastName, lastUpdated = :lastUpdated where s.rollNumber in (select max(a.rollNumber) from Student a)")
@SequenceGenerator(name = "student_sequence", initialValue = 1004, allocationSize = 1)
public class StudentDomain {

	@Id
	@Column(name = "rollNumber")
	@GeneratedValue(generator = "student_sequence")
	private Long rollNumber;

	@Column(name = "firstName")
	private String firstName;

	@Column(name = "lastName")
	private String lastName;

	@Column(name = "gender")
	private String gender;

	@Column(name = "dateOfBirth")
	private LocalDate dateOfBirth;

	@Column(name = "lastUpdated")
	private LocalDateTime lastUpdated;

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

	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public LocalDateTime getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(LocalDateTime lastUpdated) {
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

	public StudentDomain withDateOfBirth(LocalDate dateOfBirth) {
		setDateOfBirth(dateOfBirth);
		return this;
	}

	public StudentDomain withLastUpdated(LocalDateTime lastUpdated) {
		setLastUpdated(lastUpdated);
		return this;
	}

}
