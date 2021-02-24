/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.integration.kafka.inbound;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;

import org.springframework.core.AttributeAccessor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.core.Pausable;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.kafka.support.RawRecordHeaderErrorMessageStrategy;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.adapter.RecordMessagingMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.converter.ConversionException;
import org.springframework.kafka.support.converter.KafkaMessageHeaders;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * Inbound gateway.
 *
 * @param <K> the key type.
 * @param <V> the request value type.
 * @param <R> the reply value type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Urs Keller
 *
 * @since 5.4
 *
 */
public class KafkaInboundGateway<K, V, R> extends MessagingGatewaySupport implements Pausable, OrderlyShutdownCapable {

	private static final ThreadLocal<AttributeAccessor> ATTRIBUTES_HOLDER = new ThreadLocal<>();

	private final IntegrationRecordMessageListener listener = new IntegrationRecordMessageListener();

	private final AbstractMessageListenerContainer<K, V> messageListenerContainer;

	private final KafkaTemplate<K, R> kafkaTemplate;

	private RetryTemplate retryTemplate;

	private RecoveryCallback<?> recoveryCallback;

	private BiConsumer<Map<TopicPartition, Long>, ConsumerSeekAware.ConsumerSeekCallback> onPartitionsAssignedSeekCallback;

	private boolean bindSourceRecord;

	private boolean containerDeliveryAttemptPresent;

	/**
	 * Construct an instance with the provided container.
	 * @param messageListenerContainer the container.
	 * @param kafkaTemplate the kafka template.
	 */
	public KafkaInboundGateway(AbstractMessageListenerContainer<K, V> messageListenerContainer,
			KafkaTemplate<K, R> kafkaTemplate) {

		Assert.notNull(messageListenerContainer, "messageListenerContainer is required");
		Assert.notNull(kafkaTemplate, "kafkaTemplate is required");
		Assert.isNull(messageListenerContainer.getContainerProperties().getMessageListener(),
				"Container must not already have a listener");
		this.messageListenerContainer = messageListenerContainer;
		this.messageListenerContainer.setAutoStartup(false);
		this.kafkaTemplate = kafkaTemplate;
		setErrorMessageStrategy(new RawRecordHeaderErrorMessageStrategy());
	}

	/**
	 * Set the message converter; must be a {@link RecordMessageConverter} or
	 * {@link org.springframework.kafka.support.converter.BatchMessageConverter} depending on mode.
	 * @param messageConverter the converter.
	 */
	public void setMessageConverter(RecordMessageConverter messageConverter) {
		this.listener.setMessageConverter(messageConverter);
	}

	/**
	 * When using a type-aware message converter (such as {@code StringJsonMessageConverter},
	 * set the payload type the converter should create. Defaults to {@link Object}.
	 * @param payloadType the type.
	 */
	public void setPayloadType(Class<?> payloadType) {
		this.listener.setFallbackType(payloadType);
	}

	/**
	 * Specify a {@link RetryTemplate} instance to wrap
	 * {@link KafkaInboundGateway.IntegrationRecordMessageListener} into
	 * {@link RetryingMessageListenerAdapter}.
	 * @param retryTemplate the {@link RetryTemplate} to use.
	 */
	public void setRetryTemplate(RetryTemplate retryTemplate) {
		this.retryTemplate = retryTemplate;
	}

	/**
	 * A {@link RecoveryCallback} instance for retry operation;
	 * if null, the exception will be thrown to the container after retries are exhausted
	 * (unless an error channel is configured).
	 * Does not make sense if {@link #setRetryTemplate(RetryTemplate)} isn't specified.
	 * @param recoveryCallback the recovery callback.
	 */
	public void setRecoveryCallback(RecoveryCallback<?> recoveryCallback) {
		this.recoveryCallback = recoveryCallback;
	}

	/**
	 * Specify a {@link BiConsumer} for seeks management during
	 * {@link ConsumerSeekAware.ConsumerSeekCallback#onPartitionsAssigned(Map, ConsumerSeekAware.ConsumerSeekCallback)}
	 * call from the {@link org.springframework.kafka.listener.KafkaMessageListenerContainer}.
	 * This is called from the internal
	 * {@link org.springframework.kafka.listener.adapter.MessagingMessageListenerAdapter} implementation.
	 * @param onPartitionsAssignedCallback the {@link BiConsumer} to use
	 * @since 3.0.4
	 * @see ConsumerSeekAware#onPartitionsAssigned
	 */
	public void setOnPartitionsAssignedSeekCallback(
			BiConsumer<Map<TopicPartition, Long>, ConsumerSeekAware.ConsumerSeekCallback> onPartitionsAssignedCallback) {
		this.onPartitionsAssignedSeekCallback = onPartitionsAssignedCallback;
	}

	/**
	 * Set to true to bind the source consumer record in the header named
	 * {@link IntegrationMessageHeaderAccessor#SOURCE_DATA}.
	 * @param bindSourceRecord true to bind.
	 * @since 3.1.4
	 */
	public void setBindSourceRecord(boolean bindSourceRecord) {
		this.bindSourceRecord = bindSourceRecord;
	}

	@Override
	protected void onInit() {
		super.onInit();
		MessageListener<K, V> kafkaListener = this.listener;
		if (this.retryTemplate != null) {
			kafkaListener =
					new RetryingMessageListenerAdapter<>(kafkaListener, this.retryTemplate, this.recoveryCallback);
			this.retryTemplate.registerListener(this.listener);
		}
		ContainerProperties containerProperties = this.messageListenerContainer.getContainerProperties();
		containerProperties.setMessageListener(kafkaListener);
		this.containerDeliveryAttemptPresent = containerProperties.isDeliveryAttemptHeader();
	}

	@Override
	protected void doStart() {
		super.doStart();
		this.messageListenerContainer.start();
	}

	@Override
	protected void doStop() {
		super.doStop();
		this.messageListenerContainer.stop();
	}

	@Override
	public void pause() {
		this.messageListenerContainer.pause();
	}

	@Override
	public void resume() {
		this.messageListenerContainer.resume();
	}

	@Override
	public boolean isPaused() {
		return this.messageListenerContainer.isContainerPaused();
	}

	@Override
	public String getComponentType() {
		return "kafka:inbound-gateway";
	}

	@Override
	public int beforeShutdown() {
		this.messageListenerContainer.stop();
		return getPhase();
	}

	@Override
	public int afterShutdown() {
		return getPhase();
	}

	/**
	 * If there's a retry template, it will set the attributes holder via the listener. If
	 * there's no retry template, but there's an error channel, we create a new attributes
	 * holder here. If an attributes holder exists (by either method), we set the
	 * attributes for use by the {@link org.springframework.integration.support.ErrorMessageStrategy}.
	 * @param record the record.
	 * @param message the message.
	 */
	private void setAttributesIfNecessary(Object record, Message<?> message) {
		boolean needHolder = getErrorChannel() != null && this.retryTemplate == null;
		boolean needAttributes = needHolder | this.retryTemplate != null;
		if (needHolder) {
			ATTRIBUTES_HOLDER.set(ErrorMessageUtils.getAttributeAccessor(null, null));
		}
		if (needAttributes) {
			AttributeAccessor attributes = ATTRIBUTES_HOLDER.get();
			if (attributes != null) {
				attributes.setAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY, message);
				attributes.setAttribute(KafkaHeaders.RAW_DATA, record);
			}
		}
	}

	@Override
	protected AttributeAccessor getErrorMessageAttributes(Message<?> message) {
		AttributeAccessor attributes = ATTRIBUTES_HOLDER.get();
		if (attributes == null) {
			return super.getErrorMessageAttributes(message);
		}
		else {
			return attributes;
		}
	}

	private class IntegrationRecordMessageListener extends RecordMessagingMessageListenerAdapter<K, V>
			implements RetryListener {

		IntegrationRecordMessageListener() {
			super(null, null); // NOSONAR - out of use
		}

		@Override
		public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
			if (KafkaInboundGateway.this.onPartitionsAssignedSeekCallback != null) {
				KafkaInboundGateway.this.onPartitionsAssignedSeekCallback.accept(assignments, callback);
			}
		}

		@Override
		public void onMessage(ConsumerRecord<K, V> record, Acknowledgment acknowledgment, Consumer<?, ?> consumer) {
			Message<?> message = null;
			try {
				message = enhanceHeaders(toMessagingMessage(record, acknowledgment, consumer), record);
				setAttributesIfNecessary(record, message);
			}
			catch (RuntimeException e) {
				if (getErrorChannel() != null) {
					KafkaInboundGateway.this.messagingTemplate.send(getErrorChannel(), buildErrorMessage(null,
							new ConversionException("Failed to convert to message for: " + record, e)));
				}
			}
			if (message != null) {
				try {
					Message<?> reply = sendAndReceiveMessage(message);
					if (reply != null) {
						reply = enhanceReply(message, reply);
						KafkaInboundGateway.this.kafkaTemplate.send(reply);
					}
				}
				finally {
					if (KafkaInboundGateway.this.retryTemplate == null) {
						ATTRIBUTES_HOLDER.remove();
					}
				}
			}
			else {
				KafkaInboundGateway.this.logger.debug(() -> "Converter returned a null message for: " + record);
			}
		}

		private Message<?> enhanceHeaders(Message<?> message, ConsumerRecord<K, V> record) {
			Message<?> messageToReturn = message;
			if (message.getHeaders() instanceof KafkaMessageHeaders) {
				Map<String, Object> rawHeaders = ((KafkaMessageHeaders) message.getHeaders()).getRawHeaders();
				if (KafkaInboundGateway.this.retryTemplate != null) {
					AtomicInteger deliveryAttempt =
							new AtomicInteger(((RetryContext) ATTRIBUTES_HOLDER.get()).getRetryCount() + 1);
					rawHeaders.put(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, deliveryAttempt);
				}
				else if (KafkaInboundGateway.this.containerDeliveryAttemptPresent) {
					Header header = record.headers().lastHeader(KafkaHeaders.DELIVERY_ATTEMPT);
					rawHeaders.put(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT,
							new AtomicInteger(ByteBuffer.wrap(header.value()).getInt()));
				}
				if (KafkaInboundGateway.this.bindSourceRecord) {
					rawHeaders.put(IntegrationMessageHeaderAccessor.SOURCE_DATA, record);
				}
			}
			else {
				MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
				if (KafkaInboundGateway.this.retryTemplate != null) {
					AtomicInteger deliveryAttempt =
							new AtomicInteger(((RetryContext) ATTRIBUTES_HOLDER.get()).getRetryCount() + 1);
					builder.setHeader(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, deliveryAttempt);
				}
				else if (KafkaInboundGateway.this.containerDeliveryAttemptPresent) {
					Header header = record.headers().lastHeader(KafkaHeaders.DELIVERY_ATTEMPT);
					builder.setHeader(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT,
							new AtomicInteger(ByteBuffer.wrap(header.value()).getInt()));
				}
				if (KafkaInboundGateway.this.bindSourceRecord) {
					builder.setHeader(IntegrationMessageHeaderAccessor.SOURCE_DATA, record);
				}
				messageToReturn = builder.build();
			}
			return messageToReturn;
		}

		private Message<?> enhanceReply(Message<?> message, Message<?> reply) {
			AbstractIntegrationMessageBuilder<?> builder = null;
			MessageHeaders replyHeaders = reply.getHeaders();
			MessageHeaders requestHeaders = message.getHeaders();
			if (replyHeaders.get(KafkaHeaders.CORRELATION_ID) == null &&
					requestHeaders.get(KafkaHeaders.CORRELATION_ID) != null) {
				builder = getMessageBuilderFactory().fromMessage(reply)
						.setHeader(KafkaHeaders.CORRELATION_ID, requestHeaders.get(KafkaHeaders.CORRELATION_ID));
			}
			if (replyHeaders.get(KafkaHeaders.TOPIC) == null &&
					requestHeaders.get(KafkaHeaders.REPLY_TOPIC) != null) {
				if (builder == null) {
					builder = getMessageBuilderFactory().fromMessage(reply);
				}
				builder.setHeader(KafkaHeaders.TOPIC, requestHeaders.get(KafkaHeaders.REPLY_TOPIC));
			}
			if (replyHeaders.get(KafkaHeaders.PARTITION_ID) == null &&
					requestHeaders.get(KafkaHeaders.REPLY_PARTITION) != null) {
				if (builder == null) {
					builder = getMessageBuilderFactory().fromMessage(reply);
				}
				builder.setHeader(KafkaHeaders.PARTITION_ID, requestHeaders.get(KafkaHeaders.REPLY_PARTITION));
			}
			if (builder != null) {
				return builder.build();
			}
			return reply;
		}

		@Override
		public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
			if (KafkaInboundGateway.this.retryTemplate != null) {
				ATTRIBUTES_HOLDER.set(context);
			}
			return true;
		}

		@Override
		public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
				Throwable throwable) {

			ATTRIBUTES_HOLDER.remove();
		}

		@Override
		public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
				Throwable throwable) {
			// Empty
		}

	}

}
