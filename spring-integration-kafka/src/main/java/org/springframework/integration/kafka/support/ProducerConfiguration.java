/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.integration.kafka.support;

import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Soby Chacko
 * @author Rajasekar Elango
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 * @author Marius Bogoevici
 * @author Artem Bilan
 * @since 0.5
 */
public class ProducerConfiguration<K, V> {

	private final Producer<K, V> producer;

	private final ProducerMetadata<K, V> producerMetadata;

	private ConversionService conversionService;

	public ProducerConfiguration(ProducerMetadata<K, V> producerMetadata, Producer<K, V> producer) {
		Assert.notNull(producerMetadata);
		Assert.notNull(producer);
		this.producerMetadata = producerMetadata;
		this.producer = producer;
		GenericConversionService genericConversionService = new GenericConversionService();
		genericConversionService.addConverter(Object.class, byte[].class, new SerializingConverter());
		this.conversionService = genericConversionService;
	}

	public void setConversionService(ConversionService conversionService) {
		Assert.notNull(conversionService, "Conversion service must not be null");
		this.conversionService = conversionService;
	}

	public ProducerMetadata<K, V> getProducerMetadata() {
		return this.producerMetadata;
	}

	public Future<RecordMetadata> send(String topic, K messageKey, V messagePayload) {
		if (this.getProducerMetadata().getPartitioner() != null) {
			String targetTopic = StringUtils.hasText(topic) ? topic : this.producerMetadata.getTopic();
			int partition = this.getProducerMetadata().getPartitioner().partition(messageKey,
					this.producer.partitionsFor(targetTopic).size());
			return this.send(targetTopic, partition, messageKey, messagePayload);
		}
		return this.send(topic, null, messageKey, messagePayload);
	}

	public Future<RecordMetadata> send(String topic, Integer partition, K messageKey, V messagePayload) {
		String targetTopic = StringUtils.hasText(topic) ? topic : this.producerMetadata.getTopic();
		return this.producer.send(new ProducerRecord<>(targetTopic, partition, messageKey, messagePayload));
	}

	public Future<RecordMetadata> convertAndSend(String topic, Integer partition, Object messageKey,
	                                             Object messagePayload) {
		return send(topic, partition, convertKeyIfNecessary(messageKey), convertPayloadIfNecessary(messagePayload));
	}

	public Future<RecordMetadata> convertAndSend(String topic, Object messageKey, Object messagePayload) {
		return send(topic, convertKeyIfNecessary(messageKey), convertPayloadIfNecessary(messagePayload));
	}

	private K convertKeyIfNecessary(Object messageKey) {
		if (messageKey != null) {
			if (getProducerMetadata().getKeyClassType().isAssignableFrom(
					messageKey.getClass())) {
				return getProducerMetadata().getKeyClassType().cast(messageKey);
			}
			return this.conversionService.convert(messageKey, this.producerMetadata.getKeyClassType());
		}
		else {
			return null;
		}
	}

	private V convertPayloadIfNecessary(Object messagePayload) {
		if (messagePayload != null) {
			if (getProducerMetadata().getKeyClassType().isAssignableFrom(
					messagePayload.getClass())) {
				return getProducerMetadata().getValueClassType().cast(messagePayload);
			}
			return this.conversionService.convert(messagePayload, this.producerMetadata.getValueClassType());
		}
		else {
			return null;
		}
	}

	public void stop() {
		this.producer.close();
	}

	@Override
	public String toString() {
		return "ProducerConfiguration{" +
				"producer=" + this.producer +
				", producerMetadata=" + this.producerMetadata +
				", conversionService=" + this.conversionService +
				'}';
	}

}
