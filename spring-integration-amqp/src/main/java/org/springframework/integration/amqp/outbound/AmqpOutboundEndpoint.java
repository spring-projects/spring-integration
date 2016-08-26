/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.amqp.outbound;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ReturnCallback;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.Lifecycle;
import org.springframework.integration.amqp.support.MappingUtils;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Adapter that converts and sends Messages to an AMQP Exchange.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 */
public class AmqpOutboundEndpoint extends AbstractAmqpOutboundEndpoint
		implements RabbitTemplate.ConfirmCallback, ReturnCallback {

	private final AmqpTemplate amqpTemplate;

	private volatile boolean expectReply;

	public AmqpOutboundEndpoint(AmqpTemplate amqpTemplate) {
		Assert.notNull(amqpTemplate, "amqpTemplate must not be null");
		this.amqpTemplate = amqpTemplate;
		if (amqpTemplate instanceof RabbitTemplate) {
			setConnectionFactory(((RabbitTemplate) amqpTemplate).getConnectionFactory());
		}
	}

	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}


	@Override
	public String getComponentType() {
		return this.expectReply ? "amqp:outbound-gateway" : "amqp:outbound-channel-adapter";
	}

	@Override
	protected void endpointInit() {
		if (getConfirmCorrelationExpression() != null) {
			Assert.isInstanceOf(RabbitTemplate.class, this.amqpTemplate,
					"RabbitTemplate implementation is required for publisher confirms");
			((RabbitTemplate) this.amqpTemplate).setConfirmCallback(this);
		}
		if (getReturnChannel() != null) {
			Assert.isInstanceOf(RabbitTemplate.class, this.amqpTemplate,
					"RabbitTemplate implementation is required for publisher confirms");
			((RabbitTemplate) this.amqpTemplate).setReturnCallback(this);
		}
	}

	@Override
	protected void doStop() {
		if (this.amqpTemplate instanceof Lifecycle) {
			((Lifecycle) this.amqpTemplate).stop();
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		CorrelationData correlationData = generateCorrelationData(requestMessage);
		String exchangeName = generateExchangeName(requestMessage);
		String routingKey = generateRoutingKey(requestMessage);
		if (this.expectReply) {
			return this.sendAndReceive(exchangeName, routingKey, requestMessage, correlationData);
		}
		else {
			this.send(exchangeName, routingKey, requestMessage, correlationData);
			return null;
		}
	}

	private void send(String exchangeName, String routingKey,
			final Message<?> requestMessage, CorrelationData correlationData) {
		if (this.amqpTemplate instanceof RabbitTemplate) {
			MessageConverter converter = ((RabbitTemplate) this.amqpTemplate).getMessageConverter();
			org.springframework.amqp.core.Message amqpMessage = MappingUtils.mapMessage(requestMessage, converter,
					getHeaderMapper(), getDefaultDeliveryMode());
			((RabbitTemplate) this.amqpTemplate).send(exchangeName, routingKey, amqpMessage, correlationData);
		}
		else {
			this.amqpTemplate.convertAndSend(exchangeName, routingKey, requestMessage.getPayload(),
					new MessagePostProcessor() {
						@Override
						public org.springframework.amqp.core.Message postProcessMessage(
								org.springframework.amqp.core.Message message) throws AmqpException {
							getHeaderMapper().fromHeadersToRequest(requestMessage.getHeaders(),
									message.getMessageProperties());
							return message;
						}
					});
		}
	}

	private Message<?> sendAndReceive(String exchangeName, String routingKey, Message<?> requestMessage,
			CorrelationData correlationData) {
		Assert.isInstanceOf(RabbitTemplate.class, this.amqpTemplate,
				"RabbitTemplate implementation is required for publisher confirms");
		MessageConverter converter = ((RabbitTemplate) this.amqpTemplate).getMessageConverter();
		org.springframework.amqp.core.Message amqpMessage = MappingUtils.mapMessage(requestMessage, converter,
				getHeaderMapper(), getDefaultDeliveryMode());
		org.springframework.amqp.core.Message amqpReplyMessage =
				((RabbitTemplate) this.amqpTemplate).sendAndReceive(exchangeName, routingKey, amqpMessage,
						correlationData);

		if (amqpReplyMessage == null) {
			return null;
		}
		return buildReplyMessage(converter, amqpReplyMessage);
	}

	@Override
	public void confirm(CorrelationData correlationData, boolean ack, String cause) {
		handleConfirm(correlationData, ack, cause);
	}

	@Override
	public void returnedMessage(org.springframework.amqp.core.Message message, int replyCode, String replyText,
			String exchange, String routingKey) {
		// safe to cast; we asserted we have a RabbitTemplate in doInit()
		MessageConverter converter = ((RabbitTemplate) this.amqpTemplate).getMessageConverter();
		Message<?> returned = buildReturnedMessage(message, replyCode, replyText, exchange,
				routingKey, converter);
		getReturnChannel().send(returned);
	}

}
