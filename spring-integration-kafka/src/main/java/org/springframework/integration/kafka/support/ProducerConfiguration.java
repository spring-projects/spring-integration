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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
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
 * @author Martin Dam
 * @since 0.5
 */
public class ProducerConfiguration<K, V> {

	private final Producer<K, V> producer;

	private final ProducerMetadata<K, V> producerMetadata;

	private ConversionService conversionService;

	private ProducerListener producerListener;

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

	public void setProducerListener(ProducerListener producerListener) {
		this.producerListener = producerListener;
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
		//If partition is sent by producer context then that takes precedence over custom partitioner.
		if (partition == null && this.getProducerMetadata().getPartitioner() != null) {
			partition = this.getProducerMetadata().getPartitioner().partition(messageKey,
					this.producer.partitionsFor(targetTopic).size());
		}

		ProducerRecord<K, V> record = new ProducerRecord<>(targetTopic, partition, messageKey, messagePayload);
		Future<RecordMetadata> future;
		if (producerListener == null) {
			future = this.producer.send(record);
		}
		else {
			ProducerListenerInvokingCallback callback =
					new ProducerListenerInvokingCallback(targetTopic, partition, messageKey, messagePayload,
							producerListener);
			future = this.producer.send(record, callback);
		}
		if (!producerMetadata.isSync()) {
			return future;
		}
		else {
			try {
				if (producerMetadata.getSendTimeout() <= 0) {
					future.get();
				}
				else {
					future.get(producerMetadata.getSendTimeout(), TimeUnit.MILLISECONDS);
				}
			}
			catch (InterruptedException | ExecutionException | TimeoutException e) {
				throw new KafkaException(e);
			}
			return future;
		}
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
			if (getProducerMetadata().getValueClassType().isAssignableFrom(
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
