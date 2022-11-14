/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jpa.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jpa.support.parametersource.ExpressionEvaluatingParameterSourceFactory;
import org.springframework.integration.jpa.support.parametersource.ParameterSource;
import org.springframework.integration.jpa.support.parametersource.ParameterSourceFactory;
import org.springframework.integration.jpa.test.JpaTestUtils;
import org.springframework.integration.jpa.test.entity.Gender;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 2.2
 *
 */
@Transactional
public class AbstractJpaOperationsTests {

	@Autowired
	protected PlatformTransactionManager transactionManager;

	@Autowired
	protected EntityManager entityManager;

	public void testGetAllStudents() {

		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final List<?> students = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);
		assertThat(students.size() == 3).isTrue();

	}

	public void testGetAllStudentsWithMaxResults() {

		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final List<?> students = jpaOperations.getResultListForClass(StudentDomain.class, 0, 2);
		assertThat(students.size() == 2)
				.as(String.format("Was expecting 2 Students to be returned but got '%s'.", students.size())).isTrue();

	}

	public void testExecuteUpdate() {

		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final StudentDomain student = JpaTestUtils.getTestStudent();

		List<?> students = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);
		assertThat(students.size() == 3).isTrue();

		ParameterSourceFactory requestParameterSourceFactory =
				new ExpressionEvaluatingParameterSourceFactory(mock(BeanFactory.class));
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);

		int updatedRecords = jpaOperations.executeUpdate("update Student s " +
				"set s.lastName = :lastName, s.lastUpdated = :lastUpdated " +
				"where s.rollNumber in (select max(a.rollNumber) from Student a)", source);

		entityManager.flush();

		assertThat(1 == updatedRecords).isTrue();
		assertThat(student.getRollNumber()).isNull();

	}

	public void testExecuteUpdateWithNamedQuery() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final StudentDomain student = JpaTestUtils.getTestStudent();

		ParameterSourceFactory requestParameterSourceFactory =
				new ExpressionEvaluatingParameterSourceFactory(mock(BeanFactory.class));
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);

		int updatedRecords = jpaOperations.executeUpdateWithNamedQuery("updateStudent", source);

		entityManager.flush();

		assertThat(1 == updatedRecords).isTrue();
		assertThat(student.getRollNumber()).isNull();
	}

	public void testExecuteUpdateWithNativeQuery() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final StudentDomain student = JpaTestUtils.getTestStudent();

		ExpressionEvaluatingParameterSourceFactory requestParameterSourceFactory =
				new ExpressionEvaluatingParameterSourceFactory(mock(BeanFactory.class));
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);

		int updatedRecords = jpaOperations.executeUpdateWithNativeQuery("update Student " +
				"set lastName = :lastName, lastUpdated = :lastUpdated " +
				"where rollNumber in (select max(a.rollNumber) from Student a)", source);

		entityManager.flush();

		assertThat(1 == updatedRecords).isTrue();
		assertThat(student.getRollNumber()).isNull();
	}

	public void testExecuteSelectWithNativeQueryReturningEntityClass() throws ParseException {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		String selectSqlQuery = "select * from Student where lastName = 'Last One'";

		Class<?> entityClass = StudentDomain.class;

		List<?> students = jpaOperations.getResultListForNativeQuery(selectSqlQuery, entityClass, null, 0, 0);

		assertThat(students.size() == 1).isTrue();

		StudentDomain retrievedStudent = (StudentDomain) students.iterator().next();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

		assertThat(retrievedStudent.getDateOfBirth()).isEqualTo(formatter.parse("1980/01/01"));
		assertThat(retrievedStudent.getFirstName()).isEqualTo("First One");
		assertThat(retrievedStudent.getGender()).isEqualTo(Gender.MALE);
		assertThat(retrievedStudent.getLastName()).isEqualTo("Last One");
		assertThat(retrievedStudent.getLastUpdated()).isNotNull();

	}

	public void testExecuteSelectWithNativeQuery() throws ParseException {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		String selectSqlQuery = "select rollNumber, firstName, lastName, gender, dateOfBirth, lastUpdated " +
				"from Student where lastName = 'Last One'";

		List<?> students = jpaOperations.getResultListForNativeQuery(selectSqlQuery, null, null, 0, 0);

		assertThat(students.size() == 1).isTrue();

		Object[] retrievedStudent = (Object[]) students.iterator().next();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

		assertThat(retrievedStudent[0]).isNotNull();
		assertThat(retrievedStudent[1]).isEqualTo("First One");
		assertThat(retrievedStudent[2]).isEqualTo("Last One");
		assertThat(retrievedStudent[3]).isEqualTo("M");
		assertThat(retrievedStudent[4]).isEqualTo(formatter.parse("1980/01/01"));
		assertThat(retrievedStudent[5]).isNotNull();

	}

	public void testExecuteUpdateWithNativeNamedQuery() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final StudentDomain student = JpaTestUtils.getTestStudent();

		ParameterSourceFactory requestParameterSourceFactory =
				new ExpressionEvaluatingParameterSourceFactory(mock(BeanFactory.class));
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);

		int updatedRecords = jpaOperations.executeUpdateWithNamedQuery("updateStudentNativeQuery", source);

		entityManager.flush();

		assertThat(1 == updatedRecords).isTrue();
		assertThat(student.getRollNumber()).isNull();
	}

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#merge(java.lang.Object)}.
	 */
	public void testMerge() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final StudentDomain student = JpaTestUtils.getTestStudent();

		assertThat(student.getRollNumber()).isNull();
		final StudentDomain savedStudent = (StudentDomain) jpaOperations.merge(student);
		entityManager.flush();
		assertThat(student.getRollNumber()).isNull();
		assertThat(savedStudent).isNotNull();
		assertThat(savedStudent.getRollNumber()).isNotNull();

		assertThat(student != savedStudent).isTrue();
	}

	public void testMergeCollection() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final StudentDomain student1 = JpaTestUtils.getTestStudent();
		final StudentDomain student2 = JpaTestUtils.getTestStudent();
		final StudentDomain student3 = JpaTestUtils.getTestStudent();

		student1.setFirstName("Karl");
		student2.setFirstName("Otto");
		student3.setFirstName("Wilhelm");

		List<StudentDomain> students = new ArrayList<StudentDomain>(3);

		students.add(student1);
		students.add(student2);
		students.add(student3);

		assertThat(student1.getRollNumber()).isNull();
		assertThat(student2.getRollNumber()).isNull();
		assertThat(student3.getRollNumber()).isNull();

		Object savedStudents = jpaOperations.merge(students, 10, true);

		assertThat(savedStudents instanceof List<?>).isTrue();

		@SuppressWarnings("unchecked")
		List<StudentDomain> savedStudentCollection = (List<StudentDomain>) savedStudents;

		assertThat(savedStudentCollection.get(0).getRollNumber()).isNotNull();
		assertThat(savedStudentCollection.get(1).getRollNumber()).isNotNull();
		assertThat(savedStudentCollection.get(2).getRollNumber()).isNotNull();
	}

	public void testMergeNullCollection() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		try {
			jpaOperations.merge(null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("The object to merge must not be null.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");

	}

	public void testMergeCollectionWithNullElement() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final List<?> studentsFromDbBeforeTest = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(studentsFromDbBeforeTest.size()).isEqualTo(3);

		final StudentDomain student1 = JpaTestUtils.getTestStudent();
		final StudentDomain student2 = null;
		final StudentDomain student3 = JpaTestUtils.getTestStudent();

		student1.setFirstName("Karl");
		student3.setFirstName("Wilhelm");

		List<StudentDomain> students = new ArrayList<StudentDomain>(3);

		students.add(student1);
		students.add(student2);
		students.add(student3);

		assertThat(student1.getRollNumber()).isNull();
		assertThat(student2).isNull();
		assertThat(student3.getRollNumber()).isNull();

		Object savedStudents = jpaOperations.merge(students);
		entityManager.flush();

		assertThat(savedStudents instanceof List<?>).isTrue();

		@SuppressWarnings("unchecked")
		List<StudentDomain> savedStudentCollection = (List<StudentDomain>) savedStudents;

		assertThat(savedStudentCollection.get(0).getRollNumber()).isNotNull();
		assertThat(savedStudentCollection.get(1).getRollNumber()).isNotNull();

	}

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#persist(java.lang.Object)}.
	 */
	public void testPersist() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final StudentDomain student = JpaTestUtils.getTestStudent();

		assertThat(student.getRollNumber()).isNull();
		jpaOperations.persist(student, 1, false);
		assertThat(student.getRollNumber()).isNotNull();

		assertThat(entityManager.contains(student)).isTrue();
	}

	public void testPersistCollection() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final StudentDomain student1 = JpaTestUtils.getTestStudent();
		final StudentDomain student2 = JpaTestUtils.getTestStudent();
		final StudentDomain student3 = JpaTestUtils.getTestStudent();

		student1.setFirstName("Karl");
		student2.setFirstName("Otto");
		student3.setFirstName("Wilhelm");

		List<StudentDomain> students = new ArrayList<StudentDomain>(3);

		students.add(student1);
		students.add(student2);
		students.add(student3);

		assertThat(student1.getRollNumber()).isNull();
		assertThat(student2.getRollNumber()).isNull();
		assertThat(student3.getRollNumber()).isNull();

		jpaOperations.persist(students, 1, true);
		assertThat(student1.getRollNumber()).isNotNull();
		assertThat(student2.getRollNumber()).isNotNull();
		assertThat(student3.getRollNumber()).isNotNull();

		assertThat(entityManager.contains(student1)).isFalse();
		assertThat(entityManager.contains(student2)).isFalse();
		assertThat(entityManager.contains(student3)).isFalse();
	}

	public void testPersistNullCollection() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		try {
			jpaOperations.persist(null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("The object to persist must not be null.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");

	}

	public void testPersistCollectionWithNullElement() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final List<?> studentsFromDbBeforeTest = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(studentsFromDbBeforeTest.size()).isEqualTo(3);

		final StudentDomain student1 = JpaTestUtils.getTestStudent();
		final StudentDomain student2 = null;
		final StudentDomain student3 = JpaTestUtils.getTestStudent();

		student1.setFirstName("Karl");
		student3.setFirstName("Wilhelm");

		List<StudentDomain> students = new ArrayList<StudentDomain>(3);

		students.add(student1);
		students.add(student2);
		students.add(student3);

		assertThat(student1.getRollNumber()).isNull();
		assertThat(student2).isNull();
		assertThat(student3.getRollNumber()).isNull();

		jpaOperations.persist(students, 10, false);

		assertThat(student1.getRollNumber()).isNotNull();
		assertThat(student3.getRollNumber()).isNotNull();

		final List<?> studentsFromDb = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(studentsFromDb).isNotNull();
		assertThat(studentsFromDb.size()).isEqualTo(5);
	}

	public void testDeleteInBatch() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final List<?> students = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(students).isNotNull();


		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		// explicitly setting the transaction name is something that can only be done programmatically
		def.setName("SomeOtherTxName");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		TransactionStatus status = transactionManager.getTransaction(def);

		jpaOperations.deleteInBatch(students);

		entityManager.flush();

		transactionManager.commit(status);

		final List<?> studentsFromDb = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(studentsFromDb).isNotNull();
		assertThat(studentsFromDb.size() == 0).isTrue();

	}

	protected JpaOperations getJpaOperations(EntityManager entityManager) {

		final DefaultJpaOperations jpaOperationsImpl = new DefaultJpaOperations();
		jpaOperationsImpl.setEntityManager(entityManager);
		jpaOperationsImpl.afterPropertiesSet();

		return jpaOperationsImpl;
	}

	public void testDelete() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final List<?> studentsFromDb = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(studentsFromDb).isNotNull();
		assertThat(studentsFromDb.size() == 3).isTrue();


		final StudentDomain student = jpaOperations.find(StudentDomain.class, 1001L);

		assertThat(student).isNotNull();


		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		// explicitly setting the transaction name is something that can only be done programmatically
		def.setName("SomeOtherTxName");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		TransactionStatus status = transactionManager.getTransaction(def);

		jpaOperations.delete(student);

		entityManager.flush();

		transactionManager.commit(status);

		final List<?> studentsFromDbAfterDelete = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(studentsFromDbAfterDelete).isNotNull();
		assertThat(studentsFromDbAfterDelete.size() == 2).isTrue();

	}

	public void testDeleteInBatchWithEmptyCollection() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final List<?> students = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(students).isNotNull();
		assertThat(students.size() == 3).isTrue();

		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		// explicitly setting the transaction name is something that can only be done programmatically
		def.setName("SomeOtherTxName");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		TransactionStatus status = transactionManager.getTransaction(def);

		jpaOperations.deleteInBatch(new ArrayList<StudentDomain>(0));

		entityManager.flush();

		transactionManager.commit(status);

		final List<?> studentsFromDb = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(studentsFromDb).isNotNull();
		assertThat(studentsFromDb.size() == 3).isTrue(); //Nothing should have happened

	}

	public void testGetAllStudentsFromThirdRecord() {
		JpaOperations jpaOperations = getJpaOperations(entityManager);
		List<?> results = jpaOperations.getResultListForClass(StudentDomain.class, 2, 0);
		assertThat(results.size()).isEqualTo(1);
	}


	public void testGetAllStudentsUsingNativeQueryFromThirdRecord() {
		JpaOperations jpaOperations = getJpaOperations(entityManager);
		String query = "select * from Student";
		List<?> results = jpaOperations.getResultListForNativeQuery(query, StudentDomain.class, null, 2, 0);
		assertThat(results.size()).isEqualTo(1);
	}


	public void testGetAllStudentsUsingNamedQueryFromThirdRecord() {
		JpaOperations jpaOperations = getJpaOperations(entityManager);
		List<?> results = jpaOperations.getResultListForNamedQuery("selectAllStudents", null, 2, 0);
		assertThat(results.size()).isEqualTo(1);
	}

	public void testGetAllStudentsUsingJPAQueryFromThirdRecord() {
		JpaOperations jpaOperations = getJpaOperations(entityManager);
		String query = "select s from Student s";
		List<?> results = jpaOperations.getResultListForQuery(query, null, 2, 0);
		assertThat(results.size()).isEqualTo(1);
	}

	public void testWithNegativeMaxNumberofResults() {
		JpaOperations jpaOperations = getJpaOperations(entityManager);
		String query = "select s from Student s";
		List<?> results = jpaOperations.getResultListForQuery(query, null, 0, -1);
		assertThat(results.size()).isEqualTo(3);
	}

}
