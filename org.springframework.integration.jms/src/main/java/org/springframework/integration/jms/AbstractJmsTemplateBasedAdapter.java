/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.core.JmsTemplate;
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

	private volatile DestinationResolver destinationResolver;

	private volatile JmsTemplate jmsTemplate;

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

	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
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
		}
		if (this.destinationResolver != null) {
			jmsTemplate.setDestinationResolver(this.destinationResolver);
		}
		return jmsTemplate;
	}

	protected abstract void configureMessageConverter(JmsTemplate jmsTemplate, JmsHeaderMapper headerMapper);

}
