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

package org.springframework.integration.handler.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class DefaultMessageHandlerCreatorTests {

	@Test
	public void testPayloadAsMethodParameterAndObjectAsReturnValue() throws Exception {
		DefaultMessageHandlerCreator creator = new DefaultMessageHandlerCreator();
		MessageHandler handler = creator.createHandler(new TestHandler(),
				TestHandler.class.getMethod("acceptPayloadAndReturnObject", String.class), null);
		Message<?> result = handler.handle(new StringMessage("testing"));
		assertEquals("testing-1", result.getPayload());
	}

	@Test
	public void testPayloadAsMethodParameterAndMessageAsReturnValue() throws Exception {
		DefaultMessageHandlerCreator creator = new DefaultMessageHandlerCreator();
		MessageHandler handler = creator.createHandler(new TestHandler(),
				TestHandler.class.getMethod("acceptPayloadAndReturnMessage", String.class), null);
		Message<?> result = handler.handle(new StringMessage("testing"));
		assertEquals("testing-2", result.getPayload());
	}

	@Test
	public void testMessageAsMethodParameterAndObjectAsReturnValue() throws Exception {
		DefaultMessageHandlerCreator creator = new DefaultMessageHandlerCreator();
		MessageHandler handler = creator.createHandler(new TestHandler(),
				TestHandler.class.getMethod("acceptMessageAndReturnObject", Message.class), null);
		Message<?> result = handler.handle(new StringMessage("testing"));
		assertEquals("testing-3", result.getPayload());
	}

	@Test
	public void testMessageAsMethodParameterAndMessageAsReturnValue() throws Exception {
		DefaultMessageHandlerCreator creator = new DefaultMessageHandlerCreator();
		MessageHandler handler = creator.createHandler(new TestHandler(),
				TestHandler.class.getMethod("acceptMessageAndReturnMessage", Message.class), null);
		Message<?> result = handler.handle(new StringMessage("testing"));
		assertEquals("testing-4", result.getPayload());
	}

	@Test
	public void testMessageSubclassAsMethodParameterAndMessageAsReturnValue() throws Exception {
		DefaultMessageHandlerCreator creator = new DefaultMessageHandlerCreator();
		MessageHandler handler = creator.createHandler(new TestHandler(),
				TestHandler.class.getMethod("acceptMessageSubclassAndReturnMessage", StringMessage.class), null);
		Message<?> result = handler.handle(new StringMessage("testing"));
		assertEquals("testing-5", result.getPayload());
	}

	@Test
	public void testMessageSubclassAsMethodParameterAndMessageSubclassAsReturnValue() throws Exception {
		DefaultMessageHandlerCreator creator = new DefaultMessageHandlerCreator();
		MessageHandler handler = creator.createHandler(new TestHandler(),
				TestHandler.class.getMethod("acceptMessageSubclassAndReturnMessageSubclass", StringMessage.class), null);
		Message<?> result = handler.handle(new StringMessage("testing"));
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
