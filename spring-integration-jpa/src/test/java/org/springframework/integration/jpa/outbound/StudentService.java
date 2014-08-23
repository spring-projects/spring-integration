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

import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * @author Amol Nayak
 * @author Artem Bilan
 * @since 2.2
 */
public interface StudentService {

	StudentDomain getStudent(StudentDomain student);

	StudentDomain getStudentWithException(Long id);

	StudentDomain getStudent(Long id);
	StudentDomain deleteStudent(StudentDomain student);

	@SuppressWarnings("deprecation")
	@org.springframework.integration.annotation.Payload("new java.util.Date()")
	List<StudentDomain> getAllStudentsDeprecated();

	@Payload("new java.util.Date()")
	List<StudentDomain> getAllStudents();

	List<StudentDomain> getAllStudentsFromGivenRecord(int recordNumber);

	StudentDomain persistStudent(StudentDomain student);

	StudentDomain persistStudentUsingMerge(StudentDomain studentToPersist);

	StudentDomain getStudentWithParameters(String firstName);

	StudentDomain getStudent2(Long id);

	StudentDomain persistStudent2(StudentDomain studentToPersist);

	List<StudentDomain> getStudents(int maxNumberOfRecords);
}
