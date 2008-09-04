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
		ChannelResolver channelResolver = new ChannelResolver() {
			public List<MessageChannel> resolveChannels(Message<?> message) {
				return null;
			}
		};
		RouterEndpoint endpoint = new RouterEndpoint(channelResolver);
		Message<String> message = new StringMessage("test");
		assertFalse(endpoint.send(message));
	}

	@Test(expected = MessageDeliveryException.class)
	public void nullChannelThrowsExceptionWhenResolutionRequired() {
		ChannelResolver channelResolver = new ChannelResolver() {
			public List<MessageChannel> resolveChannels(Message<?> message) {
				return null;
			}
		};
		RouterEndpoint endpoint = new RouterEndpoint(channelResolver);
		endpoint.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		endpoint.send(message);
	}

	@Test
	public void emptyChannelListIgnoredByDefault() {
		ChannelResolver channelResolver = new ChannelResolver() {
			public List<MessageChannel> resolveChannels(Message<?> message) {
				return Collections.emptyList();
			}
		};
		RouterEndpoint endpoint = new RouterEndpoint(channelResolver);
		Message<String> message = new StringMessage("test");
		assertFalse(endpoint.send(message));
	}

	@Test(expected = MessageDeliveryException.class)
	public void emptyChannelListThrowsExceptionWhenResolutionRequired() {
		ChannelResolver channelResolver = new ChannelResolver() {
			public List<MessageChannel> resolveChannels(Message<?> message) {
				return Collections.emptyList();
			}
		};
		RouterEndpoint endpoint = new RouterEndpoint(channelResolver);
		endpoint.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		endpoint.send(message);
	}

	@Test
	public void nullChannelNameArrayIgnoredByDefault() {
		AbstractMultiChannelNameResolver channelNameResolver = new AbstractMultiChannelNameResolver() {
			public String[] resolveChannelNames(Message<?> message) {
				return null;
			}
		};
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		RouterEndpoint endpoint = new RouterEndpoint(channelNameResolver);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> message = new StringMessage("test");
		assertFalse(endpoint.send(message));
	}

	@Test(expected = MessageDeliveryException.class)
	public void nullChannelNameArrayThrowsExceptionWhenResolutionRequired() {
		AbstractMultiChannelNameResolver channelNameResolver = new AbstractMultiChannelNameResolver() {
			public String[] resolveChannelNames(Message<?> message) {
				return null;
			}
		};
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		RouterEndpoint endpoint = new RouterEndpoint(channelNameResolver);
		endpoint.setChannelRegistry(channelRegistry);
		endpoint.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		endpoint.send(message);
	}


	@Test
	public void emptyChannelNameArrayIgnoredByDefault() {
		AbstractMultiChannelNameResolver channelNameResolver = new AbstractMultiChannelNameResolver() {
			public String[] resolveChannelNames(Message<?> message) {
				return new String[] {};
			}
		};
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		RouterEndpoint endpoint = new RouterEndpoint(channelNameResolver);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> message = new StringMessage("test");
		assertFalse(endpoint.send(message));
	}

	@Test(expected = MessageDeliveryException.class)
	public void emptyChannelNameArrayThrowsExceptionWhenResolutionRequired() {
		AbstractMultiChannelNameResolver channelNameResolver = new AbstractMultiChannelNameResolver() {
			public String[] resolveChannelNames(Message<?> message) {
				return new String[] {};
			}
		};
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		RouterEndpoint endpoint = new RouterEndpoint(channelNameResolver);
		endpoint.setChannelRegistry(channelRegistry);
		endpoint.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		endpoint.send(message);
	}

	@Test(expected = MessagingException.class)
	public void testChannelRegistryIsRequiredWhenUsingChannelNameResolverWithSingleChannelRouter() {
		AbstractSingleChannelNameResolver channelNameResolver = new AbstractSingleChannelNameResolver() {
			public String resolveChannelName(Message<?> message) {
				return "notImportant";
			}
		};
		RouterEndpoint endpoint = new RouterEndpoint(channelNameResolver);
		endpoint.send(new StringMessage("this should fail"));
	}

	@Test(expected = MessagingException.class)
	public void testChannelRegistryIsRequiredWhenUsingChannelNameResolverWithMultiChannelRouter() {
		AbstractMultiChannelNameResolver channelNameResolver = new AbstractMultiChannelNameResolver() {
			public String[] resolveChannelNames(Message<?> message) {
				return new String[] { "notImportant" };
			}
		};
		RouterEndpoint endpoint = new RouterEndpoint(channelNameResolver);
		endpoint.send(new StringMessage("this should fail"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testChannelResolverMustNotBeNull() {
		AbstractSingleChannelNameResolver channelNameResolver = null;
		new RouterEndpoint(channelNameResolver);
	}

}
