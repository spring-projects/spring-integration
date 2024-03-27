/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jms;

import jakarta.jms.Destination;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.expression.ExpressionUtils;
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
 * @author Artem Bilan
 */
public class JmsSendingMessageHandler extends AbstractMessageHandler {

	private final JmsTemplate jmsTemplate;

	private Destination destination;

	private String destinationName;

	private JmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();

	private boolean extractPayload = true;

	private ExpressionEvaluatingMessageProcessor<?> destinationExpressionProcessor;

	private Expression deliveryModeExpression;

	private Expression timeToLiveExpression;

	private EvaluationContext evaluationContext;

	public JmsSendingMessageHandler(JmsTemplate jmsTemplate) {
		Assert.notNull(jmsTemplate, "'jmsTemplate' must not be null");
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
		setPrimaryExpression(destinationExpression);
	}

	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "'headerMapper' cannot be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * Specify whether the payload should be extracted from each integration
	 * Message to be used as the JMS Message body.
	 * <p>The default value is <code>true</code>. To force passing of the full
	 * Spring Integration Message instead, set this to <code>false</code>.
	 * @param extractPayload true to extract the payload.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	/**
	 * Specify a SpEL expression to evaluate a {@code deliveryMode} for the JMS message to send.
	 * This option is applied only of QoS is enabled on the {@link JmsTemplate}.
	 * @param deliveryModeExpression to use
	 * @since 5.1
	 * @see #setDeliveryModeExpression(Expression)
	 */
	public void setDeliveryModeExpressionString(String deliveryModeExpression) {
		setDeliveryModeExpression(EXPRESSION_PARSER.parseExpression(deliveryModeExpression));
	}

	/**
	 * Specify a SpEL expression to evaluate a {@code deliveryMode} for the JMS message to send.
	 * This option is applied only of QoS is enabled on the {@link JmsTemplate}.
	 * @param deliveryModeExpression to use
	 * @since 5.1
	 */
	public void setDeliveryModeExpression(Expression deliveryModeExpression) {
		this.deliveryModeExpression = deliveryModeExpression;
	}

	/**
	 * Specify a SpEL expression to evaluate a {@code timeToLive} for the JMS message to send.
	 * @param timeToLiveExpression to use
	 * @since 5.1
	 * @see #setTimeToLiveExpression(Expression)
	 */
	public void setTimeToLiveExpressionString(String timeToLiveExpression) {
		setTimeToLiveExpression(EXPRESSION_PARSER.parseExpression(timeToLiveExpression));
	}

	/**
	 * Specify a SpEL expression to evaluate a {@code timeToLive} for the JMS message to send.
	 * @param timeToLiveExpression to use
	 * @since 5.1
	 */
	public void setTimeToLiveExpression(Expression timeToLiveExpression) {
		this.timeToLiveExpression = timeToLiveExpression;
	}

	@Override
	public String getComponentType() {
		return "jms:outbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		BeanFactory beanFactory = getBeanFactory();
		if (this.destinationExpressionProcessor != null) {
			this.destinationExpressionProcessor.setBeanFactory(beanFactory);
			ConversionService conversionService = getConversionService();
			if (conversionService != null) {
				this.destinationExpressionProcessor.setConversionService(conversionService);
			}
		}
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(beanFactory);
	}

	@Override
	protected void handleMessageInternal(final Message<?> message) {
		Object objectToSend = (this.extractPayload) ? message.getPayload() : message;
		MessagePostProcessor messagePostProcessor = new HeaderMappingMessagePostProcessor(message, this.headerMapper);

		if (this.jmsTemplate instanceof DynamicJmsTemplate && this.jmsTemplate.isExplicitQosEnabled()) {
			Integer priority = StaticMessageHeaderAccessor.getPriority(message);
			if (priority != null) {
				DynamicJmsTemplateProperties.setPriority(priority);
			}
			if (this.deliveryModeExpression != null) {
				Integer deliveryMode =
						this.deliveryModeExpression.getValue(this.evaluationContext, message, Integer.class);

				if (deliveryMode != null) {
					DynamicJmsTemplateProperties.setDeliveryMode(deliveryMode);
				}
			}
			if (this.timeToLiveExpression != null) {
				Long timeToLive = this.timeToLiveExpression.getValue(this.evaluationContext, message, Long.class);
				if (timeToLive != null) {
					DynamicJmsTemplateProperties.setTimeToLive(timeToLive);
				}
			}
		}
		try {
			send(determineDestination(message), objectToSend, messagePostProcessor);
		}
		finally {
			DynamicJmsTemplateProperties.clearPriority();
			DynamicJmsTemplateProperties.clearDeliveryMode();
			DynamicJmsTemplateProperties.clearTimeToLive();
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
						"Evaluation of destinationExpression failed to produce a Destination or destination name. " +
								"Result was: " + result);
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

	private record HeaderMappingMessagePostProcessor(Message<?> integrationMessage, JmsHeaderMapper headerMapper)
			implements MessagePostProcessor {

		@Override
		public jakarta.jms.Message postProcessMessage(jakarta.jms.Message jmsMessage) {
			this.headerMapper.fromHeaders(this.integrationMessage.getHeaders(), jmsMessage);
			return jmsMessage;
		}

	}

}
