/*
 * Copyright 2002-2012 the original author or authors.
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
import static org.junit.Assert.assertNotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.persistence.EntityManager;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jpa.support.parametersource.ExpressionEvaluatingParameterSourceFactory;
import org.springframework.integration.jpa.support.parametersource.ParameterSource;
import org.springframework.integration.jpa.support.parametersource.ParameterSourceFactory;
import org.springframework.integration.jpa.test.JpaTestUtils;
import org.springframework.integration.jpa.test.entity.Gender;
import org.springframework.integration.jpa.test.entity.Student;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * @author Gunnar Hillert
 * @since 2.2
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback=false)
@Transactional
public class JpaOperationsTests {

	@Autowired
	private PlatformTransactionManager transactionManager;
	
	@Autowired
	private EntityManager entityManager;
	

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#executeUpdate(java.lang.String, org.springframework.integration.jpa.core.JpaQLParameterSource)}.
	 */
	@Test
	public void testExecuteUpdate() {
		
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final Student student = JpaTestUtils.getTestStudent();
		
		List<?> students = jpaOperations.getResultListForClass(Student.class, 0, 0);
		Assert.assertTrue(students.size() == 2);
		
		ParameterSourceFactory requestParameterSourceFactory = new ExpressionEvaluatingParameterSourceFactory();
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);
		
		int updatedRecords = jpaOperations.executeUpdate("update Student s set s.lastName = :lastName, lastUpdated = :lastUpdated "
								 +  "where s.id in (select max(a.id) from Student a)", source);

		entityManager.flush();
		
		Assert.assertTrue( 1 == updatedRecords);
		Assert.assertNull(student.getRollNumber());
		
	}

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#executeUpdateWithNamedQuery(java.lang.String, org.springframework.integration.jpa.core.JpaQLParameterSource)}.
	 */
	@Test
	public void testExecuteUpdateWithNamedQuery() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final Student student = JpaTestUtils.getTestStudent();
		
		List<?> students = jpaOperations.getResultListForClass(Student.class, 0, 0);
		Assert.assertTrue(students.size() == 2);
		
		ParameterSourceFactory requestParameterSourceFactory = new ExpressionEvaluatingParameterSourceFactory();
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);
		
		int updatedRecords = jpaOperations.executeUpdateWithNamedQuery("updateStudent", source);

		entityManager.flush();
		
		Assert.assertTrue( 1 == updatedRecords);
		Assert.assertNull(student.getRollNumber());
	}

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#executeUpdateWithNativeQuery(java.lang.String, org.springframework.integration.jpa.core.JpaQLParameterSource)}.
	 */
	@Test
	public void testExecuteUpdateWithNativeQuery() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final Student student = JpaTestUtils.getTestStudent();
		
		List<?> students = jpaOperations.getResultListForClass(Student.class, 0, 0);
		Assert.assertTrue(students.size() == 2);
		
		ParameterSourceFactory requestParameterSourceFactory = new ExpressionEvaluatingParameterSourceFactory();
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);
				
		int updatedRecords = jpaOperations.executeUpdateWithNativeQuery("update Student s set s.lastName = :lastName, lastUpdated = :lastUpdated "
				 +  "where s.rollNumber in (select max(a.rollNumber) from Student a)", source);

		entityManager.flush();
		
		Assert.assertTrue( 1 == updatedRecords);
		Assert.assertNull(student.getRollNumber());
	}

	/**
	 * Test method for {@link DefaultJpaOperations#getResultListForNativeQuery(String, Class, JpaQLParameterSource, int, int)}.
	 * @throws ParseException 
	 */
	@Test
	public void testExecuteSelectWithNativeQueryReturningEntityClass() throws ParseException {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		String selectSqlQuery = "select * from Student where lastName = 'Last One'";
		
		Class<?> entityClass = Student.class;

		List<?> students = jpaOperations.getResultListForNativeQuery(selectSqlQuery, entityClass, null, 0, 0);
		
		Assert.assertTrue(students.size() == 1);
		
		Student retrievedStudent = (Student) students.iterator().next();

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
	@Test
	public void testExecuteSelectWithNativeQuery() throws ParseException {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		String selectSqlQuery = "select * from Student where lastName = 'Last One'";
		
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
	
	@Test
	public void testExecuteUpdateWithNativeNamedQuery() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final Student student = JpaTestUtils.getTestStudent();
		
		List<?> students = jpaOperations.getResultListForClass(Student.class, 0, 0);
		Assert.assertTrue(students.size() == 2);
		
		ParameterSourceFactory requestParameterSourceFactory = new ExpressionEvaluatingParameterSourceFactory();
		ParameterSource source = requestParameterSourceFactory.createParameterSource(student);
				
		int updatedRecords = jpaOperations.executeUpdateWithNamedQuery("updateStudentNativeQuery", source);

		entityManager.flush();
		
		Assert.assertTrue( 1 == updatedRecords);
		Assert.assertNull(student.getRollNumber());
	}
	
	
	
	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#merge(java.lang.Object)}.
	 */
	@Test
	public void testMerge() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final Student student = JpaTestUtils.getTestStudent();
		
		Assert.assertNull(student.getRollNumber());
		final Student savedStudent = (Student) jpaOperations.merge(student);
		entityManager.flush();
		Assert.assertNull(student.getRollNumber());
		Assert.assertNotNull(savedStudent);
		Assert.assertNotNull(savedStudent.getRollNumber());
		
		Assert.assertTrue(student != savedStudent);
	}

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#persist(java.lang.Object)}.
	 */
	@Test
	public void testPersist() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final Student student = JpaTestUtils.getTestStudent();
		
		Assert.assertNull(student.getRollNumber());
		jpaOperations.persist(student);
		entityManager.flush();
		Assert.assertNotNull(student.getRollNumber());
	}

	@Test
	public void testDeleteInBatch() {
		final JpaOperations jpaOperations = getJpaOperations(entityManager);

		final List<Object> students = jpaOperations.findAll(Student.class);
		
		Assert.assertNotNull(students);
		
		
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		// explicitly setting the transaction name is something that can only be done programmatically
		def.setName("SomeOtherTxName");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		
		TransactionStatus status = transactionManager.getTransaction(def);
		
		jpaOperations.deleteInBatch(students);
//		for (Object object : students) {
//			jpaOperations.delete((Student) object);
//		}
		entityManager.flush();

		transactionManager.commit(status);
		
		final List<Object> studentsFromDb = jpaOperations.findAll(Student.class);

		Assert.assertNotNull(studentsFromDb);
		Assert.assertTrue(studentsFromDb.size() == 0);

	}
	
	private JpaOperations getJpaOperations(EntityManager entityManager) {
		
		final DefaultJpaOperations jpaOperationsImpl = new DefaultJpaOperations();
		jpaOperationsImpl.setEntityManager(entityManager);
		jpaOperationsImpl.afterPropertiesSet();
		
		return jpaOperationsImpl;
	}
	
}
