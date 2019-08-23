/*
 * Copyright 2018-2019 the original author or authors.
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

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.kafka.inbound.KafkaMessageSource;
import org.springframework.integration.kafka.inbound.KafkaMessageSource.KafkaAckCallbackFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConsumerProperties;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

/**
 * Spec for a polled Kafka inbound channel adapter.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 *
 * @author Gary Russell
 * @author Anshul Mehra
 *
 * @since 3.0.1
 *
 */
public class KafkaInboundChannelAdapterSpec<K, V>
		extends MessageSourceSpec<KafkaInboundChannelAdapterSpec<K, V>, KafkaMessageSource<K, V>> {

	/**
	 * Create an initial {@link KafkaMessageSource} with the consumer factory and
	 * topics.
	 * @param consumerFactory the consumer factory.
	 * @param allowMultiFetch true to allow {@code max.poll.records > 1}.
	 * @param topics the topics.
	 * @deprecated in favor of
	 * {@link #KafkaInboundChannelAdapterSpec(ConsumerFactory, ConsumerProperties, boolean)}
	 */
	@Deprecated
	KafkaInboundChannelAdapterSpec(ConsumerFactory<K, V> consumerFactory, boolean allowMultiFetch, String... topics) {
		this.target = new KafkaMessageSource<>(consumerFactory, new ConsumerProperties(topics), allowMultiFetch);
	}

	/**
	 * Create an initial {@link KafkaMessageSource} with the consumer factory and
	 * topics with a custom ack callback factory.
	 * @param consumerFactory the consumer factory.
	 * @param ackCallbackFactory the callback factory.
	 * @param allowMultiFetch true to allow {@code max.poll.records > 1}.
	 * @param topics the topics.
	 * @deprecated in favor of
	 * {@link #KafkaInboundChannelAdapterSpec(ConsumerFactory, ConsumerProperties,
	 * KafkaAckCallbackFactory, boolean)}
	 */
	@Deprecated
	KafkaInboundChannelAdapterSpec(ConsumerFactory<K, V> consumerFactory,
			KafkaAckCallbackFactory<K, V> ackCallbackFactory, boolean allowMultiFetch, String... topics) {

		this.target = new KafkaMessageSource<>(consumerFactory, new ConsumerProperties(topics), ackCallbackFactory, allowMultiFetch);
	}

	/**
	 * Create an initial {@link KafkaMessageSource} with the consumer factory and
	 * topics with a custom ack callback factory.
	 * @param consumerFactory the consumer factory.
	 * @param consumerProperties the consumer properties.
	 * @param allowMultiFetch true to allow {@code max.poll.records > 1}.
	 */
	KafkaInboundChannelAdapterSpec(ConsumerFactory<K, V> consumerFactory,
			ConsumerProperties consumerProperties, boolean allowMultiFetch) {
		this.target = new KafkaMessageSource<>(consumerFactory, consumerProperties, allowMultiFetch);
	}

	/**
	 * Create an initial {@link KafkaMessageSource} with the consumer factory and
	 * topics with a custom ack callback factory.
	 * @param consumerFactory the consumer factory.
	 * @param consumerProperties the consumer properties.
	 * @param ackCallbackFactory the callback factory.
	 * @param allowMultiFetch true to allow {@code max.poll.records > 1}.
	 */
	KafkaInboundChannelAdapterSpec(ConsumerFactory<K, V> consumerFactory,
			ConsumerProperties consumerProperties,
			KafkaAckCallbackFactory<K, V> ackCallbackFactory, boolean allowMultiFetch) {

		this.target = new KafkaMessageSource<>(consumerFactory, consumerProperties, ackCallbackFactory, allowMultiFetch);
	}

	/**
	 * Set the group.id property for the consumer.
	 * @param groupId the group id.
	 * @return the spec.
	 * @see ConsumerProperties
	 * @deprecated in favor of using {@link ConsumerProperties}
	 */
	@Deprecated
	public KafkaInboundChannelAdapterSpec<K, V> groupId(String groupId) {
		this.target.setGroupId(groupId);
		return this;
	}

	/**
	 * Set the client.id property for the consumer.
	 * @param clientId the client id.
	 * @return the spec.
	 * @see ConsumerProperties
	 * @deprecated in favor of using {@link ConsumerProperties}
	 */
	@Deprecated
	public KafkaInboundChannelAdapterSpec<K, V> clientId(String clientId) {
		this.target.setClientId(clientId);
		return this;
	}

	/**
	 * Set the pollTimeout for the poll() operations.
	 * @param pollTimeout the poll timeout.
	 * @return the spec.
	 * @see ConsumerProperties
	 * @deprecated in favor of using {@link ConsumerProperties}
	 */
	@Deprecated
	public KafkaInboundChannelAdapterSpec<K, V> pollTimeout(long pollTimeout) {
		this.target.setPollTimeout(pollTimeout);
		return this;
	}

	/**
	 * Set the message converter to replace the default.
	 * {@link MessagingMessageConverter}.
	 * @param messageConverter the converter.
	 * @return the spec.
	 */
	public KafkaInboundChannelAdapterSpec<K, V> messageConverter(RecordMessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return this;
	}

	/**
	 * Set the payload type.
	 * Only applies if a type-aware message converter is provided.
	 * @param type the type to convert to.
	 * @return the spec.
	 */
	public KafkaInboundChannelAdapterSpec<K, V> payloadType(Class<?> type) {
		this.target.setPayloadType(type);
		return this;
	}

	/**
	 * Set a rebalance listener.
	 * @param rebalanceListener the rebalance listener.
	 * @return the spec.
	 * @see ConsumerProperties
	 * @deprecated in favor of using {@link ConsumerProperties}
	 */
	@Deprecated
	public KafkaInboundChannelAdapterSpec<K, V> rebalanceListener(ConsumerRebalanceListener rebalanceListener) {
		this.target.setRebalanceListener(rebalanceListener);
		return this;
	}

	/**
	 * Set to true to include the raw {@link ConsumerRecord} as headers with keys
	 * {@link KafkaHeaders#RAW_DATA} and
	 * {@link IntegrationMessageHeaderAccessor#SOURCE_DATA}. enabling callers to have
	 * access to the record to process errors.
	 * @param rawMessageHeader true to include the header.
	 * @return the spec.
	 */
	public KafkaInboundChannelAdapterSpec<K, V> rawMessageHeader(boolean rawMessageHeader) {
		this.target.setRawMessageHeader(rawMessageHeader);
		return this;
	}

}
