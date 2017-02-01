/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.integration.kafka.dsl;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ErrorHandler;
import org.springframework.kafka.listener.adapter.FilteringAcknowledgingMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.listener.adapter.RetryingAcknowledgingMessageListenerAdapter;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.kafka.support.TopicPartitionInitialOffset;
import org.springframework.kafka.support.converter.BatchMessageConverter;
import org.springframework.kafka.support.converter.MessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * A {@link MessageProducerSpec} implementation for the {@link KafkaMessageDrivenChannelAdapter}.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 * @param <S> the target {@link KafkaMessageDrivenChannelAdapterSpec} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
public class KafkaMessageDrivenChannelAdapterSpec<K, V, S extends KafkaMessageDrivenChannelAdapterSpec<K, V, S>>
		extends MessageProducerSpec<S, KafkaMessageDrivenChannelAdapter<K, V>> {

	KafkaMessageDrivenChannelAdapterSpec(AbstractMessageListenerContainer<K, V> messageListenerContainer,
			KafkaMessageDrivenChannelAdapter.ListenerMode listenerMode) {
		super(new KafkaMessageDrivenChannelAdapter<>(messageListenerContainer, listenerMode));
	}

	/**
	 * Set the message converter; must be a {@link RecordMessageConverter} or
	 * {@link BatchMessageConverter} depending on mode.
	 * @param messageConverter the converter.
	 * @return the spec
	 */
	public S messageConverter(MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * Set the message converter to use with a record-based consumer.
	 * @param messageConverter the converter.
	 * @return the spec
	 */
	public S recordMessageConverter(RecordMessageConverter messageConverter) {
		this.target.setRecordMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * Set the message converter to use with a batch-based consumer.
	 * @param messageConverter the converter.
	 * @return the spec
	 */
	public S batchMessageConverter(BatchMessageConverter messageConverter) {
		this.target.setBatchMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * Specify a {@link RecordFilterStrategy} to wrap
	 * {@code KafkaMessageDrivenChannelAdapter.IntegrationRecordMessageListener} into
	 * {@link FilteringAcknowledgingMessageListenerAdapter}.
	 * @param recordFilterStrategy the {@link RecordFilterStrategy} to use.
	 * @return the spec
	 */
	public S recordFilterStrategy(RecordFilterStrategy<K, V> recordFilterStrategy) {
		this.target.setRecordFilterStrategy(recordFilterStrategy);
		return _this();
	}

	/**
	 * A {@code boolean} flag to indicate if {@link FilteringAcknowledgingMessageListenerAdapter}
	 * should acknowledge discarded records or not.
	 * Does not make sense if {@link #recordFilterStrategy(RecordFilterStrategy)} isn't specified.
	 * @param ackDiscarded true to ack (commit offset for) discarded messages.
	 * @return the spec
	 */
	public S ackDiscarded(boolean ackDiscarded) {
		this.target.setAckDiscarded(ackDiscarded);
		return _this();
	}

	/**
	 * Specify a {@link RetryTemplate} instance to wrap
	 * {@code KafkaMessageDrivenChannelAdapter.IntegrationRecordMessageListener} into
	 * {@link RetryingAcknowledgingMessageListenerAdapter}.
	 * @param retryTemplate the {@link RetryTemplate} to use.
	 * @return the spec
	 */
	public S retryTemplate(RetryTemplate retryTemplate) {
		this.target.setRetryTemplate(retryTemplate);
		return _this();
	}

	/**
	 * A {@link RecoveryCallback} instance for retry operation;
	 * if null, the exception will be thrown to the container after retries are exhausted.
	 * Does not make sense if {@link #retryTemplate(RetryTemplate)} isn't specified.
	 * @param recoveryCallback the recovery callback.
	 * @return the spec
	 */
	public S recoveryCallback(RecoveryCallback<Void> recoveryCallback) {
		this.target.setRecoveryCallback(recoveryCallback);
		return _this();
	}

	/**
	 /**
	 * The {@code boolean} flag to specify the order how
	 * {@link RetryingAcknowledgingMessageListenerAdapter} and
	 * {@link FilteringAcknowledgingMessageListenerAdapter} are wrapped to each other,
	 * if both of them are present.
	 * Does not make sense if only one of {@link RetryTemplate} or
	 * {@link RecordFilterStrategy} is present, or any.
	 * @param filterInRetry the order for {@link RetryingAcknowledgingMessageListenerAdapter} and
	 * {@link FilteringAcknowledgingMessageListenerAdapter} wrapping. Defaults to {@code false}.
	 * @return the spec
	 */
	public S filterInRetry(boolean filterInRetry) {
		this.target.setFilterInRetry(filterInRetry);
		return _this();
	}

	/**
	 * A {@link ConcurrentMessageListenerContainer} configuration {@link KafkaMessageDrivenChannelAdapterSpec}
	 * extension.
	 * @param <K> the key type.
	 * @param <V> the value type.
	 */
	public static class KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V> extends
			KafkaMessageDrivenChannelAdapterSpec<K, V, KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V>>
			implements ComponentsRegistration {

		private final KafkaMessageListenerContainerSpec<K, V> spec;

		KafkaMessageDrivenChannelAdapterListenerContainerSpec(KafkaMessageListenerContainerSpec<K, V> spec,
				KafkaMessageDrivenChannelAdapter.ListenerMode listenerMode) {
			super(spec.container, listenerMode);
			this.spec = spec;
		}

		/**
		 * Configure a listener container by invoking the {@link Consumer} callback, with a
		 * {@link KafkaMessageListenerContainerSpec} argument.
		 * @param configurer the configurer Java 8 Lambda.
		 * @return the spec.
		 */
		public KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V> configureListenerContainer(
				Consumer<KafkaMessageListenerContainerSpec<K, V>> configurer) {
			Assert.notNull(configurer, "The 'configurer' cannot be null");
			configurer.accept(this.spec);
			return _this();
		}

		@Override
		public Collection<Object> getComponentsToRegister() {
			return Collections.singleton(this.spec.container);
		}

	}

	/**
	 * A helper class in the Builder pattern style to delegate options to the
	 * {@link ConcurrentMessageListenerContainer}.
	 *
	 * @param <K> the key type.
	 * @param <V> the value type.
	 */
	public static class KafkaMessageListenerContainerSpec<K, V> {

		private final ConcurrentMessageListenerContainer<K, V> container;


		KafkaMessageListenerContainerSpec(ConsumerFactory<K, V> consumerFactory,
				ContainerProperties containerProperties) {
			this.container = new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
		}

		KafkaMessageListenerContainerSpec(ConsumerFactory<K, V> consumerFactory,
				TopicPartitionInitialOffset... topicPartitions) {
			this(consumerFactory, new ContainerProperties(topicPartitions));
		}

		KafkaMessageListenerContainerSpec(ConsumerFactory<K, V> consumerFactory, String... topics) {
			this(consumerFactory, new ContainerProperties(topics));
		}

		KafkaMessageListenerContainerSpec(ConsumerFactory<K, V> consumerFactory, Pattern topicPattern) {
			this(consumerFactory, new ContainerProperties(topicPattern));
		}

		/**
		 * Specify a concurrency maximum number for the {@link AbstractMessageListenerContainer}.
		 * @param concurrency the concurrency maximum number.
		 * @return the spec.
		 * @see ConcurrentMessageListenerContainer#setConcurrency(int)
		 */
		public KafkaMessageListenerContainerSpec<K, V> concurrency(int concurrency) {
			this.container.setConcurrency(concurrency);
			return this;
		}

		/**
		 * Specify an {@link ErrorHandler} for the {@link AbstractMessageListenerContainer}.
		 * @param errorHandler the {@link ErrorHandler}.
		 * @return the spec.
		 * @see ErrorHandler
		 */
		public KafkaMessageListenerContainerSpec<K, V> errorHandler(ErrorHandler errorHandler) {
			this.container.getContainerProperties().setErrorHandler(errorHandler);
			return this;
		}

		/**
		 * Set the ack mode to use when auto ack (in the configuration properties) is false.
		 * <ul>
		 * <li>RECORD: Ack after each record has been passed to the listener.</li>
		 * <li>BATCH: Ack after each batch of records received from the consumer has been
		 * passed to the listener</li>
		 * <li>TIME: Ack after this number of milliseconds; (should be greater than
		 * {@code #setPollTimeout(long) pollTimeout}.</li>
		 * <li>COUNT: Ack after at least this number of records have been received</li>
		 * <li>MANUAL: Listener is responsible for acking - use a
		 * {@link AcknowledgingMessageListener}.
		 * </ul>
		 * @param ackMode the {@link AbstractMessageListenerContainer.AckMode}; default BATCH.
		 * @return the spec.
		 * @see AbstractMessageListenerContainer.AckMode
		 */
		public KafkaMessageListenerContainerSpec<K, V> ackMode(AbstractMessageListenerContainer.AckMode ackMode) {
			this.container.getContainerProperties().setAckMode(ackMode);
			return this;
		}

		/**
		 * Set the max time to block in the consumer waiting for records.
		 * @param pollTimeout the timeout in ms; default 1000.
		 * @return the spec.
		 * @see ContainerProperties#setPollTimeout(long)
		 */
		public KafkaMessageListenerContainerSpec<K, V> pollTimeout(long pollTimeout) {
			this.container.getContainerProperties().setPollTimeout(pollTimeout);
			return this;
		}

		/**
		 * Set the number of outstanding record count after which offsets should be
		 * committed when {@link AbstractMessageListenerContainer.AckMode#COUNT}
		 * or {@link AbstractMessageListenerContainer.AckMode#COUNT_TIME} is being used.
		 * @param count the count
		 * @return the spec.
		 * @see ContainerProperties#setAckCount(int)
		 */
		public KafkaMessageListenerContainerSpec<K, V> ackCount(int count) {
			this.container.getContainerProperties().setAckCount(count);
			return this;
		}

		/**
		 * Set the time (ms) after which outstanding offsets should be committed when
		 * {@link AbstractMessageListenerContainer.AckMode#TIME} or
		 * {@link AbstractMessageListenerContainer.AckMode#COUNT_TIME} is being used.
		 * Should be larger than zero.
		 * @param millis the time
		 * @return the spec.
		 * @see ContainerProperties#setAckTime(long)
		 */
		public KafkaMessageListenerContainerSpec<K, V> ackTime(long millis) {
			this.container.getContainerProperties().setAckTime(millis);
			return this;
		}

		/**
		 * Set the executor for threads that poll the consumer.
		 * @param consumerTaskExecutor the executor
		 * @return the spec.
		 * @see ContainerProperties#setConsumerTaskExecutor(AsyncListenableTaskExecutor)
		 */
		public KafkaMessageListenerContainerSpec<K, V> consumerTaskExecutor(
				AsyncListenableTaskExecutor consumerTaskExecutor) {
			this.container.getContainerProperties().setConsumerTaskExecutor(consumerTaskExecutor);
			return this;
		}

		/**
		 * Set the executor for threads that invoke the listener.
		 * @param listenerTaskExecutor the executor
		 * @return the spec.
		 * @see ContainerProperties#setListenerTaskExecutor(AsyncListenableTaskExecutor)
		 */
		public KafkaMessageListenerContainerSpec<K, V> listenerTaskExecutor(
				AsyncListenableTaskExecutor listenerTaskExecutor) {
			this.container.getContainerProperties().setListenerTaskExecutor(listenerTaskExecutor);
			return this;
		}

		/**
		 * When using Kafka group management and {@link #pauseEnabled(boolean)} is
		 * true, set the delay after which the consumer should be paused. Default 10000.
		 * @param pauseAfter the delay.
		 * @return the spec.
		 * @see ContainerProperties#setPauseAfter(long)
		 */
		public KafkaMessageListenerContainerSpec<K, V> pauseAfter(long pauseAfter) {
			this.container.getContainerProperties().setPauseAfter(pauseAfter);
			return this;
		}

		/**
		 * Set to true to avoid rebalancing when this consumer is slow or throws a
		 * qualifying exception - pause the consumer. Default: true.
		 * @param pauseEnabled true to pause.
		 * @return the spec.
		 * @see #pauseAfter(long)
		 * @see ContainerProperties#setPauseEnabled(boolean)
		 */
		public KafkaMessageListenerContainerSpec<K, V> pauseEnabled(boolean pauseEnabled) {
			this.container.getContainerProperties().setPauseEnabled(pauseEnabled);
			return this;
		}

		/**
		 * Set the queue depth for handoffs from the consumer thread to the listener
		 * thread. Default 1 (up to 2 in process).
		 * @param queueDepth the queue depth.
		 * @return the spec.
		 * @see ContainerProperties#setQueueDepth(int)
		 */
		public KafkaMessageListenerContainerSpec<K, V> queueDepth(int queueDepth) {
			this.container.getContainerProperties().setQueueDepth(queueDepth);
			return this;
		}

		/**
		 * Set the timeout for shutting down the container. This is the maximum amount of
		 * time that the invocation to {@code #stop(Runnable)} will block for, before
		 * returning.
		 * @param shutdownTimeout the shutdown timeout.
		 * @return the spec.
		 * @see ContainerProperties#setShutdownTimeout(long)
		 */
		public KafkaMessageListenerContainerSpec<K, V> shutdownTimeout(long shutdownTimeout) {
			this.container.getContainerProperties().setShutdownTimeout(shutdownTimeout);
			return this;
		}

		/**
		 * Set the user defined {@link ConsumerRebalanceListener} implementation.
		 * @param consumerRebalanceListener the {@link ConsumerRebalanceListener} instance
		 * @return the spec.
		 * @see ContainerProperties#setConsumerRebalanceListener(ConsumerRebalanceListener)
		 */
		public KafkaMessageListenerContainerSpec<K, V> consumerRebalanceListener(
				ConsumerRebalanceListener consumerRebalanceListener) {
			this.container.getContainerProperties().setConsumerRebalanceListener(consumerRebalanceListener);
			return this;
		}

		/**
		 * Set the commit callback; by default a simple logging callback is used to log
		 * success at DEBUG level and failures at ERROR level.
		 * @param commitCallback the callback.
		 * @return the spec.
		 * @see ContainerProperties#setCommitCallback(OffsetCommitCallback)
		 */
		public KafkaMessageListenerContainerSpec<K, V> commitCallback(OffsetCommitCallback commitCallback) {
			this.container.getContainerProperties().setCommitCallback(commitCallback);
			return this;
		}

		/**
		 * Set whether or not to call consumer.commitSync() or commitAsync() when the
		 * container is responsible for commits. Default true. See
		 * https://github.com/spring-projects/spring-kafka/issues/62 At the time of
		 * writing, async commits are not entirely reliable.
		 * @param syncCommits true to use commitSync().
		 * @return the spec.
		 * @see ContainerProperties#setSyncCommits(boolean)
		 */
		public KafkaMessageListenerContainerSpec<K, V> syncCommits(boolean syncCommits) {
			this.container.getContainerProperties().setSyncCommits(syncCommits);
			return this;
		}

		/**
		 * Set the idle event interval; when set, an event is emitted if a poll returns
		 * no records and this interval has elapsed since a record was returned.
		 * @param idleEventInterval the interval.
		 * @return the spec.
		 * @see ContainerProperties#setIdleEventInterval(Long)
		 */
		public KafkaMessageListenerContainerSpec<K, V> idleEventInterval(Long idleEventInterval) {
			this.container.getContainerProperties().setIdleEventInterval(idleEventInterval);
			return this;
		}

		/**
		 * Set whether the container should ack messages that throw exceptions or not.
		 * @param ackOnError whether the container should acknowledge messages that throw
		 * exceptions.
		 * @return the spec.
		 * @see ContainerProperties#setAckOnError(boolean)
		 */
		public KafkaMessageListenerContainerSpec<K, V> ackOnError(boolean ackOnError) {
			this.container.getContainerProperties().setAckOnError(ackOnError);
			return this;
		}

	}

}
