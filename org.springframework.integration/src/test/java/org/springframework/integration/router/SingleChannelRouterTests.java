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

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.TestChannelResolver;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class SingleChannelRouterTests {

	@Test
	public void routeWithChannelResolver() {
		final QueueChannel channel = new QueueChannel();
		AbstractSingleChannelRouter router = new AbstractSingleChannelRouter() {
			public MessageChannel determineTargetChannel(Message<?> message) {
				return channel;
			}
		};
		Message<String> message = new StringMessage("test");
		router.onMessage(message);
		Message<?> result = channel.receive(25);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
	}

	@Test
	public void routeWithChannelNameResolver() {
		AbstractSingleChannelNameRouter router = new AbstractSingleChannelNameRouter() {
			public String determineTargetChannelName(Message<?> message) {
				return "testChannel";
			}
		};
		QueueChannel channel = new QueueChannel();
		channel.setBeanName("testChannel");
		TestChannelResolver channelResolver = new TestChannelResolver();
		channelResolver.addChannel(channel);
		router.setChannelResolver(channelResolver);
		Message<String> message = new StringMessage("test");
		router.onMessage(message);
		Message<?> result = channel.receive(25);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
	}

	@Test
	public void nullChannelResultIgnored() {
		AbstractSingleChannelRouter router = new AbstractSingleChannelRouter() {
			public MessageChannel determineTargetChannel(Message<?> message) {
				return null;
			}
		};
		Message<String> message = new StringMessage("test");
		router.onMessage(message);
	}

	@Test(expected = MessagingException.class)
	public void channelNameResolutionFailure() {
		AbstractSingleChannelNameRouter router = new AbstractSingleChannelNameRouter() {
			public String determineTargetChannelName(Message<?> message) {
				return "noSuchChannel";
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		Message<String> message = new StringMessage("test");
		router.onMessage(message);
	}

}
