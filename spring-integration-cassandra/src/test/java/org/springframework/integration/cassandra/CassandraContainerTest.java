/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.cassandra;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The base contract for JUnit tests based on the container for Apache Cassandra.
 * The Testcontainers 'reuse' option must be disabled,so, Ryuk container is started
 * and will clean all the containers up from this test suite after JVM exit.
 * Since the MqSQL container instance is shared via static property, it is going to be
 * started only once per JVM, therefore the target Docker container is reused automatically.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
@Testcontainers(disabledWithoutDocker = true)
public interface CassandraContainerTest {

	CassandraContainer<?> CASSANDRA_CONTAINER = new CassandraContainer<>("cassandra:4.1");

	@BeforeAll
	static void startContainer() {
		System.setProperty("datastax-java-driver.basic.request.timeout", "10 seconds");
		CASSANDRA_CONTAINER.start();
	}

}
