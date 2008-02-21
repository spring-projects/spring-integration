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
import javax.jms.JMSException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.jms.support.converter.SimpleMessageConverter;

/**
 * A target adapter for sending JMS Messages.
 * 
 * @author Mark Fisher
 */
public class JmsTargetAdapter implements MessageHandler, InitializingBean {

	public static final String JMS_CORRELATION_ID = "JMSCorrelationID";

	public static final String JMS_REPLY_TO = "JMSReplyTo";

	public static final String JMS_TYPE = "JMSType";


	private ConnectionFactory connectionFactory;

	private Destination destination;

	private String destinationName;

	private JmsTemplate jmsTemplate;

	private JmsMessagePostProcessor jmsMessagePostProcessor = new DefaultJmsMessagePostProcessor();


	public JmsTargetAdapter(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public JmsTargetAdapter(ConnectionFactory connectionFactory, Destination destination) {
		this.connectionFactory = connectionFactory;
		this.destination = destination;
		this.initJmsTemplate();
	}

	public JmsTargetAdapter(ConnectionFactory connectionFactory, String destinationName) {
		this.connectionFactory = connectionFactory;
		this.destinationName = destinationName;
		this.initJmsTemplate();
	}

	/**
	 * No-arg constructor provided for convenience when configuring with
	 * setters. Note that the initialization callback will validate.
	 */
	public JmsTargetAdapter() {
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

	public void setJmsMessagePostProcessor(JmsMessagePostProcessor jmsMessagePostProcessor) {
		this.jmsMessagePostProcessor = jmsMessagePostProcessor;
	}

	public void afterPropertiesSet() {
		if (this.jmsTemplate == null) {
			if (this.connectionFactory == null || (this.destination == null && this.destinationName == null)) {
				throw new MessagingConfigurationException("Either a 'jmsTemplate' or " +
						"*both* 'connectionFactory' and 'destination' (or 'destination-name') are required.");
			}
			this.initJmsTemplate();
		}
		if (this.jmsTemplate.getMessageConverter() == null) {
			this.jmsTemplate.setMessageConverter(new SimpleMessageConverter());
		}
	}

	private void initJmsTemplate() {
		this.jmsTemplate = new JmsTemplate();
		this.jmsTemplate.setConnectionFactory(this.connectionFactory);
		if (this.destination != null) {
			this.jmsTemplate.setDefaultDestination(this.destination);
		}
		else {
			this.jmsTemplate.setDefaultDestinationName(this.destinationName);
		}
	}

	public final Message<?> handle(final Message<?> message) {
		if (message == null) {
			return null;
		}
		this.jmsTemplate.convertAndSend(message.getPayload(), new MessagePostProcessor() {
			public javax.jms.Message postProcessMessage(javax.jms.Message jmsMessage) throws JMSException {
				if (jmsMessagePostProcessor != null) {
					jmsMessagePostProcessor.postProcessJmsMessage(jmsMessage, message.getHeader());
				}
				return jmsMessage;
			}
		});
		return null;
	}

}
