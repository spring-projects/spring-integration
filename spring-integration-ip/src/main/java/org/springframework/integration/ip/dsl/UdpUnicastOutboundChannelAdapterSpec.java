/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.ip.dsl;

import java.util.function.Function;

import org.springframework.messaging.Message;

/**
 * A {@link org.springframework.integration.dsl.MessageHandlerSpec} for
 * {@link org.springframework.integration.ip.udp.UnicastSendingMessageHandler}s.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class UdpUnicastOutboundChannelAdapterSpec
		extends AbstractUdpOutboundChannelAdapterSpec<UdpUnicastOutboundChannelAdapterSpec> {

	protected UdpUnicastOutboundChannelAdapterSpec(String host, int port) {
		super(host, port);
	}

	protected UdpUnicastOutboundChannelAdapterSpec(Function<Message<?>, ?> destinationFunction) {
		super(destinationFunction);
	}

	protected UdpUnicastOutboundChannelAdapterSpec(String destinationExpression) {
		super(destinationExpression);
	}

}
