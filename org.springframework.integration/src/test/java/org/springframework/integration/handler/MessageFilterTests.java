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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * @author Mark Fisher
 */
public class MessageFilterTests {

	@Test
	public void filterAcceptsMessage() {
		MessageFilter filter = new MessageFilter(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return true;
			}
		});
		Message<?> message = new StringMessage("test");
		assertEquals(message, filter.handle(message));
	}

	@Test
	public void filterRejectsMessage() {
		MessageFilter filter = new MessageFilter(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return false;
			}
		});
		assertNull(filter.handle(new StringMessage("test")));
	}

	@Test
	public void filterAcceptsWithChannels() {
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		MessageFilter filter = new MessageFilter(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return true;
			}
		});
		filter.setInputChannel(inputChannel);
		filter.setOutputChannel(outputChannel);
		filter.start();
		Message<?> message = new StringMessage("test");
		assertTrue(inputChannel.send(message));
		Message<?> reply = outputChannel.receive(0);
		assertNotNull(reply);
		assertEquals(message, reply);
	}

	@Test
	public void filterRejectsWithChannels() {
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		MessageFilter filter = new MessageFilter(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return false;
			}
		});
		filter.setInputChannel(inputChannel);
		filter.setOutputChannel(outputChannel);
		filter.start();
		Message<?> message = new StringMessage("test");
		assertTrue(inputChannel.send(message));
		assertNull(outputChannel.receive(0));
	}

}
