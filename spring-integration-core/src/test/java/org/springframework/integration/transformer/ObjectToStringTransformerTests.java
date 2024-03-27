/*
 * Copyright 2002-2024 the original author or authors.
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

import java.nio.charset.Charset;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Andrew Cowlin
 * @author Gary Russell
 */
public class ObjectToStringTransformerTests {

	@Test
	public void stringPayload() {
		Transformer transformer = new ObjectToStringTransformer();
		Message<?> result = transformer.transform(new GenericMessage<String>("foo"));
		assertThat(result.getPayload()).isEqualTo("foo");
	}

	@Test
	public void objectPayload() {
		Transformer transformer = new ObjectToStringTransformer();
		Message<?> result = transformer.transform(new GenericMessage<TestBean>(new TestBean()));
		assertThat(result.getPayload()).isEqualTo("test");
	}

	@Test
	public void byteArrayPayload() throws Exception {
		Transformer transformer = new ObjectToStringTransformer();
		Message<?> result = transformer.transform(new GenericMessage<byte[]>(("foo" + '\u0fff').getBytes("UTF-8")));
		assertThat(result.getPayload()).isEqualTo("foo" + '\u0fff');
	}

	@Test
	public void byteArrayPayloadCharset() throws Exception {
		String defaultCharsetName = Charset.defaultCharset().toString();
		Transformer transformer = new ObjectToStringTransformer(defaultCharsetName);
		Message<?> result = transformer.transform(new GenericMessage<byte[]>("foo".getBytes(defaultCharsetName)));
		assertThat(result.getPayload()).isEqualTo("foo");
	}

	@Test
	public void charArrayPayload() {
		Transformer transformer = new ObjectToStringTransformer();
		Message<?> result = transformer.transform(new GenericMessage<char[]>("foo".toCharArray()));
		assertThat(result.getPayload()).isEqualTo("foo");
	}

	private static class TestBean {

		TestBean() {
			super();
		}

		@Override
		public String toString() {
			return "test";
		}

	}

}
