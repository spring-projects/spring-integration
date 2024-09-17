/*
 * Copyright 2022-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
