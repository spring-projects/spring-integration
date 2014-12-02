/*
 * Copyright 2013-2014 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.serializer.DefaultEncoder;

/**
 * @author Soby Chacko
 * @author Rajasekar Elango
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 * @since 0.5
 */
public class ProducerConfiguration<K, V> {

	private final Producer<K, V> producer;

	private final ProducerMetadata<K, V> producerMetadata;

	public ProducerConfiguration(ProducerMetadata<K, V> producerMetadata, Producer<K, V> producer) {
		Assert.notNull(producerMetadata);
		Assert.notNull(producer);
		this.producerMetadata = producerMetadata;
		this.producer = producer;
	}

	public ProducerMetadata<K, V> getProducerMetadata() {
		return this.producerMetadata;
	}

	public Producer<K, V> getProducer() {
		return this.producer;
	}

	public void send(final Message<?> message) throws Exception {
		final V v = getPayload(message);

		String topic = message.getHeaders().containsKey("topic")
				? message.getHeaders().get("topic", String.class)
				: this.producerMetadata.getTopic();

		if (message.getHeaders().containsKey("messageKey")) {
			this.producer.send(new KeyedMessage<K, V>(topic, getKey(message), v));
		}
		else {
			this.producer.send(new KeyedMessage<K, V>(topic, v));
		}
	}

	@SuppressWarnings("unchecked")
	private V getPayload(final Message<?> message) throws Exception {
		if (this.producerMetadata.getValueEncoder() instanceof DefaultEncoder) {
			return (V) getByteStream(message.getPayload());
		}
		else if (producerMetadata.getValueClassType().isAssignableFrom(message.getPayload().getClass())) {
			return producerMetadata.getValueClassType().cast(message.getPayload());
		}
		throw new MessageHandlingException(message, "Message payload type is not matching with what is configured");
	}

	@SuppressWarnings("unchecked")
	private K getKey(final Message<?> message) throws Exception {
		final Object key = message.getHeaders().get("messageKey");

		if (this.producerMetadata.getKeyEncoder() instanceof DefaultEncoder) {
			return (K) getByteStream(key);
		}
		return message.getHeaders().get("messageKey", this.producerMetadata.getKeyClassType());
	}

	private static boolean isRawByteArray(final Object obj) {
		return obj instanceof byte[];
	}

	private static byte[] getByteStream(final Object obj) throws IOException {
		if (isRawByteArray(obj)) {
			return (byte[]) obj;
		}
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final ObjectOutputStream os = new ObjectOutputStream(out);
		os.writeObject(obj);
		return out.toByteArray();
	}

	@Override
	public boolean equals(final Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public String toString() {
		return "ProducerConfiguration [producerMetadata=" + this.producerMetadata + "]";
	}

	public void stop() {
		this.producer.close();
	}

}
