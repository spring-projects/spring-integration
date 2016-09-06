/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.kafka.inbound;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.adapter.FilteringAcknowledgingMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.listener.adapter.RecordMessagingMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.RetryingAcknowledgingMessageListenerAdapter;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.converter.MessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.retry.RecoveryCallback;
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
 *
 */
public class KafkaMessageDrivenChannelAdapter<K, V> extends MessageProducerSupport implements OrderlyShutdownCapable {

	private final AbstractMessageListenerContainer<K, V> messageListenerContainer;

	private final RecordMessagingMessageListenerAdapter<K, V> listener = new IntegrationMessageListener();

	private RecordFilterStrategy<K, V> recordFilterStrategy;

	private boolean ackDiscarded;

	private RetryTemplate retryTemplate;

	private RecoveryCallback<Void> recoveryCallback;

	private boolean filterInRetry;

	public KafkaMessageDrivenChannelAdapter(AbstractMessageListenerContainer<K, V> messageListenerContainer) {
		Assert.notNull(messageListenerContainer, "messageListenerContainer is required");
		Assert.isNull(messageListenerContainer.getContainerProperties().getMessageListener(),
				"Container must not already have a listener");
		this.messageListenerContainer = messageListenerContainer;
		this.messageListenerContainer.setAutoStartup(false);
	}

	/**
	 * Set the message converter; must be a {@link RecordMessageConverter}.
	 * @param messageConverter the converter.
	 * @deprecated in favor of {@link #setRecordMessageConverter(RecordMessageConverter)}.
	 */
	@Deprecated
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.isInstanceOf(RecordMessageConverter.class, messageConverter);
		this.listener.setMessageConverter((RecordMessageConverter) messageConverter);
	}

	/**
	 * Set the message converter to use with a record-based consumer.
	 * @param messageConverter the converter.
	 * @since 2.1
	 */
	public void setRecordMessageConverter(RecordMessageConverter messageConverter) {
		this.listener.setMessageConverter(messageConverter);
	}

	/**
	 * Specify a {@link RecordFilterStrategy} to wrap
	 * {@link KafkaMessageDrivenChannelAdapter.IntegrationMessageListener} into
	 * {@link FilteringAcknowledgingMessageListenerAdapter}.
	 * @param recordFilterStrategy the {@link RecordFilterStrategy} to use.
	 * @since 2.0.1
	 */
	public void setRecordFilterStrategy(RecordFilterStrategy<K, V> recordFilterStrategy) {
		this.recordFilterStrategy = recordFilterStrategy;
	}

	/**
	 * A {@code boolean} flag to indicate if {@link FilteringAcknowledgingMessageListenerAdapter}
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
	 * {@link KafkaMessageDrivenChannelAdapter.IntegrationMessageListener} into
	 * {@link RetryingAcknowledgingMessageListenerAdapter}.
	 * @param retryTemplate the {@link RetryTemplate} to use.
	 * @since 2.0.1
	 */
	public void setRetryTemplate(RetryTemplate retryTemplate) {
		this.retryTemplate = retryTemplate;
	}

	/**
	 * A {@link RecoveryCallback} instance for retry operation;
	 * if null, the exception will be thrown to the container after retries are exhausted.
	 * Does not make sense if {@link #setRetryTemplate(RetryTemplate)} isn't specified.
	 * @param recoveryCallback the recovery callback.
	 * @since 2.0.1
	 */
	public void setRecoveryCallback(RecoveryCallback<Void> recoveryCallback) {
		this.recoveryCallback = recoveryCallback;
	}

	/**
	 * The {@code boolean} flag to specify the order how
	 * {@link RetryingAcknowledgingMessageListenerAdapter} and
	 * {@link FilteringAcknowledgingMessageListenerAdapter} are wrapped to each other,
	 * if both of them are present.
	 * Does not make sense if only one of {@link RetryTemplate} or
	 * {@link RecordFilterStrategy} is present, or any.
	 * @param filterInRetry the order for {@link RetryingAcknowledgingMessageListenerAdapter} and
	 * {@link FilteringAcknowledgingMessageListenerAdapter} wrapping. Defaults to {@code false}.
	 * @since 2.0.1
	 */
	public void setFilterInRetry(boolean filterInRetry) {
		this.filterInRetry = filterInRetry;
	}

	@Override
	protected void onInit() {
		super.onInit();

		AcknowledgingMessageListener<K, V> listener = this.listener;

		boolean filterInRetry = this.filterInRetry && this.retryTemplate != null && this.recordFilterStrategy != null;

		if (filterInRetry) {
			listener = new FilteringAcknowledgingMessageListenerAdapter<>(listener, this.recordFilterStrategy,
					this.ackDiscarded);
			listener = new RetryingAcknowledgingMessageListenerAdapter<>(listener, this.retryTemplate,
					this.recoveryCallback);
		}
		else {
			if (this.retryTemplate != null) {
				listener = new RetryingAcknowledgingMessageListenerAdapter<>(listener, this.retryTemplate,
						this.recoveryCallback);
			}
			if (this.recordFilterStrategy != null) {
				listener = new FilteringAcknowledgingMessageListenerAdapter<>(listener, this.recordFilterStrategy,
						this.ackDiscarded);
			}
		}

		this.messageListenerContainer.getContainerProperties().setMessageListener(listener);
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
	public String getComponentType() {
		return "kafka:message-driven-channel-adapter";
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

	private class IntegrationMessageListener extends RecordMessagingMessageListenerAdapter<K, V> {

		IntegrationMessageListener() {
			super(null, null);
		}

		@Override
		public void onMessage(ConsumerRecord<K, V> record, Acknowledgment acknowledgment) {
			Message<?> message = toMessagingMessage(record, acknowledgment);
			sendMessage(message);
		}

	}

}
