/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.MessageHeaders;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MessageHeadersTests {

	@Test
	public void testTimestamp() {
		MessageHeaders headers = new MessageHeaders(null);
		assertThat(headers.getTimestamp()).isNotNull();
	}

	@Test
	public void testTimestampOverwritten() throws Exception {
		MessageHeaders headers1 = new MessageHeaders(null);
		Thread.sleep(50L);
		MessageHeaders headers2 = new MessageHeaders(headers1);
		assertThat(headers2.getTimestamp()).isNotSameAs(headers1.getTimestamp());
	}

	@Test
	public void testIdOverwritten() {
		MessageHeaders headers1 = new MessageHeaders(null);
		MessageHeaders headers2 = new MessageHeaders(headers1);
		assertThat(headers2.getId()).isNotSameAs(headers1.getId());
	}

	@Test
	public void testId() {
		MessageHeaders headers = new MessageHeaders(null);
		assertThat(headers.getId()).isNotNull();
	}

	@Test
	public void testNonTypedAccessOfHeaderValue() {
		int value = 123;
		Map<String, Object> map = new HashMap<>();
		map.put("test", value);
		MessageHeaders headers = new MessageHeaders(map);
		assertThat(headers.get("test")).isEqualTo(value);
	}

	@Test
	public void testTypedAccessOfHeaderValue() {
		int value = 123;
		Map<String, Object> map = new HashMap<>();
		map.put("test", value);
		MessageHeaders headers = new MessageHeaders(map);
		assertThat(headers.get("test", Integer.class)).isEqualTo(value);
	}

	@Test
	public void testHeaderValueAccessWithIncorrectType() {
		int value = 123;
		Map<String, Object> map = new HashMap<>();
		map.put("test", value);
		MessageHeaders headers = new MessageHeaders(map);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> headers.get("test", String.class))
				.withMessageContaining(
						"Expected [class java.lang.String] but actual type is [class java.lang.Integer]");
	}

	@Test
	public void testNullHeaderValue() {
		Map<String, Object> map = new HashMap<>();
		MessageHeaders headers = new MessageHeaders(map);
		assertThat(headers.get("nosuchattribute")).isNull();
	}

	@Test
	public void testNullHeaderValueWithTypedAccess() {
		Map<String, Object> map = new HashMap<>();
		MessageHeaders headers = new MessageHeaders(map);
		assertThat(headers.get("nosuchattribute", String.class)).isNull();
	}

	@Test
	public void testHeaderKeys() {
		Map<String, Object> map = new HashMap<>();
		map.put("key1", "val1");
		map.put("key2", 123);
		MessageHeaders headers = new MessageHeaders(map);
		Set<String> keys = headers.keySet();
		assertThat(keys.contains("key1")).isTrue();
		assertThat(keys.contains("key2")).isTrue();
	}

	@Test
	public void serializeWithAllSerializableHeaders() throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("name", "joe");
		map.put("age", 42);
		MessageHeaders input = new MessageHeaders(map);
		MessageHeaders output = (MessageHeaders) serializeAndDeserialize(input);
		assertThat(output.get("name")).isEqualTo("joe");
		assertThat(output.get("age")).isEqualTo(42);
	}

	@Test
	public void serializeWithNonSerializableHeader() throws Exception {
		Object address = new Object();
		Map<String, Object> map = new HashMap<>();
		map.put("name", "joe");
		map.put("address", address);
		MessageHeaders input = new MessageHeaders(map);
		MessageHeaders output = (MessageHeaders) serializeAndDeserialize(input);
		assertThat(output.get("name")).isEqualTo("joe");
		assertThat(output.get("address")).isNull();
	}


	private static Object serializeAndDeserialize(Object object) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(baos);
		out.writeObject(object);
		out.close();
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream in = new ObjectInputStream(bais);
		Object result = in.readObject();
		in.close();
		return result;
	}

}
