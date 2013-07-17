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
package org.springframework.integration.kafka.serializer.avro;

import kafka.serializer.Encoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Soby Chacko
 * @since 0.5
 */
public class AvroReflectDatumBackedKafkaEncoder<T> extends AvroDatumSupport<T> implements Encoder<T> {
	private static final Log LOG = LogFactory.getLog(AvroReflectDatumBackedKafkaEncoder.class);

	private final DatumWriter<T> writer;

	public AvroReflectDatumBackedKafkaEncoder(final Class<T> clazz) {
		this.writer = new ReflectDatumWriter<T>(clazz);
	}

	@Override
	@SuppressWarnings("unchecked")
	public byte[] toBytes(final T source) {
		return toBytes(source, writer);
	}
}
