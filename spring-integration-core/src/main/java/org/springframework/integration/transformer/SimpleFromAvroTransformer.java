/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.transformer;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.avro.generic.GenericContainer;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An avro transformer to create generated {@link GenericContainer} objects
 * from {@code byte[]}.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class SimpleFromAvroTransformer extends AbstractTransformer {

	private final Class<? extends GenericContainer> type;

	private final DecoderFactory decoderFactory = new DecoderFactory();

	public SimpleFromAvroTransformer(Class<? extends GenericContainer> type) {
		this.type = type;
	}

	@Override
	protected Object doTransform(Message<?> message) {
		Assert.state(message.getPayload() instanceof byte[], "Payload must be a byte[]");
		DatumReader<?> reader = new SpecificDatumReader<>(this.type);
		try {
			return reader.read(null, this.decoderFactory.binaryDecoder((byte[]) message.getPayload(), null));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}


}
