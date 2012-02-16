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

import org.springframework.integration.annotation.Payload;
import org.springframework.integration.jpa.test.entity.StudentDomain;

public interface StudentService {

	StudentDomain getStudent(StudentDomain student);

	StudentDomain getStudentWithException(Long id);

	StudentDomain getStudent(Long id);
	StudentDomain deleteStudent(StudentDomain student);

	@Payload("new java.util.Date()")
	List<StudentDomain> getAllStudents();

	StudentDomain persistStudent(StudentDomain student);

	StudentDomain persistStudentUsingMerge(StudentDomain studentToPersist);

	StudentDomain getStudentWithParameters(String firstName);

}
