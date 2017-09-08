/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.support.json;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import java.util.Collections;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
public class EmbeddedJsonHeadersMessageMapperTests {

	@Test
	public void testEmbedAll() throws Exception {
		EmbeddedJsonHeadersMessageMapper mapper = new EmbeddedJsonHeadersMessageMapper();
		GenericMessage<String> message = new GenericMessage<>("foo");
		assertThat(mapper.toMessage(mapper.fromMessage(message)), equalTo(message));

	}

	@Test
	public void testEmbedSome() throws Exception {
		EmbeddedJsonHeadersMessageMapper mapper = new EmbeddedJsonHeadersMessageMapper("id");
		GenericMessage<String> message = new GenericMessage<>("foo");
		Message<?> decoded = mapper.toMessage(mapper.fromMessage(message));
		assertThat(decoded.getPayload(), equalTo(message.getPayload()));
		assertThat(decoded.getHeaders().getId(), equalTo(message.getHeaders().getId()));
	}

	@Test
	public void testBytesEmbedAll() throws Exception {
		EmbeddedJsonHeadersMessageMapper mapper = new EmbeddedJsonHeadersMessageMapper();
		GenericMessage<byte[]> message = new GenericMessage<>("foo".getBytes());
		Thread.sleep(2);
		byte[] bytes = mapper.fromMessage(message);
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		int headerLen = bb.getInt();
		byte[] headerBytes = new byte[headerLen];
		bb.get(headerBytes);
		String headers = new String(headerBytes);
		assertThat(headers, containsString(message.getHeaders().getId().toString()));
		assertThat(headers, containsString(String.valueOf(message.getHeaders().getTimestamp())));
		assertThat(bb.getInt(), equalTo(3));
		assertThat(bb.remaining(), equalTo(3));
		assertThat((char) bb.get(), equalTo('f'));
		assertThat((char) bb.get(), equalTo('o'));
		assertThat((char) bb.get(), equalTo('o'));
	}

	@Test
	public void testBytesEmbedSome() throws Exception {
		EmbeddedJsonHeadersMessageMapper mapper = new EmbeddedJsonHeadersMessageMapper("I*");
		GenericMessage<byte[]> message = new GenericMessage<>("foo".getBytes(), Collections.singletonMap("bar", "baz"));
		byte[] bytes = mapper.fromMessage(message);
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		int headerLen = bb.getInt();
		byte[] headerBytes = new byte[headerLen];
		bb.get(headerBytes);
		String headers = new String(headerBytes);
		assertThat(headers, containsString(message.getHeaders().getId().toString()));
		assertThat(headers, not(containsString("bar")));
		assertThat(bb.getInt(), equalTo(3));
		assertThat(bb.remaining(), equalTo(3));
		assertThat((char) bb.get(), equalTo('f'));
		assertThat((char) bb.get(), equalTo('o'));
		assertThat((char) bb.get(), equalTo('o'));
	}

	@Test
	public void testBytesEmbedAllJson() throws Exception {
		EmbeddedJsonHeadersMessageMapper mapper = new EmbeddedJsonHeadersMessageMapper();
		mapper.setRawBytes(false);
		GenericMessage<byte[]> message = new GenericMessage<>("foo".getBytes());
		byte[] mappedBytes = mapper.fromMessage(message);
		String mapped = new String(mappedBytes);
		assertThat(mapped, containsString("[B\",\"Zm9v"));
		@SuppressWarnings("unchecked")
		Message<byte[]> decoded = (Message<byte[]>) mapper.toMessage(mappedBytes);
		assertThat(new String(decoded.getPayload()), equalTo("foo"));

	}

	@Test
	public void testBytesDecodeAll() throws Exception {
		EmbeddedJsonHeadersMessageMapper mapper = new EmbeddedJsonHeadersMessageMapper();
		GenericMessage<byte[]> message = new GenericMessage<>("foo".getBytes());
		Message<?> decoded = mapper.toMessage(mapper.fromMessage(message));
		assertThat(decoded, equalTo(message));
	}

}
