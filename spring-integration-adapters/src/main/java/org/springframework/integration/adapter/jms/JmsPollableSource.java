/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.adapter.jms;

import java.util.Arrays;
import java.util.Collection;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.adapter.PollableSource;
import org.springframework.jms.core.JmsTemplate;

/**
 * A source for receiving JMS Messages with a polling listener. This source is
 * only recommended for very low message volume. Otherwise, the
 * {@link JmsMessageDrivenSourceAdapter} that uses Spring's MessageListener
 * container support is highly recommended.
 * 
 * @author Mark Fisher
 */
public class JmsPollableSource implements PollableSource<Object>, InitializingBean {

	private ConnectionFactory connectionFactory;

	private Destination destination;

	private String destinationName;

	private JmsTemplate jmsTemplate;


	public JmsPollableSource(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public JmsPollableSource(ConnectionFactory connectionFactory, Destination destination) {
		this.connectionFactory = connectionFactory;
		this.destination = destination;
		this.initJmsTemplate();
	}

	public JmsPollableSource(ConnectionFactory connectionFactory, String destinationName) {
		this.connectionFactory = connectionFactory;
		this.destinationName = destinationName;
		this.initJmsTemplate();
	}

	/**
	 * No-arg constructor provided for convenience when configuring with
	 * setters. Note that the initialization callback will validate.
	 */
	public JmsPollableSource() {
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

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public void afterPropertiesSet() {
		if (this.jmsTemplate == null) {
			if (this.connectionFactory == null || (this.destination == null && this.destinationName == null)) {
				throw new MessagingConfigurationException("Either a 'jmsTemplate' or "
						+ "both 'connectionFactory' and 'destination' (or 'destinationName') are required.");
			}
			this.initJmsTemplate();
		}
	}

	private void initJmsTemplate() {
		this.jmsTemplate = new JmsTemplate();
		this.jmsTemplate.setConnectionFactory(this.connectionFactory);
		if (this.destination != null) {
			this.jmsTemplate.setDefaultDestination(this.destination);
		}
		else if (this.destinationName != null) {
			this.jmsTemplate.setDefaultDestinationName(this.destinationName);
		}
		else {
			throw new MessagingConfigurationException("either 'destination' or 'destinationName' is required");
		}
	}

	public Collection<Object> poll(int limit) {
		return Arrays.asList(this.jmsTemplate.receiveAndConvert());
	}

}
