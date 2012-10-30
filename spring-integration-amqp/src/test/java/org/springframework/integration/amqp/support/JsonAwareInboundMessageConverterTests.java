/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.amqp.support;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class JsonAwareInboundMessageConverterTests {

	private String foobar = "{\"foo\":\"bar\"}";

	@Test
	public void testJsonToString() {
		Message amqpMessage = buildMessage("application/json");
		JsonAwareInboundMessageConverter converter = new JsonAwareInboundMessageConverter(java.lang.String.class);
		assertEquals(foobar, converter.fromMessage(amqpMessage));
	}

	@Test
	public void testJsonTextToString() {
		Message amqpMessage = buildMessage("text/json");
		JsonAwareInboundMessageConverter converter = new JsonAwareInboundMessageConverter(
				org.springframework.integration.amqp.support.JsonAwareInboundMessageConverterTests.Foo.class);
		assertEquals(foobar, converter.fromMessage(amqpMessage));
	}

	@Test
	public void testPlainTextToString() {
		Message amqpMessage = buildMessage("text/plain");
		JsonAwareInboundMessageConverter converter = new JsonAwareInboundMessageConverter(
				org.springframework.integration.amqp.support.JsonAwareInboundMessageConverterTests.Foo.class);
		assertEquals(foobar, converter.fromMessage(amqpMessage));
	}

	@Test
	public void testJsonToObject() throws Exception {
		Message amqpMessage = buildMessage("application/json");
		JsonAwareInboundMessageConverter converter = new JsonAwareInboundMessageConverter(
				org.springframework.integration.amqp.support.JsonAwareInboundMessageConverterTests.Foo.class);
		assertEquals(new Foo("bar"), converter.fromMessage(amqpMessage));
	}

	private Message buildMessage(String contentType) {
		MessageProperties messageProperties = new MessageProperties();
		messageProperties.setContentType(contentType);
		byte[] body = foobar.getBytes();
		Message amqpMessage = new Message(body, messageProperties);
		return amqpMessage;
	}

	public static class Foo {
		private String foo;

		public Foo() {
		}

		public Foo(String string) {
			this.foo = string;
		}

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((foo == null) ? 0 : foo.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Foo other = (Foo) obj;
			if (foo == null) {
				if (other.foo != null)
					return false;
			}
			else if (!foo.equals(other.foo))
				return false;
			return true;
		}
	}
}
