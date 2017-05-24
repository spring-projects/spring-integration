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

import java.util.regex.Pattern;

import org.apache.kafka.common.TopicPartition;

import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.kafka.support.TopicPartitionInitialOffset;

/**
 * Factory class for Apache Kafka components.
 *
 * @author Artem Bilan
 * @author Nasko Vasilev
 *
 * @since 3.0
 */
public final class Kafka {

	/**
	 * Create an initial {@link KafkaProducerMessageHandlerSpec}.
	 * @param kafkaTemplate the {@link KafkaTemplate} to use
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @param <S> the {@link KafkaProducerMessageHandlerSpec} extension type.
	 * @return the KafkaProducerMessageHandlerSpec.
	 */
	public static <K, V, S extends KafkaProducerMessageHandlerSpec<K, V, S>> KafkaProducerMessageHandlerSpec<K, V, S>
	outboundChannelAdapter(KafkaTemplate<K, V> kafkaTemplate) {
		return new KafkaProducerMessageHandlerSpec<>(kafkaTemplate);
	}

	/**
	 * Create an initial {@link KafkaProducerMessageHandlerSpec} with ProducerFactory.
	 * @param producerFactory the {@link ProducerFactory} Java 8 Lambda.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the KafkaProducerMessageHandlerSpec.
	 * @see <a href="https://kafka.apache.org/documentation.html#producerconfigs">Kafka Producer Configs</a>
	 */
	public static <K, V> KafkaProducerMessageHandlerSpec.KafkaProducerMessageHandlerTemplateSpec<K, V>
	outboundChannelAdapter(ProducerFactory<K, V> producerFactory) {
		return new KafkaProducerMessageHandlerSpec.KafkaProducerMessageHandlerTemplateSpec<>(producerFactory);
	}

	/**
	 * Create an initial {@link KafkaMessageDrivenChannelAdapterSpec}.
	 * @param listenerContainer the {@link AbstractMessageListenerContainer}.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @param <S> the {@link KafkaMessageDrivenChannelAdapterSpec} extension type.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.
	 */
	public static <K, V, S extends KafkaMessageDrivenChannelAdapterSpec<K, V, S>>
	KafkaMessageDrivenChannelAdapterSpec<K, V, S> messageDrivenChannelAdapter(
			AbstractMessageListenerContainer<K, V> listenerContainer) {
		return messageDrivenChannelAdapter(listenerContainer, KafkaMessageDrivenChannelAdapter.ListenerMode.record);
	}

	/**
	 * Create an initial {@link KafkaMessageDrivenChannelAdapterSpec}.
	 * @param listenerContainer the {@link AbstractMessageListenerContainer}.
	 * @param listenerMode the {@link KafkaMessageDrivenChannelAdapter.ListenerMode}.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @param <A> the {@link KafkaMessageDrivenChannelAdapterSpec} extension type.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.
	 */
	public static <K, V, A extends KafkaMessageDrivenChannelAdapterSpec<K, V, A>>
	KafkaMessageDrivenChannelAdapterSpec<K, V, A> messageDrivenChannelAdapter(
			AbstractMessageListenerContainer<K, V> listenerContainer,
			KafkaMessageDrivenChannelAdapter.ListenerMode listenerMode) {
		return new KafkaMessageDrivenChannelAdapterSpec<>(listenerContainer, listenerMode);
	}

	/**
	 * Create an initial
	 * {@link KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec}.
	 * @param consumerFactory the {@link ConsumerFactory}.
	 * @param containerProperties the {@link ContainerProperties} to use.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec.
	 */
	public static <K, V>
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V>
	messageDrivenChannelAdapter(ConsumerFactory<K, V> consumerFactory, ContainerProperties containerProperties) {
		return messageDrivenChannelAdapter(consumerFactory, containerProperties,
				KafkaMessageDrivenChannelAdapter.ListenerMode.record);
	}

	/**
	 * Create an initial
	 * {@link KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec}.
	 * @param consumerFactory the {@link ConsumerFactory}.
	 * @param containerProperties the {@link ContainerProperties} to use.
	 * @param listenerMode the {@link KafkaMessageDrivenChannelAdapter.ListenerMode}.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec.
	 */
	public static <K, V>
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V>
	messageDrivenChannelAdapter(ConsumerFactory<K, V> consumerFactory, ContainerProperties containerProperties,
			KafkaMessageDrivenChannelAdapter.ListenerMode listenerMode) {
		return messageDrivenChannelAdapter(
				new KafkaMessageDrivenChannelAdapterSpec.KafkaMessageListenerContainerSpec<>(consumerFactory,
						containerProperties), listenerMode);
	}

	/**
	 * Create an initial
	 * {@link KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec}.
	 * @param consumerFactory the {@link ConsumerFactory}.
	 * @param topicPartitions the {@link TopicPartition} vararg.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec.
	 */
	public static <K, V>
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V>
	messageDrivenChannelAdapter(ConsumerFactory<K, V> consumerFactory,
			TopicPartitionInitialOffset... topicPartitions) {
		return messageDrivenChannelAdapter(consumerFactory, KafkaMessageDrivenChannelAdapter.ListenerMode.record,
				topicPartitions);
	}

	/**
	 * Create an initial
	 * {@link KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec}.
	 * @param consumerFactory the {@link ConsumerFactory}.
	 * @param listenerMode the {@link KafkaMessageDrivenChannelAdapter.ListenerMode}.
	 * @param topicPartitions the {@link TopicPartition} vararg.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec.
	 */
	public static <K, V>
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V>
	messageDrivenChannelAdapter(ConsumerFactory<K, V> consumerFactory,
			KafkaMessageDrivenChannelAdapter.ListenerMode listenerMode,
			TopicPartitionInitialOffset... topicPartitions) {
		return messageDrivenChannelAdapter(
				new KafkaMessageDrivenChannelAdapterSpec.KafkaMessageListenerContainerSpec<>(consumerFactory,
						topicPartitions), listenerMode);
	}

	/**
	 * Create an initial
	 * {@link KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec}.
	 * @param consumerFactory the {@link ConsumerFactory}.
	 * @param topics the topics vararg.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec.
	 */
	public static <K, V>
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V>
	messageDrivenChannelAdapter(ConsumerFactory<K, V> consumerFactory, String... topics) {
		return messageDrivenChannelAdapter(consumerFactory, KafkaMessageDrivenChannelAdapter.ListenerMode.record,
				topics);
	}

	/**
	 * Create an initial
	 * {@link KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec}.
	 * @param consumerFactory the {@link ConsumerFactory}.
	 * @param listenerMode the {@link KafkaMessageDrivenChannelAdapter.ListenerMode}.
	 * @param topics the topics vararg.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec.
	 */
	public static <K, V>
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V>
	messageDrivenChannelAdapter(ConsumerFactory<K, V> consumerFactory,
			KafkaMessageDrivenChannelAdapter.ListenerMode listenerMode, String... topics) {
		return messageDrivenChannelAdapter(
				new KafkaMessageDrivenChannelAdapterSpec.KafkaMessageListenerContainerSpec<>(consumerFactory,
						topics), listenerMode);
	}

	/**
	 * Create an initial
	 * {@link KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec}.
	 * @param consumerFactory the {@link ConsumerFactory}.
	 * @param topicPattern the topicPattern vararg.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec.
	 */
	public static <K, V>
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V>
	messageDrivenChannelAdapter(ConsumerFactory<K, V> consumerFactory, Pattern topicPattern) {
		return messageDrivenChannelAdapter(consumerFactory, KafkaMessageDrivenChannelAdapter.ListenerMode.record,
				topicPattern);
	}

	/**
	 * Create an initial
	 * {@link KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec}.
	 * @param consumerFactory the {@link ConsumerFactory}.
	 * @param listenerMode the {@link KafkaMessageDrivenChannelAdapter.ListenerMode}.
	 * @param topicPattern the topicPattern vararg.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec.
	 */
	public static <K, V>
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V>
	messageDrivenChannelAdapter(ConsumerFactory<K, V> consumerFactory,
			KafkaMessageDrivenChannelAdapter.ListenerMode listenerMode, Pattern topicPattern) {
		return messageDrivenChannelAdapter(
				new KafkaMessageDrivenChannelAdapterSpec.KafkaMessageListenerContainerSpec<>(consumerFactory,
						topicPattern),
				listenerMode);
	}

	private static <K, V>
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V>
	messageDrivenChannelAdapter(KafkaMessageDrivenChannelAdapterSpec.KafkaMessageListenerContainerSpec<K, V> spec,
			KafkaMessageDrivenChannelAdapter.ListenerMode listenerMode) {
		return new KafkaMessageDrivenChannelAdapterSpec
				.KafkaMessageDrivenChannelAdapterListenerContainerSpec<>(spec, listenerMode);
	}

	private Kafka() {
		super();
	}

}
