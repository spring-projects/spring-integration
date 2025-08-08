/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.support.management.observation;

import io.micrometer.observation.transport.RequestReplyReceiverContext;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * The {@link RequestReplyReceiverContext} extension for a {@link Message} contract with inbound gateways.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class MessageRequestReplyReceiverContext extends RequestReplyReceiverContext<Message<?>, Message<?>> {

	private final Message<?> message;

	private final String gatewayName;

	public MessageRequestReplyReceiverContext(Message<?> message, @Nullable String gatewayName) {
		super((carrier, key) -> carrier.getHeaders().get(key, String.class));
		this.message = message;
		this.gatewayName = gatewayName != null ? gatewayName : "unknown";
	}

	@Override
	public Message<?> getCarrier() {
		return this.message;
	}

	public String getGatewayName() {
		return this.gatewayName;
	}

}
