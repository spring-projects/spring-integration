/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.jpa.outbound;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jpa.test.JpaTestUtils;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.MessagingException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Amol Nayak
 * @since 2.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
@DirtiesContext
public class JpaOutboundGatewayTests {

	@Autowired
	private StudentService studentService;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@After
	public void cleanUp() {
		this.jdbcTemplate.execute("delete from Student where rollNumber > 1003");
	}

	@Test
	public void getStudent() {
		final StudentDomain student = studentService.getStudent(1001L);
		Assert.assertNotNull(student);
	}

	@Test
	public void getAllStudentsStartingFromGivenRecord() {
		List<?> students = studentService.getAllStudentsFromGivenRecord(1);
		Assert.assertNotNull(students);
		Assert.assertEquals(2, students.size());
	}

	@Test
	public void getAllStudentsWithMaxNumberOfRecords() {
		List<?> students = studentService.getStudents(1);
		Assert.assertNotNull(students);
		Assert.assertEquals(1, students.size());
	}


	@Test
	public void deleteNonExistingStudent() {

		StudentDomain student = JpaTestUtils.getTestStudent();
		student.setRollNumber(3424234234L);

		try {
			studentService.deleteStudent(student);
		} catch (IllegalArgumentException e) {
			return;
		}

		Assert.fail("Was expecting a MessageHandlingException to be thrown.");
	}

	@Test
	public void getStudentWithException() {
		try {
			studentService.getStudentWithException(1001L);
		} catch (MessagingException e) {
			Assert.assertEquals("The Jpa operation returned more than 1 result object but expectSingleResult was 'true'.",
					e.getMessage());

			return;
		}

		Assert.fail("Was expecting a MessageHandlingException to be thrown.");
	}

	@Test
	public void getStudentStudentWithPositionalParameters() {

		StudentDomain student = studentService.getStudentWithParameters("First Two");

		Assert.assertEquals("First Two", student.getFirstName());
		Assert.assertEquals("Last Two", student.getLastName());
	}

	@Test
	public void getAllStudents() {

		final List<StudentDomain> students = studentService.getAllStudents();
		Assert.assertNotNull(students);
		Assert.assertTrue(students.size() == 3);

	}

	@Test
	@Transactional
	public void persistStudent() {

		final StudentDomain studentToPersist = JpaTestUtils.getTestStudent();
		Assert.assertNull(studentToPersist.getRollNumber());

		final StudentDomain persistedStudent = studentService.persistStudent(studentToPersist);
		Assert.assertNotNull(persistedStudent);
		Assert.assertNotNull(persistedStudent.getRollNumber());

	}

	@Test
	@Transactional
	public void persistStudentUsingMerge() {

		final StudentDomain studentToPersist = JpaTestUtils.getTestStudent();
		Assert.assertNull(studentToPersist.getRollNumber());

		final StudentDomain persistedStudent = studentService.persistStudentUsingMerge(studentToPersist);
		Assert.assertNotNull(persistedStudent);
		Assert.assertNotNull(persistedStudent.getRollNumber());

	}

	@Test
	public void testRetrievingGatewayInsideChain() {
		final StudentDomain student = studentService.getStudent2(1001L);
		Assert.assertNotNull(student);
	}

	@Test
	@Transactional
	public void testUpdatingGatewayInsideChain() {

		final StudentDomain studentToPersist = JpaTestUtils.getTestStudent();
		Assert.assertNull(studentToPersist.getRollNumber());

		final StudentDomain persistedStudent = studentService.persistStudent2(studentToPersist);
		Assert.assertNotNull(persistedStudent);
		Assert.assertNotNull(persistedStudent.getRollNumber());

	}

}
