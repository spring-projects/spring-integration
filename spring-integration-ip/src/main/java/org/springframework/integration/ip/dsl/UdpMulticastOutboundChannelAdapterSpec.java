/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.ip.dsl;

import java.util.function.Function;

import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.ip.udp.MulticastSendingMessageHandler;
import org.springframework.messaging.Message;

/**
 * A {@link org.springframework.integration.dsl.MessageHandlerSpec} for
 * {@link MulticastSendingMessageHandler}s.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class UdpMulticastOutboundChannelAdapterSpec
		extends AbstractUdpOutboundChannelAdapterSpec<UdpMulticastOutboundChannelAdapterSpec> {

	protected UdpMulticastOutboundChannelAdapterSpec(String host, int port) {
		this.target = new MulticastSendingMessageHandler(host, port);
	}

	protected UdpMulticastOutboundChannelAdapterSpec(String destinationExpression) {
		this.target = new MulticastSendingMessageHandler(destinationExpression);
	}

	protected UdpMulticastOutboundChannelAdapterSpec(Function<Message<?>, ?> destinationFunction) {
		this.target = new MulticastSendingMessageHandler(new FunctionExpression<>(destinationFunction));
	}

	/**
	 * @param timeToLive the timeToLive.
	 * @return the spec.
	 * @see MulticastSendingMessageHandler#setTimeToLive(int)
	 */
	public UdpMulticastOutboundChannelAdapterSpec timeToLive(int timeToLive) {
		((MulticastSendingMessageHandler) this.target).setTimeToLive(timeToLive);
		return _this();
	}

}
