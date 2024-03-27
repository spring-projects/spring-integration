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

package org.springframework.integration.jpa.outbound;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jpa.test.JpaTestUtils;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.MessagingException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Amol Nayak
 *
 * @since 2.2
 */
@SpringJUnitConfig
@Transactional
@DirtiesContext
public class JpaOutboundGatewayTests {

	@Autowired
	private StudentService studentService;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@AfterEach
	public void cleanUp() {
		this.jdbcTemplate.execute("delete from Student where rollNumber > 1003");
	}

	@Test
	public void getStudent() {
		StudentDomain student = studentService.getStudent(1001L);
		assertThat(student).isNotNull();
	}

	@Test
	public void getAllStudentsStartingFromGivenRecord() {
		List<?> students = studentService.getAllStudentsFromGivenRecord(1);
		assertThat(students).isNotNull().hasSize(2);
	}

	@Test
	public void getAllStudentsWithMaxNumberOfRecords() {
		List<?> students = studentService.getStudents(1);
		assertThat(students).isNotNull().hasSize(1);
	}

	@Test
	public void deleteNonExistingStudent() {
		StudentDomain student = JpaTestUtils.getTestStudent();
		student.setRollNumber(3424234234L);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> studentService.deleteStudent(student))
				.withMessageStartingWith("Removing a detached instance");
	}

	@Test
	public void getStudentWithException() {
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> studentService.getStudentWithException(1001L))
				.withMessage("The Jpa operation returned more than 1 result for expectSingleResult mode.");
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
		assertThat(students).isNotNull().hasSize(3);
	}

	@Test
	@Transactional
	public void persistStudent() {
		StudentDomain studentToPersist = JpaTestUtils.getTestStudent();
		assertThat(studentToPersist.getRollNumber()).isNull();

		StudentDomain persistedStudent = studentService.persistStudent(studentToPersist);
		assertThat(persistedStudent).isNotNull();
		assertThat(persistedStudent.getRollNumber()).isNotNull();
	}

	@Test
	@Transactional
	public void persistStudentUsingMerge() {
		StudentDomain studentToPersist = JpaTestUtils.getTestStudent();
		assertThat(studentToPersist.getRollNumber()).isNull();

		StudentDomain persistedStudent = studentService.persistStudentUsingMerge(studentToPersist);
		assertThat(persistedStudent).isNotNull();
		assertThat(persistedStudent.getRollNumber()).isNotNull();

	}

	@Test
	public void testRetrievingGatewayInsideChain() {
		StudentDomain student = studentService.getStudent2(1001L);
		assertThat(student).isNotNull();
	}

	@Test
	@Transactional
	public void testUpdatingGatewayInsideChain() {
		StudentDomain studentToPersist = JpaTestUtils.getTestStudent();
		assertThat(studentToPersist.getRollNumber()).isNull();

		StudentDomain persistedStudent = studentService.persistStudent2(studentToPersist);
		assertThat(persistedStudent).isNotNull();
		assertThat(persistedStudent.getRollNumber()).isNotNull();

	}

	@Test
	public void testJpaRepositoryAsService() {
		List<StudentDomain> students = this.studentService.getStudentsUsingJpaRepository("F");
		assertThat(students).hasSize(2);
	}

	@Test
	@Transactional
	public void testDeleteMany() {
		List<StudentDomain> allStudents = this.studentService.getAllStudents();
		assertThat(allStudents).hasSize(3);
		this.studentService.deleteStudents(allStudents);
		assertThat(this.studentService.getAllStudents()).isEmpty();
	}

}
