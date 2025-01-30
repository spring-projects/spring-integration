/*
 * Copyright 2015-2025 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;

import org.springframework.core.AttributeAccessor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.core.Pausable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer;
import org.springframework.integration.kafka.support.RawRecordHeaderErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.JacksonJsonUtils;
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
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.DefaultKafkaHeaderMapper;
import org.springframework.kafka.support.JacksonPresent;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.converter.BatchMessageConverter;
import org.springframework.kafka.support.converter.ConversionException;
import org.springframework.kafka.support.converter.KafkaMessageHeaders;
import org.springframework.kafka.support.converter.MessageConverter;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;
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
public class KafkaMessageDrivenChannelAdapter<K, V> extends MessageProducerSupport
		implements KafkaInboundEndpoint, OrderlyShutdownCapable, Pausable {

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

		if (JacksonPresent.isJackson2Present()) {
			MessagingMessageConverter messageConverter = new MessagingMessageConverter();
			// For consistency with the rest of Spring Integration channel adapters
			messageConverter.setGenerateMessageId(true);
			messageConverter.setGenerateTimestamp(true);
			DefaultKafkaHeaderMapper headerMapper = new DefaultKafkaHeaderMapper();
			headerMapper.addTrustedPackages(JacksonJsonUtils.DEFAULT_TRUSTED_PACKAGES.toArray(new String[0]));
			messageConverter.setHeaderMapper(headerMapper);
			this.recordListener.setMessageConverter(messageConverter);
			this.batchListener.setMessageConverter(messageConverter);
		}
	}

	/**
	 * Set the message converter; must be a {@link RecordMessageConverter} or
	 * {@link BatchMessageConverter} depending on mode.
	 * @param messageConverter the converter.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		if (messageConverter instanceof RecordMessageConverter recordMessageConverter) {
			this.recordListener.setMessageConverter(recordMessageConverter);
		}
		else if (messageConverter instanceof BatchMessageConverter batchMessageConverter) {
			this.batchListener.setBatchMessageConverter(batchMessageConverter);
		}
		else {
			throw new IllegalArgumentException(
					"Message converter must be a 'RecordMessageConverter' or 'BatchMessageConverter'");
		}
	}

	/**
	 * Set the message converter to use with a record-based consumer.
	 * @param messageConverter the converter.
	 */
	public void setRecordMessageConverter(RecordMessageConverter messageConverter) {
		this.recordListener.setMessageConverter(messageConverter);
	}

	/**
	 * Set the message converter to use with a batch-based consumer.
	 * @param messageConverter the converter.
	 */
	public void setBatchMessageConverter(BatchMessageConverter messageConverter) {
		this.batchListener.setBatchMessageConverter(messageConverter);
	}

	/**
	 * Specify a {@link RecordFilterStrategy} to wrap
	 * {@link KafkaMessageDrivenChannelAdapter.IntegrationRecordMessageListener} into
	 * {@link FilteringMessageListenerAdapter}.
	 * @param recordFilterStrategy the {@link RecordFilterStrategy} to use.
	 */
	public void setRecordFilterStrategy(RecordFilterStrategy<K, V> recordFilterStrategy) {
		this.recordFilterStrategy = recordFilterStrategy;
	}

	/**
	 * A {@code boolean} flag to indicate if {@link FilteringMessageListenerAdapter}
	 * should acknowledge discarded records or not.
	 * Does not make sense if {@link #setRecordFilterStrategy(RecordFilterStrategy)} isn't specified.
	 * @param ackDiscarded true to ack (commit offset for) discarded messages.
	 */
	public void setAckDiscarded(boolean ackDiscarded) {
		this.ackDiscarded = ackDiscarded;
	}

	/**
	 * Specify a {@link RetryTemplate} instance to use for retrying deliveries.
	 * <p>
	 * IMPORTANT: This form of retry is blocking and could cause a rebalance if the
	 * aggregate retry delays across all polled records might exceed the
	 * {@code max.poll.interval.ms}. Instead, consider adding a
	 * {@code DefaultErrorHandler} to the listener container, configured with a
	 * {@link KafkaErrorSendingMessageRecoverer}.
	 * @param retryTemplate the {@link RetryTemplate} to use.
	 */
	public void setRetryTemplate(RetryTemplate retryTemplate) {
		Assert.isTrue(retryTemplate == null || this.mode.equals(ListenerMode.record),
				"Retry is not supported with mode=batch");
		this.retryTemplate = retryTemplate;
	}

	/**
	 * A {@link RecoveryCallback} instance for retry operation; if null, the exception
	 * will be thrown to the container after retries are exhausted (unless an error
	 * channel is configured). Only used if a
	 * {@link #setRetryTemplate(RetryTemplate)} is specified. Default is an
	 * {@link ErrorMessageSendingRecoverer} if an error channel has been provided. Set to
	 * null if you wish to throw the exception back to the container after retries are exhausted.
	 * @param recoveryCallback the recovery callback.
	 */
	public void setRecoveryCallback(RecoveryCallback<?> recoveryCallback) {
		this.recoveryCallback = recoveryCallback;
	}

	/**
	 * The {@code boolean} flag to specify the order in which the filter and retry
	 * operations are performed.
	 * Does not make sense if only one of {@link RetryTemplate} or
	 * {@link RecordFilterStrategy} is present, or none.
	 * When true, the filter is called for each retry; when false, the filter is only
	 * called once for each delivery from the container.
	 * @param filterInRetry true to filter for each retry. Defaults to {@code false}.
	 */
	public void setFilterInRetry(boolean filterInRetry) {
		this.filterInRetry = filterInRetry;
	}

	/**
	 * When using a type-aware message converter such as {@code StringJsonMessageConverter},
	 * set the payload type the converter should create. Defaults to {@link Object}.
	 * @param payloadType the type.
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

		ContainerProperties containerProperties = this.messageListenerContainer.getContainerProperties();
		if (this.mode.equals(ListenerMode.record)) {
			MessageListener<K, V> listener = this.recordListener;

			boolean doFilterInRetry =
					this.filterInRetry && this.retryTemplate != null && this.recordFilterStrategy != null;

			if (this.retryTemplate != null) {
				MessageChannel errorChannel = getErrorChannel();
				if (this.recoveryCallback != null && errorChannel != null) {
					this.recoveryCallback = new ErrorMessageSendingRecoverer(errorChannel, getErrorMessageStrategy());
				}
			}
			if (!doFilterInRetry && this.recordFilterStrategy != null) {
				listener = new FilteringMessageListenerAdapter<>(listener, this.recordFilterStrategy,
						this.ackDiscarded);
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
	 * @param conversionError a conversion error occurred.
	 */
	private void setAttributesIfNecessary(Object record, @Nullable Message<?> message, boolean conversionError) {
		boolean needHolder = ATTRIBUTES_HOLDER.get() == null
				&& (getErrorChannel() != null && (this.retryTemplate == null || conversionError));
		boolean needAttributes = needHolder || this.retryTemplate != null;
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

	private class IntegrationRecordMessageListener extends RecordMessagingMessageListenerAdapter<K, V> {

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
			Message<?> message;
			try {
				message = toMessagingMessage(record, acknowledgment, consumer);
			}
			catch (RuntimeException ex) {
				if (KafkaMessageDrivenChannelAdapter.this.retryTemplate == null) {
					setAttributesIfNecessary(record, null, true);
				}

				RuntimeException exception = new ConversionException("Failed to convert to message", record, ex);
				if (sendErrorMessageIfNecessary(null, exception)) {
					return;
				}
				else {
					throw ex;
				}
			}

			RetryTemplate template = KafkaMessageDrivenChannelAdapter.this.retryTemplate;
			if (template != null) {
				doWithRetry(template, KafkaMessageDrivenChannelAdapter.this.recoveryCallback, record, acknowledgment,
						consumer, () -> {
							if (!KafkaMessageDrivenChannelAdapter.this.filterInRetry || passesFilter(record)) {
								sendMessageIfAny(enhanceHeadersAndSaveAttributes(message, record), record);
							}
						});
			}
			else {
				sendMessageIfAny(enhanceHeadersAndSaveAttributes(message, record), record);
			}
		}

		private boolean passesFilter(ConsumerRecord<K, V> record) {
			RecordFilterStrategy<K, V> filter = KafkaMessageDrivenChannelAdapter.this.recordFilterStrategy;
			return filter == null || !filter.filter(record);
		}

		private Message<?> enhanceHeadersAndSaveAttributes(Message<?> message, ConsumerRecord<K, V> record) {
			Supplier<Message<?>> messageSupplier = () -> message;
			BiConsumer<String, Object> headersAcceptor;

			if (message.getHeaders() instanceof KafkaMessageHeaders kafkaMessageHeaders) {
				Map<String, Object> rawHeaders = kafkaMessageHeaders.getRawHeaders();
				headersAcceptor = rawHeaders::put;
			}
			else {
				MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
				headersAcceptor = builder::setHeader;
				messageSupplier = builder::build;
			}

			if (KafkaMessageDrivenChannelAdapter.this.retryTemplate != null) {
				AtomicInteger deliveryAttempt =
						new AtomicInteger(((RetryContext) ATTRIBUTES_HOLDER.get()).getRetryCount() + 1);
				headersAcceptor.accept(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, deliveryAttempt);
			}
			else if (KafkaMessageDrivenChannelAdapter.this.containerDeliveryAttemptPresent) {
				Header header = record.headers().lastHeader(KafkaHeaders.DELIVERY_ATTEMPT);
				headersAcceptor.accept(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT,
						new AtomicInteger(ByteBuffer.wrap(header.value()).getInt()));
			}
			if (KafkaMessageDrivenChannelAdapter.this.bindSourceRecord) {
				headersAcceptor.accept(IntegrationMessageHeaderAccessor.SOURCE_DATA, record);
			}

			Message<?> messageToReturn = messageSupplier.get();

			setAttributesIfNecessary(record, messageToReturn, false);
			return messageToReturn;
		}

	}

	private class IntegrationBatchMessageListener extends BatchMessagingMessageListenerAdapter<K, V> {

		IntegrationBatchMessageListener() {
			super(null, null); // NOSONAR - out of use
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
			if (!KafkaMessageDrivenChannelAdapter.this.filterInRetry) {
				message = toMessage(records, acknowledgment, consumer);
			}
			if (message != null) {
				sendMessageIfAny(message, records);
			}
		}

		@Nullable
		private Message<?> toMessage(List<ConsumerRecord<K, V>> records, Acknowledgment acknowledgment,
				Consumer<?, ?> consumer) {

			Message<?> message = null;
			try {
				message = toMessagingMessage(records, acknowledgment, consumer);
				setAttributesIfNecessary(records, message, false);
			}
			catch (RuntimeException ex) {
				Exception exception = new ConversionException("Failed to convert to message",
						new ArrayList<>(records), ex);
				MessageChannel errorChannel = getErrorChannel();
				if (errorChannel != null) {
					getMessagingTemplate().send(errorChannel, buildErrorMessage(message, exception));
				}
				else {
					throw ex;
				}
			}
			return message;
		}

	}

}
