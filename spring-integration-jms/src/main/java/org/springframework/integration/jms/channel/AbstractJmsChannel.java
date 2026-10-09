/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.channel;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.jms.DynamicJmsTemplateProperties;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A base {@link AbstractMessageChannel} implementation for JMS-backed message channels.
 *
 * @author Mark Fisher
 * @author Gary Russell
 *
 * @since 7.0
 *
 * @see PollableJmsChannel
 * @see SubscribableJmsChannel
 */
public abstract class AbstractJmsChannel extends AbstractMessageChannel {

	private final JmsTemplate jmsTemplate;

	public AbstractJmsChannel(JmsTemplate jmsTemplate) {
		Assert.notNull(jmsTemplate, "jmsTemplate must not be null");
		this.jmsTemplate = jmsTemplate;
	}

	JmsTemplate getJmsTemplate() {
		return this.jmsTemplate;
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		try {
			DynamicJmsTemplateProperties.setPriority(new IntegrationMessageHeaderAccessor(message).getPriority());
			this.jmsTemplate.convertAndSend(message);
		}
		finally {
			DynamicJmsTemplateProperties.clearPriority();
		}
		return true;
	}

}
