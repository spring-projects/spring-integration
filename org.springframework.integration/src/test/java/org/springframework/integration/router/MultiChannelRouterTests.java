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

package org.springframework.integration.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.TestChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class MultiChannelRouterTests {

	@Test
	public void routeWithChannelResolver() {
		final QueueChannel channel1 = new QueueChannel();
		final QueueChannel channel2 = new QueueChannel();
		ChannelResolver channelResolver = new ChannelResolver() {
			public List<MessageChannel> resolveChannels(Message<?> message) {
				List<MessageChannel> channels = new ArrayList<MessageChannel>();
				channels.add(channel1);
				channels.add(channel2);
				return channels;
			}
		};
		RouterEndpoint endpoint = new RouterEndpoint(channelResolver);
		Message<String> message = new StringMessage("test");
		endpoint.onMessage(message);
		Message<?> result1 = channel1.receive(25);
		assertNotNull(result1);
		assertEquals("test", result1.getPayload());
		Message<?> result2 = channel2.receive(25);
		assertNotNull(result2);
		assertEquals("test", result2.getPayload());
	}

	@Test
	public void routeWithChannelNameResolver() {
		AbstractChannelNameResolver channelNameResolver = new AbstractChannelNameResolver() {
			public String[] resolveChannelNames(Message<?> message) {
				return new String[] {"channel1", "channel2"};
			}
		};
		QueueChannel channel1 = new QueueChannel();
		QueueChannel channel2 = new QueueChannel();
		channel1.setBeanName("channel1");
		channel2.setBeanName("channel2");
		ChannelRegistry channelRegistry = new TestChannelRegistry();
		channelRegistry.registerChannel(channel1);
		channelRegistry.registerChannel(channel2);
		RouterEndpoint endpoint = new RouterEndpoint(channelNameResolver);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> message = new StringMessage("test");
		endpoint.onMessage(message);
		Message<?> result1 = channel1.receive(25);
		assertNotNull(result1);
		assertEquals("test", result1.getPayload());
		Message<?> result2 = channel2.receive(25);
		assertNotNull(result2);
		assertEquals("test", result2.getPayload());
	}

	@Test(expected = MessagingException.class)
	public void channelNameLookupFailure() {
		AbstractChannelNameResolver channelNameResolver = new AbstractChannelNameResolver() {
			public String[] resolveChannelNames(Message<?> message) {
				return new String[] {"noSuchChannel"};
			}
		};
		ChannelRegistry channelRegistry = new TestChannelRegistry();
		RouterEndpoint endpoint = new RouterEndpoint(channelNameResolver);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> message = new StringMessage("test");
		endpoint.onMessage(message);
	}

	@Test(expected = MessagingException.class)
	public void channelRegistryNotAvailable() {
		AbstractChannelNameResolver channelNameResolver = new AbstractChannelNameResolver() {
			public String[] resolveChannelNames(Message<?> message) {
				return new String[] {"noSuchChannel"};
			}
		};
		RouterEndpoint endpoint = new RouterEndpoint(channelNameResolver);
		Message<String> message = new StringMessage("test");
		endpoint.onMessage(message);
	}

}
