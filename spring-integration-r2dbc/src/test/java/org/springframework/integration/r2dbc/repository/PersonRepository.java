/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.r2dbc.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.integration.r2dbc.entity.Person;

/**
 *  @author Rohan Mukesh
 *
 *  @since 5.4
 */
public interface PersonRepository extends ReactiveCrudRepository<Person, Integer> {

}
