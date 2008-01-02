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

import org.junit.Test;

import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PointToPointChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class RoutingMessageHandlerTests {

	@Test
	public void testRoutingWithChannelResolver() {
		final PointToPointChannel channel = new PointToPointChannel();
		ChannelResolver channelResolver = new ChannelResolver() {
			public MessageChannel resolve(Message<?> message) {
				return channel;
			}
		};
		RoutingMessageHandler router = new RoutingMessageHandler();
		router.setChannelResolver(channelResolver);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("123", "test");
		router.handle(message);
		Message<?> result = channel.receive(25);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
	}

	@Test
	public void testRoutingWithChannelNameResolver() {
		ChannelNameResolver channelNameResolver = new ChannelNameResolver() {
			public String resolve(Message<?> message) {
				return "testChannel";
			}
		};
		PointToPointChannel channel = new PointToPointChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("testChannel", channel);
		RoutingMessageHandler router = new RoutingMessageHandler();
		router.setChannelNameResolver(channelNameResolver);
		router.setChannelRegistry(channelRegistry);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("123", "test");
		router.handle(message);
		Message<?> result = channel.receive(25);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
	}

	@Test(expected=MessagingConfigurationException.class)
	public void testConfiguringBothChannelResolverAndChannelNameResolverIsNotAllowed() {
		ChannelResolver channelResolver = new ChannelResolver() {
			public MessageChannel resolve(Message<?> message) {
				return new PointToPointChannel();
			}
		};
		ChannelNameResolver channelNameResolver = new ChannelNameResolver() {
			public String resolve(Message<?> message) {
				return "";
			}
		};
		RoutingMessageHandler router = new RoutingMessageHandler();
		router.setChannelResolver(channelResolver);		
		router.setChannelNameResolver(channelNameResolver);
		router.afterPropertiesSet();
	}

	@Test(expected=MessageHandlingException.class)
	public void testChannelResolutionFailure() {
		ChannelResolver channelResolver = new ChannelResolver() {
			public MessageChannel resolve(Message<?> message) {
				return null;
			}
		};
		RoutingMessageHandler router = new RoutingMessageHandler();
		router.setChannelResolver(channelResolver);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("123", "test");
		router.handle(message);
	}

	@Test(expected=MessageHandlingException.class)
	public void testChannelNameResolutionFailure() {
		ChannelNameResolver channelNameResolver = new ChannelNameResolver() {
			public String resolve(Message<?> message) {
				return "noSuchChannel";
			}
		};
		PointToPointChannel channel = new PointToPointChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("testChannel", channel);
		RoutingMessageHandler router = new RoutingMessageHandler();
		router.setChannelNameResolver(channelNameResolver);
		router.setChannelRegistry(channelRegistry);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("123", "test");
		router.handle(message);
	}

}
