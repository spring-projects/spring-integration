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
package org.springframework.integration.jpa.outbound;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.MessagingException;
import org.springframework.integration.jpa.test.JpaTestUtils;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=true)
public class JpaOutboundGatewayTests {

	@Autowired
	private StudentService studentService;

	@Test
	@DirtiesContext
	public void getStudent() {
		final StudentDomain student = studentService.getStudent(1001L);
		Assert.assertNotNull(student);
	}

	@Test
	@DirtiesContext
	@Transactional
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
	@DirtiesContext
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
	@DirtiesContext
	public void getStudentStudentWithPositionalParameters() {

		StudentDomain student = studentService.getStudentWithParameters("First Two");

		Assert.assertEquals("First Two", student.getFirstName());
		Assert.assertEquals("Last Two", student.getLastName());
	}

	@Test
	@DirtiesContext
	public void getAllStudents() {

		final List<StudentDomain> students = studentService.getAllStudents();
		Assert.assertNotNull(students);
		Assert.assertTrue(students.size() == 3);

	}

	@Test
	@DirtiesContext
	@Transactional
	public void persistStudent() {

		final StudentDomain studentToPersist = JpaTestUtils.getTestStudent();
		Assert.assertNull(studentToPersist.getRollNumber());

		final StudentDomain persistedStudent = studentService.persistStudent(studentToPersist);
		Assert.assertNotNull(persistedStudent);
		Assert.assertNotNull(persistedStudent.getRollNumber());

	}

	@Test
	@DirtiesContext
	@Transactional
	public void persistStudentUsingMerge() {

		final StudentDomain studentToPersist = JpaTestUtils.getTestStudent();
		Assert.assertNull(studentToPersist.getRollNumber());

		final StudentDomain persistedStudent = studentService.persistStudentUsingMerge(studentToPersist);
		Assert.assertNotNull(persistedStudent);
		Assert.assertNotNull(persistedStudent.getRollNumber());

	}

}
