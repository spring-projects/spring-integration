/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageListener;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.gateway.SimpleMessageMapper;
import org.springframework.integration.message.InboundMessageMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * A {@link MessageChannel} implementation that is actually backed by a JMS
 * Destination. This class is useful as a drop-in replacement for any
 * Spring Integration channel. The benefit of using this channel is that
 * the full power of any JMS provider is available with only minimal
 * configuration changes and without requiring any code changes. The most
 * obvious benefit is the ability to delegate message persistence to the
 * JMS provider.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class JmsDestinationBackedMessageChannel extends MessageListenerContainerConfigurationSupport
		implements SubscribableChannel, MessageListener, BeanNameAware, SmartLifecycle, InitializingBean {

	private final JmsTemplate jmsTemplate = new JmsTemplate();

	private final InboundMessageMapper<Object> mapper = new SimpleMessageMapper();

	private volatile MessageDispatcher dispatcher;

	private volatile String name;


	public JmsDestinationBackedMessageChannel(ConnectionFactory connectionFactory, Destination destination) {
		this.setConnectionFactory(connectionFactory);
		this.setDestination(destination);
	}

	public JmsDestinationBackedMessageChannel(ConnectionFactory connectionFactory, String destinationName, boolean isPubSub) {
		this.setConnectionFactory(connectionFactory);
		this.setDestinationName(destinationName);
		this.setPubSubDomain(isPubSub);
	}


	@Override
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		super.setConnectionFactory(connectionFactory);
		this.jmsTemplate.setConnectionFactory(connectionFactory);
	}

	@Override
	public void setDestination(Destination destination) {
		super.setDestination(destination);
		this.jmsTemplate.setDefaultDestination(destination);
	}

	@Override
	public void setDestinationName(String destinationName) {
		super.setDestinationName(destinationName);
		this.jmsTemplate.setDefaultDestinationName(destinationName);
	}

	@Override
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		super.setDestinationResolver(destinationResolver);
		this.jmsTemplate.setDestinationResolver(destinationResolver);
	}

	@Override
	public void setPubSubDomain(boolean pubSubDomain) {
		super.setPubSubDomain(pubSubDomain);
		this.jmsTemplate.setPubSubDomain(pubSubDomain);
	}

	public void setDeliveryPersistent(boolean deliveryPersistent) {
		this.jmsTemplate.setDeliveryPersistent(deliveryPersistent);
	}

	public void setExplicitQosEnabled(boolean explicitQosEnabled) {
		this.jmsTemplate.setExplicitQosEnabled(explicitQosEnabled);
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		this.jmsTemplate.setMessageConverter(messageConverter);
	}

	public void setMessageIdEnabled(boolean messageIdEnabled) {
		this.jmsTemplate.setMessageIdEnabled(messageIdEnabled);
	}

	public void setMessageTimestampEnabled(boolean messageTimestampEnabled) {
		this.jmsTemplate.setMessageTimestampEnabled(messageTimestampEnabled);
	}

	public void setPriority(int priority) {
		this.jmsTemplate.setPriority(priority);
	}

	@Override
	public void setPubSubNoLocal(boolean pubSubNoLocal) {
		super.setPubSubNoLocal(pubSubNoLocal);
		this.jmsTemplate.setPubSubNoLocal(pubSubNoLocal);
	}

	@Override
	public void setSessionAcknowledgeMode(int sessionAcknowledgeMode) {
		super.setSessionAcknowledgeMode(sessionAcknowledgeMode);
		this.jmsTemplate.setSessionAcknowledgeMode(sessionAcknowledgeMode);
	}

	@Override
	public void setSessionTransacted(boolean sessionTransacted) {
		super.setSessionTransacted(sessionTransacted);
		this.jmsTemplate.setSessionTransacted(sessionTransacted);
	}

	public void setTimeToLive(long timeToLive) {
		this.jmsTemplate.setTimeToLive(timeToLive);
	}

	public void setBeanName(String beanName) {
		this.name = beanName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		AbstractMessageListenerContainer container = this.getListenerContainer();
		this.configureDispatcher(container.isPubSubDomain());
		container.setMessageListener(this);
		if (!container.isActive()) {
			container.afterPropertiesSet();
		}
	}

	private void configureDispatcher(boolean isPubSub) {
		if (isPubSub) {
			this.dispatcher = new BroadcastingDispatcher();
		}
		else {
			UnicastingDispatcher unicastingDispatcher = new UnicastingDispatcher();
			unicastingDispatcher.setLoadBalancingStrategy(new RoundRobinLoadBalancingStrategy());
			this.dispatcher = unicastingDispatcher;
		}
	}

	public String getName() {
		return this.name;
	}

	public boolean subscribe(MessageHandler handler) {
		return this.dispatcher.addHandler(handler);
	}

	public boolean unsubscribe(MessageHandler handler) {
		return this.dispatcher.removeHandler(handler);
	}

	public boolean send(Message<?> message) {
		this.jmsTemplate.convertAndSend(message);
		return true;
	}

	public boolean send(Message<?> message, long timeout) {
		return this.send(message);
	}


	// MessageListener implementation

	public void onMessage(javax.jms.Message message) {
		try {
			Object o = this.jmsTemplate.getMessageConverter().fromMessage(message);
			this.dispatcher.dispatch(this.mapper.toMessage(o));
		}
		catch (Exception e) {
			throw new MessagingException("failed to handle incoming JMS Message", e);
		}
	}

}
