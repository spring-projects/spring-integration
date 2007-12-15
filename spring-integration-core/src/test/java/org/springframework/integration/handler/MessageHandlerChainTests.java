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
public class MessageHandlerChainTests {

	@Test
	public void testSimpleChain() {
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.add(new TestHandler("a"));
		chain.add(new TestHandler("b"));
		chain.add(new TestHandler("c"));
		chain.add(new TestHandler("d"));
		Message result = chain.handle(new StringMessage(1, "!"));
		assertEquals("!abcd", result.getPayload());
	}

	@Test
	public void testChainWithInterceptors() {
		MessageHandler handler1 = new TestHandler("*"); 
		MessageHandler handler2 = new TestInterceptingHandler("2", handler1);
		MessageHandler handler3 = new TestInterceptingHandler("3", handler2);
		MessageHandler handler4 = new TestInterceptingHandler("4", handler3);
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.add(new TestHandler("a"));
		chain.add(handler4);
		chain.add(new TestHandler("b"));		
		Message result = chain.handle(new StringMessage(1, "!"));
		assertEquals("234!a*234b", result.getPayload());
	}


	private static class TestHandler implements MessageHandler {

		private String text;

		TestHandler(String text) {
			this.text = text;
		}

		public Message handle(Message message) {
			return new StringMessage(1, message.getPayload() + text);
		}
	}


	private static class TestInterceptingHandler extends InterceptingMessageHandler {

		private String text;

		TestInterceptingHandler(String text, MessageHandler target) {
			super(target);
			this.text = text;
		}

		public Message handle(Message message, MessageHandler target) {
			message = target.handle(new StringMessage(1, text + message.getPayload()));
			return new StringMessage(1, message.getPayload() + text);
		}
	}

}
