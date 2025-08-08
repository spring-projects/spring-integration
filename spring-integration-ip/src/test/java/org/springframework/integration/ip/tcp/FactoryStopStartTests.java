/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp;

import org.junit.Test;

import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;

/**
 * @author Gary Russell
 * @since 2.0.4
 *
 */
public class FactoryStopStartTests {

	@Test
	public void testRestart() {
		AbstractServerConnectionFactory factory = new TcpNetServerConnectionFactory(0);
		factory.setSoTimeout(10000);
		factory.start();
		factory.stop();
		factory.start();
		factory.stop();
	}

	public static void main(String[] args) {
		new FactoryStopStartTests().testRestart();
	}

}
