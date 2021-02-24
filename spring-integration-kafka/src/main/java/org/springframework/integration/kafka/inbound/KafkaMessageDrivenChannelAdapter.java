/*
 * Copyright 2015-2021 the original author or authors.
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
import java.util.List;
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
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.kafka.support.RawRecordHeaderErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.adapter.BatchMessagingMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.FilteringBatchMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.FilteringMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.listener.adapter.RecordMessagingMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.converter.BatchMessageConverter;
import org.springframework.kafka.support.converter.ConversionException;
import org.springframework.kafka.support.converter.KafkaMessageHeaders;
import org.springframework.kafka.support.converter.MessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * Message-driven channel adapter.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 *
 * @author Marius Bogoevici
 * @author Gary Russell
 * @author Artem Bilan
 * @author Urs Keller
 *
 * @since 5.4
 */
public class KafkaMessageDrivenChannelAdapter<K, V> extends MessageProducerSupport implements OrderlyShutdownCapable,
		Pausable {

	private static final ThreadLocal<AttributeAccessor> ATTRIBUTES_HOLDER = new ThreadLocal<>();

	private final AbstractMessageListenerContainer<K, V> messageListenerContainer;

	private final IntegrationRecordMessageListener recordListener = new IntegrationRecordMessageListener();

	private final IntegrationBatchMessageListener batchListener = new IntegrationBatchMessageListener();

	private final ListenerMode mode;

	private RecordFilterStrategy<K, V> recordFilterStrategy;

	private boolean ackDiscarded;

	private RetryTemplate retryTemplate;

	private RecoveryCallback<?> recoveryCallback;

	private boolean filterInRetry;

	private BiConsumer<Map<TopicPartition, Long>, ConsumerSeekAware.ConsumerSeekCallback> onPartitionsAssignedSeekCallback;

	private boolean bindSourceRecord;

	private boolean containerDeliveryAttemptPresent;

	/**
	 * Construct an instance with mode {@link ListenerMode#record}.
	 * @param messageListenerContainer the container.
	 */
	public KafkaMessageDrivenChannelAdapter(AbstractMessageListenerContainer<K, V> messageListenerContainer) {
		this(messageListenerContainer, ListenerMode.record);
	}

	/**
	 * Construct an instance with the provided mode.
	 * @param messageListenerContainer the container.
	 * @param mode the mode.
	 * @since 1.2
	 */
	public KafkaMessageDrivenChannelAdapter(AbstractMessageListenerContainer<K, V> messageListenerContainer,
			ListenerMode mode) {

		Assert.notNull(messageListenerContainer, "messageListenerContainer is required");
		Assert.isNull(messageListenerContainer.getContainerProperties().getMessageListener(),
				"Container must not already have a listener");
		this.messageListenerContainer = messageListenerContainer;
		this.messageListenerContainer.setAutoStartup(false);
		this.mode = mode;
		setErrorMessageStrategy(new RawRecordHeaderErrorMessageStrategy());
	}

	/**
	 * Set the message converter; must be a {@link RecordMessageConverter} or
	 * {@link BatchMessageConverter} depending on mode.
	 * @param messageConverter the converter.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		if (messageConverter instanceof RecordMessageConverter) {
			this.recordListener.setMessageConverter((RecordMessageConverter) messageConverter);
		}
		else if (messageConverter instanceof BatchMessageConverter) {
			this.batchListener.setBatchMessageConverter((BatchMessageConverter) messageConverter);
		}
		else {
			throw new IllegalArgumentException(
					"Message converter must be a 'RecordMessageConverter' or 'BatchMessageConverter'");
		}

	}

	/**
	 * Set the message converter to use with a record-based consumer.
	 * @param messageConverter the converter.
	 * @since 2.1
	 */
	public void setRecordMessageConverter(RecordMessageConverter messageConverter) {
		this.recordListener.setMessageConverter(messageConverter);
	}

	/**
	 * Set the message converter to use with a batch-based consumer.
	 * @param messageConverter the converter.
	 * @since 2.1
	 */
	public void setBatchMessageConverter(BatchMessageConverter messageConverter) {
		this.batchListener.setBatchMessageConverter(messageConverter);
	}

	/**
	 * Specify a {@link RecordFilterStrategy} to wrap
	 * {@link KafkaMessageDrivenChannelAdapter.IntegrationRecordMessageListener} into
	 * {@link FilteringMessageListenerAdapter}.
	 * @param recordFilterStrategy the {@link RecordFilterStrategy} to use.
	 * @since 2.0.1
	 */
	public void setRecordFilterStrategy(RecordFilterStrategy<K, V> recordFilterStrategy) {
		this.recordFilterStrategy = recordFilterStrategy;
	}

	/**
	 * A {@code boolean} flag to indicate if {@link FilteringMessageListenerAdapter}
	 * should acknowledge discarded records or not.
	 * Does not make sense if {@link #setRecordFilterStrategy(RecordFilterStrategy)} isn't specified.
	 * @param ackDiscarded true to ack (commit offset for) discarded messages.
	 * @since 2.0.1
	 */
	public void setAckDiscarded(boolean ackDiscarded) {
		this.ackDiscarded = ackDiscarded;
	}

	/**
	 * Specify a {@link RetryTemplate} instance to wrap
	 * {@link KafkaMessageDrivenChannelAdapter.IntegrationRecordMessageListener} into
	 * {@link RetryingMessageListenerAdapter}.
	 * @param retryTemplate the {@link RetryTemplate} to use.
	 * @since 2.0.1
	 */
	public void setRetryTemplate(RetryTemplate retryTemplate) {
		Assert.isTrue(retryTemplate == null || this.mode.equals(ListenerMode.record),
				"Retry is not supported with mode=batch");
		this.retryTemplate = retryTemplate;
	}

	/**
	 * A {@link RecoveryCallback} instance for retry operation;
	 * if null, the exception will be thrown to the container after retries are exhausted
	 * (unless an error channel is configured).
	 * Does not make sense if {@link #setRetryTemplate(RetryTemplate)} isn't specified.
	 * @param recoveryCallback the recovery callback.
	 * @since 2.0.1
	 */
	public void setRecoveryCallback(RecoveryCallback<?> recoveryCallback) {
		this.recoveryCallback = recoveryCallback;
	}

	/**
	 * The {@code boolean} flag to specify the order how
	 * {@link RetryingMessageListenerAdapter} and
	 * {@link FilteringMessageListenerAdapter} are wrapped to each other,
	 * if both of them are present.
	 * Does not make sense if only one of {@link RetryTemplate} or
	 * {@link RecordFilterStrategy} is present, or any.
	 * @param filterInRetry the order for {@link RetryingMessageListenerAdapter} and
	 * {@link FilteringMessageListenerAdapter} wrapping. Defaults to {@code false}.
	 * @since 2.0.1
	 */
	public void setFilterInRetry(boolean filterInRetry) {
		this.filterInRetry = filterInRetry;
	}

	/**
	 * When using a type-aware message converter (such as {@code StringJsonMessageConverter},
	 * set the payload type the converter should create. Defaults to {@link Object}.
	 * @param payloadType the type.
	 * @since 2.1.1
	 */
	public void setPayloadType(Class<?> payloadType) {
		this.recordListener.setFallbackType(payloadType);
		this.batchListener.setFallbackType(payloadType);
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
	 * Does not apply to batch listeners.
	 * @param bindSourceRecord true to bind.
	 * @since 3.1.4
	 */
	public void setBindSourceRecord(boolean bindSourceRecord) {
		this.bindSourceRecord = bindSourceRecord;
	}

	@Override
	public String getComponentType() {
		return "kafka:message-driven-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();

		if (this.retryTemplate != null) {
			Assert.state(getErrorChannel() == null, "Cannot have an 'errorChannel' property when a 'RetryTemplate' is "
					+ "provided; use an 'ErrorMessageSendingRecoverer' in the 'recoveryCallback' property to "
					+ "send an error message when retries are exhausted");
		}
		ContainerProperties containerProperties = this.messageListenerContainer.getContainerProperties();
		if (this.mode.equals(ListenerMode.record)) {
			MessageListener<K, V> listener = this.recordListener;

			boolean doFilterInRetry = this.filterInRetry && this.retryTemplate != null
					&& this.recordFilterStrategy != null;

			if (doFilterInRetry) {
				listener = new FilteringMessageListenerAdapter<>(listener, this.recordFilterStrategy,
						this.ackDiscarded);
				listener = new RetryingMessageListenerAdapter<>(listener, this.retryTemplate,
						this.recoveryCallback);
				this.retryTemplate.registerListener(this.recordListener);
			}
			else {
				if (this.retryTemplate != null) {
					listener = new RetryingMessageListenerAdapter<>(listener, this.retryTemplate,
							this.recoveryCallback);
					this.retryTemplate.registerListener(this.recordListener);
				}
				if (this.recordFilterStrategy != null) {
					listener = new FilteringMessageListenerAdapter<>(listener, this.recordFilterStrategy,
							this.ackDiscarded);
				}
			}
			containerProperties.setMessageListener(listener);
		}
		else {
			BatchMessageListener<K, V> listener = this.batchListener;

			if (this.recordFilterStrategy != null) {
				listener = new FilteringBatchMessageListenerAdapter<>(listener, this.recordFilterStrategy,
						this.ackDiscarded);
			}
			containerProperties.setMessageListener(listener);
		}
		this.containerDeliveryAttemptPresent = containerProperties.isDeliveryAttemptHeader();
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
	 * @since 2.1.1
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

	private void sendMessageIfAny(Message<?> message, Object kafkaConsumedObject) {
		if (message != null) {
			try {
				sendMessage(message);
			}
			finally {
				if (KafkaMessageDrivenChannelAdapter.this.retryTemplate == null) {
					ATTRIBUTES_HOLDER.remove();
				}
			}
		}
		else {
			KafkaMessageDrivenChannelAdapter.this.logger.debug(() -> "Converter returned a null message for: "
					+ kafkaConsumedObject);
		}
	}

	/**
	 * The listener mode for the container, record or batch.
	 * @since 1.2
	 *
	 */
	public enum ListenerMode {

		/**
		 * Each {@link Message} will be converted from a single {@code ConsumerRecord}.
		 */
		record,

		/**
		 * Each {@link Message} will be converted from the {@code ConsumerRecords}
		 * returned by a poll.
		 */
		batch
	}

	private class IntegrationRecordMessageListener extends RecordMessagingMessageListenerAdapter<K, V>
			implements RetryListener {

		IntegrationRecordMessageListener() {
			super(null, null); // NOSONAR - out of use
		}

		@Override
		public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
			if (KafkaMessageDrivenChannelAdapter.this.onPartitionsAssignedSeekCallback != null) {
				KafkaMessageDrivenChannelAdapter.this.onPartitionsAssignedSeekCallback.accept(assignments, callback);
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
				RuntimeException exception = new ConversionException("Failed to convert to message for: " + record, e);
				sendErrorMessageIfNecessary(null, exception);
			}

			sendMessageIfAny(message, record);
		}

		private Message<?> enhanceHeaders(Message<?> message, ConsumerRecord<K, V> record) {
			Message<?> messageToReturn = message;
			if (message.getHeaders() instanceof KafkaMessageHeaders) {
				Map<String, Object> rawHeaders = ((KafkaMessageHeaders) message.getHeaders()).getRawHeaders();
				if (KafkaMessageDrivenChannelAdapter.this.retryTemplate != null) {
					AtomicInteger deliveryAttempt =
							new AtomicInteger(((RetryContext) ATTRIBUTES_HOLDER.get()).getRetryCount() + 1);
					rawHeaders.put(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, deliveryAttempt);
				}
				else if (KafkaMessageDrivenChannelAdapter.this.containerDeliveryAttemptPresent) {
					Header header = record.headers().lastHeader(KafkaHeaders.DELIVERY_ATTEMPT);
					rawHeaders.put(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT,
							new AtomicInteger(ByteBuffer.wrap(header.value()).getInt()));
				}
				if (KafkaMessageDrivenChannelAdapter.this.bindSourceRecord) {
					rawHeaders.put(IntegrationMessageHeaderAccessor.SOURCE_DATA, record);
				}
			}
			else {
				MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
				if (KafkaMessageDrivenChannelAdapter.this.retryTemplate != null) {
					AtomicInteger deliveryAttempt =
							new AtomicInteger(((RetryContext) ATTRIBUTES_HOLDER.get()).getRetryCount() + 1);
					builder.setHeader(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, deliveryAttempt);
				}
				else if (KafkaMessageDrivenChannelAdapter.this.containerDeliveryAttemptPresent) {
					Header header = record.headers().lastHeader(KafkaHeaders.DELIVERY_ATTEMPT);
					builder.setHeader(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT,
							new AtomicInteger(ByteBuffer.wrap(header.value()).getInt()));
				}
				if (KafkaMessageDrivenChannelAdapter.this.bindSourceRecord) {
					builder.setHeader(IntegrationMessageHeaderAccessor.SOURCE_DATA, record);
				}
				messageToReturn = builder.build();
			}
			return messageToReturn;
		}

		@Override
		public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
			if (KafkaMessageDrivenChannelAdapter.this.retryTemplate != null) {
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

	private class IntegrationBatchMessageListener extends BatchMessagingMessageListenerAdapter<K, V>
			implements RetryListener {

		IntegrationBatchMessageListener() {
			super(null, null); // NOSONAR - out if use
		}

		@Override
		public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
			if (KafkaMessageDrivenChannelAdapter.this.onPartitionsAssignedSeekCallback != null) {
				KafkaMessageDrivenChannelAdapter.this.onPartitionsAssignedSeekCallback.accept(assignments, callback);
			}
		}

		@Override
		public void onMessage(List<ConsumerRecord<K, V>> records, Acknowledgment acknowledgment,
				Consumer<?, ?> consumer) {

			Message<?> message = null;
			try {
				message = toMessagingMessage(records, acknowledgment, consumer);
				setAttributesIfNecessary(records, message);
			}
			catch (RuntimeException e) {
				Exception exception = new ConversionException("Failed to convert to message for: " + records, e);
				MessageChannel errorChannel = getErrorChannel();
				if (errorChannel != null) {
					getMessagingTemplate().send(errorChannel, new ErrorMessage(exception));
				}
			}

			sendMessageIfAny(message, records);
		}

		@Override
		public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
			if (KafkaMessageDrivenChannelAdapter.this.retryTemplate != null) {
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
