/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.jpa.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
		assertThat(student).isNotNull();
	}

	@Test
	public void getAllStudentsStartingFromGivenRecord() {
		List<?> students = studentService.getAllStudentsFromGivenRecord(1);
		assertThat(students).isNotNull();
		assertThat(students.size()).isEqualTo(2);
	}

	@Test
	public void getAllStudentsWithMaxNumberOfRecords() {
		List<?> students = studentService.getStudents(1);
		assertThat(students).isNotNull();
		assertThat(students.size()).isEqualTo(1);
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
			assertThat(e.getMessage()).startsWith("Removing a detached instance");
		}

	}

	@Test
	public void getStudentWithException() {
		try {
			studentService.getStudentWithException(1001L);
			fail("MessageHandlingException is expected");
		}
		catch (MessagingException e) {
			assertThat(e.getMessage())
					.isEqualTo("The Jpa operation returned more than 1 result for expectSingleResult mode.");
		}
	}

	@Test
	public void getStudentStudentWithPositionalParameters() {

		StudentDomain student = studentService.getStudentWithParameters("First Two");

		assertThat(student.getFirstName()).isEqualTo("First Two");
		assertThat(student.getLastName()).isEqualTo("Last Two");
	}

	@Test
	public void getAllStudents() {
		List<StudentDomain> students = studentService.getAllStudents();
		assertThat(students).isNotNull();
		assertThat(students.size()).isEqualTo(3);
	}

	@Test
	@Transactional
	public void persistStudent() {

		final StudentDomain studentToPersist = JpaTestUtils.getTestStudent();
		assertThat(studentToPersist.getRollNumber()).isNull();

		final StudentDomain persistedStudent = studentService.persistStudent(studentToPersist);
		assertThat(persistedStudent).isNotNull();
		assertThat(persistedStudent.getRollNumber()).isNotNull();

	}

	@Test
	@Transactional
	public void persistStudentUsingMerge() {

		final StudentDomain studentToPersist = JpaTestUtils.getTestStudent();
		assertThat(studentToPersist.getRollNumber()).isNull();

		final StudentDomain persistedStudent = studentService.persistStudentUsingMerge(studentToPersist);
		assertThat(persistedStudent).isNotNull();
		assertThat(persistedStudent.getRollNumber()).isNotNull();

	}

	@Test
	public void testRetrievingGatewayInsideChain() {
		final StudentDomain student = studentService.getStudent2(1001L);
		assertThat(student).isNotNull();
	}

	@Test
	@Transactional
	public void testUpdatingGatewayInsideChain() {

		final StudentDomain studentToPersist = JpaTestUtils.getTestStudent();
		assertThat(studentToPersist.getRollNumber()).isNull();

		final StudentDomain persistedStudent = studentService.persistStudent2(studentToPersist);
		assertThat(persistedStudent).isNotNull();
		assertThat(persistedStudent.getRollNumber()).isNotNull();

	}

	@Test
	public void testJpaRepositoryAsService() {
		List<StudentDomain> students = this.studentService.getStudentsUsingJpaRepository("F");
		assertThat(students.size()).isEqualTo(2);
	}

}
