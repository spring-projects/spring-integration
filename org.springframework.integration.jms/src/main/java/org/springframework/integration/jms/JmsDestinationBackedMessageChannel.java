/*
 * Copyright 2002-2009 the original author or authors.
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
import javax.jms.Topic;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.gateway.SimpleMessageMapper;
import org.springframework.integration.message.InboundMessageMapper;
import org.springframework.integration.message.MessageHandler;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

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
public class JmsDestinationBackedMessageChannel implements SubscribableChannel, MessageListener,
		BeanNameAware, SmartLifecycle, InitializingBean {

	private final JmsTemplate jmsTemplate = new JmsTemplate();

	private final DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();

	private final InboundMessageMapper<Object> mapper = new SimpleMessageMapper();

	private volatile MessageDispatcher dispatcher;

	private volatile String name;


	public JmsDestinationBackedMessageChannel(ConnectionFactory connectionFactory, Destination destination) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		Assert.notNull(destination, "destination must not be null");
		this.jmsTemplate.setConnectionFactory(connectionFactory);
		this.jmsTemplate.setDefaultDestination(destination);
		this.initDispatcher(destination instanceof Topic);
	}

	public JmsDestinationBackedMessageChannel(ConnectionFactory connectionFactory, String destinationName, boolean isPubSub) {
		this(connectionFactory, destinationName, isPubSub, null);
	}

	public JmsDestinationBackedMessageChannel(ConnectionFactory connectionFactory, String destinationName, boolean isPubSub, DestinationResolver destinationResolver) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		Assert.hasText(destinationName, "destinationName is required");
		this.jmsTemplate.setConnectionFactory(connectionFactory);
		if (destinationResolver != null) {
			this.jmsTemplate.setDestinationResolver(destinationResolver);
		}
		this.jmsTemplate.setDefaultDestinationName(destinationName);
		this.jmsTemplate.setPubSubDomain(isPubSub);
		this.initDispatcher(isPubSub);
	}


	public void setBeanName(String beanName) {
		this.name = beanName;
	}

	private void initDispatcher(boolean isPubSub) {
		if (isPubSub) {
			this.dispatcher = new BroadcastingDispatcher();
		}
		else {
			UnicastingDispatcher unicastingDispatcher = new UnicastingDispatcher();
			unicastingDispatcher.setLoadBalancingStrategy(new RoundRobinLoadBalancingStrategy());
			this.dispatcher = unicastingDispatcher;
		}
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.container.setTransactionManager(transactionManager);
	}

	public void afterPropertiesSet() throws Exception {
		this.container.setConnectionFactory(this.jmsTemplate.getConnectionFactory());
		Destination destination = this.jmsTemplate.getDefaultDestination();
		if (destination != null) {
			this.container.setDestination(destination);
		}
		else {
			this.container.setDestinationName(this.jmsTemplate.getDefaultDestinationName());
			this.container.setPubSubDomain(this.jmsTemplate.isPubSubDomain());
		}
		this.container.setMessageListener(this);
		this.container.afterPropertiesSet();
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

	// SmartLifecycle implementation (delegates to the MessageListener container)

	public int getPhase() {
		return this.container.getPhase();
	}

	public boolean isAutoStartup() {
		return this.container.isAutoStartup();
	}

	public boolean isRunning() {
		return this.container.isRunning();
	}

	public void start() {
		this.container.start();
	}

	public void stop() {
		this.container.stop();
	}

	public void stop(Runnable callback) {
		this.container.stop(callback);
	}

}
