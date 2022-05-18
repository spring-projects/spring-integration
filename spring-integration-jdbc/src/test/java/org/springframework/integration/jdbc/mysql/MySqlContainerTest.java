/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.integration.jdbc.mysql;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.integration.test.util.TestUtils;

/**
 * The base contract for JUnit tests based on the container for MqSQL.
 *
 * @author Artem Bilan
 *
 * @since 5.5.7
 */
@Testcontainers(disabledWithoutDocker = true)
public interface MySqlContainerTest {

	@Container
	MySQLContainer<?> MY_SQL_CONTAINER = new MySQLContainer<>("mysql").withReuse(true);


	static String getDriverClassName() {
		return MY_SQL_CONTAINER.getDriverClassName();
	}

	static String getJdbcUrl() {
		return MY_SQL_CONTAINER.getJdbcUrl();
	}

	static String getUsername() {
		return MY_SQL_CONTAINER.getUsername();
	}

	static String getPassword() {
		return MY_SQL_CONTAINER.getPassword();
	}

}
