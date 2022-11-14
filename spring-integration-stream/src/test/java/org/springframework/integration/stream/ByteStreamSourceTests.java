/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.stream;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class ByteStreamSourceTests {

	@Test
	public void testEndOfStream() {
		byte[] bytes = new byte[] {1, 2, 3};
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		ByteStreamReadingMessageSource source = new ByteStreamReadingMessageSource(stream);
		source.setBeanFactory(mock(BeanFactory.class));
		Message<?> message1 = source.receive();
		byte[] payload = (byte[]) message1.getPayload();
		assertThat(payload.length).isEqualTo(3);
		assertThat(payload[0]).isEqualTo((byte) 1);
		assertThat(payload[1]).isEqualTo((byte) 2);
		assertThat(payload[2]).isEqualTo((byte) 3);
		Message<?> message2 = source.receive();
		assertThat(message2).isNull();
	}

	@Test
	public void testByteArrayIsTruncated() {
		byte[] bytes = new byte[] {0, 1, 2, 3, 4, 5};
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		ByteStreamReadingMessageSource source = new ByteStreamReadingMessageSource(stream);
		source.setBytesPerMessage(4);
		source.setBeanFactory(mock(BeanFactory.class));
		Message<?> message1 = source.receive();
		assertThat(((byte[]) message1.getPayload()).length).isEqualTo(4);
		Message<?> message2 = source.receive();
		assertThat(((byte[]) message2.getPayload()).length).isEqualTo(2);
		Message<?> message3 = source.receive();
		assertThat(message3).isNull();
	}

	@Test
	public void testByteArrayIsNotTruncated() {
		byte[] bytes = new byte[] {0, 1, 2, 3, 4, 5};
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		ByteStreamReadingMessageSource source = new ByteStreamReadingMessageSource(stream);
		source.setBytesPerMessage(4);
		source.setShouldTruncate(false);
		source.setBeanFactory(mock(BeanFactory.class));
		Message<?> message1 = source.receive();
		assertThat(((byte[]) message1.getPayload()).length).isEqualTo(4);
		Message<?> message2 = source.receive();
		assertThat(((byte[]) message2.getPayload()).length).isEqualTo(4);
		assertThat(((byte[]) message2.getPayload())[0]).isEqualTo((byte) 4);
		assertThat(((byte[]) message2.getPayload())[1]).isEqualTo((byte) 5);
		assertThat(((byte[]) message2.getPayload())[2]).isEqualTo((byte) 0);
		assertThat(((byte[]) message2.getPayload())[3]).isEqualTo((byte) 0);
		Message<?> message3 = source.receive();
		assertThat(message3).isNull();
	}

}
