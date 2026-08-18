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

package com.springframework.integration.mqtt.client;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

/**
 * The base contract for JUnit tests based on the container for Proxy.
 * The Testcontainers 'reuse' option must be disabled,so, Ryuk container is started
 * and will clean all the containers up from this test suite after JVM exit.
 * Since the Toxiproxy container instance is shared via static property, it is going to be
 * started only once per JVM, therefore the target Docker container is reused automatically.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
@Testcontainers(disabledWithoutDocker = true)
public interface ToxiproxyContainerTest {

	Network NETWORK = Network.newNetwork();

	ToxiproxyContainer PROXY_CONTAINER = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.12.0")
			.withNetwork(NETWORK);

	int PROXY_PORT_FOR_HIVEMQ = 8666;

	@BeforeAll
	static void startContainer() {
		PROXY_CONTAINER.start();
	}

}
