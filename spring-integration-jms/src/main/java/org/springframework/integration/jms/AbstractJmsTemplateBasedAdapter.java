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
import javax.jms.DeliveryMode;
import javax.jms.Destination;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.util.Assert;

/**
 * Base class for adapters that delegate to a {@link JmsTemplate}.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractJmsTemplateBasedAdapter implements InitializingBean {

	private volatile ConnectionFactory connectionFactory;

	private volatile Destination destination;

	private volatile String destinationName;

	private volatile boolean pubSubDomain;

	private volatile DestinationResolver destinationResolver;

	private volatile int deliveryMode = javax.jms.Message.DEFAULT_DELIVERY_MODE;

	private volatile long timeToLive = javax.jms.Message.DEFAULT_TIME_TO_LIVE;

	private volatile int priority = javax.jms.Message.DEFAULT_PRIORITY;

	private volatile boolean explicitQosEnabled;

	private volatile JmsTemplate jmsTemplate;

	private volatile MessageConverter messageConverter;

	private volatile JmsHeaderMapper headerMapper;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public AbstractJmsTemplateBasedAdapter(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public AbstractJmsTemplateBasedAdapter(ConnectionFactory connectionFactory, Destination destination) {
		this.connectionFactory = connectionFactory;
		this.destination = destination;
	}

	public AbstractJmsTemplateBasedAdapter(ConnectionFactory connectionFactory, String destinationName) {
		this.connectionFactory = connectionFactory;
		this.destinationName = destinationName;
	}

	/**
	 * No-arg constructor provided for convenience when configuring with
	 * setters. Note that the initialization callback will validate.
	 */
	public AbstractJmsTemplateBasedAdapter() {
	}


	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	/**
	 * Provide a {@link MessageConverter} strategy to use for converting
	 * between Spring Integration Messages and JMS Messages.
	 * <p>
	 * The default is a {@link HeaderMappingMessageConverter} that delegates to
	 * a {@link SimpleMessageConverter}.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	/**
	 * @see JmsTemplate#setExplicitQosEnabled(boolean)
	 */
	public void setExplicitQosEnabled(boolean explicitQosEnabled) {
		this.explicitQosEnabled = explicitQosEnabled;
	}

	/**
	 * @see JmsTemplate#setTimeToLive(long)
	 */
	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	/**
	 * @see JmsTemplate#setDeliveryMode(int)
	 */
	public void setDeliveryMode(int deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	/**
	 * @see JmsTemplate#setDeliveryPersistent(boolean)
	 */
	public void setDeliveryPersistent(boolean deliveryPersistent) {
		this.deliveryMode = deliveryPersistent ?
				DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
	}

	/**
	 * @see JmsTemplate#setPriority(int)
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	protected JmsTemplate getJmsTemplate() {
		if (this.jmsTemplate == null) {
			this.afterPropertiesSet();
		}
		return this.jmsTemplate;
	}

	public void afterPropertiesSet() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			if (this.jmsTemplate == null) {
				Assert.isTrue(this.connectionFactory != null
						&& (this.destination != null || this.destinationName != null),
						"Either a 'jmsTemplate' or *both* 'connectionFactory' and"
						+ " 'destination' (or 'destination-name') are required.");
				this.jmsTemplate = this.createDefaultJmsTemplate();
			}
			this.jmsTemplate.setExplicitQosEnabled(this.explicitQosEnabled);
			this.jmsTemplate.setTimeToLive(this.timeToLive);
			this.jmsTemplate.setPriority(this.priority);
			this.jmsTemplate.setDeliveryMode(this.deliveryMode);
			if (this.messageConverter != null) {
				this.jmsTemplate.setMessageConverter(this.messageConverter);
			}
			this.configureMessageConverter(this.jmsTemplate, this.headerMapper);
			this.initialized = true;
		}
	}

	private JmsTemplate createDefaultJmsTemplate() {
		JmsTemplate jmsTemplate = new JmsTemplate();
		jmsTemplate.setConnectionFactory(this.connectionFactory);
		if (this.destination != null) {
			jmsTemplate.setDefaultDestination(this.destination);
		}
		else {
			jmsTemplate.setDefaultDestinationName(this.destinationName);
			jmsTemplate.setPubSubDomain(this.pubSubDomain);
		}
		if (this.destinationResolver != null) {
			jmsTemplate.setDestinationResolver(this.destinationResolver);
		}
		return jmsTemplate;
	}

	protected abstract void configureMessageConverter(JmsTemplate jmsTemplate, JmsHeaderMapper headerMapper);

}
