/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

/**
 * The base contract for JUnit tests based on the container for Proxy.
 * The Testcontainers 'reuse' option must be disabled, so, Ryuk container is started
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

	int PROXY_PORT_FOR_MQTT = 8666;

	@BeforeAll
	static void startContainer() {
		PROXY_CONTAINER.start();
	}

}
