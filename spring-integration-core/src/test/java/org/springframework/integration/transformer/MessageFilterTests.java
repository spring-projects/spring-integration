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

package org.springframework.integration.transformer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * @author Mark Fisher
 */
public class MessageFilterTests {

	@Test
	public void testFilterAcceptsMessage() {
		final AtomicBoolean secondHandlerReceived = new AtomicBoolean();
		MessageHandlerChain chain = new MessageHandlerChain();
		MessageFilter filter = new MessageFilter(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return true;
			}
		});
		chain.add(filter);
		chain.add(new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				secondHandlerReceived.set(true);
				return null;
			}
		});
		chain.handle(new StringMessage("test"));
		assertTrue(secondHandlerReceived.get());
	}

	@Test
	public void testFilterRejectsMessage() {
		final AtomicBoolean secondHandlerReceived = new AtomicBoolean();
		MessageHandlerChain chain = new MessageHandlerChain();
		MessageFilter filter = new MessageFilter(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return false;
			}
		});
		chain.add(filter);
		chain.add(new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				secondHandlerReceived.set(true);
				return null;
			}
		});
		chain.handle(new StringMessage("test"));
		assertFalse(secondHandlerReceived.get());
	}

}
