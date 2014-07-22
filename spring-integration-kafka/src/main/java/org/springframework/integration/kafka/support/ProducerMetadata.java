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
import kafka.serializer.DefaultEncoder;
import kafka.serializer.Encoder;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Soby Chacko
 * @author Rajasekar Elango
 * @since 0.5
 */
public class ProducerMetadata<K,V> implements InitializingBean {
	private Encoder<K> keyEncoder;
	private Encoder<V> valueEncoder;
	private Class<K> keyClassType;
	private Class<V> valueClassType;
	private final String topic;
	private String compressionCodec = "default";
	private Partitioner partitioner;
	private boolean async = false;
	private String batchNumMessages;

	public ProducerMetadata(final String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return topic;
	}

	public Encoder<K> getKeyEncoder() {
		return keyEncoder;
	}

	public void setKeyEncoder(final Encoder<K> keyEncoder) {
		this.keyEncoder = keyEncoder;
	}

	public Encoder<V> getValueEncoder() {
		return valueEncoder;
	}

	public void setValueEncoder(final Encoder<V> valueEncoder) {
		this.valueEncoder = valueEncoder;
	}

	public Class<K> getKeyClassType() {
		return keyClassType;
	}

	public void setKeyClassType(final Class<K> keyClassType) {
		this.keyClassType = keyClassType;
	}

	public Class<V> getValueClassType() {
		return valueClassType;
	}

	public void setValueClassType(final Class<V> valueClassType) {
		this.valueClassType = valueClassType;
	}

	//TODO: Use an enum
	public String getCompressionCodec() {
		if (compressionCodec.equalsIgnoreCase("gzip")) {
			return "1";
		} else if (compressionCodec.equalsIgnoreCase("snappy")) {
			return "2";
		}

		return "0";
	}

	public void setCompressionCodec(final String compressionCodec) {
		this.compressionCodec = compressionCodec;
	}

	public Partitioner getPartitioner() {
		return partitioner;
	}

	public void setPartitioner(final Partitioner partitioner) {
		this.partitioner = partitioner;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() throws Exception {
		if (valueEncoder == null) {
			setValueEncoder((Encoder<V>) new DefaultEncoder(null));
		}

		if (keyEncoder == null) {
			setKeyEncoder((Encoder<K>) getValueEncoder());
		}
	}

	public boolean isAsync() {
		return async;
	}

	public void setAsync(final boolean async) {
		this.async = async;
	}

	public String getBatchNumMessages() {
		return batchNumMessages;
	}

	public void setBatchNumMessages(final String batchNumMessages) {
		this.batchNumMessages = batchNumMessages;
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
		builder.append("ProducerMetadata [keyEncoder=").append(keyEncoder).append(", valueEncoder=")
				.append(valueEncoder).append(", topic=").append(topic).append(", compressionCodec=")
				.append(compressionCodec).append(", partitioner=").append(partitioner).append(", async=").append(async)
				.append(", batchNumMessages=").append(batchNumMessages).append("]");
		return builder.toString();
	}
}
