/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.List;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(purgedMessages.size()).isEqualTo(3);
		assertThat(channel.receive(0)).isNull();
	}

	@Test
	public void testPurgeAllWithSelector() {
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<String>("test1"));
		channel.send(new GenericMessage<String>("test2"));
		channel.send(new GenericMessage<String>("test3"));
		ChannelPurger purger = new ChannelPurger(message -> false, channel);
		List<Message<?>> purgedMessages = purger.purge();
		assertThat(purgedMessages.size()).isEqualTo(3);
		assertThat(channel.receive(0)).isNull();
	}

	@Test
	public void testPurgeNoneWithSelector() {
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<String>("test1"));
		channel.send(new GenericMessage<String>("test2"));
		channel.send(new GenericMessage<String>("test3"));
		ChannelPurger purger = new ChannelPurger(message -> true, channel);
		List<Message<?>> purgedMessages = purger.purge();
		assertThat(purgedMessages.size()).isEqualTo(0);
		assertThat(channel.receive(0)).isNotNull();
		assertThat(channel.receive(0)).isNotNull();
		assertThat(channel.receive(0)).isNotNull();
	}

	@Test
	public void testPurgeSubsetWithSelector() {
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<String>("test1"));
		channel.send(new GenericMessage<String>("test2"));
		channel.send(new GenericMessage<String>("test3"));
		ChannelPurger purger = new ChannelPurger(message -> (message.getPayload().equals("test2")), channel);
		List<Message<?>> purgedMessages = purger.purge();
		assertThat(purgedMessages.size()).isEqualTo(2);
		Message<?> message = channel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("test2");
		assertThat(channel.receive(0)).isNull();
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
		assertThat(purgedMessages.size()).isEqualTo(4);
		assertThat(channel1.receive(0)).isNull();
		assertThat(channel2.receive(0)).isNull();
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
		ChannelPurger purger = new ChannelPurger(message -> (message.getPayload().equals("test2")), channel1, channel2);
		List<Message<?>> purgedMessages = purger.purge();
		assertThat(purgedMessages.size()).isEqualTo(4);
		Message<?> message1 = channel1.receive(0);
		assertThat(message1).isNotNull();
		assertThat(message1.getPayload()).isEqualTo("test2");
		assertThat(channel1.receive(0)).isNull();
		Message<?> message2 = channel2.receive(0);
		assertThat(message2).isNotNull();
		assertThat(message2.getPayload()).isEqualTo("test2");
		assertThat(channel2.receive(0)).isNull();
	}

	@Test
	public void testPurgeNoneWithSelectorAndMultipleChannels() {
		QueueChannel channel1 = new QueueChannel();
		QueueChannel channel2 = new QueueChannel();
		channel1.send(new GenericMessage<String>("test1"));
		channel1.send(new GenericMessage<String>("test2"));
		channel2.send(new GenericMessage<String>("test1"));
		channel2.send(new GenericMessage<String>("test2"));
		ChannelPurger purger = new ChannelPurger(message -> true, channel1, channel2);
		List<Message<?>> purgedMessages = purger.purge();
		assertThat(purgedMessages.size()).isEqualTo(0);
		assertThat(channel1.receive(0)).isNotNull();
		assertThat(channel1.receive(0)).isNotNull();
		assertThat(channel2.receive(0)).isNotNull();
		assertThat(channel2.receive(0)).isNotNull();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullChannel() {
		QueueChannel channel = null;
		new ChannelPurger(channel);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyChannelArray() {
		QueueChannel[] channels = new QueueChannel[0];
		new ChannelPurger(channels);
	}

}
