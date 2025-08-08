/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.ip.tcp;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.HelloWorldInterceptorFactory;

/**
 * @author Gary Russell
 * @author Mário Dias
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class AbstractTcpChannelAdapterTests {

	private static final ApplicationEventPublisher NOOP_PUBLISHER = event -> {
	};

	protected HelloWorldInterceptorFactory newInterceptorFactory() {
		return newInterceptorFactory(NOOP_PUBLISHER);
	}

	protected HelloWorldInterceptorFactory newInterceptorFactory(ApplicationEventPublisher applicationEventPublisher) {
		HelloWorldInterceptorFactory factory = new HelloWorldInterceptorFactory();
		factory.setApplicationEventPublisher(applicationEventPublisher);
		return factory;
	}

	protected void noopPublisher(AbstractConnectionFactory connectionFactory) {
		connectionFactory.setApplicationEventPublisher(NOOP_PUBLISHER);
	}

}
