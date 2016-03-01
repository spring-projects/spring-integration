/*
 * Copyright 2016 the original author or authors.
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

import org.springframework.amqp.core.AmqpMessageReturnedException;
import org.springframework.amqp.rabbit.core.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.core.AsyncRabbitTemplate.RabbitMessageFuture;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.concurrent.ListenableFutureCallback;

import reactor.core.support.Assert;

/**
 * An outbound gateway where the sending thread is released immediately and the reply
 * is sent on the async template's listener container thread.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public class AsyncOutboundGateway extends AbstractAmqpOutboundEndpoint {

	private final AsyncRabbitTemplate template;

	private MessageConverter messageConverter = new SimpleMessageConverter();

	public AsyncOutboundGateway(AsyncRabbitTemplate template) {
		Assert.notNull(template, "AsyncRabbitTemplate cannot be null");
		this.template = template;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	@Override
	public String getComponentType() {
		return "amqp:outbound-async-gateway";
	}

	@Override
	protected void endpointInit() {
		super.endpointInit();
		setAsyncReplySupported(true);
		// TODO - add getConnectionFactory() to async template.
		// this.setConnectionFactory(template.getConnectionFactory());
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Assert.state(getAsyncReplySupported(), "Gateway not initialized - did you call afterPropertiesSet() ?");
		RabbitMessageFuture future = this.template.sendAndReceive(generateExchangeName(requestMessage),
				generateRoutingKey(requestMessage), mapMessage(requestMessage, this.messageConverter));
		future.addCallback(new FutureCallback(requestMessage));
		CorrelationData correlationData = generateCorrelationData(requestMessage);
		if (correlationData != null && future.getConfirm() != null) {
			future.getConfirm().addCallback(new CorrelationCallback(correlationData, future));
		}
		return null;
	}

	private final class FutureCallback implements ListenableFutureCallback<org.springframework.amqp.core.Message> {

		private final Message<?> requestMessage;

		public FutureCallback(Message<?> requestMessage) {
			this.requestMessage = requestMessage;
		}

		@Override
		public void onSuccess(org.springframework.amqp.core.Message result) {
			Message<?> replyMessage = null;
			try {
				replyMessage = buildReplyMessage(messageConverter, result);
				sendOutputs(replyMessage, requestMessage);
			}
			catch (Exception e) {
				Exception exceptionToLogAndSend = e;
				if (!(e instanceof MessagingException)) {
					exceptionToLogAndSend = new MessageHandlingException(requestMessage, e);
					if (replyMessage != null) {
						exceptionToLogAndSend = new MessagingException(replyMessage, exceptionToLogAndSend);
					}
				}
				logger.error("Failed to send async reply: " + result.toString(), exceptionToLogAndSend);
				sendErrorMessage(exceptionToLogAndSend, this.requestMessage.getHeaders().getErrorChannel());
			}
		}

		@Override
		public void onFailure(Throwable ex) {
			if (ex instanceof AmqpMessageReturnedException) {
				if (getReturnChannel() == null) {
					logger.error("Returned message received and no return channel "
							+ ((AmqpMessageReturnedException) ex).getReturnedMessage());
				}
				else {
					AmqpMessageReturnedException amre = (AmqpMessageReturnedException) ex;
					Message<?> returnedMessage = buildReturnedMessage(
							amre.getReturnedMessage(), amre.getReplyCode(), amre.getReplyText(), amre.getExchange(),
							amre.getRoutingKey(), messageConverter);
					sendOutput(returnedMessage, getReturnChannel(), true);
				}
			}
			else {
				sendErrorMessage(ex, this.requestMessage.getHeaders().getErrorChannel());
			}
		}

		private void sendErrorMessage(Throwable ex, Object errorChannel) {
			Throwable result = ex;
			if (!(ex instanceof MessagingException)) {
				result = new MessageHandlingException(this.requestMessage, ex);
			}
			if (errorChannel == null) {
				logger.error("Async exception received and no 'errorChannel' header exists; cannot route "
						+ "exception to caller", result);
			}
			else {
				try {
					sendOutput(new ErrorMessage(result), errorChannel, true);
				}
				catch (Exception e) {
					Exception exceptionToLog = e;
					if (!(e instanceof MessagingException)) {
						exceptionToLog = new MessageHandlingException(this.requestMessage, e);
					}
					logger.error("Failed to send async reply", exceptionToLog);
				}
			}
		}

	}

	private final class CorrelationCallback implements ListenableFutureCallback<Boolean> {

		private final CorrelationData correlationData;

		private final RabbitMessageFuture replyFuture;

		public CorrelationCallback(CorrelationData correlationData, RabbitMessageFuture replyFuture) {
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
