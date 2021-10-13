/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.amqp.outbound;

import org.springframework.amqp.core.AmqpMessageReturnedException;
import org.springframework.amqp.core.AmqpReplyTimeoutException;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate.RabbitMessageFuture;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.CorrelationData.Confirm;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.integration.amqp.support.MappingUtils;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * An outbound gateway where the sending thread is released immediately and the reply
 * is sent on the async template's listener container thread.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class AsyncAmqpOutboundGateway extends AbstractAmqpOutboundEndpoint {

	private final AsyncRabbitTemplate template;

	private final MessageConverter messageConverter;

	public AsyncAmqpOutboundGateway(AsyncRabbitTemplate template) {
		Assert.notNull(template, "AsyncRabbitTemplate cannot be null");
		this.template = template;
		this.messageConverter = template.getMessageConverter();
		Assert.notNull(this.messageConverter, "the template's message converter cannot be null");
		setConnectionFactory(this.template.getConnectionFactory());
		setAsync(true);
	}

	@Override
	public String getComponentType() {
		return "amqp:outbound-async-gateway";
	}

	@Override
	protected RabbitTemplate getRabbitTemplate() {
		return this.template.getRabbitTemplate();
	}

	@Override
	protected void doStart() {
		super.doStart();
		this.template.start();
	}

	@Override
	protected void doStop() {
		this.template.stop();
		super.doStop();
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		org.springframework.amqp.core.Message amqpMessage = MappingUtils.mapMessage(requestMessage,
				this.messageConverter, getHeaderMapper(), getDefaultDeliveryMode(), isHeadersMappedLast());
		addDelayProperty(requestMessage, amqpMessage);
		RabbitMessageFuture future = this.template.sendAndReceive(generateExchangeName(requestMessage),
				generateRoutingKey(requestMessage), amqpMessage);
		CorrelationData correlationData = generateCorrelationData(requestMessage);
		if (correlationData != null && future.getConfirm() != null) {
			future.getConfirm().addCallback(new CorrelationCallback(correlationData, future));
		}
		future.addCallback(new FutureCallback(requestMessage, correlationData));
		return null;
	}

	private final class FutureCallback implements ListenableFutureCallback<org.springframework.amqp.core.Message> {

		private final Message<?> requestMessage;

		private final CorrelationDataWrapper correlationData;

		FutureCallback(Message<?> requestMessage, CorrelationData correlationData) {
			this.requestMessage = requestMessage;
			this.correlationData = (CorrelationDataWrapper) correlationData;
		}

		@Override
		public void onSuccess(org.springframework.amqp.core.Message result) {
			AbstractIntegrationMessageBuilder<?> replyMessageBuilder = null;
			try {
				replyMessageBuilder = buildReply(AsyncAmqpOutboundGateway.this.messageConverter, result);
				sendOutputs(replyMessageBuilder, this.requestMessage);
			}
			catch (Exception ex) {
				Exception exceptionToLogAndSend = ex;
				if (!(ex instanceof MessagingException)) { // NOSONAR
					exceptionToLogAndSend = new MessageHandlingException(this.requestMessage,
							"failed to handle a message in the [" + AsyncAmqpOutboundGateway.this + ']', ex);
					if (replyMessageBuilder != null) {
						exceptionToLogAndSend =
								new MessagingException(replyMessageBuilder.build(), exceptionToLogAndSend);
					}
				}
				logger.error(exceptionToLogAndSend, () -> "Failed to send async reply: " + result.toString());
				sendErrorMessage(this.requestMessage, exceptionToLogAndSend);
			}
		}

		@Override
		public void onFailure(Throwable ex) {
			Throwable exceptionToSend = ex;
			if (ex instanceof AmqpReplyTimeoutException) {
				if (getRequiresReply()) {
					exceptionToSend =
							new ReplyRequiredException(this.requestMessage, "Timeout on async request/reply", ex);
				}
				else {
					logger.debug(() -> "Reply not required and async timeout for " + this.requestMessage);
					return;
				}
			}
			if (ex instanceof AmqpMessageReturnedException) {
				AmqpMessageReturnedException amre = (AmqpMessageReturnedException) ex;
				MessageChannel returnChannel = getReturnChannel();
				if (returnChannel != null) {
					Message<?> returnedMessage = buildReturnedMessage(
							new ReturnedMessage(amre.getReturnedMessage(), amre.getReplyCode(), amre.getReplyText(),
									amre.getExchange(), amre.getRoutingKey()),
							AsyncAmqpOutboundGateway.this.messageConverter);
					sendOutput(returnedMessage, returnChannel, true);
				}
				this.correlationData.setReturned(amre.getReturned());
				/*
				 *  Complete the user's future (if present) since the async template will only complete
				 *  once, successfully, or with a failure.
				 */
				this.correlationData.getFuture().set(new Confirm(true, null));
			}
			else {
				sendErrorMessage(this.requestMessage, exceptionToSend);
			}
		}

	}

	private final class CorrelationCallback implements ListenableFutureCallback<Boolean> {

		private final CorrelationData correlationData;

		private final RabbitMessageFuture replyFuture;

		CorrelationCallback(CorrelationData correlationData, RabbitMessageFuture replyFuture) {
			this.correlationData = correlationData;
			this.replyFuture = replyFuture;
		}

		@Override
		public void onSuccess(Boolean result) {
			try {
				handleConfirm(this.correlationData, result, this.replyFuture.getNackCause());
			}
			catch (Exception e) {
				logger.error("Failed to send publisher confirm");
			}
		}

		@Override
		public void onFailure(Throwable ex) {
		}

	}

}
