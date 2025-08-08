/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import org.springframework.expression.MethodFilter;
import org.springframework.integration.expression.ControlBusMethodFilter;
import org.springframework.integration.handler.ExpressionCommandMessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.MessageHandler;

/**
 * FactoryBean for creating {@link MessageHandler} instances to handle a message as a SpEL expression.
 *
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ExpressionControlBusFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<MessageHandler> {

	private static final MethodFilter METHOD_FILTER = new ControlBusMethodFilter();

	private Long sendTimeout;

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	@Override
	protected MessageHandler createHandler() {
		ExpressionCommandMessageProcessor processor =
				new ExpressionCommandMessageProcessor(METHOD_FILTER, getBeanFactory());
		ServiceActivatingHandler handler = new ServiceActivatingHandler(processor);
		if (this.sendTimeout != null) {
			handler.setSendTimeout(this.sendTimeout);
		}
		return handler;
	}

}
