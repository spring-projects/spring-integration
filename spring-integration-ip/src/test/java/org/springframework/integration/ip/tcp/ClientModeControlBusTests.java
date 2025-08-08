/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp;

import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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

	@Before
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
		assertThat(TestUtils.getPropertyValue(tcpIn, "taskScheduler")).isSameAs(taskScheduler);
		controlBus.voidResult("@tcpIn.retryConnection()");
	}

	public interface ControlBus {

		boolean boolResult(String command);

		void voidResult(String command);

	}

}
