/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ip.dsl;

import java.util.function.Function;

import org.springframework.integration.ip.udp.outbound.UnicastSendingMessageHandler;
import org.springframework.messaging.Message;

/**
 * A {@link org.springframework.integration.dsl.MessageHandlerSpec} for
 * {@link UnicastSendingMessageHandler}s.
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
