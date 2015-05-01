/*
 * Copyright 2002-2013 the original author or authors.
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

import kafka.producer.Partitioner;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.kafka.common.serialization.Serializer;

import org.springframework.util.Assert;

/**
 *
 * @author Soby Chacko
 * @author Rajasekar Elango
 * @author Marius Bogoevici
 *
 * @since 0.5
 */
public class ProducerMetadata<K,V> {

	private final Class<K> keyClassType;

	private final Class<V> valueClassType;

	private Serializer<K> keySerializer;

	private Serializer<V> valueSerializer;

	private final String topic;

	private Partitioner partitioner;

	private CompressionType compressionType = CompressionType.none;

	private int batchBytes = 16384;

	public ProducerMetadata(final String topic, Class<K> keyClassType, Class<V> valueClassType, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
		Assert.notNull(topic, "Topic cannot be null");
		Assert.notNull(keyClassType, "Key class type serializer cannot be null");
		Assert.notNull(valueClassType, "Value class type cannot be null");
		Assert.notNull(keySerializer, "Value serializer cannot be null");
		Assert.notNull(valueSerializer, "Value serializer cannot be null");
		this.topic = topic;
		this.keyClassType = keyClassType;
		this.valueClassType = valueClassType;
		this.keySerializer = keySerializer;
		this.valueSerializer = valueSerializer;
	}

	public String getTopic() {
		return topic;
	}

	public Serializer<K> getKeySerializer() {
		return keySerializer;
	}

	public Serializer<V> getValueSerializer() {
		return valueSerializer;
	}

	public CompressionType getCompressionType() {
		return compressionType;
	}

	public void setCompressionType(CompressionType compressionType) {
		Assert.notNull(compressionType, "Compression type cannot be null");
		this.compressionType = compressionType;
	}

	public int getBatchBytes() {
		return batchBytes;
	}

	public void setBatchBytes(int batchBytes) {
		Assert.isTrue(batchBytes > 0, "Buffer size must be greater than zero");
		this.batchBytes = batchBytes;
	}

	public Partitioner getPartitioner() {
		return partitioner;
	}

	public void setPartitioner(Partitioner partitioner) {
		this.partitioner = partitioner;
	}

	public Class<K> getKeyClassType() {
		return keyClassType;
	}

	public Class<V> getValueClassType() {
		return valueClassType;
	}

	@Override
	public boolean equals(final Object obj){
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ProducerMetadata [keyEncoder=").append(keySerializer)
				.append(", valueEncoder=").append(valueSerializer)
				.append(", topic=").append(topic)
				.append(", compressionType=").append(compressionType)
				.append("batchBytes").append(batchBytes).append("]");
		return builder.toString();
	}

	public enum CompressionType {
		none,
		gzip,
		snappy
	}
}
