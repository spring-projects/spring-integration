/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.outbound;

import java.util.List;

import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * @author Amol Nayak
 * @author Artem Bilan
 *
 * @since 2.2
 */
public interface StudentService {

	StudentDomain getStudent(StudentDomain student);

	StudentDomain getStudentWithException(Long id);

	StudentDomain getStudent(Long id);

	StudentDomain deleteStudent(StudentDomain student);

	List<StudentDomain> deleteStudents(List<StudentDomain> students);

	@Payload("new java.util.Date()")
	List<StudentDomain> getAllStudents();

	List<StudentDomain> getAllStudentsFromGivenRecord(int recordNumber);

	StudentDomain persistStudent(StudentDomain student);

	StudentDomain persistStudentUsingMerge(StudentDomain studentToPersist);

	StudentDomain getStudentWithParameters(String firstName);

	StudentDomain getStudent2(Long id);

	StudentDomain persistStudent2(StudentDomain studentToPersist);

	List<StudentDomain> getStudents(int maxNumberOfRecords);

	List<StudentDomain> getStudentsUsingJpaRepository(String gender);

}
