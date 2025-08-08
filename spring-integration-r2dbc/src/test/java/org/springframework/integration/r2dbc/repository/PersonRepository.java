/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
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
