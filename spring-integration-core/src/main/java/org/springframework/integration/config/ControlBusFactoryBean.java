/*
 * Copyright 2024-present the original author or authors.
 */

package org.springframework.integration.config;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.handler.ControlBusMessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.MessageHandler;

/**
 * FactoryBean for creating {@link MessageHandler} instances to handle a message with a Control Bus command.
 *
 * @author Artem Bilan
 *
 * @since 6.4
 */
public class ControlBusFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<MessageHandler> {

	@Nullable
	private Long sendTimeout;

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	@Override
	protected MessageHandler createHandler() {
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new ControlBusMessageProcessor());
		if (this.sendTimeout != null) {
			handler.setSendTimeout(this.sendTimeout);
		}
		return handler;
	}

}
