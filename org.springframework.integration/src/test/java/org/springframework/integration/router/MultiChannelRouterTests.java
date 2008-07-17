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

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class MultiChannelRouterTests {

	@Test
	public void testRoutingWithChannelResolver() {
		final QueueChannel channel1 = new QueueChannel();
		final QueueChannel channel2 = new QueueChannel();
		MultiChannelResolver channelResolver = new MultiChannelResolver() {
			public List<MessageChannel> resolve(Message<?> message) {
				List<MessageChannel> channels = new ArrayList<MessageChannel>();
				channels.add(channel1);
				channels.add(channel2);
				return channels;
			}
		};
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelResolver(channelResolver);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("test");
		router.handle(message);
		Message<?> result1 = channel1.receive(25);
		assertNotNull(result1);
		assertEquals("test", result1.getPayload());
		Message<?> result2 = channel2.receive(25);
		assertNotNull(result2);
		assertEquals("test", result2.getPayload());
	}

	@Test
	public void testRoutingWithChannelNameResolver() {
		MultiChannelNameResolver channelNameResolver = new MultiChannelNameResolver() {
			public String[] resolve(Message<?> message) {
				return new String[] {"channel1", "channel2"};
			}
		};
		QueueChannel channel1 = new QueueChannel();
		QueueChannel channel2 = new QueueChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("channel1", channel1);
		channelRegistry.registerChannel("channel2", channel2);
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		router.setChannelRegistry(channelRegistry);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("test");
		router.handle(message);
		Message<?> result1 = channel1.receive(25);
		assertNotNull(result1);
		assertEquals("test", result1.getPayload());
		Message<?> result2 = channel2.receive(25);
		assertNotNull(result2);
		assertEquals("test", result2.getPayload());
	}

	@Test(expected=ConfigurationException.class)
	public void testConfiguringBothChannelResolverAndChannelNameResolverIsNotAllowed() {
		MultiChannelResolver channelResolver = new MultiChannelResolver() {
			public List<MessageChannel> resolve(Message<?> message) {
				return null;
			}
		};
		MultiChannelNameResolver channelNameResolver = new MultiChannelNameResolver() {
			public String[] resolve(Message<?> message) {
				return null;
			}
		};
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelResolver(channelResolver);		
		router.setChannelNameResolver(channelNameResolver);
		router.afterPropertiesSet();
	}

	@Test
	public void testChannelResolutionFailureIgnoredByDefault() {
		MultiChannelResolver channelResolver = new MultiChannelResolver() {
			public List<MessageChannel> resolve(Message<?> message) {
				return null;
			}
		};
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelResolver(channelResolver);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("test");
		router.handle(message);
	}

	@Test(expected=MessageHandlingException.class)
	public void testChannelResolutionFailureThrowsExceptionWhenResolutionRequired() {
		MultiChannelResolver channelResolver = new MultiChannelResolver() {
			public List<MessageChannel> resolve(Message<?> message) {
				return null;
			}
		};
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelResolver(channelResolver);
		router.setResolutionRequired(true);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("test");
		router.handle(message);
	}

	@Test
	public void testChannelNameResolutionFailureIgnoredByDefault() {
		MultiChannelNameResolver channelNameResolver = new MultiChannelNameResolver() {
			public String[] resolve(Message<?> message) {
				return new String[] {"noSuchChannel"};
			}
		};
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		router.setChannelRegistry(channelRegistry);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("test");
		router.handle(message);
	}

	@Test(expected=MessageHandlingException.class)
	public void testChannelNameResolutionFailureThrowsExceptionWhenResolutionRequired() {
		MultiChannelNameResolver channelNameResolver = new MultiChannelNameResolver() {
			public String[] resolve(Message<?> message) {
				return null;
			}
		};
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		router.setChannelRegistry(channelRegistry);
		router.setResolutionRequired(true);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("test");
		router.handle(message);
	}

	@Test(expected=ConfigurationException.class)
	public void testChannelRegistryIsRequiredWhenUsingChannelNameResolver() {
		MultiChannelNameResolver channelNameResolver = new MultiChannelNameResolver() {
			public String[] resolve(Message<?> message) {
				return new String[] {"notImportant"};
			}
		};
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		router.resolveChannels(new StringMessage("this should fail"));
	}

	@Test(expected=ConfigurationException.class)
	public void testValidateChannelRegistryIsPresentWhenUsingChannelNameResolver() {
		MultiChannelNameResolver channelNameResolver = new MultiChannelNameResolver() {
			public String[] resolve(Message<?> message) {
				return new String[] {"notImportant"};
			}
		};
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		router.afterPropertiesSet();
	}

	@Test(expected=ConfigurationException.class)
	public void testChannelResolverIsRequired() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelRegistry(channelRegistry);
		router.afterPropertiesSet();
	}

}
