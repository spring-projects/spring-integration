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

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.GenericMessageListenerContainer;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.integration.dsl.MessageHandlerSpec}
 * implementation for the {@link org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler}
 * as a gateway.
 * @param <K> the key type.
 * @param <V> the outbound value type.
 * @param <R> the reply value type.
 * @param <S> the {@link KafkaProducerMessageHandlerSpec} extension type.
 *
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public class KafkaOutboundGatewaySpec<K, V, R, S extends KafkaOutboundGatewaySpec<K, V, R, S>>
		extends KafkaProducerMessageHandlerSpec<K, V, S> {

	KafkaOutboundGatewaySpec(ReplyingKafkaTemplate<K, V, R> kafkaTemplate) {
		super(kafkaTemplate);
	}

	/**
	 * Set a message converter for replies (when a gateway).
	 * @param messageConverter the converter.
	 * @return the spec.
	 */
	public S replyMessageConverter(RecordMessageConverter messageConverter) {
		this.target.setReplyMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * A {@link org.springframework.kafka.core.KafkaTemplate}-based {@link KafkaProducerMessageHandlerSpec} extension.
	 *
	 * @param <K> the key type.
	 * @param <V> the outbound value type.
	 * @param <R> the reply value type.
	 */
	public static class KafkaGatewayMessageHandlerTemplateSpec<K, V, R>
		extends KafkaOutboundGatewaySpec<K, V, R, KafkaGatewayMessageHandlerTemplateSpec<K, V, R>>
			implements ComponentsRegistration {

		private final ReplyingKafkaTemplateSpec<K, V, R> kafkaTemplateSpec;

		@SuppressWarnings("unchecked")
		KafkaGatewayMessageHandlerTemplateSpec(ProducerFactory<K, V> producerFactory,
				GenericMessageListenerContainer<K, R> replyContainer) {

			super(new ReplyingKafkaTemplate<>(producerFactory, replyContainer));
			this.kafkaTemplateSpec =
					new ReplyingKafkaTemplateSpec<>((ReplyingKafkaTemplate<K, V, R>) this.target.getKafkaTemplate());
		}

		/**
		 * Configure a Kafka Template by invoking the {@link Consumer} callback, with a
		 * {@link KafkaTemplateSpec} argument.
		 * @param configurer the configurer Java 8 Lambda.
		 * @return the spec.
		 */
		public KafkaGatewayMessageHandlerTemplateSpec<K, V, R> configureKafkaTemplate(
				Consumer<ReplyingKafkaTemplateSpec<K, V, R>> configurer) {

			Assert.notNull(configurer, "The 'configurer' cannot be null");
			configurer.accept(this.kafkaTemplateSpec);
			return _this();
		}

		@Override
		public Map<Object, String> getComponentsToRegister() {
			return Collections.singletonMap(this.kafkaTemplateSpec.get(), this.kafkaTemplateSpec.getId());
		}

	}

	/**
	 * An {@link org.springframework.integration.dsl.IntegrationComponentSpec}
	 * implementation for the {@link org.springframework.kafka.core.KafkaTemplate}.
	 *
	 * @param <K> the key type.
	 * @param <V> the request value type.
	 * @param <R> the reply value type.
	 */
	public static class ReplyingKafkaTemplateSpec<K, V, R> extends KafkaTemplateSpec<K, V> {

		ReplyingKafkaTemplateSpec(ReplyingKafkaTemplate<K, V, R> kafkaTemplate) {
			super(kafkaTemplate);
		}

		@SuppressWarnings("unchecked")
		public ReplyingKafkaTemplateSpec<K, V, R> taskScheduler(TaskScheduler scheduler) {
			((ReplyingKafkaTemplate<K, V, R>) this.target).setTaskScheduler(scheduler);
			return this;
		}

		/**
		 * Default reply timeout.
		 * @param replyTimeout the timeout.
		 * @return the spec.
		 */
		@SuppressWarnings("unchecked")
		public ReplyingKafkaTemplateSpec<K, V, R> defaultReplyTimeout(Duration replyTimeout) {
			((ReplyingKafkaTemplate<K, V, R>) this.target).setDefaultReplyTimeout(replyTimeout);
			return this;
		}

	}

}
