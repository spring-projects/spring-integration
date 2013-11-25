/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.jpa.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Assert;

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

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#executeUpdate(java.lang.String, org.springframework.integration.jpa.core.JpaQLParameterSource)}.
	 */
	public void testGetAllStudents() {

		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final List<?> students = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);
		Assert.assertTrue(students.size() == 3);

	}

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#executeUpdate(java.lang.String, org.springframework.integration.jpa.core.JpaQLParameterSource)}.
	 */
	public void testGetAllStudentsWithMaxResults() {

		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final List<?> students = jpaOperations.getResultListForClass(StudentDomain.class, 0, 2);
		Assert.assertTrue(String.format("Was expecting 2 Students to be returned but got '%s'.", students.size()),
						  students.size() == 2);

	}

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#executeUpdate(java.lang.String, org.springframework.integration.jpa.core.JpaQLParameterSource)}.
	 */
	public void testExecuteUpdate() {

		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final StudentDomain student = JpaTestUtils.getTestStudent();

		List<?> students = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);
		Assert.assertTrue(students.size() == 3);

		ParameterSourceFactory requestParameterSourceFactory =
				new ExpressionEvaluatingParameterSourceFactory(mock(BeanFactory.class));
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);

		int updatedRecords = jpaOperations.executeUpdate("update Student s set s.lastName = :lastName, s.lastUpdated = :lastUpdated "
								+  "where s.rollNumber in (select max(a.rollNumber) from Student a)", source);

		entityManager.flush();

		Assert.assertTrue( 1 == updatedRecords);
		Assert.assertNull(student.getRollNumber());

	}

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#executeUpdateWithNamedQuery(java.lang.String, org.springframework.integration.jpa.core.JpaQLParameterSource)}.
	 */
	public void testExecuteUpdateWithNamedQuery() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final StudentDomain student = JpaTestUtils.getTestStudent();

		ParameterSourceFactory requestParameterSourceFactory =
				new ExpressionEvaluatingParameterSourceFactory(mock(BeanFactory.class));
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);

		int updatedRecords = jpaOperations.executeUpdateWithNamedQuery("updateStudent", source);

		entityManager.flush();

		Assert.assertTrue( 1 == updatedRecords);
		Assert.assertNull(student.getRollNumber());
	}

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#executeUpdateWithNativeQuery(java.lang.String, org.springframework.integration.jpa.core.JpaQLParameterSource)}.
	 */
	public void testExecuteUpdateWithNativeQuery() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final StudentDomain student = JpaTestUtils.getTestStudent();

		ExpressionEvaluatingParameterSourceFactory requestParameterSourceFactory =
				new ExpressionEvaluatingParameterSourceFactory(mock(BeanFactory.class));
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);

		int updatedRecords = jpaOperations.executeUpdateWithNativeQuery("update Student set lastName = :lastName, lastUpdated = :lastUpdated "
				+  "where rollNumber in (select max(a.rollNumber) from Student a)", source);

		entityManager.flush();

		Assert.assertTrue( 1 == updatedRecords);
		Assert.assertNull(student.getRollNumber());
	}

	/**
	 * Test method for {@link DefaultJpaOperations#getResultListForNativeQuery(String, Class, JpaQLParameterSource, int, int)}.
	 * @throws ParseException
	 */
	public void testExecuteSelectWithNativeQueryReturningEntityClass() throws ParseException {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		String selectSqlQuery = "select * from Student where lastName = 'Last One'";

		Class<?> entityClass = StudentDomain.class;

		List<?> students = jpaOperations.getResultListForNativeQuery(selectSqlQuery, entityClass, null, 0, 0);

		Assert.assertTrue(students.size() == 1);

		StudentDomain retrievedStudent = (StudentDomain) students.iterator().next();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

		assertEquals(formatter.parse("1980/01/01"), retrievedStudent.getDateOfBirth());
		assertEquals("First One", retrievedStudent.getFirstName());
		assertEquals(Gender.MALE, retrievedStudent.getGender());
		assertEquals("Last One", retrievedStudent.getLastName());
		assertNotNull(retrievedStudent.getLastUpdated());

	}

	/**
	 * Test method for {@link DefaultJpaOperations#getResultListForNativeQuery(String, Class, JpaQLParameterSource, int, int)}.
	 * @throws ParseException
	 */
	public void testExecuteSelectWithNativeQuery() throws ParseException {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		String selectSqlQuery = "select rollNumber, firstName, lastName, gender, dateOfBirth, lastUpdated from Student where lastName = 'Last One'";

		List<?> students = jpaOperations.getResultListForNativeQuery(selectSqlQuery, null, null, 0, 0);

		Assert.assertTrue(students.size() == 1);

		Object[] retrievedStudent = (Object[]) students.iterator().next();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

		assertNotNull(retrievedStudent[0]);
		assertEquals("First One", retrievedStudent[1]);
		assertEquals("Last One", retrievedStudent[2]);
		assertEquals("M", retrievedStudent[3]);
		assertEquals(formatter.parse("1980/01/01"), retrievedStudent[4]);
		assertNotNull(retrievedStudent[5]);

	}

	public void testExecuteUpdateWithNativeNamedQuery() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final StudentDomain student = JpaTestUtils.getTestStudent();

		ParameterSourceFactory requestParameterSourceFactory =
				new ExpressionEvaluatingParameterSourceFactory(mock(BeanFactory.class));
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);

		int updatedRecords = jpaOperations.executeUpdateWithNamedQuery("updateStudentNativeQuery", source);

		entityManager.flush();

		Assert.assertTrue( 1 == updatedRecords);
		Assert.assertNull(student.getRollNumber());
	}

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#merge(java.lang.Object)}.
	 */
	public void testMerge() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final StudentDomain student = JpaTestUtils.getTestStudent();

		Assert.assertNull(student.getRollNumber());
		final StudentDomain savedStudent = (StudentDomain) jpaOperations.merge(student);
		entityManager.flush();
		Assert.assertNull(student.getRollNumber());
		Assert.assertNotNull(savedStudent);
		Assert.assertNotNull(savedStudent.getRollNumber());

		Assert.assertTrue(student != savedStudent);
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

		Assert.assertNull(student1.getRollNumber());
		Assert.assertNull(student2.getRollNumber());
		Assert.assertNull(student3.getRollNumber());

		Object savedStudents = jpaOperations.merge(students, 10, true);

		Assert.assertTrue(savedStudents instanceof List<?>);

		@SuppressWarnings("unchecked")
		List<StudentDomain> savedStudentCollection = (List<StudentDomain>) savedStudents;

		Assert.assertNotNull(savedStudentCollection.get(0).getRollNumber());
		Assert.assertNotNull(savedStudentCollection.get(1).getRollNumber());
		Assert.assertNotNull(savedStudentCollection.get(2).getRollNumber());
	}

	public void testMergeNullCollection() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		try {
			jpaOperations.merge(null);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("The object to merge must not be null.", e.getMessage());
			return;
		}

		Assert.fail("Expected an IllegalArgumentException to be thrown.");

	}

	public void testMergeCollectionWithNullElement() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final List<?> studentsFromDbBeforeTest = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		Assert.assertEquals(3, studentsFromDbBeforeTest.size());

		final StudentDomain student1 = JpaTestUtils.getTestStudent();
		final StudentDomain student2 = null;
		final StudentDomain student3 = JpaTestUtils.getTestStudent();

		student1.setFirstName("Karl");
		student3.setFirstName("Wilhelm");

		List<StudentDomain> students = new ArrayList<StudentDomain>(3);

		students.add(student1);
		students.add(student2);
		students.add(student3);

		Assert.assertNull(student1.getRollNumber());
		Assert.assertNull(student2);
		Assert.assertNull(student3.getRollNumber());

		Object savedStudents = jpaOperations.merge(students);
		entityManager.flush();

		Assert.assertTrue(savedStudents instanceof List<?>);

		@SuppressWarnings("unchecked")
		List<StudentDomain> savedStudentCollection = (List<StudentDomain>) savedStudents;

		Assert.assertNotNull(savedStudentCollection.get(0).getRollNumber());
		Assert.assertNotNull(savedStudentCollection.get(1).getRollNumber());

	}

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#persist(java.lang.Object)}.
	 */
	public void testPersist() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final StudentDomain student = JpaTestUtils.getTestStudent();

		Assert.assertNull(student.getRollNumber());
		jpaOperations.persist(student, 1, false);
		Assert.assertNotNull(student.getRollNumber());

		assertTrue(entityManager.contains(student));
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

		Assert.assertNull(student1.getRollNumber());
		Assert.assertNull(student2.getRollNumber());
		Assert.assertNull(student3.getRollNumber());

		jpaOperations.persist(students, 1, true);
		Assert.assertNotNull(student1.getRollNumber());
		Assert.assertNotNull(student2.getRollNumber());
		Assert.assertNotNull(student3.getRollNumber());

		assertFalse(entityManager.contains(student1));
		assertFalse(entityManager.contains(student2));
		assertFalse(entityManager.contains(student3));
	}

	public void testPersistNullCollection() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		try {
			jpaOperations.persist(null);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("The object to persist must not be null.", e.getMessage());
			return;
		}

		Assert.fail("Expected an IllegalArgumentException to be thrown.");

	}

	public void testPersistCollectionWithNullElement() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final List<?> studentsFromDbBeforeTest = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		Assert.assertEquals(3, studentsFromDbBeforeTest.size());

		final StudentDomain student1 = JpaTestUtils.getTestStudent();
		final StudentDomain student2 = null;
		final StudentDomain student3 = JpaTestUtils.getTestStudent();

		student1.setFirstName("Karl");
		student3.setFirstName("Wilhelm");

		List<StudentDomain> students = new ArrayList<StudentDomain>(3);

		students.add(student1);
		students.add(student2);
		students.add(student3);

		Assert.assertNull(student1.getRollNumber());
		Assert.assertNull(student2);
		Assert.assertNull(student3.getRollNumber());

		jpaOperations.persist(students, 10, false);

		Assert.assertNotNull(student1.getRollNumber());
		Assert.assertNotNull(student3.getRollNumber());

		final List<?> studentsFromDb = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		Assert.assertNotNull(studentsFromDb);
		Assert.assertEquals(5, studentsFromDb.size());
	}

	public void testDeleteInBatch() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final List<?> students = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		Assert.assertNotNull(students);


		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		// explicitly setting the transaction name is something that can only be done programmatically
		def.setName("SomeOtherTxName");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		TransactionStatus status = transactionManager.getTransaction(def);

		jpaOperations.deleteInBatch(students);

		entityManager.flush();

		transactionManager.commit(status);

		final List<?> studentsFromDb = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		Assert.assertNotNull(studentsFromDb);
		Assert.assertTrue(studentsFromDb.size() == 0);

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

		Assert.assertNotNull(studentsFromDb);
		Assert.assertTrue(studentsFromDb.size() == 3);


		final StudentDomain student = jpaOperations.find(StudentDomain.class, 1001L);

		Assert.assertNotNull(student);


		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		// explicitly setting the transaction name is something that can only be done programmatically
		def.setName("SomeOtherTxName");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		TransactionStatus status = transactionManager.getTransaction(def);

		jpaOperations.delete(student);

		entityManager.flush();

		transactionManager.commit(status);

		final List<?> studentsFromDbAfterDelete = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		Assert.assertNotNull(studentsFromDbAfterDelete);
		Assert.assertTrue(studentsFromDbAfterDelete.size() == 2);

	}

	public void testDeleteInBatchWithEmptyCollection() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final List<?> students = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		Assert.assertNotNull(students);
		Assert.assertTrue(students.size() == 3);

		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		// explicitly setting the transaction name is something that can only be done programmatically
		def.setName("SomeOtherTxName");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		TransactionStatus status = transactionManager.getTransaction(def);

		jpaOperations.deleteInBatch(new ArrayList<StudentDomain>(0));

		entityManager.flush();

		transactionManager.commit(status);

		final List<?> studentsFromDb = jpaOperations.getResultListForClass(StudentDomain.class, 0, 0);

		Assert.assertNotNull(studentsFromDb);
		Assert.assertTrue(studentsFromDb.size() == 3); //Nothing should have happened

	}

	public void testGetAllStudentsFromThirdRecord() {
		JpaOperations jpaOperations = getJpaOperations(entityManager);
		List<?> results = jpaOperations.getResultListForClass(StudentDomain.class, 2, 0);
		assertEquals(1, results.size());
	}


	public void testGetAllStudentsUsingNativeQueryFromThirdRecord() {
		JpaOperations jpaOperations = getJpaOperations(entityManager);
		String query = "select * from Student";
		List<?> results = jpaOperations.getResultListForNativeQuery(query, StudentDomain.class, null, 2, 0);
		assertEquals(1, results.size());
	}


	public void testGetAllStudentsUsingNamedQueryFromThirdRecord() {
		JpaOperations jpaOperations = getJpaOperations(entityManager);
		List<?> results = jpaOperations.getResultListForNamedQuery("selectAllStudents", null, 2, 0);
		assertEquals(1, results.size());
	}

	public void testGetAllStudentsUsingJPAQueryFromThirdRecord() {
		JpaOperations jpaOperations = getJpaOperations(entityManager);
		String query = "select s from Student s";
		List<?> results = jpaOperations.getResultListForQuery(query, null, 2, 0);
		assertEquals(1, results.size());
	}

	public void testWithNegativeMaxNumberofResults() {
		JpaOperations jpaOperations = getJpaOperations(entityManager);
		String query = "select s from Student s";
		List<?> results = jpaOperations.getResultListForQuery(query, null, 0, -1);
		assertEquals(3, results.size());
	}
}
