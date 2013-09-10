/*
 * Copyright 2002-2010 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Jonas Partner
 */
public class MessageTransformingChannelInterceptorTests {

	private QueueChannel channel; 

	private GenericMessage<String> message;

	private TestTransformer transformer;

	private MessageTransformingChannelInterceptor channelInterceptor;


	@Before
	public void setUp() {
		channel = new QueueChannel();
		message = new GenericMessage<String>("test");
		transformer = new TestTransformer();
		channelInterceptor = new MessageTransformingChannelInterceptor(transformer);
		channel.addInterceptor(channelInterceptor);
	}


	@Test
	public void testTransformOnReceive() {
		channelInterceptor.setTransformOnSend(false);
		channel.send(message);
		assertFalse("Transformer incorrectly invoked on send", transformer.invoked);
		Message<?> msg = channel.receive(1);
		assertEquals("Wrong message", message, msg);
		assertTrue("Transformer not invoked on receive", transformer.invoked);
	}

	@Test
	public void testTransformOnSend() {
		channelInterceptor.setTransformOnSend(true);
		channel.send(message);
		assertTrue("Transformer not invoked on send", transformer.invoked);
		Message<?> msg = channel.receive(1);
		assertEquals("Wrong message", message, msg);
		assertEquals("Transformer invoked on receive", 1, transformer.invokedCount);
	}


	private static class TestTransformer implements Transformer {

		boolean invoked = false;

		int invokedCount = 0;

		public Message<?> transform(Message<?> message) {
			invoked = true;
			invokedCount++;
			return message;
		}

	}

}
