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

package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 */
public class ChannelPurgerTests {

	@Test
	public void testPurgeAllWithoutSelector() {
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<String>("test1"));
		channel.send(new GenericMessage<String>("test2"));
		channel.send(new GenericMessage<String>("test3"));
		ChannelPurger purger = new ChannelPurger(channel);
		List<Message<?>> purgedMessages = purger.purge();
		assertEquals(3, purgedMessages.size());
		assertNull(channel.receive(0));
	}

	@Test
	public void testPurgeAllWithSelector() {
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<String>("test1"));
		channel.send(new GenericMessage<String>("test2"));
		channel.send(new GenericMessage<String>("test3"));
		ChannelPurger purger = new ChannelPurger(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return false;
			}
		}, channel);
		List<Message<?>> purgedMessages = purger.purge();
		assertEquals(3, purgedMessages.size());
		assertNull(channel.receive(0));
	}

	@Test
	public void testPurgeNoneWithSelector() {
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<String>("test1"));
		channel.send(new GenericMessage<String>("test2"));
		channel.send(new GenericMessage<String>("test3"));
		ChannelPurger purger = new ChannelPurger(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return true;
			}
		}, channel);
		List<Message<?>> purgedMessages = purger.purge();
		assertEquals(0, purgedMessages.size());
		assertNotNull(channel.receive(0));
		assertNotNull(channel.receive(0));
		assertNotNull(channel.receive(0));
	}

	@Test
	public void testPurgeSubsetWithSelector() {
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<String>("test1"));
		channel.send(new GenericMessage<String>("test2"));
		channel.send(new GenericMessage<String>("test3"));
		ChannelPurger purger = new ChannelPurger(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return (message.getPayload().equals("test2"));
			}
		}, channel);
		List<Message<?>> purgedMessages = purger.purge();
		assertEquals(2, purgedMessages.size());
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertEquals("test2", message.getPayload());
		assertNull(channel.receive(0));
	}

	@Test
	public void testMultipleChannelsWithNoSelector() {
		QueueChannel channel1 = new QueueChannel();
		QueueChannel channel2 = new QueueChannel();
		channel1.send(new GenericMessage<String>("test1"));
		channel1.send(new GenericMessage<String>("test2"));
		channel2.send(new GenericMessage<String>("test1"));
		channel2.send(new GenericMessage<String>("test2"));
		ChannelPurger purger = new ChannelPurger(channel1, channel2);
		List<Message<?>> purgedMessages = purger.purge();
		assertEquals(4, purgedMessages.size());
		assertNull(channel1.receive(0));
		assertNull(channel2.receive(0));
	}

	@Test
	public void testMultipleChannelsWithSelector() {
		QueueChannel channel1 = new QueueChannel();
		QueueChannel channel2 = new QueueChannel();
		channel1.send(new GenericMessage<String>("test1"));
		channel1.send(new GenericMessage<String>("test2"));
		channel1.send(new GenericMessage<String>("test3"));
		channel2.send(new GenericMessage<String>("test1"));
		channel2.send(new GenericMessage<String>("test2"));
		channel2.send(new GenericMessage<String>("test3"));		
		ChannelPurger purger = new ChannelPurger(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return (message.getPayload().equals("test2"));
			}
		}, channel1, channel2);
		List<Message<?>> purgedMessages = purger.purge();
		assertEquals(4, purgedMessages.size());
		Message<?> message1 = channel1.receive(0);
		assertNotNull(message1);
		assertEquals("test2", message1.getPayload());
		assertNull(channel1.receive(0));
		Message<?> message2 = channel2.receive(0);
		assertNotNull(message2);
		assertEquals("test2", message2.getPayload());
		assertNull(channel2.receive(0));		
	}

	@Test
	public void testPurgeNoneWithSelectorAndMultipleChannels() {
		QueueChannel channel1 = new QueueChannel();
		QueueChannel channel2 = new QueueChannel();
		channel1.send(new GenericMessage<String>("test1"));
		channel1.send(new GenericMessage<String>("test2"));
		channel2.send(new GenericMessage<String>("test1"));
		channel2.send(new GenericMessage<String>("test2"));
		ChannelPurger purger = new ChannelPurger(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return true;
			}
		}, channel1, channel2);
		List<Message<?>> purgedMessages = purger.purge();
		assertEquals(0, purgedMessages.size());
		assertNotNull(channel1.receive(0));
		assertNotNull(channel1.receive(0));
		assertNotNull(channel2.receive(0));
		assertNotNull(channel2.receive(0));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullChannel() {
		QueueChannel channel = null;
		new ChannelPurger(channel);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testEmptyChannelArray() {
		QueueChannel[] channels = new QueueChannel[0];
		new ChannelPurger(channels);
	}

}
