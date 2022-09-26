/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.amqp.support;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Provides a static {@link RabbitMQContainer} that can be shared across test classes.
 *
 * @author Chris Bono
 * @author Gary Russell
 */
@Testcontainers(disabledWithoutDocker = true)
public interface RabbitTestContainer {

	RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:management")
			.withExposedPorts(5672, 15672, 5552)
			.withEnv("RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS", "-rabbitmq_stream advertised_host localhost")
			.withPluginsEnabled("rabbitmq_stream")
			.withStartupTimeout(Duration.ofMinutes(2));

	@BeforeAll
	static void startContainer() {
		RABBITMQ.start();
	}

	static int amqpPort() {
		return RABBITMQ.getMappedPort(5672);
	}

	static int managementPort() {
		return RABBITMQ.getMappedPort(15672);
	}

	static int streamPort() {
		return RABBITMQ.getMappedPort(5552);
	}

}
