/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.mqtt.client;

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
