/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.jms.dsl;

import jakarta.jms.ConnectionFactory;

import org.springframework.integration.jms.SubscribableJmsChannel;

/**
 * A {@link JmsMessageChannelSpec} for a {@link org.springframework.integration.jms.SubscribableJmsChannel}
 * configured with a topic.
 *
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 5.0
 */
public class JmsPublishSubscribeMessageChannelSpec
		extends JmsMessageChannelSpec<JmsPublishSubscribeMessageChannelSpec, SubscribableJmsChannel> {

	protected JmsPublishSubscribeMessageChannelSpec(ConnectionFactory connectionFactory) {
		super(connectionFactory);
		this.jmsChannelFactoryBean.setPubSubDomain(true);
	}

	/**
	 * @param durable the durable.
	 * @return the current {@link JmsPublishSubscribeMessageChannelSpec}.
	 * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setSubscriptionDurable(boolean)
	 */
	public JmsPublishSubscribeMessageChannelSpec subscriptionDurable(boolean durable) {
		this.jmsChannelFactoryBean.setSubscriptionDurable(durable);
		return _this();
	}

	/**
	 * @param durableSubscriptionName the durableSubscriptionName.
	 * @return the current {@link JmsPublishSubscribeMessageChannelSpec}.
	 * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setDurableSubscriptionName(String)
	 */
	public JmsPublishSubscribeMessageChannelSpec durableSubscriptionName(String durableSubscriptionName) {
		this.jmsChannelFactoryBean.setDurableSubscriptionName(durableSubscriptionName);
		return _this();
	}

	/**
	 * @param clientId the clientId.
	 * @return the current {@link JmsPublishSubscribeMessageChannelSpec}.
	 * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setClientId(String)
	 */
	public JmsPublishSubscribeMessageChannelSpec clientId(String clientId) {
		this.jmsChannelFactoryBean.setClientId(clientId);
		return _this();
	}

	/**
	 * Only applies if the {@link #containerType(Class)} is a
	 * {@link org.springframework.jms.listener.DefaultMessageListenerContainer}
	 * or a {@link org.springframework.jms.listener.SimpleMessageListenerContainer}.
	 * @param pubSubNoLocal the pubSubNoLocal.
	 * @return the current {@link JmsPublishSubscribeMessageChannelSpec}.
	 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setPubSubNoLocal(boolean)
	 * @see org.springframework.jms.listener.SimpleMessageListenerContainer#setPubSubNoLocal(boolean)
	 */
	public JmsPublishSubscribeMessageChannelSpec pubSubNoLocal(boolean pubSubNoLocal) {
		this.jmsChannelFactoryBean.setPubSubNoLocal(pubSubNoLocal);
		return _this();
	}

}
