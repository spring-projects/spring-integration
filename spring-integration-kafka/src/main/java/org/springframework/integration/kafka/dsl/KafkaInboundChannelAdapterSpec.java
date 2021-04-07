/*
 * Copyright 2018-2020 the original author or authors.
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

import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.kafka.inbound.KafkaMessageSource;
import org.springframework.integration.kafka.inbound.KafkaMessageSource.KafkaAckCallbackFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConsumerProperties;
import org.springframework.kafka.support.converter.RecordMessageConverter;

/**
 * Spec for a polled Apache Kafka inbound channel adapter.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 *
 * @author Gary Russell
 * @author Anshul Mehra
 * @author Artem Bilan
 *
 * @since 5.4
 *
 */
public class KafkaInboundChannelAdapterSpec<K, V>
		extends MessageSourceSpec<KafkaInboundChannelAdapterSpec<K, V>, KafkaMessageSource<K, V>> {

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
	 * Set the message converter to replace the default.
	 * {@link org.springframework.kafka.support.converter.MessagingMessageConverter}.
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
	 * Set to true to include the raw {@link org.apache.kafka.clients.consumer.ConsumerRecord} as headers with keys
	 * {@link org.springframework.kafka.support.KafkaHeaders#RAW_DATA} and
	 * {@link org.springframework.integration.IntegrationMessageHeaderAccessor#SOURCE_DATA}. enabling callers to have
	 * access to the record to process errors.
	 * @param rawMessageHeader true to include the header.
	 * @return the spec.
	 */
	public KafkaInboundChannelAdapterSpec<K, V> rawMessageHeader(boolean rawMessageHeader) {
		this.target.setRawMessageHeader(rawMessageHeader);
		return this;
	}

}
