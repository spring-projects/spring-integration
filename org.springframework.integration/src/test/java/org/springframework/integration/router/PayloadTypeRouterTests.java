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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class PayloadTypeRouterTests {

	@Test
	public void resolveByPayloadType() {
		QueueChannel stringChannel = new QueueChannel();
		QueueChannel integerChannel = new QueueChannel();
		Map<Class<?>, MessageChannel> channelMappings = new ConcurrentHashMap<Class<?>, MessageChannel>();
		channelMappings.put(String.class, stringChannel);
		channelMappings.put(Integer.class, integerChannel);
		PayloadTypeChannelResolver resolver = new PayloadTypeChannelResolver();
		resolver.setChannelMappings(channelMappings);
		Message<String> message1 = new StringMessage("test");
		Message<Integer> message2 = new GenericMessage<Integer>(123);
		MessageChannel result1 = resolver.resolveChannel(message1);
		MessageChannel result2 = resolver.resolveChannel(message2);
		assertEquals(stringChannel, result1);
		assertEquals(integerChannel, result2);
	}

	@Test
	public void resolveByPayloadTypeWithRouterEndpoint() {
		QueueChannel stringChannel = new QueueChannel();
		QueueChannel integerChannel = new QueueChannel();
		stringChannel.setBeanName("stringChannel");
		integerChannel.setBeanName("integerChannel");
		Map<Class<?>, MessageChannel> channelMappings = new ConcurrentHashMap<Class<?>, MessageChannel>();
		channelMappings.put(String.class, stringChannel);
		channelMappings.put(Integer.class, integerChannel);
		DefaultChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel(stringChannel);
		channelRegistry.registerChannel(integerChannel);
		PayloadTypeChannelResolver resolver = new PayloadTypeChannelResolver();
		resolver.setChannelMappings(channelMappings);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> message1 = new StringMessage("test");
		Message<Integer> message2 = new GenericMessage<Integer>(123);
		endpoint.send(message1);
		endpoint.send(message2);
		Message<?> reply1 = stringChannel.receive(0);
		Message<?> reply2 = integerChannel.receive(0);
		assertEquals("test", reply1.getPayload());
		assertEquals(123, reply2.getPayload());
	}

	@Test
	public void routingToDefaultChannelWhenNoTypeMatches() {
		QueueChannel stringChannel = new QueueChannel();
		stringChannel.setBeanName("stringChannel");
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		DefaultChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel(stringChannel);
		channelRegistry.registerChannel(defaultChannel);
		Map<Class<?>, MessageChannel> channelMappings = new ConcurrentHashMap<Class<?>, MessageChannel>();
		channelMappings.put(String.class, stringChannel);
		PayloadTypeChannelResolver resolver = new PayloadTypeChannelResolver();
		resolver.setChannelMappings(channelMappings);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		endpoint.setDefaultOutputChannel(defaultChannel);
		Message<String> message1 = new StringMessage("test");
		Message<Integer> message2 = new GenericMessage<Integer>(123);
		endpoint.send(message1);
		endpoint.send(message2);
		Message<?> result1 = stringChannel.receive(25);
		assertNotNull(result1);
		assertEquals("test", result1.getPayload());
		Message<?> result2 = defaultChannel.receive(25);
		assertNotNull(result2);
		assertEquals(123, result2.getPayload());
	}

}
