/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * An "artificial" {@link MessageChannel} implementation which will be unwrapped to the
 * {@link MessageChannel} bean on the bean registration phase.
 * For internal use only.
 *
 * @param name the name of the target {@link MessageChannel} bean.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see org.springframework.integration.dsl.context.IntegrationFlowBeanPostProcessor
 */
public record MessageChannelReference(String name) implements MessageChannel {

	public MessageChannelReference {
		Assert.notNull(name, "'name' must not be null");
	}

	@Override
	public boolean send(Message<?> message) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		throw new UnsupportedOperationException();
	}

}
