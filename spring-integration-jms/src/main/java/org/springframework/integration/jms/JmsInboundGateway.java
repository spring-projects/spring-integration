/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.jms;

import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.jms.listener.AbstractMessageListenerContainer;

/**
 * A wrapper around the {@link JmsMessageDrivenEndpoint} implementing
 * {@link MessagingGatewaySupport}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jms.inbound.JmsInboundGateway}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class JmsInboundGateway extends org.springframework.integration.jms.inbound.JmsInboundGateway {

	public JmsInboundGateway(AbstractMessageListenerContainer listenerContainer,
			org.springframework.integration.jms.inbound.ChannelPublishingJmsMessageListener listener) {

		super(listenerContainer, listener);
	}

}
