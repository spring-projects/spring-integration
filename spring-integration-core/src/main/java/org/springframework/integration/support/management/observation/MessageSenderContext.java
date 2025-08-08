/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.support.management.observation;

import io.micrometer.observation.transport.SenderContext;

import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.Message;

/**
 * The {@link SenderContext} extension for {@link Message} context.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class MessageSenderContext extends SenderContext<MutableMessage<?>> {

	private final MutableMessage<?> message;

	private final String producerName;

	public MessageSenderContext(MutableMessage<?> message, String producerName) {
		super((carrier, key, value) -> carrier.getHeaders().put(key, value));
		this.message = message;
		this.producerName = producerName;
	}

	@Override
	public MutableMessage<?> getCarrier() {
		return this.message;
	}

	public String getProducerName() {
		return this.producerName;
	}

}
