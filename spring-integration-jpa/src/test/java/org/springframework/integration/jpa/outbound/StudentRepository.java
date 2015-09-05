package org.springframework.integration.jpa.outbound;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.stereotype.Repository;

/**
 * @author Artem Bilan
 * @since 4.2
 */
@Repository
public interface StudentRepository extends JpaRepository<StudentDomain, Long> {

	List<StudentDomain> findByGender(String gender);

}
