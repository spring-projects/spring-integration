/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.integration.debezium.snapshots;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Christian Tzolov
 */
@Testcontainers(disabledWithoutDocker = true)
public interface PostgresTestContainer {

	@Container
	GenericContainer<?> container = new GenericContainer<>(
			new ImageFromDockerfile()
					.withFileFromClasspath("inventory.sql", "/docker/postgres/inventory.sql")
					.withFileFromClasspath("Dockerfile", "/docker/postgres/Dockerfile"))
							.withEnv("POSTGRES_USER", "postgres")
							.withEnv("POSTGRES_PASSWORD", "postgres")
							.withExposedPorts(5432)
							.waitingFor(new LogMessageWaitStrategy()
									.withRegEx(".*database system is ready to accept connections*."));

	static int mappedPort() {
		return container.getMappedPort(5432);
	}

}
