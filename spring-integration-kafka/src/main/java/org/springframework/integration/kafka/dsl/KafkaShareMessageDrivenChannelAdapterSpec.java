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

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.kafka.inbound.KafkaShareMessageDrivenChannelAdapter;
import org.springframework.kafka.listener.AbstractShareKafkaMessageListenerContainer;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.util.Assert;

/**
 * A {@link MessageProducerSpec} implementation for the
 * {@link KafkaShareMessageDrivenChannelAdapter}.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 * @param <S> the target {@link KafkaShareMessageDrivenChannelAdapterSpec} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 7.2
 */
public class KafkaShareMessageDrivenChannelAdapterSpec<K, V, S extends KafkaShareMessageDrivenChannelAdapterSpec<K, V, S>>
		extends MessageProducerSpec<S, KafkaShareMessageDrivenChannelAdapter<K, V>>
		implements ComponentsRegistration {

	private final AbstractShareKafkaMessageListenerContainer<K, V> container;

	KafkaShareMessageDrivenChannelAdapterSpec(AbstractShareKafkaMessageListenerContainer<K, V> shareListenerContainer) {
		super(new KafkaShareMessageDrivenChannelAdapter<>(shareListenerContainer));
		this.container = shareListenerContainer;
	}

	/**
	 * Set the message converter to use.
	 * @param messageConverter the converter.
	 * @return the spec.
	 */
	public S recordMessageConverter(RecordMessageConverter messageConverter) {
		this.target.setRecordMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * Specify a {@link RecordFilterStrategy} to discard records before they are
	 * converted and sent.
	 * @param recordFilterStrategy the {@link RecordFilterStrategy} to use.
	 * @return the spec.
	 */
	public S recordFilterStrategy(RecordFilterStrategy<K, V> recordFilterStrategy) {
		this.target.setRecordFilterStrategy(recordFilterStrategy);
		return _this();
	}

	/**
	 * When using a type-aware message converter (such as {@code StringJsonMessageConverter}),
	 * set the payload type the converter should create. Defaults to {@link Object}.
	 * @param payloadType the type.
	 * @return the spec.
	 */
	public S payloadType(Class<?> payloadType) {
		this.target.setPayloadType(payloadType);
		return _this();
	}

	/**
	 * Set to true to bind the source consumer record in the header named
	 * {@link org.springframework.integration.IntegrationMessageHeaderAccessor#SOURCE_DATA}.
	 * @param bindSourceRecord true to bind.
	 * @return the spec.
	 */
	public S bindSourceRecord(boolean bindSourceRecord) {
		this.target.setBindSourceRecord(bindSourceRecord);
		return _this();
	}

	@Override
	public Map<Object, @Nullable String> getComponentsToRegister() {
		return Collections.<Object, @Nullable String>singletonMap(
				this.container, getId() == null ? null : getId() + ".container");
	}

	/**
	 * A {@link org.springframework.kafka.listener.ShareKafkaMessageListenerContainer} configuration
	 * {@link KafkaShareMessageDrivenChannelAdapterSpec} extension.
	 * @param <K> the key type.
	 * @param <V> the value type.
	 */
	public static class KafkaShareMessageDrivenChannelAdapterListenerContainerSpec<K, V> extends
			KafkaShareMessageDrivenChannelAdapterSpec<K, V,
					KafkaShareMessageDrivenChannelAdapterListenerContainerSpec<K, V>> {

		private final KafkaShareMessageListenerContainerSpec<K, V> spec;

		KafkaShareMessageDrivenChannelAdapterListenerContainerSpec(KafkaShareMessageListenerContainerSpec<K, V> spec) {
			super(spec.getObject());
			this.spec = spec;
		}

		/**
		 * Configure a listener container by invoking the {@link Consumer} callback, with a
		 * {@link KafkaShareMessageListenerContainerSpec} argument.
		 * @param configurer the configurer Java 8 Lambda.
		 * @return the spec.
		 */
		public KafkaShareMessageDrivenChannelAdapterListenerContainerSpec<K, V> configureListenerContainer(
				Consumer<KafkaShareMessageListenerContainerSpec<K, V>> configurer) {

			Assert.notNull(configurer, "The 'configurer' cannot be null");
			configurer.accept(this.spec);
			return _this();
		}

		@Override
		public Map<Object, @Nullable String> getComponentsToRegister() {
			return Collections.<Object, @Nullable String>singletonMap(this.spec.getObject(), this.spec.getId());
		}

	}

}
