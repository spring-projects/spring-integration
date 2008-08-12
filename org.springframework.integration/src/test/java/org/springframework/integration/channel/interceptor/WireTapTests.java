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

package org.springframework.integration.channel.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * @author Mark Fisher
 */
public class WireTapTests {

	@Test
	public void wireTapWithNoSelectors() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		mainChannel.addInterceptor(new WireTap(secondaryChannel));
		mainChannel.send(new StringMessage("testing"));
		Message<?> original = mainChannel.receive(0);
		assertNotNull(original);
		Message<?> intercepted = secondaryChannel.receive(0);
		assertNotNull(intercepted);
		assertEquals(original, intercepted);
	}

	@Test
	public void wireTapWithRejectingSelector() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		List<MessageSelector> selectors = new ArrayList<MessageSelector>();
		selectors.add(new TestSelector(true));
		selectors.add(new TestSelector(false));
		mainChannel.addInterceptor(new WireTap(secondaryChannel, selectors));
		mainChannel.send(new StringMessage("testing"));
		Message<?> original = mainChannel.receive(0);
		assertNotNull(original);
		Message<?> intercepted = secondaryChannel.receive(0);
		assertNull(intercepted);
	}

	@Test
	public void wireTapWithAcceptingSelectors() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		List<MessageSelector> selectors = new ArrayList<MessageSelector>();
		selectors.add(new TestSelector(true));
		selectors.add(new TestSelector(true));
		mainChannel.addInterceptor(new WireTap(secondaryChannel, selectors));
		mainChannel.send(new StringMessage("testing"));
		Message<?> original = mainChannel.receive(0);
		assertNotNull(original);
		Message<?> intercepted = secondaryChannel.receive(0);
		assertNotNull(intercepted);
		assertEquals(original, intercepted);
	}

	@Test(expected = IllegalArgumentException.class)
	public void wireTapTargetMustNotBeNull() {
		new WireTap(null);
	}

	@Test
	public void simpleTargetWireTap() {
		QueueChannel mainChannel = new QueueChannel();
		TestTarget secondaryTarget = new TestTarget();
		mainChannel.addInterceptor(new WireTap(secondaryTarget));
		assertNull(secondaryTarget.getLastMessage());
		Message<?> message = new StringMessage("testing");
		mainChannel.send(message);
		Message<?> original = mainChannel.receive(0);
		Message<?> intercepted = secondaryTarget.getLastMessage();
		assertNotNull(original);
		assertNotNull(intercepted);
		assertEquals(original, intercepted);
	}

	@Test
	public void interceptedMessageContainsHeaderValue() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		mainChannel.addInterceptor(new WireTap(secondaryChannel));
		String headerName = "testAttribute";
		Message<String> message = MessageBuilder.fromPayload("testing")
				.setHeader(headerName, new Integer(123)).build();
		mainChannel.send(message);
		Message<?> original = mainChannel.receive(0);
		Message<?> intercepted = secondaryChannel.receive(0);
		Object originalAttribute = original.getHeaders().get(headerName);
		Object interceptedAttribute = intercepted.getHeaders().get(headerName);
		assertNotNull(originalAttribute);
		assertNotNull(interceptedAttribute);
		assertEquals(originalAttribute, interceptedAttribute);
	}


	private static class TestSelector implements MessageSelector {

		private boolean shouldAccept;

		public TestSelector(boolean shouldAccept) {
			this.shouldAccept = shouldAccept;
		}

		public boolean accept(Message<?> message) {
			return this.shouldAccept;
		}
	}

	private static class TestTarget implements MessageTarget {

		private volatile Message<?> lastMessage;

		public boolean send(Message<?> message) {
			this.lastMessage = message;
			return true;
		}

		public Message<?> getLastMessage() {
			return this.lastMessage;
		}
	}

}
