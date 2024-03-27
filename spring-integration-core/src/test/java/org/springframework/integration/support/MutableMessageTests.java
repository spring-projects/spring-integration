/*
 * Copyright 2015-2024 the original author or authors.
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

package org.springframework.integration.support;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stuart Williams
 * @author Nathan Kurtyka
 *
 * @since 4.2
 */
public class MutableMessageTests {

	@Test
	public void testMessageIdTimestampRemains() {

		UUID uuid = UUID.randomUUID();
		long timestamp = System.currentTimeMillis();

		Object payload = new Object();
		Map<String, Object> headerMap = new HashMap<>();

		headerMap.put(MessageHeaders.ID, uuid);
		headerMap.put(MessageHeaders.TIMESTAMP, timestamp);

		MutableMessage<Object> mutableMessage = new MutableMessage<>(payload, headerMap);
		MutableMessageHeaders headers = mutableMessage.getHeaders();

		assertThat(headers.getRawHeaders()).containsEntry(MessageHeaders.ID, uuid);
		assertThat(headers.getRawHeaders()).containsEntry(MessageHeaders.TIMESTAMP, timestamp);
	}

	@Test
	public void testMessageHeaderIsSettable() {

		Object payload = new Object();
		Map<String, Object> headerMap = new HashMap<>();
		Map<String, Object> additional = new HashMap<>();

		MutableMessage<Object> mutableMessage = new MutableMessage<>(payload, headerMap);
		MutableMessageHeaders headers = mutableMessage.getHeaders();

		// Should not throw an UnsupportedOperationException
		headers.put("foo", "bar");
		headers.put("eep", "bar");
		headers.remove("eep");
		headers.putAll(additional);

		assertThat(headers.getRawHeaders()).containsEntry("foo", "bar");
	}

	@Test
	public void testMessageHeaderIsSerializable() {

		Object payload = new Object();

		UUID uuid = UUID.nameUUIDFromBytes(((System.currentTimeMillis() - System.nanoTime()) + "").getBytes());
		Long timestamp = System.currentTimeMillis();

		// UUID as String; timestamp as String
		Map<String, Object> headerMapStrings = new HashMap<>();
		headerMapStrings.put(MessageHeaders.ID, uuid.toString());
		headerMapStrings.put(MessageHeaders.TIMESTAMP, timestamp.toString());
		MutableMessage<Object> mutableMessageStrings = new MutableMessage<>(payload, headerMapStrings);
		assertThat(mutableMessageStrings.getHeaders().getId()).isEqualTo(uuid);
		assertThat(mutableMessageStrings.getHeaders().getTimestamp()).isEqualTo(timestamp);

		// UUID as byte[]; timestamp as Long
		Map<String, Object> headerMapByte = new HashMap<>();
		byte[] uuidAsBytes =
				ByteBuffer.allocate(16)
						.putLong(uuid.getMostSignificantBits())
						.putLong(uuid.getLeastSignificantBits())
						.array();

		headerMapByte.put(MessageHeaders.ID, uuidAsBytes);
		headerMapByte.put(MessageHeaders.TIMESTAMP, timestamp);
		MutableMessage<Object> mutableMessageBytes = new MutableMessage<>(payload, headerMapByte);
		assertThat(mutableMessageBytes.getHeaders().getId()).isEqualTo(uuid);
		assertThat(mutableMessageBytes.getHeaders().getTimestamp()).isEqualTo(timestamp);
	}

}
