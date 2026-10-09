/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.pulsar;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The base contract for JUnit tests based on the container for the Apache Pulsar broker.
 * The Testcontainers 'reuse' option must be disabled, so, Ryuk container is started
 * and will clean all the containers up from this test suite after JVM exit.
 * Since the Pulsar container instance is shared via static property, it is going to be
 * started only once per JVM, therefore the target Docker container is reused automatically.
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 */
@Testcontainers(disabledWithoutDocker = true)
public interface PulsarContainerTest {

	PulsarContainer PULSAR_CONTAINER = new PulsarContainer(DockerImageName.parse("apachepulsar/pulsar:4.2.5"))
			.withEnv("PULSAR_MEM", "-Xms256m -Xmx512m -XX:MaxDirectMemorySize=256m");

	@BeforeAll
	static void startContainer() {
		PULSAR_CONTAINER.start();
	}

}
