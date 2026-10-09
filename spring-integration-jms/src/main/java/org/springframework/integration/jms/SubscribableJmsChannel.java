/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import org.springframework.integration.channel.BroadcastCapableChannel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;

/**
 * An {@link org.springframework.integration.jms.channel.AbstractJmsChannel} implementation
 * for message-driven subscriptions.
 * Also implements a {@link BroadcastCapableChannel} to represent possible pub-sub semantics
 * when configured against JMS topic.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jms.channel.SubscribableJmsChannel}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class SubscribableJmsChannel extends org.springframework.integration.jms.channel.SubscribableJmsChannel {

	public SubscribableJmsChannel(AbstractMessageListenerContainer container, JmsTemplate jmsTemplate) {
		super(container, jmsTemplate);
	}

}
