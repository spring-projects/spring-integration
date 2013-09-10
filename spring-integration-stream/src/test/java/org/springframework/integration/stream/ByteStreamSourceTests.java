/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 */
public class ByteStreamSourceTests {

	@Test
	public void testEndOfStream() {
		byte[] bytes = new byte[] {1,2,3};
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		ByteStreamReadingMessageSource source = new ByteStreamReadingMessageSource(stream);
		Message<?> message1 = source.receive();
		byte[] payload = (byte[]) message1.getPayload();
		assertEquals(3, payload.length);
		assertEquals(1, payload[0]);
		assertEquals(2, payload[1]);
		assertEquals(3, payload[2]);
		Message<?> message2 = source.receive();
		assertNull(message2);
	}

	@Test
	public void testByteArrayIsTruncated() {
		byte[] bytes = new byte[] {0,1,2,3,4,5};
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		ByteStreamReadingMessageSource source = new ByteStreamReadingMessageSource(stream);
		source.setBytesPerMessage(4);
		Message<?> message1 = source.receive();
		assertEquals(4, ((byte[]) message1.getPayload()).length);
		Message<?> message2 = source.receive();
		assertEquals(2, ((byte[]) message2.getPayload()).length);
		Message<?> message3 = source.receive();
		assertNull(message3);
	}

	@Test
	public void testByteArrayIsNotTruncated() {
		byte[] bytes = new byte[] {0,1,2,3,4,5};
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		ByteStreamReadingMessageSource source = new ByteStreamReadingMessageSource(stream);
		source.setBytesPerMessage(4);
		source.setShouldTruncate(false);
		Message<?> message1 = source.receive();
		assertEquals(4, ((byte[]) message1.getPayload()).length);
		Message<?> message2 = source.receive();
		assertEquals(4, ((byte[]) message2.getPayload()).length);
		assertEquals(4, ((byte[]) message2.getPayload())[0]);
		assertEquals(5, ((byte[]) message2.getPayload())[1]);
		assertEquals(0, ((byte[]) message2.getPayload())[2]);
		assertEquals(0, ((byte[]) message2.getPayload())[3]);
		Message<?> message3 = source.receive();
		assertNull(message3);
	}

}
