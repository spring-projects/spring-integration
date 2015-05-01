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

package org.springframework.integration.kafka.listener;

import java.nio.ByteBuffer;
import java.util.Map;

import kafka.serializer.Decoder;
import kafka.serializer.Encoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.common.serialization.Serializer;

import org.springframework.integration.kafka.util.LoggingUtils;

/**
 * Kafka {@link Encoder} and {@link Decoder} for Long values.
 *
 * @author Marius Bogoevici
 */
public class LongSerializerDecoder implements Serializer<Long>, Decoder<Long> {

	private Log log = LogFactory.getLog(LongSerializerDecoder.class);

	@Override
	public Long fromBytes(byte[] bytes) {
		if (bytes == null || bytes.length <= 0) {
			return null;
		}
		else {
			try {
				return ByteBuffer.wrap(bytes).getLong(0);
			}
			catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug("Cannot decode value: " + LoggingUtils.asCommaSeparatedHexDump(bytes));
				}
				return null;
			}
		}
	}

	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {
		// no-op
	}

	@Override
	public byte[] serialize(String topic, Long data) {
		if (data == null) {
			return null;
		}
		else {
			return ByteBuffer.allocate(8).putLong(data).array();
		}
	}

	@Override
	public void close() {
		// no-op
	}
}
