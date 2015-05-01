/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.util;

import java.util.Map;

import kafka.serializer.DefaultEncoder;
import kafka.serializer.Encoder;
import org.apache.kafka.common.serialization.Serializer;

/**
 * An adapter from the pre-0.8.2 Kafka {@link Encoder} to the {@link Serializer} interface used
 * by the new client.
 *
 * @author Marius Bogoevici
 */
public class EncoderAdaptingSerializer<T> implements Serializer<T> {

	private final Encoder<T> encoder;

	public EncoderAdaptingSerializer(Encoder<T> encoder) {
		this.encoder = encoder;
	}

	public Encoder<T> getEncoder() {
		return encoder;
	}

	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {
		// no-op
	}

	@Override
	public byte[] serialize(String topic, T data) {
		return encoder.toBytes(data);
	}

	@Override
	public void close() {
		// no-op;
	}
}
