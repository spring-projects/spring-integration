/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.expression.Expression;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
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

	private volatile ExpressionEvaluatingMessageProcessor<?> destinationExpressionProcessor;


	public JmsSendingMessageHandler(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public void setDestination(Destination destination) {
		Assert.isTrue(this.destinationName == null && this.destinationExpressionProcessor == null,
				"The 'destination', 'destinationName', and 'destinationExpression' properties are mutually exclusive.");
		this.destination = destination;
	}

	public void setDestinationName(String destinationName) {
		Assert.isTrue(this.destination == null && this.destinationExpressionProcessor == null,
				"The 'destination', 'destinationName', and 'destinationExpression' properties are mutually exclusive.");
		this.destinationName = destinationName;
	}

	public void setDestinationExpression(Expression destinationExpression) {
		Assert.isTrue(this.destination == null && this.destinationName == null,
				"The 'destination', 'destinationName', and 'destinationExpression' properties are mutually exclusive.");
		this.destinationExpressionProcessor = new ExpressionEvaluatingMessageProcessor<Object>(destinationExpression);
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
	 *
	 * @param extractPayload true to extract the payload.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	public String getComponentType() {
		return "jms:outbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		if (this.destinationExpressionProcessor != null) {
			this.destinationExpressionProcessor.setBeanFactory(getBeanFactory());
			this.destinationExpressionProcessor.setConversionService(getConversionService());
		}
	}

	@Override
	protected void handleMessageInternal(final Message<?> message) throws Exception {
		if (message == null) {
			throw new IllegalArgumentException("message must not be null");
		}
		Object destination = this.determineDestination(message);
		Object objectToSend = (this.extractPayload) ? message.getPayload() : message;
		MessagePostProcessor messagePostProcessor = new HeaderMappingMessagePostProcessor(message, this.headerMapper);
		try {
			DynamicJmsTemplateProperties.setPriority(new IntegrationMessageHeaderAccessor(message).getPriority());
			this.send(destination, objectToSend, messagePostProcessor);
		}
		finally {
			DynamicJmsTemplateProperties.clearPriority();
		}
	}

	private Object determineDestination(Message<?> message) {
		if (this.destination != null) {
			return this.destination;
		}
		if (this.destinationName != null) {
			return this.destinationName;
		}
		if (this.destinationExpressionProcessor != null) {
			Object result = this.destinationExpressionProcessor.processMessage(message);
			if (!(result instanceof Destination || result instanceof String)) {
				throw new MessageDeliveryException(message,
						"Evaluation of destinationExpression failed to produce a Destination or destination name. Result was: " + result);
			}
			return result;
		}
		return null;
	}

	private void send(Object destination, Object objectToSend, MessagePostProcessor messagePostProcessor) {
		if (destination instanceof Destination) {
			this.jmsTemplate.convertAndSend((Destination) destination, objectToSend, messagePostProcessor);
		}
		else if (destination instanceof String) {
			this.jmsTemplate.convertAndSend((String) destination, objectToSend, messagePostProcessor);
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

		@Override
		public javax.jms.Message postProcessMessage(javax.jms.Message jmsMessage) throws JMSException {
			this.headerMapper.fromHeaders(this.integrationMessage.getHeaders(), jmsMessage);
			return jmsMessage;
		}
	}

}
