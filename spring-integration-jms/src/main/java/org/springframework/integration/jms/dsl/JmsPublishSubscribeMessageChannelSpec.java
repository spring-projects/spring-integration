/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jms.dsl;

import javax.jms.ConnectionFactory;

import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * A {@link JmsMessageChannelSpec} for a {@link org.springframework.integration.jms.SubscribableJmsChannel}
 * configured with a topic.
 *
 * @author Artem Bilan
 * @since 5.0
 */
public class JmsPublishSubscribeMessageChannelSpec
		extends JmsMessageChannelSpec<JmsPublishSubscribeMessageChannelSpec> {

	JmsPublishSubscribeMessageChannelSpec(ConnectionFactory connectionFactory) {
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
	 * Only applies if the {@link #containerType(Class)} is a {@link DefaultMessageListenerContainer}
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
