/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.jdbc.postgres;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The base contract for JUnit tests based on the container for Postgres.
 * The Testcontainers 'reuse' option must be disabled, so, Ryuk container is started
 * and will clean all the containers up from this test suite after JVM exit.
 * Since the Postgres container instance is shared via static property, it is going to be
 * started only once per JVM, therefore the target Docker container is reused automatically.
 *
 * @author Artem Bilan
 * @author Rafael Winterhalter
 * @author Johannes Edmeier
 *
 * @since 6.0
 */
@Testcontainers(disabledWithoutDocker = true)
public interface PostgresContainerTest {

	PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:11")
			.withInitScript("org/springframework/integration/jdbc/schema-postgresql.sql");

	@BeforeAll
	static void startContainer() {
		POSTGRES_CONTAINER.start();
	}

	static String getDriverClassName() {
		return POSTGRES_CONTAINER.getDriverClassName();
	}

	static String getJdbcUrl() {
		return POSTGRES_CONTAINER.getJdbcUrl();
	}

	static String getUsername() {
		return POSTGRES_CONTAINER.getUsername();
	}

	static String getPassword() {
		return POSTGRES_CONTAINER.getPassword();
	}

}
