package org.springframework.integration.jpa.outbound;

import java.util.List;

import org.springframework.integration.annotation.Payload;
import org.springframework.integration.jpa.test.entity.Student;

public interface StudentService {

	Student getStudent(Student student);
	
	Student getStudentWithException(Long id);
	
 	Student getStudent(Long id);
	Student deleteStudent(Student student);
	
	@Payload("new java.util.Date()")
	List<Student> getAllStudents();

	Student persistStudent(Student student);

	Student persistStudentUsingMerge(Student studentToPersist);
	
	Student getStudentWithParameters(String firstName);
	
}
