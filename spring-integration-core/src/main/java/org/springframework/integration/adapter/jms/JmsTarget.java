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

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.adapter.Target;
import org.springframework.jms.core.JmsTemplate;

/**
 * A target for sending JMS Messages.
 * 
 * @author Mark Fisher
 */
public class JmsTarget implements Target<Object>, InitializingBean {

	private ConnectionFactory connectionFactory;

	private Destination destination;

	private JmsTemplate jmsTemplate;


	public JmsTarget(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public JmsTarget(ConnectionFactory connectionFactory, Destination destination) {
		this.connectionFactory = connectionFactory;
		this.destination = destination;
		this.initJmsTemplate();
	}

	/**
	 * No-arg constructor provided for convenience when configuring with
	 * setters. Note that the initialization callback will validate.
	 */
	public JmsTarget() {
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public void afterPropertiesSet() {
		if (this.jmsTemplate == null) {
			if (this.connectionFactory == null || this.destination == null) {
				throw new MessagingConfigurationException("Either a 'jmsTemplate' or " +
						"*both* 'connectionFactory' and 'destination' are required.");
			}
			this.initJmsTemplate();
		}
	}

	private void initJmsTemplate() {
		this.jmsTemplate = new JmsTemplate();
		this.jmsTemplate.setConnectionFactory(this.connectionFactory);
		this.jmsTemplate.setDefaultDestination(this.destination);
	}

	public boolean send(Object object) {
		this.jmsTemplate.convertAndSend(object);
		return true;
	}

}
