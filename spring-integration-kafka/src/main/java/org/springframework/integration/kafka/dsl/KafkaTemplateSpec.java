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

import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.converter.RecordMessageConverter;

/**
 * An {@link IntegrationComponentSpec} implementation for the {@link KafkaTemplate}.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.4
 */
public class KafkaTemplateSpec<K, V>
		extends IntegrationComponentSpec<KafkaTemplateSpec<K, V>, KafkaTemplate<K, V>> {

	KafkaTemplateSpec(KafkaTemplate<K, V> kafkaTemplate) {
		this.target = kafkaTemplate;
	}

	KafkaTemplateSpec(ProducerFactory<K, V> producerFactory) {
		this.target = new KafkaTemplate<>(producerFactory);
	}

	public KafkaTemplate<K, V> getTemplate() {
		return this.target;
	}

	@Override
	public KafkaTemplateSpec<K, V> id(String id) { // NOSONAR - visibility
		return super.id(id);
	}

	/**
	 /**
	 * Set the default topic for send methods where a topic is not
	 * providing.
	 * @param defaultTopic the topic.
	 * @return the spec
	 */
	public KafkaTemplateSpec<K, V> defaultTopic(String defaultTopic) {
		this.target.setDefaultTopic(defaultTopic);
		return this;
	}

	/**
	 * Set a {@link ProducerListener} which will be invoked when Kafka acknowledges
	 * a send operation. By default a {@link org.springframework.kafka.support.LoggingProducerListener} is configured
	 * which logs errors only.
	 * @param producerListener the listener; may be {@code null}.
	 * @return the spec
	 */
	public KafkaTemplateSpec<K, V> producerListener(ProducerListener<K, V> producerListener) {
		this.target.setProducerListener(producerListener);
		return this;
	}

	/**
	 * Set the message converter to use.
	 * @param messageConverter the message converter.
	 * @return the spec
	 */
	public KafkaTemplateSpec<K, V> messageConverter(RecordMessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return this;
	}

}
