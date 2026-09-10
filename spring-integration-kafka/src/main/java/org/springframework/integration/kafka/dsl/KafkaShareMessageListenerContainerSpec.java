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

package org.springframework.integration.kafka.dsl;

import java.time.Duration;

import org.apache.kafka.clients.consumer.AcknowledgementCommitCallback;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.kafka.core.ShareConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ShareConsumerRecordRecoverer;
import org.springframework.kafka.listener.ShareKafkaMessageListenerContainer;

/**
 * A helper class in the Builder pattern style to delegate options to the
 * {@link ShareKafkaMessageListenerContainer}.
 * <p>
 * Only options that the share consumer container actually honors are exposed here;
 * unlike {@link KafkaMessageListenerContainerSpec}, options such as {@code ackMode},
 * {@code pollTimeout}, {@code consumerRebalanceListener}, and {@code idleEventInterval}
 * are not applicable to share consumers and are deliberately omitted.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 *
 * @author Artem Bilan
 *
 * @since 7.2
 */
public class KafkaShareMessageListenerContainerSpec<K, V>
		extends IntegrationComponentSpec<KafkaShareMessageListenerContainerSpec<K, V>,
				ShareKafkaMessageListenerContainer<K, V>> {

	KafkaShareMessageListenerContainerSpec(ShareConsumerFactory<K, V> shareConsumerFactory, String... topics) {
		this(shareConsumerFactory, new ContainerProperties(topics));
	}

	KafkaShareMessageListenerContainerSpec(ShareConsumerFactory<K, V> shareConsumerFactory,
			ContainerProperties containerProperties) {

		this.target = new ShareKafkaMessageListenerContainer<>(shareConsumerFactory, containerProperties);
	}

	@Override
	public KafkaShareMessageListenerContainerSpec<K, V> id(String id) {
		return super.id(id);
	}

	/**
	 * Specify the number of consumer threads to create within the container. Each
	 * thread creates its own {@code ShareConsumer} instance and all of them
	 * participate in the same share group.
	 * @param concurrency the concurrency.
	 * @return the spec.
	 * @see ShareKafkaMessageListenerContainer#setConcurrency(int)
	 */
	public KafkaShareMessageListenerContainerSpec<K, V> concurrency(int concurrency) {
		this.target.setConcurrency(concurrency);
		return this;
	}

	/**
	 * Set the {@code client.id} to use for the consumer(s).
	 * @param clientId the client id.
	 * @return the spec.
	 * @see ShareKafkaMessageListenerContainer#setClientId(String)
	 */
	public KafkaShareMessageListenerContainerSpec<K, V> clientId(String clientId) {
		this.target.setClientId(clientId);
		return this;
	}

	/**
	 * Set the group id for this container. Overrides any {@code group.id} property
	 * provided by the consumer factory configuration.
	 * @param groupId the group id.
	 * @return the spec.
	 * @see ContainerProperties#setGroupId(String)
	 */
	public KafkaShareMessageListenerContainerSpec<K, V> groupId(String groupId) {
		this.target.getContainerProperties().setGroupId(groupId);
		return this;
	}

	/**
	 * Set the acknowledgment mode for the container.
	 * @param shareAckMode the {@link ContainerProperties.ShareAckMode}; default
	 * {@code EXPLICIT}.
	 * @return the spec.
	 * @see ContainerProperties#setShareAckMode(ContainerProperties.ShareAckMode)
	 */
	public KafkaShareMessageListenerContainerSpec<K, V> shareAckMode(ContainerProperties.ShareAckMode shareAckMode) {
		this.target.getContainerProperties().setShareAckMode(shareAckMode);
		return this;
	}

	/**
	 * Set the timeout for detecting unacknowledged records in
	 * {@link ContainerProperties.ShareAckMode#MANUAL} mode. Default 30 seconds.
	 * @param shareAcknowledgmentTimeout the timeout.
	 * @return the spec.
	 * @see ContainerProperties#setShareAcknowledgmentTimeout(Duration)
	 */
	public KafkaShareMessageListenerContainerSpec<K, V> shareAcknowledgmentTimeout(
			Duration shareAcknowledgmentTimeout) {

		this.target.getContainerProperties().setShareAcknowledgmentTimeout(shareAcknowledgmentTimeout);
		return this;
	}

	/**
	 * Set whether to use {@code commitSync()} or {@code commitAsync()} for share
	 * consumer acknowledgment commits. Default {@code true} (sync).
	 * @param syncShareCommits true to use {@code commitSync()}.
	 * @return the spec.
	 * @see ContainerProperties#setSyncShareCommits(boolean)
	 */
	public KafkaShareMessageListenerContainerSpec<K, V> syncShareCommits(boolean syncShareCommits) {
		this.target.getContainerProperties().setSyncShareCommits(syncShareCommits);
		return this;
	}

	/**
	 * Set the callback to be invoked when acknowledgement commits complete; useful
	 * with {@link #syncShareCommits(boolean) syncShareCommits(false)} for visibility
	 * into async commit success or failure.
	 * @param acknowledgementCommitCallback the callback.
	 * @return the spec.
	 * @see ContainerProperties#setAcknowledgementCommitCallback(AcknowledgementCommitCallback)
	 */
	public KafkaShareMessageListenerContainerSpec<K, V> acknowledgementCommitCallback(
			AcknowledgementCommitCallback acknowledgementCommitCallback) {

		this.target.getContainerProperties().setAcknowledgementCommitCallback(acknowledgementCommitCallback);
		return this;
	}

	/**
	 * Set a {@link ShareConsumerRecordRecoverer} to use when a listener throws an
	 * exception. The recoverer determines whether to accept, release, or reject the
	 * failed record. Default: reject (archive, no redelivery).
	 * @param shareConsumerRecordRecoverer the recoverer.
	 * @return the spec.
	 * @see ShareKafkaMessageListenerContainer#setShareConsumerRecordRecoverer(ShareConsumerRecordRecoverer)
	 */
	public KafkaShareMessageListenerContainerSpec<K, V> shareConsumerRecordRecoverer(
			ShareConsumerRecordRecoverer shareConsumerRecordRecoverer) {

		this.target.setShareConsumerRecordRecoverer(shareConsumerRecordRecoverer);
		return this;
	}

	/**
	 * Set the executor for threads that poll the consumer(s). Needs at least
	 * {@link #concurrency(int) concurrency} threads.
	 * @param consumerTaskExecutor the executor.
	 * @return the spec.
	 * @see ContainerProperties#setListenerTaskExecutor(AsyncTaskExecutor)
	 */
	public KafkaShareMessageListenerContainerSpec<K, V> listenerTaskExecutor(AsyncTaskExecutor consumerTaskExecutor) {
		this.target.getContainerProperties().setListenerTaskExecutor(consumerTaskExecutor);
		return this;
	}

	/**
	 * Set the maximum time to wait for the consumer thread(s) to start.
	 * @param consumerStartTimeout the timeout.
	 * @return the spec.
	 * @see ContainerProperties#setConsumerStartTimeout(Duration)
	 */
	public KafkaShareMessageListenerContainerSpec<K, V> consumerStartTimeout(Duration consumerStartTimeout) {
		this.target.getContainerProperties().setConsumerStartTimeout(consumerStartTimeout);
		return this;
	}

}
