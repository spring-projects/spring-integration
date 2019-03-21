/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.jpa.outbound;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
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
 *
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
		assertNotNull(student);
	}

	@Test
	public void getAllStudentsStartingFromGivenRecord() {
		List<?> students = studentService.getAllStudentsFromGivenRecord(1);
		assertNotNull(students);
		assertEquals(2, students.size());
	}

	@Test
	public void getAllStudentsWithMaxNumberOfRecords() {
		List<?> students = studentService.getStudents(1);
		assertNotNull(students);
		assertEquals(1, students.size());
	}


	@Test
	public void deleteNonExistingStudent() {

		StudentDomain student = JpaTestUtils.getTestStudent();
		student.setRollNumber(3424234234L);

		try {
			studentService.deleteStudent(student);
			fail("IllegalArgumentException is expected");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), startsWith("Removing a detached instance"));
		}

	}

	@Test
	public void getStudentWithException() {
		try {
			studentService.getStudentWithException(1001L);
			fail("MessageHandlingException is expected");
		}
		catch (MessagingException e) {
			assertEquals("The Jpa operation returned more than 1 result for expectSingleResult mode.",
					e.getMessage());
		}
	}

	@Test
	public void getStudentStudentWithPositionalParameters() {

		StudentDomain student = studentService.getStudentWithParameters("First Two");

		assertEquals("First Two", student.getFirstName());
		assertEquals("Last Two", student.getLastName());
	}

	@Test
	public void getAllStudents() {
		List<StudentDomain> students = studentService.getAllStudents();
		assertNotNull(students);
		assertEquals(3, students.size());
	}

	@Test
	@Transactional
	public void persistStudent() {

		final StudentDomain studentToPersist = JpaTestUtils.getTestStudent();
		assertNull(studentToPersist.getRollNumber());

		final StudentDomain persistedStudent = studentService.persistStudent(studentToPersist);
		assertNotNull(persistedStudent);
		assertNotNull(persistedStudent.getRollNumber());

	}

	@Test
	@Transactional
	public void persistStudentUsingMerge() {

		final StudentDomain studentToPersist = JpaTestUtils.getTestStudent();
		assertNull(studentToPersist.getRollNumber());

		final StudentDomain persistedStudent = studentService.persistStudentUsingMerge(studentToPersist);
		assertNotNull(persistedStudent);
		assertNotNull(persistedStudent.getRollNumber());

	}

	@Test
	public void testRetrievingGatewayInsideChain() {
		final StudentDomain student = studentService.getStudent2(1001L);
		assertNotNull(student);
	}

	@Test
	@Transactional
	public void testUpdatingGatewayInsideChain() {

		final StudentDomain studentToPersist = JpaTestUtils.getTestStudent();
		assertNull(studentToPersist.getRollNumber());

		final StudentDomain persistedStudent = studentService.persistStudent2(studentToPersist);
		assertNotNull(persistedStudent);
		assertNotNull(persistedStudent.getRollNumber());

	}

	@Test
	public void testJpaRepositoryAsService() {
		List<StudentDomain> students = this.studentService.getStudentsUsingJpaRepository("F");
		assertEquals(2, students.size());
	}

}
