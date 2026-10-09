/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.jms.core.JmsTemplate;

/**
 * A base {@link AbstractMessageChannel} implementation for JMS-backed message channels.
 *
 * @author Mark Fisher
 * @author Gary Russell
 *
 * @since 2.0
 *
 * @see org.springframework.integration.jms.channel.PollableJmsChannel
 * @see org.springframework.integration.jms.channel.SubscribableJmsChannel
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jms.channel.AbstractJmsChannel}
 */
@Deprecated(forRemoval = true, since = "7.0")
public abstract class AbstractJmsChannel extends org.springframework.integration.jms.channel.AbstractJmsChannel {

	public AbstractJmsChannel(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}

}
