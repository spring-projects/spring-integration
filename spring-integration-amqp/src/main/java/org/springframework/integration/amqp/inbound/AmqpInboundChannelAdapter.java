/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.amqp.inbound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.batch.BatchingStrategy;
import org.springframework.amqp.rabbit.batch.SimpleBatchingStrategy;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareBatchMessageListener;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.retry.MessageBatchRecoverer;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.core.AttributeAccessor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.AmqpMessageHeaderErrorMessageStrategy;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.amqp.support.EndpointUtils;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryOperations;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import com.rabbitmq.client.Channel;

/**
 * Adapter that receives Messages from an AMQP Queue, converts them into
 * Spring Integration Messages, and sends the results to a Message Channel.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class AmqpInboundChannelAdapter extends MessageProducerSupport implements
		OrderlyShutdownCapable {

	/**
	 * Header containing {@code List<Map<String, Object>} headers when batch mode
	 * is {@link BatchMode#EXTRACT_PAYLOADS_WITH_HEADERS}.
	 */
	public static final String CONSOLIDATED_HEADERS = AmqpHeaders.PREFIX + "batchedHeaders";

	/**
	 * Defines the payload type when the listener container is configured with consumerBatchEnabled.
	 */
	public enum BatchMode {

		/**
		 * Payload is a {@code List<Message<?>>} where each element is a message is
		 * converted from the Spring AMQP Message.
		 */
		MESSAGES,

		/**
		 * Payload is a {@code List<?>} where each element is the converted body of the
		 * Spring AMQP Message.
		 */
		EXTRACT_PAYLOADS,

		/**
		 * Payload is a {@code List<?>} where each element is the converted body of the
		 * Spring AMQP Message. The headers for each message are provided in a header
		 * {@link AmqpInboundChannelAdapter#CONSOLIDATED_HEADERS}.
		 */
		EXTRACT_PAYLOADS_WITH_HEADERS

	}

	private static final ThreadLocal<AttributeAccessor> ATTRIBUTES_HOLDER = new ThreadLocal<>();

	private final AbstractMessageListenerContainer messageListenerContainer;

	private MessageConverter messageConverter = new SimpleMessageConverter();

	private AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();

	private RetryTemplate retryTemplate;

	private RecoveryCallback<?> recoveryCallback;

	private MessageRecoverer messageRecoverer;

	private BatchingStrategy batchingStrategy = new SimpleBatchingStrategy(0, 0, 0L);

	private boolean bindSourceMessage;

	private BatchMode batchMode = BatchMode.MESSAGES;

	public AmqpInboundChannelAdapter(AbstractMessageListenerContainer listenerContainer) {
		Assert.notNull(listenerContainer, "listenerContainer must not be null");
		Assert.isNull(listenerContainer.getMessageListener(),
				"The listenerContainer provided to an AMQP inbound Channel Adapter " +
						"must not have a MessageListener configured since the adapter " +
						"configure its own listener implementation.");
		this.messageListenerContainer = listenerContainer;
		this.messageListenerContainer.setAutoStartup(false);
		setErrorMessageStrategy(new AmqpMessageHeaderErrorMessageStrategy());
	}


	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "messageConverter must not be null");
		this.messageConverter = messageConverter;
	}

	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * Set a {@link RetryTemplate} to use for retrying a message delivery within the
	 * adapter. Unlike adding retry at the container level, this can be used with an
	 * {@code ErrorMessageSendingRecoverer} {@link RecoveryCallback} to publish to the
	 * error channel after retries are exhausted. You generally should not configure an
	 * error channel when using retry here, use a {@link RecoveryCallback} instead.
	 * @param retryTemplate the template.
	 * @since 4.3.10.
	 * @see #setRecoveryCallback(RecoveryCallback)
	 */
	public void setRetryTemplate(RetryTemplate retryTemplate) {
		this.retryTemplate = retryTemplate;
	}

	/**
	 * Set a {@link RecoveryCallback} when using retry within the adapter.
	 * Mutually exclusive with {@link #setMessageRecoverer(MessageRecoverer)}.
	 * @param recoveryCallback the callback.
	 * @since 4.3.10
	 * @see #setRetryTemplate(RetryTemplate)
	 */
	public void setRecoveryCallback(RecoveryCallback<?> recoveryCallback) {
		this.recoveryCallback = recoveryCallback;
	}

	/**
	 * Configure a {@link MessageRecoverer} for retry operations.
	 * A more AMQP-specific convenience instead of {@link #setRecoveryCallback(RecoveryCallback)}.
	 * @param messageRecoverer the {@link MessageRecoverer} to use.
	 * @since 5.5
	 */
	public void setMessageRecoverer(MessageRecoverer messageRecoverer) {
		this.messageRecoverer = messageRecoverer;
	}

	/**
	 * Set a batching strategy to use when de-batching messages created by a batching
	 * producer (such as the BatchingRabbitTemplate).
	 * Default is {@link SimpleBatchingStrategy}.
	 * @param batchingStrategy the strategy.
	 * @since 5.2
	 */
	public void setBatchingStrategy(BatchingStrategy batchingStrategy) {
		Assert.notNull(batchingStrategy, "'batchingStrategy' cannot be null");
		this.batchingStrategy = batchingStrategy;
	}

	/**
	 * Set to true to bind the source message in the header named
	 * {@link IntegrationMessageHeaderAccessor#SOURCE_DATA}.
	 * @param bindSourceMessage true to bind.
	 * @since 5.1.6
	 */
	public void setBindSourceMessage(boolean bindSourceMessage) {
		this.bindSourceMessage = bindSourceMessage;
	}

	/**
	 * When the listener container is configured with consumerBatchEnabled, set the payload
	 * type for messages generated for the batches. Default is {@link BatchMode#MESSAGES}.
	 * @param batchMode the batch mode.
	 * @since 5.3
	 */
	public void setBatchMode(BatchMode batchMode) {
		Assert.notNull(batchMode, "'batchMode' cannot be null");
		this.batchMode = batchMode;
	}

	@Override
	public String getComponentType() {
		return "amqp:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		if (this.retryTemplate != null) {
			Assert.state(getErrorChannel() == null, "Cannot have an 'errorChannel' property when a 'RetryTemplate' is "
					+ "provided; use an 'ErrorMessageSendingRecoverer' in the 'recoveryCallback' property to "
					+ "send an error message when retries are exhausted");
			setupRecoveryCallbackIfAny();
		}
		Listener messageListener;
		if (this.messageListenerContainer.isConsumerBatchEnabled()) {
			messageListener = new BatchListener();
		}
		else {
			messageListener = new Listener();
		}
		this.messageListenerContainer.setMessageListener(messageListener);
		this.messageListenerContainer.afterPropertiesSet();
		super.onInit();
	}

	private void setupRecoveryCallbackIfAny() {
		Assert.state(this.recoveryCallback == null || this.messageRecoverer == null,
				"Only one of 'recoveryCallback' or 'messageRecoverer' may be provided, but not both");
		if (this.messageRecoverer != null) {
			if (this.messageListenerContainer.isConsumerBatchEnabled()) {
				Assert.isInstanceOf(MessageBatchRecoverer.class, this.messageRecoverer,
						"The 'messageRecoverer' must be an instance of MessageBatchRecoverer " +
								"when consumer configured for batch mode");
				this.recoveryCallback =
						context -> {
							@SuppressWarnings("unchecked")
							List<Message> messagesToRecover =
									(List<Message>) RetrySynchronizationManager.getContext()
											.getAttribute(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE);
							((MessageBatchRecoverer) this.messageRecoverer).recover(messagesToRecover,
									context.getLastThrowable());
							return null;
						};
			}
			else {
				this.recoveryCallback =
						context -> {
							Message messageToRecover =
									(Message) RetrySynchronizationManager.getContext()
											.getAttribute(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE);
							this.messageRecoverer.recover(messageToRecover, context.getLastThrowable());
							return null;
						};
			}
		}
	}

	@Override
	protected void doStart() {
		this.messageListenerContainer.start();
	}

	@Override
	protected void doStop() {
		this.messageListenerContainer.stop();
	}

	@Override
	public int beforeShutdown() {
		this.stop();
		return 0;
	}

	@Override
	public int afterShutdown() {
		return 0;
	}

	/**
	 * If there's a retry template, it will set the attributes holder via the listener. If
	 * there's no retry template, but there's an error channel, we create a new attributes
	 * holder here. If an attributes holder exists (by either method), we set the
	 * attributes for use by the
	 * {@link org.springframework.integration.support.ErrorMessageStrategy}.
	 * @param amqpMessage the AMQP message to use.
	 * @param message the Spring Messaging message to use.
	 * @since 4.3.10
	 */
	private void setAttributesIfNecessary(Object amqpMessage, org.springframework.messaging.Message<?> message) {
		boolean needHolder = getErrorChannel() != null && this.retryTemplate == null;
		boolean needAttributes = needHolder || this.retryTemplate != null;
		if (needHolder) {
			ATTRIBUTES_HOLDER.set(ErrorMessageUtils.getAttributeAccessor(null, null));
		}
		if (needAttributes) {
			AttributeAccessor attributes = this.retryTemplate != null
					? RetrySynchronizationManager.getContext()
					: ATTRIBUTES_HOLDER.get();
			if (attributes != null) {
				attributes.setAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY, message);
				attributes.setAttribute(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE, amqpMessage);
			}
		}
	}

	@Override
	protected AttributeAccessor getErrorMessageAttributes(org.springframework.messaging.Message<?> message) {
		AttributeAccessor attributes = ATTRIBUTES_HOLDER.get();
		if (attributes == null) {
			return super.getErrorMessageAttributes(message);
		}
		else {
			return attributes;
		}
	}

	protected class Listener implements ChannelAwareMessageListener {

		protected final MessageConverter converter = AmqpInboundChannelAdapter.this.messageConverter; // NOSONAR

		protected final boolean manualAcks = // NNOSONAR
				AcknowledgeMode.MANUAL == AmqpInboundChannelAdapter.this.messageListenerContainer.getAcknowledgeMode();

		protected final RetryOperations retryOps = AmqpInboundChannelAdapter.this.retryTemplate; // NOSONAR

		protected final RecoveryCallback<?> recoverer = AmqpInboundChannelAdapter.this.recoveryCallback; // NOSONAR

		protected Listener() {
		}

		@Override
		public void onMessage(final Message message, final Channel channel) {
			try {
				if (this.retryOps == null) {
					createAndSend(message, channel);
				}
				else {
					final org.springframework.messaging.Message<Object> toSend =
							createMessageFromAmqp(message, channel);
					this.retryOps.execute(
							context -> {
								StaticMessageHeaderAccessor.getDeliveryAttempt(toSend).incrementAndGet();
								setAttributesIfNecessary(message, toSend);
								sendMessage(toSend);
								return null;
							}, this.recoverer);
				}
			}
			catch (MessageConversionException e) {
				MessageChannel errorChannel = getErrorChannel();
				if (errorChannel != null) {
					setAttributesIfNecessary(message, null);
					getMessagingTemplate()
							.send(errorChannel,
									buildErrorMessage(null,
											EndpointUtils.errorMessagePayload(message, channel, this.manualAcks, e)));
				}
				else {
					throw e;
				}
			}
			finally {
				if (this.retryOps == null) {
					ATTRIBUTES_HOLDER.remove();
				}
			}
		}

		private void createAndSend(Message message, Channel channel) {
			org.springframework.messaging.Message<Object> messagingMessage = createMessageFromAmqp(message, channel);
			setAttributesIfNecessary(message, messagingMessage);
			sendMessage(messagingMessage);
		}

		protected org.springframework.messaging.Message<Object> createMessageFromAmqp(Message message,
				Channel channel) {

			Object payload = convertPayload(message);
			Map<String, Object> headers =
					AmqpInboundChannelAdapter.this.headerMapper.toHeadersFromRequest(message.getMessageProperties());
			if (AmqpInboundChannelAdapter.this.bindSourceMessage) {
				headers.put(IntegrationMessageHeaderAccessor.SOURCE_DATA, message);
			}
			long deliveryTag = message.getMessageProperties().getDeliveryTag();
			return createMessageFromPayload(payload, channel, headers, deliveryTag, null);
		}

		protected Object convertPayload(Message message) {
			Object payload;
			if (AmqpInboundChannelAdapter.this.batchingStrategy.canDebatch(message.getMessageProperties())) {
				List<Object> payloads = new ArrayList<>();
				AmqpInboundChannelAdapter.this.batchingStrategy.deBatch(message,
						fragment -> payloads.add(this.converter.fromMessage(fragment)));
				payload = payloads;
			}
			else {
				payload = this.converter.fromMessage(message);
			}
			return payload;
		}

		protected org.springframework.messaging.Message<Object> createMessageFromPayload(Object payload,
				Channel channel, Map<String, Object> headers, long deliveryTag,
				@Nullable List<Map<String, Object>> listHeaders) {

			if (this.manualAcks) {
				headers.put(AmqpHeaders.DELIVERY_TAG, deliveryTag);
				headers.put(AmqpHeaders.CHANNEL, channel);
			}
			if (AmqpInboundChannelAdapter.this.retryTemplate != null) {
				headers.put(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, new AtomicInteger());
			}
			if (listHeaders != null) {
				headers.put(CONSOLIDATED_HEADERS, listHeaders);
			}
			return getMessageBuilderFactory()
					.withPayload(payload)
					.copyHeaders(headers)
					.build();
		}

	}

	protected class BatchListener extends Listener implements ChannelAwareBatchMessageListener {

		private final boolean batchModeMessages = BatchMode.MESSAGES.equals(AmqpInboundChannelAdapter.this.batchMode);

		@Override
		public void onMessageBatch(List<Message> messages, Channel channel) {
			List<?> converted;
			List<Map<String, Object>> headers = null;
			if (this.batchModeMessages) {
				converted = convertMessages(messages, channel);
			}
			else {
				converted = convertPayloads(messages, channel);
				if (BatchMode.EXTRACT_PAYLOADS_WITH_HEADERS.equals(AmqpInboundChannelAdapter.this.batchMode)) {
					List<Map<String, Object>> listHeaders = new ArrayList<>();
					messages.forEach(msg -> listHeaders.add(AmqpInboundChannelAdapter.this.headerMapper
							.toHeadersFromRequest(msg.getMessageProperties())));
					headers = listHeaders;
				}
			}
			if (converted != null) {
				org.springframework.messaging.Message<?> message =
						createMessageFromPayload(converted, channel, new HashMap<>(),
								messages.get(messages.size() - 1).getMessageProperties().getDeliveryTag(), headers);
				try {
					if (this.retryOps == null) {
						setAttributesIfNecessary(messages, message);
						sendMessage(message);
					}
					else {
						this.retryOps.execute(
								context -> {
									StaticMessageHeaderAccessor.getDeliveryAttempt(message).incrementAndGet();
									if (this.batchModeMessages) {
										@SuppressWarnings("unchecked")
										List<org.springframework.messaging.Message<?>> payloads =
												(List<org.springframework.messaging.Message<?>>) message.getPayload();
										payloads.forEach(payload -> StaticMessageHeaderAccessor
												.getDeliveryAttempt(payload).incrementAndGet());
									}
									setAttributesIfNecessary(messages, message);
									sendMessage(message);
									return null;
								}, this.recoverer);
					}
				}
				finally {
					if (this.retryOps == null) {
						ATTRIBUTES_HOLDER.remove();
					}
				}
			}
		}

		private List<org.springframework.messaging.Message<?>> convertMessages(List<Message> messages,
				Channel channel) {

			List<org.springframework.messaging.Message<?>> converted = new ArrayList<>();
			try {
				messages.forEach(message -> converted.add(createMessageFromAmqp(message, channel)));
				return converted;
			}
			catch (MessageConversionException e) {
				MessageChannel errorChannel = getErrorChannel();
				if (errorChannel != null) {
					setAttributesIfNecessary(messages, null);
					getMessagingTemplate()
							.send(errorChannel, buildErrorMessage(null,
									EndpointUtils.errorMessagePayload(messages, channel, this.manualAcks, e)));
				}
				else {
					throw e;
				}
			}
			return null;
		}

		private List<?> convertPayloads(List<Message> messages, Channel channel) {
			List<Object> converted = new ArrayList<>();
			try {
				messages.forEach(message -> converted.add(this.converter.fromMessage(message)));
				return converted;
			}
			catch (MessageConversionException e) {
				MessageChannel errorChannel = getErrorChannel();
				if (errorChannel != null) {
					setAttributesIfNecessary(messages, null);
					getMessagingTemplate()
							.send(errorChannel, buildErrorMessage(null,
									EndpointUtils.errorMessagePayload(messages, channel, this.manualAcks, e)));
				}
				else {
					throw e;
				}
			}
			return null;
		}

	}

}
