/*
 * Copyright © 2021 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2021-present the original author or authors.
 */

package org.springframework.integration.mqtt;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The base contract for JUnit tests based on the container for MQTT Mosquitto broker.
 * The Testcontainers 'reuse' option must be disabled,so, Ryuk container is started
 * and will clean all the containers up from this test suite after JVM exit.
 * Since the Mosquitto container instance is shared via static property, it is going to be
 * started only once per JVM, therefore the target Docker container is reused automatically.
 *
 * @author Artem Bilan
 *
 * @since 5.5.5
 */
@Testcontainers(disabledWithoutDocker = true)
public interface MosquittoContainerTest {

	GenericContainer<?> MOSQUITTO_CONTAINER =
			new GenericContainer<>("eclipse-mosquitto:2.0.12")
					.withCommand("mosquitto -c /mosquitto-no-auth.conf")
					.withExposedPorts(1883);

	@BeforeAll
	static void startContainer() {
		MOSQUITTO_CONTAINER.start();
	}

	static String mqttUrl() {
		return "tcp://localhost:" + MOSQUITTO_CONTAINER.getFirstMappedPort();
	}

}
