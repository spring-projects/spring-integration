/*
 * Copyright 2022 the original author or authors.
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.amqp.support.MappingUtils;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.rabbit.stream.producer.RabbitStreamOperations;
import org.springframework.rabbit.stream.support.StreamMessageProperties;
import org.springframework.util.Assert;

/**
 * {@link MessageHandler} based on {@link RabbitStreamOperations}.
 *
 * @author Gary Russell
 * @author Chris Bono
 * @since 6.0
 *
 */
public class RabbitStreamMessageHandler extends AbstractMessageHandler {

	private static final int DEFAULT_CONFIRM_TIMEOUT = 10_000;

	private final RabbitStreamOperations streamOperations;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private boolean sync;

	private long confirmTimeout = DEFAULT_CONFIRM_TIMEOUT;

	private MessageChannel sendFailureChannel;

	private String sendFailureChannelName;

	private MessageChannel sendSuccessChannel;

	private String sendSuccessChannelName;

	private AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.outboundMapper();

	private boolean headersMappedLast;

	/**
	 * Create an instance with the provided {@link RabbitStreamOperations}.
	 * @param streamOperations the operations.
	 */
	public RabbitStreamMessageHandler(RabbitStreamOperations streamOperations) {
		Assert.notNull(streamOperations, "'streamOperations' cannot be null");
		this.streamOperations = streamOperations;
	}

	/**
	 * Set the failure channel. After a send failure, an
	 * {@link org.springframework.messaging.support.ErrorMessage} will be sent
	 * to this channel with a payload of the exception with the
	 * failed message.
	 * @param sendFailureChannel the failure channel.
	 */
	public void setSendFailureChannel(MessageChannel sendFailureChannel) {
		this.sendFailureChannel = sendFailureChannel;
	}

	/**
	 * Set the failure channel name. After a send failure, an
	 * {@link org.springframework.messaging.support.ErrorMessage} will be sent
	 * to this channel with a payload of the exception with the
	 * failed message.
	 * @param sendFailureChannelName the failure channel name.
	 */
	public void setSendFailureChannelName(String sendFailureChannelName) {
		this.sendFailureChannelName = sendFailureChannelName;
	}

	/**
	 * Set the success channel.
	 * @param sendSuccessChannel the success channel.
	 */
	public void setSendSuccessChannel(MessageChannel sendSuccessChannel) {
		this.sendSuccessChannel = sendSuccessChannel;
	}

	/**
	 * Set the Success channel name.
	 * @param sendSuccessChannelName the success channel name.
	 */
	public void setSendSuccessChannelName(String sendSuccessChannelName) {
		this.sendSuccessChannelName = sendSuccessChannelName;
	}

	/**
	 * Set to true to wait for a confirmation.
	 * @param sync true to wait.
	 * @see #setConfirmTimeout(long)
	 */
	public void setSync(boolean sync) {
		this.sync = sync;
	}

	/**
	 * Set the confirm timeout.
	 * @param confirmTimeout the timeout.
	 * @see #setSync(boolean)
	 */
	public void setConfirmTimeout(long confirmTimeout) {
		this.confirmTimeout = confirmTimeout;
	}

	/**
	 * Set a custom {@link AmqpHeaderMapper} for mapping request and reply headers.
	 * Defaults to {@link DefaultAmqpHeaderMapper#outboundMapper()}.
	 * @param headerMapper the {@link AmqpHeaderMapper} to use.
	 */
	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * When mapping headers for the outbound message, determine whether the headers are
	 * mapped before the message is converted, or afterwards. This only affects headers
	 * that might be added by the message converter. When false, the converter's headers
	 * win; when true, any headers added by the converter will be overridden (if the
	 * source message has a header that maps to those headers). You might wish to set this
	 * to true, for example, when using a
	 * {@link org.springframework.amqp.support.converter.SimpleMessageConverter} with a
	 * String payload that contains json; the converter will set the content type to
	 * {@code text/plain} which can be overridden to {@code application/json} by setting
	 * the {@link AmqpHeaders#CONTENT_TYPE} message header. Default: false.
	 * @param headersMappedLast true if headers are mapped after conversion.
	 */
	public void setHeadersMappedLast(boolean headersMappedLast) {
		this.headersMappedLast = headersMappedLast;
	}

	/**
	 * Return the {@link RabbitStreamOperations}.
	 * @return the operations.
	 */
	public RabbitStreamOperations getStreamOperations() {
		return this.streamOperations;
	}

	protected MessageChannel getSendFailureChannel() {
		if (this.sendFailureChannel != null) {
			return this.sendFailureChannel;
		}
		else if (this.sendFailureChannelName != null) {
			this.sendFailureChannel = getChannelResolver().resolveDestination(this.sendFailureChannelName);
			return this.sendFailureChannel;
		}
		return null;
	}

	protected MessageChannel getSendSuccessChannel() {
		if (this.sendSuccessChannel != null) {
			return this.sendSuccessChannel;
		}
		else if (this.sendSuccessChannelName != null) {
			this.sendSuccessChannel = getChannelResolver().resolveDestination(this.sendSuccessChannelName);
			return this.sendSuccessChannel;
		}
		return null;
	}

	@Override
	protected void handleMessageInternal(Message<?> requestMessage) {
		CompletableFuture<Boolean> future;
		com.rabbitmq.stream.Message streamMessage;
		if (requestMessage.getPayload() instanceof com.rabbitmq.stream.Message) {
			streamMessage = (com.rabbitmq.stream.Message) requestMessage.getPayload();
		}
		else {
			MessageConverter converter = this.streamOperations.messageConverter();
			org.springframework.amqp.core.Message amqpMessage = mapMessage(requestMessage, converter,
					this.headerMapper, this.headersMappedLast);
			streamMessage = this.streamOperations.streamMessageConverter().fromMessage(amqpMessage);
		}
		future = this.streamOperations.send(streamMessage);
		handleConfirms(requestMessage, future);
	}

	private void handleConfirms(Message<?> message, CompletableFuture<Boolean> future) {
		future.whenComplete((bool, ex) -> {
			if (ex != null) {
				MessageChannel failures = getSendFailureChannel();
				if (failures != null) {
					this.messagingTemplate.send(failures, new ErrorMessage(ex, message));
				}
			}
			else {
				MessageChannel successes = getSendSuccessChannel();
				if (successes != null) {
					this.messagingTemplate.send(successes, message);
				}
			}
		});
		if (this.sync) {
			try {
				future.get(this.confirmTimeout, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new MessageHandlingException(message, ex);
			}
			catch (ExecutionException | TimeoutException ex) {
				throw new MessageHandlingException(message, ex);
			}
		}
	}

	private static org.springframework.amqp.core.Message mapMessage(Message<?> message,
			MessageConverter converter, AmqpHeaderMapper headerMapper, boolean headersMappedLast) {

		MessageProperties amqpMessageProperties = new StreamMessageProperties();
		return MappingUtils.mapMessage(message, converter, headerMapper, headersMappedLast, headersMappedLast,
				amqpMessageProperties);
	}

}
