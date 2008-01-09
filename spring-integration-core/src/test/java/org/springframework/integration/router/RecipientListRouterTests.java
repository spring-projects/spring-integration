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

package org.springframework.integration.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class RecipientListRouterTests {

	@Test
	public void testRoutingWithChannelList() {
		SimpleChannel channel1 = new SimpleChannel();
		SimpleChannel channel2 = new SimpleChannel();
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.add(channel1);
		channels.add(channel2);
		RecipientListRouter router = new RecipientListRouter();
		router.setChannels(channels);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("123", "test");
		router.handle(message);
		Message<?> result1 = channel1.receive(25);
		assertNotNull(result1);
		assertEquals("test", result1.getPayload());
		Message<?> result2 = channel2.receive(25);
		assertNotNull(result2);
		assertEquals("test", result2.getPayload());
	}

	@Test
	public void testRoutingWithChannelNames() {
		SimpleChannel channel1 = new SimpleChannel();
		SimpleChannel channel2 = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("channel1", channel1);
		channelRegistry.registerChannel("channel2", channel2);
		RecipientListRouter router = new RecipientListRouter();
		router.setChannelNames(new String[] {"channel1", "channel2"});
		router.setChannelRegistry(channelRegistry);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("123", "test");
		router.handle(message);
		Message<?> result1 = channel1.receive(25);
		assertNotNull(result1);
		assertEquals("test", result1.getPayload());
		Message<?> result2 = channel2.receive(25);
		assertNotNull(result2);
		assertEquals("test", result2.getPayload());
	}

	@Test
	public void testRoutingToSingleChannelByName() {
		SimpleChannel channel1 = new SimpleChannel();
		SimpleChannel channel2 = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("channel1", channel1);
		channelRegistry.registerChannel("channel2", channel2);
		RecipientListRouter router = new RecipientListRouter();
		router.setChannelNames(new String[] {"channel1"});
		router.setChannelRegistry(channelRegistry);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("123", "test");
		router.handle(message);
		Message<?> result1 = channel1.receive(25);
		assertNotNull(result1);
		assertEquals("test", result1.getPayload());
		Message<?> result2 = channel2.receive(5);
		assertNull(result2);
	}

	@Test(expected=MessagingConfigurationException.class)
	public void testConfigurationExceptionWhenBothChannelsAndNamesAreProvided() {
		SimpleChannel channel1 = new SimpleChannel();
		SimpleChannel channel2 = new SimpleChannel();
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.add(channel1);
		channels.add(channel2);
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("channel1", channel1);
		channelRegistry.registerChannel("channel2", channel2);
		RecipientListRouter router = new RecipientListRouter();
		router.setChannels(channels);
		router.setChannelNames(new String[] {"channel1"});
		router.setChannelRegistry(channelRegistry);
		router.afterPropertiesSet();
	}

}
