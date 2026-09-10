/*
 * Copyright 2026-present the original author or authors.
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

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ShareConsumer;
import org.jspecify.annotations.Nullable;

import org.springframework.core.AttributeAccessor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.kafka.support.RawRecordHeaderErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.JacksonMessagingUtils;
import org.springframework.kafka.listener.AbstractShareKafkaMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.adapter.MessagingMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.listener.adapter.ShareRecordMessagingMessageListenerAdapter;
import org.springframework.kafka.support.JacksonPresent;
import org.springframework.kafka.support.JsonKafkaHeaderMapper;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.ShareAcknowledgment;
import org.springframework.kafka.support.converter.ConversionException;
import org.springframework.kafka.support.converter.KafkaMessageHeaders;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Message-driven channel adapter for Kafka share groups (KIP-932 queues), backed by a
 * {@link AbstractShareKafkaMessageListenerContainer}.
 * <p>
 * Share consumers support only explicit topics (no topic patterns or partition
 * assignment), no batch listeners, and no consumer seeks; this adapter therefore has
 * no {@code ListenerMode} and no retry template, and does not implement
 * {@code Pausable}. It also has no flag to leave a {@link RecordFilterStrategy}-discarded
 * record unacknowledged (unlike {@link KafkaMessageDrivenChannelAdapter#setAckDiscarded}):
 * a discarded record is always terminally acknowledged, since an unacknowledged record in
 * a share group is simply redelivered to another consumer rather than left uncommitted.
 * <p>
 * The behavior depends on the container's {@link ContainerProperties.ShareAckMode}:
 * <ul>
 * <li>{@link ContainerProperties.ShareAckMode#EXPLICIT} (the default)
 * and {@link ContainerProperties.ShareAckMode#IMPLICIT} - the listener receives no
 * {@link ShareAcknowledgment}; the container itself sends
 * {@link org.apache.kafka.clients.consumer.AcknowledgeType#ACCEPT} after the flow
 * returns normally, and consults the container's
 * {@link org.springframework.kafka.listener.ShareConsumerRecordRecoverer}
 * (default: reject) when the flow throws.</li>
 * <li>{@link ContainerProperties.ShareAckMode#MANUAL} - the {@link ShareAcknowledgment} is provided in the
 * {@link KafkaHeaders#ACKNOWLEDGMENT} message header (together with the
 * {@link KafkaHeaders#CONSUMER} header), and whichever flow receives the message is
 * responsible for calling exactly one of {@link ShareAcknowledgment#acknowledge()},
 * {@link ShareAcknowledgment#release()}, or {@link ShareAcknowledgment#reject()} on it.
 * The container will not poll again - and will not stop -
 * until every record from the current poll has been terminally acknowledged, so a flow
 * that never acknowledges (or hands the message off without acknowledging) stalls the
 * consumer thread permanently. This adapter acknowledges on the flow's behalf only when
 * no flow ever received the message: it rejects a record that failed conversion, and
 * accepts a record discarded by a {@link RecordFilterStrategy}.</li>
 * </ul>
 *
 * @param <K> the key type.
 * @param <V> the value type.
 *
 * @author Artem Bilan
 *
 * @since 7.2
 *
 * @see AbstractShareKafkaMessageListenerContainer
 * @see ShareAcknowledgment
 */
public class KafkaShareMessageDrivenChannelAdapter<K, V> extends MessageProducerSupport
		implements KafkaInboundEndpoint, OrderlyShutdownCapable {

	private final AbstractShareKafkaMessageListenerContainer<K, V> shareListenerContainer;

	private final IntegrationRecordMessageListener recordListener = new IntegrationRecordMessageListener();

	private final IntegrationShareRecordMessageListener shareRecordListener =
			new IntegrationShareRecordMessageListener();

	private @Nullable RecordFilterStrategy<K, V> recordFilterStrategy;

	private boolean bindSourceRecord;

	/**
	 * Construct an instance with the provided container.
	 * @param shareListenerContainer the container.
	 */
	@SuppressWarnings({"this-escape", "removal"})
	public KafkaShareMessageDrivenChannelAdapter(
			AbstractShareKafkaMessageListenerContainer<K, V> shareListenerContainer) {

		Assert.notNull(shareListenerContainer, "shareListenerContainer is required");
		Assert.isNull(shareListenerContainer.getContainerProperties().getMessageListener(),
				"Container must not already have a listener");
		this.shareListenerContainer = shareListenerContainer;
		this.shareListenerContainer.setAutoStartup(false);
		setErrorMessageStrategy(new RawRecordHeaderErrorMessageStrategy());

		MessagingMessageConverter messageConverter = new MessagingMessageConverter();
		// For consistency with the rest of Spring Integration channel adapters
		messageConverter.setGenerateMessageId(true);
		messageConverter.setGenerateTimestamp(true);

		if (JacksonPresent.isJackson3Present()) {
			JsonKafkaHeaderMapper headerMapper = new JsonKafkaHeaderMapper();
			headerMapper.addTrustedPackages(
					JacksonMessagingUtils.DEFAULT_TRUSTED_PACKAGES
							.toArray(new String[0]));
			messageConverter.setHeaderMapper(headerMapper);
		}
		else if (JacksonPresent.isJackson2Present()) {
			var headerMapper = new org.springframework.kafka.support.DefaultKafkaHeaderMapper();
			headerMapper.addTrustedPackages(
					org.springframework.integration.support.json.JacksonJsonUtils.DEFAULT_TRUSTED_PACKAGES
							.toArray(new String[0]));
			messageConverter.setHeaderMapper(headerMapper);
		}

		this.recordListener.setMessageConverter(messageConverter);
		this.shareRecordListener.setMessageConverter(messageConverter);
	}

	/**
	 * Set the message converter to use.
	 * @param messageConverter the converter.
	 */
	public void setRecordMessageConverter(RecordMessageConverter messageConverter) {
		this.recordListener.setMessageConverter(messageConverter);
		this.shareRecordListener.setMessageConverter(messageConverter);
	}

	/**
	 * Specify a {@link RecordFilterStrategy} to discard records before they are
	 * converted and sent. A discarded record is terminally acknowledged (accepted) by
	 * this adapter when the container's {@link ContainerProperties.ShareAckMode} is
	 * {@code MANUAL}.
	 * @param recordFilterStrategy the {@link RecordFilterStrategy} to use.
	 */
	public void setRecordFilterStrategy(RecordFilterStrategy<K, V> recordFilterStrategy) {
		this.recordFilterStrategy = recordFilterStrategy;
	}

	/**
	 * When using a type-aware message converter such as {@code StringJsonMessageConverter},
	 * set the payload type the converter should create. Defaults to {@link Object}.
	 * @param payloadType the type.
	 */
	public void setPayloadType(Class<?> payloadType) {
		this.recordListener.setFallbackType(payloadType);
		this.shareRecordListener.setFallbackType(payloadType);
	}

	/**
	 * Set to true to bind the source consumer record in the header named
	 * {@link IntegrationMessageHeaderAccessor#SOURCE_DATA}.
	 * @param bindSourceRecord true to bind.
	 */
	public void setBindSourceRecord(boolean bindSourceRecord) {
		this.bindSourceRecord = bindSourceRecord;
	}

	@Override
	public String getComponentType() {
		return "kafka:share-message-driven-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();

		ContainerProperties containerProperties = this.shareListenerContainer.getContainerProperties();
		if (ContainerProperties.ShareAckMode.MANUAL.equals(containerProperties.getShareAckMode())) {
			if (getErrorChannel() != null) {
				this.logger.warn(() -> "An 'errorChannel' is configured together with ShareAckMode.MANUAL. " +
						"The error channel subscriber becomes responsible for terminally acknowledging " +
						"the failed record's ShareAcknowledgment; otherwise the consumer thread stalls " +
						"and the container cannot be stopped.");
			}
			containerProperties.setMessageListener(this.shareRecordListener);
		}
		else {
			containerProperties.setMessageListener(this.recordListener);
		}
	}

	@Override
	protected void doStart() {
		this.shareListenerContainer.start();
	}

	@Override
	protected void doStop() {
		this.shareListenerContainer.stop();
	}

	@Override
	public int beforeShutdown() {
		this.shareListenerContainer.stop();
		return getPhase();
	}

	@Override
	public int afterShutdown() {
		return getPhase();
	}

	@Override
	protected AttributeAccessor getErrorMessageAttributes(@Nullable Message<?> message) {
		AttributeAccessor attributes = ATTRIBUTES_HOLDER.get();
		if (attributes == null) {
			return super.getErrorMessageAttributes(message);
		}
		else {
			return attributes;
		}
	}

	/**
	 * Process a record delivered by either listener, converting it to a
	 * {@link Message} and sending it, or terminally acknowledging it on the flow's
	 * behalf when no flow ever receives it (filtered, or failed conversion).
	 * @param record the record.
	 * @param acknowledgment the acknowledgment, or {@code null} outside
	 * {@code ShareAckMode.MANUAL}.
	 * @param messageSupplier converts the record to a {@link Message}.
	 */
	private void sendRecord(ConsumerRecord<K, V> record, @Nullable ShareAcknowledgment acknowledgment,
			Supplier<Message<?>> messageSupplier) {

		RecordFilterStrategy<K, V> filter = this.recordFilterStrategy;
		if (filter != null && filter.filter(record)) {
			acknowledgeIfNecessary(acknowledgment, ShareAcknowledgment::acknowledge);
			return;
		}

		Message<?> message;
		try {
			message = messageSupplier.get();
		}
		catch (RuntimeException ex) {
			setAttributesIfNecessary(record, null);
			RuntimeException exception = new ConversionException("Failed to convert to message", record, ex);
			if (sendErrorMessageIfNecessary(null, exception)) {
				acknowledgeIfNecessary(acknowledgment, ShareAcknowledgment::reject);
				return;
			}
			else {
				throw ex;
			}
		}

		doSendMessage(enhanceHeadersAndSaveAttributes(message, record));
	}

	private void setAttributesIfNecessary(ConsumerRecord<K, V> record, @Nullable Message<?> message) {
		if (getErrorChannel() != null) {
			AttributeAccessor attributes = ATTRIBUTES_HOLDER.get();
			if (attributes == null) {
				attributes = ErrorMessageUtils.getAttributeAccessor(null, null);
				ATTRIBUTES_HOLDER.set(attributes);
			}
			attributes.setAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY, message);
			attributes.setAttribute(KafkaHeaders.RAW_DATA, record);
		}
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

		if (this.bindSourceRecord) {
			headersAcceptor.accept(IntegrationMessageHeaderAccessor.SOURCE_DATA, record);
		}

		Message<?> messageToReturn = messageSupplier.get();

		setAttributesIfNecessary(record, messageToReturn);
		return messageToReturn;
	}

	private void doSendMessage(Message<?> message) {
		try {
			sendMessage(message);
		}
		finally {
			ATTRIBUTES_HOLDER.remove();
		}
	}

	private static void acknowledgeIfNecessary(@Nullable ShareAcknowledgment acknowledgment,
			Consumer<ShareAcknowledgment> ackAction) {

		if (acknowledgment != null) {
			try {
				ackAction.accept(acknowledgment);
			}
			catch (IllegalStateException ex) {
				// already terminally acknowledged, presumably by a downstream flow
			}
		}
	}

	private class IntegrationShareRecordMessageListener
			extends ShareRecordMessagingMessageListenerAdapter<K, V> {

		IntegrationShareRecordMessageListener() {
			super(null, null, null);
		}

		@Override
		public void onShareRecord(ConsumerRecord<K, V> record,
				@Nullable ShareAcknowledgment acknowledgment, @Nullable ShareConsumer<?, ?> consumer) {

			sendRecord(record, acknowledgment, () -> toMessagingMessage(record, acknowledgment, consumer));
		}

	}

	private class IntegrationRecordMessageListener extends MessagingMessageListenerAdapter<K, V>
			implements MessageListener<K, V> {

		IntegrationRecordMessageListener() {
			super(null, null);
		}

		@Override
		public void onMessage(ConsumerRecord<K, V> record) {
			sendRecord(record, null, () -> toMessagingMessage(record, null, null));
		}

	}

}
