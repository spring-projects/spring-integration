/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
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
