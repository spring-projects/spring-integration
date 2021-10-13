/*
 * Copyright 2016-2020 the original author or authors.
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

import java.util.regex.Pattern;

import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.inbound.KafkaMessageSource;
import org.springframework.integration.kafka.inbound.KafkaMessageSource.KafkaAckCallbackFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.ConsumerProperties;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.GenericMessageListenerContainer;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.TopicPartitionOffset;

/**
 * Factory class for Apache Kafka components.
 *
 * @author Artem Bilan
 * @author Nasko Vasilev
 * @author Gary Russell
 * @author Anshul Mehra
 *
 * @since 5.4
 */
public final class Kafka {

	/**
	 * Create an initial {@link KafkaProducerMessageHandlerSpec}.
	 * @param kafkaTemplate the {@link KafkaTemplate} to use
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the KafkaProducerMessageHandlerSpec.
	 */
	public static <K, V> KafkaProducerMessageHandlerSpec<K, V, ?> outboundChannelAdapter(
			KafkaTemplate<K, V> kafkaTemplate) {

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
	public static <K, V> KafkaProducerMessageHandlerSpec.KafkaProducerMessageHandlerTemplateSpec<K, V> outboundChannelAdapter(
			ProducerFactory<K, V> producerFactory) {

		return new KafkaProducerMessageHandlerSpec.KafkaProducerMessageHandlerTemplateSpec<>(producerFactory);
	}

	/**
	 * Create an initial {@link KafkaInboundChannelAdapterSpec} with the consumer factory and
	 * topics.
	 * @param consumerFactory the consumer factory.
	 * @param consumerProperties the consumerProperties.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the spec.
	 * @since 3.2
	 */
	public static <K, V> KafkaInboundChannelAdapterSpec<K, V> inboundChannelAdapter(
			ConsumerFactory<K, V> consumerFactory, ConsumerProperties consumerProperties) {

		return inboundChannelAdapter(consumerFactory, consumerProperties, false);
	}

	/**
	 * Create an initial {@link KafkaInboundChannelAdapterSpec} with the consumer factory and
	 * topics.
	 * @param consumerFactory the consumer factory.
	 * @param consumerProperties the consumerProperties.
	 * @param allowMultiFetch true to fetch multiple records on each poll.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the spec.
	 * @since 3.2
	 */
	public static <K, V> KafkaInboundChannelAdapterSpec<K, V> inboundChannelAdapter(
			ConsumerFactory<K, V> consumerFactory,
			ConsumerProperties consumerProperties,
			boolean allowMultiFetch) {

		return new KafkaInboundChannelAdapterSpec<>(consumerFactory, consumerProperties, allowMultiFetch);
	}

	/**
	 * Create an initial {@link KafkaInboundChannelAdapterSpec} with the consumer factory and
	 * topics with a custom ack callback factory.
	 * @param consumerFactory the consumer factory.
	 * @param consumerProperties the consumerProperties.
	 * @param ackCallbackFactory the callback factory.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the spec.
	 * @since 3.2
	 */
	public static <K, V> KafkaInboundChannelAdapterSpec<K, V> inboundChannelAdapter(
			ConsumerFactory<K, V> consumerFactory,
			ConsumerProperties consumerProperties,
			KafkaAckCallbackFactory<K, V> ackCallbackFactory) {

		return inboundChannelAdapter(consumerFactory, consumerProperties, ackCallbackFactory, false);
	}

	/**
	 * Create an initial {@link KafkaInboundChannelAdapterSpec} with the consumer factory and
	 * topics with a custom ack callback factory.
	 * @param consumerFactory the consumer factory.
	 * @param consumerProperties the consumerProperties.
	 * @param ackCallbackFactory the callback factory.
	 * @param allowMultiFetch true to fetch multiple records on each poll.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the spec.
	 * @since 3.2
	 */
	public static <K, V> KafkaInboundChannelAdapterSpec<K, V> inboundChannelAdapter(
			ConsumerFactory<K, V> consumerFactory,
			ConsumerProperties consumerProperties,
			KafkaAckCallbackFactory<K, V> ackCallbackFactory,
			boolean allowMultiFetch) {

		return new KafkaInboundChannelAdapterSpec<>(consumerFactory, consumerProperties, ackCallbackFactory,
				allowMultiFetch);
	}

	/**
	 * Create an initial {@link KafkaMessageDrivenChannelAdapterSpec}. If the listener
	 * container is not already a bean it will be registered in the application context.
	 * If the adapter spec has an {@code id}, the bean name will be that id appended with
	 * '.container'. Otherwise, the bean name will be generated from the container class
	 * name.
	 * @param listenerContainer the {@link AbstractMessageListenerContainer}.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.
	 */
	public static <K, V> KafkaMessageDrivenChannelAdapterSpec<K, V, ?> messageDrivenChannelAdapter(
			AbstractMessageListenerContainer<K, V> listenerContainer) {

		return messageDrivenChannelAdapter(listenerContainer, KafkaMessageDrivenChannelAdapter.ListenerMode.record);
	}

	/**
	 * Create an initial {@link KafkaMessageDrivenChannelAdapterSpec}. If the listener
	 * container is not already a bean it will be registered in the application context.
	 * If the adapter spec has an {@code id}, the bean name will be that id appended with
	 * '.container'. Otherwise, the bean name will be generated from the container class
	 * name.
	 * @param listenerContainer the {@link AbstractMessageListenerContainer}.
	 * @param listenerMode the {@link KafkaMessageDrivenChannelAdapter.ListenerMode}.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.
	 */
	public static <K, V> KafkaMessageDrivenChannelAdapterSpec<K, V, ?> messageDrivenChannelAdapter(
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
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V> messageDrivenChannelAdapter(
			ConsumerFactory<K, V> consumerFactory, ContainerProperties containerProperties) {

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
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V> messageDrivenChannelAdapter(
			ConsumerFactory<K, V> consumerFactory, ContainerProperties containerProperties,
			KafkaMessageDrivenChannelAdapter.ListenerMode listenerMode) {

		return messageDrivenChannelAdapter(
				new KafkaMessageListenerContainerSpec<>(consumerFactory,
						containerProperties), listenerMode);
	}

	/**
	 * Create an initial
	 * {@link KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec}.
	 * @param consumerFactory the {@link ConsumerFactory}.
	 * @param topicPartitions the {@link TopicPartitionOffset} vararg.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec.
	 */
	public static <K, V>
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V> messageDrivenChannelAdapter(
			ConsumerFactory<K, V> consumerFactory,
			TopicPartitionOffset... topicPartitions) {

		return messageDrivenChannelAdapter(consumerFactory, KafkaMessageDrivenChannelAdapter.ListenerMode.record,
				topicPartitions);
	}

	/**
	 * Create an initial
	 * {@link KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec}.
	 * @param consumerFactory the {@link ConsumerFactory}.
	 * @param listenerMode the {@link KafkaMessageDrivenChannelAdapter.ListenerMode}.
	 * @param topicPartitions the {@link TopicPartitionOffset} vararg.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec.
	 */
	public static <K, V>
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V> messageDrivenChannelAdapter(
			ConsumerFactory<K, V> consumerFactory,
			KafkaMessageDrivenChannelAdapter.ListenerMode listenerMode,
			TopicPartitionOffset... topicPartitions) {

		return messageDrivenChannelAdapter(
				new KafkaMessageListenerContainerSpec<>(consumerFactory,
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
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V> messageDrivenChannelAdapter(
			ConsumerFactory<K, V> consumerFactory, String... topics) {

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
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V> messageDrivenChannelAdapter(
			ConsumerFactory<K, V> consumerFactory, KafkaMessageDrivenChannelAdapter.ListenerMode listenerMode,
			String... topics) {

		return messageDrivenChannelAdapter(
				new KafkaMessageListenerContainerSpec<>(consumerFactory,
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
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V> messageDrivenChannelAdapter(
			ConsumerFactory<K, V> consumerFactory, Pattern topicPattern) {

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
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V> messageDrivenChannelAdapter(
			ConsumerFactory<K, V> consumerFactory,
			KafkaMessageDrivenChannelAdapter.ListenerMode listenerMode, Pattern topicPattern) {

		return messageDrivenChannelAdapter(
				new KafkaMessageListenerContainerSpec<>(consumerFactory,
						topicPattern),
				listenerMode);
	}

	/**
	 * Create an initial {@link KafkaProducerMessageHandlerSpec}.
	 * @param kafkaTemplate the {@link ReplyingKafkaTemplate} to use
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type (request).
	 * @param <R> the Kafka message value type (reply).
	 * @return the KafkaGatewayMessageHandlerSpec.
	 * @since 3.0.2
	 */
	public static <K, V, R> KafkaOutboundGatewaySpec<K, V, R, ?> outboundGateway(
			ReplyingKafkaTemplate<K, V, R> kafkaTemplate) {

		return new KafkaOutboundGatewaySpec<>(kafkaTemplate);
	}

	/**
	 * Create an initial {@link KafkaProducerMessageHandlerSpec} with ProducerFactory.
	 * @param producerFactory the {@link ProducerFactory} Java 8 Lambda.
	 * @param replyContainer a listener container for replies.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type (request).
	 * @param <R> the Kafka message value type (reply).
	 * @return the KafkaGatewayMessageHandlerSpec.
	 * @since 3.0.2
	 */
	public static <K, V, R> KafkaOutboundGatewaySpec.KafkaGatewayMessageHandlerTemplateSpec<K, V, R> outboundGateway(
			ProducerFactory<K, V> producerFactory, GenericMessageListenerContainer<K, R> replyContainer) {

		return new KafkaOutboundGatewaySpec.KafkaGatewayMessageHandlerTemplateSpec<>(producerFactory,
				replyContainer);
	}

	/**
	 * Create an initial {@link KafkaInboundGatewaySpec} with the provided container and
	 * template. If the listener container is not already a bean it will be registered in
	 * the application context. If the adapter spec has an {@code id}, the bean name will
	 * be that id appended with '.container'. Otherwise, the bean name will be generated
	 * from the container class name.
	 * @param container the container.
	 * @param template the template.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type (request).
	 * @param <R> the Kafka message value type (reply).
	 * @return the spec.
	 * @since 3.0.2
	 */
	public static <K, V, R> KafkaInboundGatewaySpec<K, V, R, ?> inboundGateway(
			AbstractMessageListenerContainer<K, V> container, KafkaTemplate<K, R> template) {

		return new KafkaInboundGatewaySpec<>(container, template);
	}

	/**
	 * Create an initial {@link KafkaInboundGatewaySpec} with the provided consumer factory,
	 * container properties and producer factory.
	 * @param consumerFactory the consumer factory.
	 * @param containerProperties the container properties.
	 * @param producerFactory the producer factory.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type (request).
	 * @param <R> the Kafka message value type (reply).
	 * @return the spec.
	 * @since 3.0.2
	 */
	public static <K, V, R> KafkaInboundGatewaySpec.KafkaInboundGatewayListenerContainerSpec<K, V, R> inboundGateway(
			ConsumerFactory<K, V> consumerFactory, ContainerProperties containerProperties,
			ProducerFactory<K, R> producerFactory) {

		return inboundGateway(
				new KafkaMessageListenerContainerSpec<>(consumerFactory, containerProperties),
				new KafkaTemplateSpec<>(producerFactory));
	}

	/**
	 * Create an initial {@link KafkaInboundGatewaySpec} with the provided container and
	 * template specs.
	 * @param containerSpec the container spec.
	 * @param templateSpec the template spec.
	 * @param <K> the Kafka message key type.
	 * @param <V> the Kafka message value type (request).
	 * @param <R> the Kafka message value type (reply).
	 * @return the spec.
	 * @since 3.0.2
	 */
	public static <K, V, R> KafkaInboundGatewaySpec.KafkaInboundGatewayListenerContainerSpec<K, V, R> inboundGateway(
			KafkaMessageListenerContainerSpec<K, V> containerSpec, KafkaTemplateSpec<K, R> templateSpec) {

		return new KafkaInboundGatewaySpec.KafkaInboundGatewayListenerContainerSpec<>(containerSpec, templateSpec);
	}

	/**
	 * Create a spec for a subscribable channel with the provided parameters.
	 * @param template the template.
	 * @param containerFactory the container factory.
	 * @param topic the topic.
	 * @return the spec.
	 * @since 3.3
	 */
	public static KafkaPointToPointChannelSpec channel(KafkaTemplate<?, ?> template,
			KafkaListenerContainerFactory<?> containerFactory, String topic) {

		return new KafkaPointToPointChannelSpec(template, containerFactory, topic);
	}

	/**
	 * Create a spec for a publish/subscribe channel with the provided parameters.
	 * @param template the template.
	 * @param containerFactory the container factory.
	 * @param topic the topic.
	 * @return the spec.
	 * @since 3.3
	 */
	public static KafkaPublishSubscribeChannelSpec publishSubscribeChannel(KafkaTemplate<?, ?> template,
			KafkaListenerContainerFactory<?> containerFactory, String topic) {

		return new KafkaPublishSubscribeChannelSpec(template, containerFactory, topic);
	}

	/**
	 * Create a spec for a pollable channel with the provided parameters.
	 * @param template the template.
	 * @param source the source.
	 * @return the spec.
	 * @since 3.3
	 */
	public static KafkaPollableChannelSpec pollableChannel(KafkaTemplate<?, ?> template,
			KafkaMessageSource<?, ?> source) {

		return new KafkaPollableChannelSpec(template, source);
	}

	private static <K, V>
	KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V> messageDrivenChannelAdapter(
			KafkaMessageListenerContainerSpec<K, V> spec, KafkaMessageDrivenChannelAdapter.ListenerMode listenerMode) {

		return new KafkaMessageDrivenChannelAdapterSpec
				.KafkaMessageDrivenChannelAdapterListenerContainerSpec<>(spec, listenerMode);
	}

	private Kafka() {
		super();
	}

}
