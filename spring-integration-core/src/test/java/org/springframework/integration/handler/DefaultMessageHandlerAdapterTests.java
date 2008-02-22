/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class DefaultMessageHandlerAdapterTests {

	@Test
	public void testPayloadAsMethodParameterAndObjectAsReturnValue() {
		DefaultMessageHandlerAdapter<TestHandler> adapter = new DefaultMessageHandlerAdapter<TestHandler>();
		adapter.setObject(new TestHandler());
		adapter.setMethodName("acceptPayloadAndReturnObject");
		adapter.afterPropertiesSet();
		Message<?> result = adapter.handle(new StringMessage("testing"));
		assertEquals("testing-1", result.getPayload());
	}

	@Test
	public void testPayloadAsMethodParameterAndMessageAsReturnValue() {
		DefaultMessageHandlerAdapter<TestHandler> adapter = new DefaultMessageHandlerAdapter<TestHandler>();
		adapter.setObject(new TestHandler());
		adapter.setMethodName("acceptPayloadAndReturnMessage");
		adapter.afterPropertiesSet();
		Message<?> result = adapter.handle(new StringMessage("testing"));
		assertEquals("testing-2", result.getPayload());
	}

	@Test
	public void testMessageAsMethodParameterAndObjectAsReturnValue() {
		DefaultMessageHandlerAdapter<TestHandler> adapter = new DefaultMessageHandlerAdapter<TestHandler>();
		adapter.setExpectsMessage(true);
		adapter.setObject(new TestHandler());
		adapter.setMethodName("acceptMessageAndReturnObject");
		adapter.afterPropertiesSet();
		Message<?> result = adapter.handle(new StringMessage("testing"));
		assertEquals("testing-3", result.getPayload());
	}

	@Test
	public void testMessageAsMethodParameterAndMessageAsReturnValue() {
		DefaultMessageHandlerAdapter<TestHandler> adapter = new DefaultMessageHandlerAdapter<TestHandler>();
		adapter.setExpectsMessage(true);
		adapter.setObject(new TestHandler());
		adapter.setMethodName("acceptMessageAndReturnMessage");
		adapter.afterPropertiesSet();
		Message<?> result = adapter.handle(new StringMessage("testing"));
		assertEquals("testing-4", result.getPayload());
	}

	@Test
	public void testMessageSubclassAsMethodParameterAndMessageAsReturnValue() {
		DefaultMessageHandlerAdapter<TestHandler> adapter = new DefaultMessageHandlerAdapter<TestHandler>();
		adapter.setExpectsMessage(true);
		adapter.setObject(new TestHandler());
		adapter.setMethodName("acceptMessageSubclassAndReturnMessage");
		adapter.afterPropertiesSet();
		Message<?> result = adapter.handle(new StringMessage("testing"));
		assertEquals("testing-5", result.getPayload());
	}

	@Test
	public void testMessageSubclassAsMethodParameterAndMessageSubclassAsReturnValue() {
		DefaultMessageHandlerAdapter<TestHandler> adapter = new DefaultMessageHandlerAdapter<TestHandler>();
		adapter.setExpectsMessage(true);
		adapter.setObject(new TestHandler());
		adapter.setMethodName("acceptMessageSubclassAndReturnMessageSubclass");
		adapter.afterPropertiesSet();
		Message<?> result = adapter.handle(new StringMessage("testing"));
		assertEquals("testing-6", result.getPayload());
	}


	private static class TestHandler {

		public String acceptPayloadAndReturnObject(String s) {
			return s + "-1";
		}

		public Message<?> acceptPayloadAndReturnMessage(String s) {
			return new StringMessage(s + "-2");
		}

		public String acceptMessageAndReturnObject(Message<?> m) {
			return m.getPayload() + "-3";
		}

		public Message<?> acceptMessageAndReturnMessage(Message<?> m) {
			return new StringMessage(m.getPayload() + "-4");
		}

		public Message<?> acceptMessageSubclassAndReturnMessage(StringMessage m) {
			return new StringMessage(m.getPayload() + "-5");
		}

		public StringMessage acceptMessageSubclassAndReturnMessageSubclass(StringMessage m) {
			return new StringMessage(m.getPayload() + "-6");
		}
	}

}
