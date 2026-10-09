/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.inbound.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class AutoStartTests {

	@Autowired
	AbstractServerConnectionFactory cfS1;

	@Autowired
	TcpReceivingChannelAdapter tcpNetIn;

	@Test
	public void testNetIn() throws Exception {
		DirectFieldAccessor dfa = new DirectFieldAccessor(cfS1);
		assertThat(dfa.getPropertyValue("serverSocket")).isNull();
		startAndStop();
		assertThat(dfa.getPropertyValue("serverSocket")).isNull();
		startAndStop();
		assertThat(dfa.getPropertyValue("serverSocket")).isNull();
	}

	/**
	 * @throws InterruptedException
	 */
	private void startAndStop() throws InterruptedException {
		tcpNetIn.start();
		TestingUtilities.waitListening(cfS1, null);
		tcpNetIn.stop();
		TestingUtilities.waitStopListening(cfS1, null);
	}

}
