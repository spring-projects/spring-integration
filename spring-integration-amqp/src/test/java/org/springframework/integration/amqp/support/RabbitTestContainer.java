/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.amqp.support;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Provides a static {@link RabbitMQContainer} that can be shared across test classes.
 *
 * @author Chris Bono
 * @author Gary Russell
 * @author Artem Bilan
 */
@Testcontainers(disabledWithoutDocker = true)
public interface RabbitTestContainer {

	RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:management")
			.withExposedPorts(5672, 15672, 5552)
			.withStartupTimeout(Duration.ofMinutes(2));

	@BeforeAll
	static void startContainer() throws IOException, InterruptedException {
		RABBITMQ.start();
		RABBITMQ.execInContainer("rabbitmq-plugins", "enable", "rabbitmq_stream");
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
