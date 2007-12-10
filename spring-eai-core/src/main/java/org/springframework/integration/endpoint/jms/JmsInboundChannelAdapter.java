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

package org.springframework.integration.endpoint.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.endpoint.AbstractInboundChannelAdapter;
import org.springframework.jms.core.JmsTemplate;

/**
 * A simple channel adapter for receiving JMS Messages with a polling listener.
 * 
 * @author Mark Fisher
 */
public class JmsInboundChannelAdapter extends AbstractInboundChannelAdapter {

	private ConnectionFactory connectionFactory;

	private Destination destination;

	private JmsTemplate jmsTemplate;


	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	public void initialize() {
		if (this.jmsTemplate == null) {
			if (this.connectionFactory == null || this.destination == null) {
				throw new MessagingConfigurationException("Either a 'jmsTemplate' or " +
						"*both* 'connectionFactory' and 'destination' are required.");
			}
			this.jmsTemplate = new JmsTemplate();
			this.jmsTemplate.setConnectionFactory(this.connectionFactory);
			this.jmsTemplate.setDefaultDestination(this.destination);
		}
	}

	@Override
	protected Object doReceiveObject() {
		return this.jmsTemplate.receiveAndConvert();
	}

}
