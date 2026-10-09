/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import org.springframework.jms.listener.AbstractMessageListenerContainer;

/**
 * A message-driven endpoint that receive JMS messages, converts them into
 * Spring Integration Messages, and then sends the result to a channel.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jms.inbound.JmsMessageDrivenEndpoint}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class JmsMessageDrivenEndpoint extends org.springframework.integration.jms.inbound.JmsMessageDrivenEndpoint {

	/**
	 * Construct an instance with an externally configured container.
	 * @param listenerContainer the container.
	 * @param listener the listener.
	 */
	public JmsMessageDrivenEndpoint(AbstractMessageListenerContainer listenerContainer,
			org.springframework.integration.jms.inbound.ChannelPublishingJmsMessageListener listener) {

		super(listenerContainer, listener);
	}

}
