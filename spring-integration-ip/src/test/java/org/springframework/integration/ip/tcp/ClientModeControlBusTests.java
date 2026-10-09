/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.inbound.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Gary Russell
 * @author Glenn Renfro
 *
 * @since 2.1
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class ClientModeControlBusTests {

	@Autowired
	ControlBus controlBus;

	@Autowired
	TcpReceivingChannelAdapter tcpIn;

	@Autowired
	AbstractServerConnectionFactory server;

	@Autowired
	AbstractClientConnectionFactory client;

	@Autowired
	TaskScheduler taskScheduler; // default

	@BeforeEach
	public void before() {
		TestingUtilities.waitListening(this.server, null);
		this.client.setPort(this.server.getPort());
		this.tcpIn.start();
	}

	@Test
	public void test() throws Exception {
		assertThat(controlBus.boolResult("@tcpIn.isClientMode()")).isTrue();
		await("Connection never established").atMost(Duration.ofSeconds(10))
				.until(() -> controlBus.boolResult("@tcpIn.isClientModeConnected()"));
		assertThat(controlBus.boolResult("@tcpIn.isRunning()")).isTrue();
		assertThat(TestUtils.<Object>getPropertyValue(tcpIn, "taskScheduler")).isSameAs(taskScheduler);
		controlBus.voidResult("@tcpIn.retryConnection()");
	}

	public interface ControlBus {

		boolean boolResult(String command);

		void voidResult(String command);

	}

}
