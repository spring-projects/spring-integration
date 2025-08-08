/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.kafka.dsl;

import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.lang.Nullable;

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
	public KafkaTemplateSpec<K, V> id(@Nullable String id) { // NOSONAR - visibility
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
