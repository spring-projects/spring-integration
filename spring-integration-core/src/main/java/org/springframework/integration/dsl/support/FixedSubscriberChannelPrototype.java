/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl.support;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * An "artificial" {@link MessageChannel} implementation which will be unwrapped to the
 * {@link org.springframework.integration.channel.FixedSubscriberChannel} on the bean
 * registration phase.
 * For internal use only.
 *
 * @author Artem Bilan
 * @since 5.0
 *
 * @see org.springframework.integration.dsl.context.IntegrationFlowBeanPostProcessor
 */
public class FixedSubscriberChannelPrototype implements MessageChannel {

	private final String name;

	public FixedSubscriberChannelPrototype() {
		this(null);
	}

	public FixedSubscriberChannelPrototype(@Nullable String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public boolean send(Message<?> message) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "FixedSubscriberChannelPrototype{" +
				"name='" + this.name + '\'' +
				'}';
	}

}
