/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The base contract for JUnit tests based on the container for MQTT Mosquitto broker.
 * The Testcontainers 'reuse' option must be disabled, so, Ryuk container is started
 * and will clean all the containers up from this test suite after JVM exit.
 * Since the Mosquitto container instance is shared via static property, it is going to be
 * started only once per JVM, therefore the target Docker container is reused automatically.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
@Testcontainers(disabledWithoutDocker = true)
public interface MqttContainerTest {

	int MQTT_PORT = 1883;

	GenericContainer<?> MQTT_CONTAINER = new GenericContainer<>("eclipse-mosquitto:2.1.2-alpine")
			.withCommand("mosquitto -c /mosquitto-no-auth.conf")
			.withExposedPorts(MQTT_PORT)
			.withNetwork(ToxiproxyContainerTest.NETWORK)
			.withNetworkAliases("mqtt-broker");

	@BeforeAll
	static void startContainer() {
		MQTT_CONTAINER.start();
	}

}
