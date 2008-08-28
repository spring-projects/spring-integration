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

import static org.junit.Assert.assertFalse;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class RouterEndpointTests {

	@Test
	public void nullChannelIgnoredByDefault() {
		MultiChannelResolver channelResolver = new MultiChannelResolver() {
			public List<MessageChannel> resolve(Message<?> message) {
				return null;
			}
		};
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelResolver(channelResolver);
		router.afterPropertiesSet();
		RouterEndpoint endpoint = new RouterEndpoint(router);
		Message<String> message = new StringMessage("test");
		assertFalse(endpoint.send(message));
	}

	@Test(expected = MessageDeliveryException.class)
	public void nullChannelThrowsExceptionWhenResolutionRequired() {
		MultiChannelResolver channelResolver = new MultiChannelResolver() {
			public List<MessageChannel> resolve(Message<?> message) {
				return null;
			}
		};
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelResolver(channelResolver);
		router.afterPropertiesSet();
		RouterEndpoint endpoint = new RouterEndpoint(router);
		endpoint.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		endpoint.send(message);
	}

	@Test
	public void emptyChannelListIgnoredByDefault() {
		MultiChannelResolver channelResolver = new MultiChannelResolver() {
			public List<MessageChannel> resolve(Message<?> message) {
				return Collections.emptyList();
			}
		};
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelResolver(channelResolver);
		router.afterPropertiesSet();
		RouterEndpoint endpoint = new RouterEndpoint(router);
		Message<String> message = new StringMessage("test");
		assertFalse(endpoint.send(message));
	}

	@Test(expected = MessageDeliveryException.class)
	public void emptyChannelListThrowsExceptionWhenResolutionRequired() {
		MultiChannelResolver channelResolver = new MultiChannelResolver() {
			public List<MessageChannel> resolve(Message<?> message) {
				return Collections.emptyList();
			}
		};
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelResolver(channelResolver);
		router.afterPropertiesSet();
		RouterEndpoint endpoint = new RouterEndpoint(router);
		endpoint.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		endpoint.send(message);
	}

	@Test
	public void nullChannelNameArrayIgnoredByDefault() {
		MultiChannelNameResolver channelNameResolver = new MultiChannelNameResolver() {
			public String[] resolve(Message<?> message) {
				return null;
			}
		};
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		router.afterPropertiesSet();
		RouterEndpoint endpoint = new RouterEndpoint(router);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> message = new StringMessage("test");
		assertFalse(endpoint.send(message));
	}

	@Test(expected = MessageDeliveryException.class)
	public void nullChannelNameArrayThrowsExceptionWhenResolutionRequired() {
		MultiChannelNameResolver channelNameResolver = new MultiChannelNameResolver() {
			public String[] resolve(Message<?> message) {
				return null;
			}
		};
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		router.afterPropertiesSet();
		RouterEndpoint endpoint = new RouterEndpoint(router);
		endpoint.setChannelRegistry(channelRegistry);
		endpoint.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		endpoint.send(message);
	}


	@Test
	public void emptyChannelNameArrayIgnoredByDefault() {
		MultiChannelNameResolver channelNameResolver = new MultiChannelNameResolver() {
			public String[] resolve(Message<?> message) {
				return new String[] {};
			}
		};
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		router.afterPropertiesSet();
		RouterEndpoint endpoint = new RouterEndpoint(router);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> message = new StringMessage("test");
		assertFalse(endpoint.send(message));
	}

	@Test(expected = MessageDeliveryException.class)
	public void emptyChannelNameArrayThrowsExceptionWhenResolutionRequired() {
		MultiChannelNameResolver channelNameResolver = new MultiChannelNameResolver() {
			public String[] resolve(Message<?> message) {
				return new String[] {};
			}
		};
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		router.afterPropertiesSet();
		RouterEndpoint endpoint = new RouterEndpoint(router);
		endpoint.setChannelRegistry(channelRegistry);
		endpoint.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		endpoint.send(message);
	}

	@Test(expected = MessagingException.class)
	public void testChannelRegistryIsRequiredWhenUsingChannelNameResolverWithSingleChannelRouter() {
		ChannelNameResolver channelNameResolver = new ChannelNameResolver() {
			public String resolve(Message<?> message) {
				return "notImportant";
			}
		};
		SingleChannelRouter router = new SingleChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		RouterEndpoint endpoint = new RouterEndpoint(router);
		endpoint.send(new StringMessage("this should fail"));
	}

	@Test(expected = MessagingException.class)
	public void testChannelRegistryIsRequiredWhenUsingChannelNameResolverWithMultiChannelRouter() {
		MultiChannelNameResolver channelNameResolver = new MultiChannelNameResolver() {
			public String[] resolve(Message<?> message) {
				return new String[] { "notImportant" };
			}
		};
		MultiChannelRouter router = new MultiChannelRouter();
		router.setChannelNameResolver(channelNameResolver);
		RouterEndpoint endpoint = new RouterEndpoint(router);
		endpoint.send(new StringMessage("this should fail"));
	}

}
