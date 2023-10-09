/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.integration.jdbc.oracle;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The base contract for JUnit tests based on the container for Oracle.
 * The Testcontainers 'reuse' option must be disabled,so, Ryuk container is started
 * and will clean all the containers up from this test suite after JVM exit.
 * Since the Oracle container instance is shared via static property, it is going to be
 * started only once per JVM, therefore the target Docker container is reused automatically.
 *
 * @author Artem Bilan
 *
 * @since 6.0.8
 */
@Testcontainers(disabledWithoutDocker = true)
public interface OracleContainerTest {

	OracleContainer ORACLE_CONTAINER =
			new OracleContainer(DockerImageName.parse("gvenzl/oracle-xe:21-slim-faststart"))
					.withInitScript("org/springframework/integration/jdbc/schema-oracle.sql");

	@BeforeAll
	static void startContainer() {
		ORACLE_CONTAINER.start();
	}

	static DataSource dataSource() {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(ORACLE_CONTAINER.getDriverClassName());
		dataSource.setUrl(ORACLE_CONTAINER.getJdbcUrl());
		dataSource.setUsername(ORACLE_CONTAINER.getUsername());
		dataSource.setPassword(ORACLE_CONTAINER.getPassword());
		return dataSource;
	}

}
