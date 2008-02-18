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

import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class SingleChannelRouterTests {

	@Test
	public void testRoutingWithChannelResolver() {
		final SimpleChannel channel = new SimpleChannel();
		ChannelResolver channelResolver = new ChannelResolver() {
			public MessageChannel resolve(Message<?> message) {
				return channel;
			}
		};
		SingleChannelRouter router = new SingleChannelRouter();
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
		SimpleChannel channel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("testChannel", channel);
		SingleChannelRouter router = new SingleChannelRouter();
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
				return new SimpleChannel();
			}
		};
		ChannelNameResolver channelNameResolver = new ChannelNameResolver() {
			public String resolve(Message<?> message) {
				return "";
			}
		};
		SingleChannelRouter router = new SingleChannelRouter();
		router.setChannelResolver(channelResolver);		
		router.setChannelNameResolver(channelNameResolver);
		router.afterPropertiesSet();
	}

	@Test
	public void testChannelResolutionFailureIgnoredByDefault() {
		ChannelResolver channelResolver = new ChannelResolver() {
			public MessageChannel resolve(Message<?> message) {
				return null;
			}
		};
		SingleChannelRouter router = new SingleChannelRouter();
		router.setChannelResolver(channelResolver);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("123", "test");
		router.handle(message);
	}

	@Test(expected=MessageHandlingException.class)
	public void testChannelResolutionFailureThrowsExceptionWhenResolutionRequired() {
		ChannelResolver channelResolver = new ChannelResolver() {
			public MessageChannel resolve(Message<?> message) {
				return null;
			}
		};
		SingleChannelRouter router = new SingleChannelRouter();
		router.setChannelResolver(channelResolver);
		router.setResolutionRequired(true);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("123", "test");
		router.handle(message);
	}

	@Test
	public void testChannelNameResolutionFailureIgnoredByDefault() {
		ChannelNameResolver channelNameResolver = new ChannelNameResolver() {
			public String resolve(Message<?> message) {
				return "noSuchChannel";
			}
		};
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelRouter router = new SingleChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		router.setChannelRegistry(channelRegistry);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("123", "test");
		router.handle(message);
	}

	@Test(expected=MessageHandlingException.class)
	public void testChannelNameResolutionFailureThrowsExceptionWhenResolutionRequired() {
		ChannelNameResolver channelNameResolver = new ChannelNameResolver() {
			public String resolve(Message<?> message) {
				return "noSuchChannel";
			}
		};
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelRouter router = new SingleChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		router.setChannelRegistry(channelRegistry);
		router.setResolutionRequired(true);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("123", "test");
		router.handle(message);
	}

	@Test(expected=MessagingConfigurationException.class)
	public void testChannelRegistryIsRequiredWhenUsingChannelNameResolver() {
		ChannelNameResolver channelNameResolver = new ChannelNameResolver() {
			public String resolve(Message<?> message) {
				return "notImportant";
			}
		};
		SingleChannelRouter router = new SingleChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		router.resolveChannels(new StringMessage("this should fail"));
	}

	@Test(expected=MessagingConfigurationException.class)
	public void testValidateChannelRegistryIsPresentWhenUsingChannelNameResolver() {
		ChannelNameResolver channelNameResolver = new ChannelNameResolver() {
			public String resolve(Message<?> message) {
				return "notImportant";
			}
		};
		SingleChannelRouter router = new SingleChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		router.afterPropertiesSet();
	}

	@Test(expected=MessagingConfigurationException.class)
	public void testChannelResolverIsRequired() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelRouter router = new SingleChannelRouter();
		router.setChannelRegistry(channelRegistry);
		router.afterPropertiesSet();
	}

}
