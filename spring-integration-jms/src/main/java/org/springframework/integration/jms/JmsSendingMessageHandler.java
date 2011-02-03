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

import javax.jms.Destination;
import javax.jms.JMSException;

import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.util.Assert;

/**
 * A MessageConsumer that sends the converted Message payload within a JMS Message.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class JmsSendingMessageHandler extends AbstractMessageHandler {

	private final JmsTemplate jmsTemplate;

	private volatile Destination destination;

	private volatile String destinationName;

	private volatile JmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();

	private volatile boolean extractPayload = true;


	public JmsSendingMessageHandler(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public void setDestination(Destination destination) {
		Assert.isNull(this.destinationName, "The 'destination' and 'destinationName' properties are mutually exclusive.");
		this.destination = destination;
	}

	public void setDestinationName(String destinationName) {
		Assert.isNull(this.destination, "The 'destination' and 'destinationName' properties are mutually exclusive.");
		this.destinationName = destinationName;
	}

	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	/**
	 * Specify whether the payload should be extracted from each integration
	 * Message to be used as the JMS Message body.
	 * 
	 * <p>The default value is <code>true</code>. To force passing of the full
	 * Spring Integration Message instead, set this to <code>false</code>.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	public String getComponentType() {
		return "jms:outbound-channel-adapter";
	}

	@Override
	protected void handleMessageInternal(final Message<?> message) throws Exception {
		if (message == null) {
			throw new IllegalArgumentException("message must not be null");
		}
		Object objectToSend = (this.extractPayload) ? message.getPayload() : message;
		MessagePostProcessor messagePostProcessor = new HeaderMappingMessagePostProcessor(message, this.headerMapper);
		try {
			DynamicJmsTemplateProperties.setPriority(message.getHeaders().getPriority());
			this.send(objectToSend, messagePostProcessor);
		}
		finally {
			DynamicJmsTemplateProperties.clearPriority();
		}
	}

	private void send(Object objectToSend, MessagePostProcessor messagePostProcessor) {
		if (this.destination != null) {
			this.jmsTemplate.convertAndSend(this.destination, objectToSend, messagePostProcessor);
		}
		else if (this.destinationName != null) {
			this.jmsTemplate.convertAndSend(this.destinationName, objectToSend, messagePostProcessor);
		}
		else { // fallback to default destination of the template
			this.jmsTemplate.convertAndSend(objectToSend, messagePostProcessor);
		}
	}


	private static class HeaderMappingMessagePostProcessor implements MessagePostProcessor {

		private final Message<?> integrationMessage;

		private final JmsHeaderMapper headerMapper;

		private HeaderMappingMessagePostProcessor(Message<?> integrationMessage, JmsHeaderMapper headerMapper) {
			this.integrationMessage = integrationMessage;
			this.headerMapper = headerMapper;
		}

		public javax.jms.Message postProcessMessage(javax.jms.Message jmsMessage) throws JMSException {
			this.headerMapper.fromHeaders(this.integrationMessage.getHeaders(), jmsMessage);
			return jmsMessage;
		}
	}


}
