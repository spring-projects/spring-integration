package org.springframework.integration.jpa.outbound;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.jpa.test.JpaTestUtils;
import org.springframework.integration.jpa.test.entity.Student;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=true)
public class JpaOutboundGatewayTests {
    
	@Autowired
	private StudentService studentService;
	
	@Test
	@DirtiesContext
	public void getStudent() {
		final Student student = studentService.getStudent(1L);
		Assert.assertNotNull(student);	
	}
	
	@Test
	@DirtiesContext
	@Transactional
	public void deleteNonExistingStudent() {
		
		Student student = JpaTestUtils.getTestStudent();
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
			studentService.getStudentWithException(1L);
		} catch (MessageHandlingException e) {
			Assert.assertEquals("The Jpa operation returned more than 1 result object but expectSingleResult was 'true'.", 
					e.getMessage());
			
			return;
		}
		
		Assert.fail("Was expecting a MessageHandlingException to be thrown.");
	}
	
	@Test
	@DirtiesContext
	public void getStudentStudentWithPositionalParameters() {

		Student student = studentService.getStudentWithParameters("First Two");

		Assert.assertEquals("First Two", student.getFirstName());
		Assert.assertEquals("Last Two", student.getLastName());
	}
	
	@Test
	@DirtiesContext
	public void getAllStudents() {

		final List<Student> students = studentService.getAllStudents();
		Assert.assertNotNull(students);
		Assert.assertTrue(students.size() == 2);
		
	}
		
	@Test
	@DirtiesContext
	@Transactional
	public void persistStudent() {

		final Student studentToPersist = JpaTestUtils.getTestStudent();
		Assert.assertNull(studentToPersist.getRollNumber());
		
		final Student persistedStudent = studentService.persistStudent(studentToPersist);
		Assert.assertNotNull(persistedStudent);
		Assert.assertNotNull(persistedStudent.getRollNumber());
		
	}
	
	@Test
	@DirtiesContext
	@Transactional
	public void persistStudentUsingMerge() {

		final Student studentToPersist = JpaTestUtils.getTestStudent();
		Assert.assertNull(studentToPersist.getRollNumber());
		
		final Student persistedStudent = studentService.persistStudentUsingMerge(studentToPersist);
		Assert.assertNotNull(persistedStudent);
		Assert.assertNotNull(persistedStudent.getRollNumber());
		
	}
	
}