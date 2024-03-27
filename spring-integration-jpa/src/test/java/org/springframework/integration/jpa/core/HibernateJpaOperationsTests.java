/*
 * Copyright 2002-2024 the original author or authors.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jpa.support.parametersource.ExpressionEvaluatingParameterSourceFactory;
import org.springframework.integration.jpa.support.parametersource.ParameterSource;
import org.springframework.integration.jpa.support.parametersource.ParameterSourceFactory;
import org.springframework.integration.jpa.test.JpaTestUtils;
import org.springframework.integration.jpa.test.entity.Gender;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
@Transactional
public class HibernateJpaOperationsTests {

	@Autowired
	protected EntityManager entityManager;

	DefaultJpaOperations jpaOperations;

	@BeforeEach
	void setup() {
		this.jpaOperations = new DefaultJpaOperations();
		this.jpaOperations.setEntityManager(this.entityManager);
		this.jpaOperations.afterPropertiesSet();
	}

	@Test
	public void testGetAllStudents() {
		final List<?> students = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);
		assertThat(students).hasSize(3);
	}

	@Test
	public void testGetAllStudentsWithMaxResults() {
		final List<?> students = jpaOperations.getResultListForClass(StudentDomain.class, 0, 2);
		assertThat(students).hasSize(2);
	}

	@Test
	public void testExecuteUpdate() {
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

		assertThat(updatedRecords).isEqualTo(1);
		assertThat(student.getRollNumber()).isNull();

	}

	@Test
	public void testExecuteUpdateWithNamedQuery() {
		final StudentDomain student = JpaTestUtils.getTestStudent();

		ParameterSourceFactory requestParameterSourceFactory =
				new ExpressionEvaluatingParameterSourceFactory(mock(BeanFactory.class));
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);

		int updatedRecords = jpaOperations.executeUpdateWithNamedQuery("updateStudent", source);

		entityManager.flush();

		assertThat(updatedRecords).isEqualTo(1);
		assertThat(student.getRollNumber()).isNull();
	}

	@Test
	public void testExecuteUpdateWithNativeQuery() {
		final StudentDomain student = JpaTestUtils.getTestStudent();

		ExpressionEvaluatingParameterSourceFactory requestParameterSourceFactory =
				new ExpressionEvaluatingParameterSourceFactory(mock(BeanFactory.class));
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);

		int updatedRecords = jpaOperations.executeUpdateWithNativeQuery("update Student " +
				"set lastName = :lastName, lastUpdated = :lastUpdated " +
				"where rollNumber in (select max(a.rollNumber) from Student a)", source);

		entityManager.flush();

		assertThat(updatedRecords).isEqualTo(1);
		assertThat(student.getRollNumber()).isNull();
	}

	@Test
	public void testExecuteSelectWithNativeQueryReturningEntityClass() throws ParseException {
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

	@Test
	public void testExecuteSelectWithNativeQuery() throws ParseException {
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

	@Test
	public void testExecuteUpdateWithNativeNamedQuery() {
		final StudentDomain student = JpaTestUtils.getTestStudent();

		ParameterSourceFactory requestParameterSourceFactory =
				new ExpressionEvaluatingParameterSourceFactory(mock(BeanFactory.class));
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);

		int updatedRecords = jpaOperations.executeUpdateWithNamedQuery("updateStudentNativeQuery", source);

		entityManager.flush();

		assertThat(updatedRecords).isEqualTo(1);
		assertThat(student.getRollNumber()).isNull();
	}

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#merge(java.lang.Object)}.
	 */
	@Test
	public void testMerge() {
		final StudentDomain student = JpaTestUtils.getTestStudent();

		assertThat(student.getRollNumber()).isNull();
		final StudentDomain savedStudent = (StudentDomain) jpaOperations.merge(student);
		entityManager.flush();
		assertThat(savedStudent).isNotNull();
		assertThat(savedStudent.getRollNumber()).isNotNull();

		assertThat(student != savedStudent).isTrue();
	}

	@Test
	public void testMergeCollection() {
		final StudentDomain student1 = JpaTestUtils.getTestStudent();
		final StudentDomain student2 = JpaTestUtils.getTestStudent();
		final StudentDomain student3 = JpaTestUtils.getTestStudent();

		student1.setFirstName("Karl");
		student2.setFirstName("Otto");
		student3.setFirstName("Wilhelm");

		List<StudentDomain> students = new ArrayList<>(3);

		students.add(student1);
		students.add(student2);
		students.add(student3);

		assertThat(student1.getRollNumber()).isNull();
		assertThat(student2.getRollNumber()).isNull();
		assertThat(student3.getRollNumber()).isNull();

		Object savedStudents = jpaOperations.merge(students, 10, true);

		assertThat(savedStudents).isInstanceOf(List.class);

		@SuppressWarnings("unchecked")
		List<StudentDomain> savedStudentCollection = (List<StudentDomain>) savedStudents;

		assertThat(savedStudentCollection.get(0).getRollNumber()).isNotNull();
		assertThat(savedStudentCollection.get(1).getRollNumber()).isNotNull();
		assertThat(savedStudentCollection.get(2).getRollNumber()).isNotNull();
	}

	@Test
	public void testMergeNullCollection() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> jpaOperations.merge(null))
				.withMessage("The object to merge must not be null.");
	}

	@Test
	public void testMergeCollectionWithNullElement() {
		final List<?> studentsFromDbBeforeTest = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(studentsFromDbBeforeTest).hasSize(3);

		final StudentDomain student1 = JpaTestUtils.getTestStudent();
		final StudentDomain student2 = null;
		final StudentDomain student3 = JpaTestUtils.getTestStudent();

		student1.setFirstName("Karl");
		student3.setFirstName("Wilhelm");

		List<StudentDomain> students = new ArrayList<>(3);

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
	@Test
	public void testPersist() {
		final StudentDomain student = JpaTestUtils.getTestStudent();

		assertThat(student.getRollNumber()).isNull();
		jpaOperations.persist(student, 1, false);
		assertThat(student.getRollNumber()).isNotNull();

		assertThat(entityManager.contains(student)).isTrue();
	}

	@Test
	public void testPersistCollection() {
		final StudentDomain student1 = JpaTestUtils.getTestStudent();
		final StudentDomain student2 = JpaTestUtils.getTestStudent();
		final StudentDomain student3 = JpaTestUtils.getTestStudent();

		student1.setFirstName("Karl");
		student2.setFirstName("Otto");
		student3.setFirstName("Wilhelm");

		List<StudentDomain> students = new ArrayList<>(3);

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

	@Test
	public void testPersistNullCollection() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> jpaOperations.persist(null))
				.withMessage("The object to persist must not be null.");
	}

	@Test
	public void testPersistCollectionWithNullElement() {
		final List<?> studentsFromDbBeforeTest = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(studentsFromDbBeforeTest.size()).isEqualTo(3);

		final StudentDomain student1 = JpaTestUtils.getTestStudent();
		final StudentDomain student2 = null;
		final StudentDomain student3 = JpaTestUtils.getTestStudent();

		student1.setFirstName("Karl");
		student3.setFirstName("Wilhelm");

		List<StudentDomain> students = new ArrayList<>(3);

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

		assertThat(studentsFromDb).hasSize(5);
	}

	@Test
	public void testDeleteInBatch() {
		final List<?> students = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(students).isNotNull();

		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		// explicitly setting the transaction name is something that can only be done programmatically
		def.setName("SomeOtherTxName");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		jpaOperations.deleteInBatch(students);

		entityManager.flush();

		final List<?> studentsFromDb = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(studentsFromDb).hasSize(0);
	}

	@Test
	public void testDelete() {
		final List<?> studentsFromDb = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(studentsFromDb).isNotNull();
		assertThat(studentsFromDb.size() == 3).isTrue();

		final StudentDomain student = jpaOperations.find(StudentDomain.class, 1001L);

		assertThat(student).isNotNull();

		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		// explicitly setting the transaction name is something that can only be done programmatically
		def.setName("SomeOtherTxName");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		jpaOperations.delete(student);

		entityManager.flush();

		final List<?> studentsFromDbAfterDelete = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(studentsFromDbAfterDelete).isNotNull();
		assertThat(studentsFromDbAfterDelete).hasSize(2);
	}

	@Test
	public void testDeleteInBatchWithEmptyCollection() {
		final List<?> students = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(students).isNotNull();
		assertThat(students.size() == 3).isTrue();

		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		// explicitly setting the transaction name is something that can only be done programmatically
		def.setName("SomeOtherTxName");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		jpaOperations.deleteInBatch(new ArrayList<StudentDomain>(0));

		entityManager.flush();

		final List<?> studentsFromDb = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		assertThat(studentsFromDb).hasSize(3);
	}

	@Test
	public void testGetAllStudentsFromThirdRecord() {
		List<?> results = jpaOperations.getResultListForClass(StudentDomain.class, 2, 0);
		assertThat(results).hasSize(1);
	}

	@Test
	public void testGetAllStudentsUsingNativeQueryFromThirdRecord() {
		String query = "select * from Student";
		List<?> results = jpaOperations.getResultListForNativeQuery(query, StudentDomain.class, null, 2, 0);
		assertThat(results).hasSize(1);
	}

	@Test
	public void testGetAllStudentsUsingNamedQueryFromThirdRecord() {
		List<?> results = jpaOperations.getResultListForNamedQuery("selectAllStudents", null, 2, 0);
		assertThat(results).hasSize(1);
	}

	@Test
	public void testGetAllStudentsUsingJPAQueryFromThirdRecord() {
		String query = "select s from Student s";
		List<?> results = jpaOperations.getResultListForQuery(query, null, 2, 0);
		assertThat(results).hasSize(1);
	}

	@Test
	public void testWithNegativeMaxNumberOfResults() {
		String query = "select s from Student s";
		List<?> results = jpaOperations.getResultListForQuery(query, null, 0, -1);
		assertThat(results).hasSize(3);
	}

}
