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

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.TestChannelMapping;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class RouterTests {

	@Test
	public void nullChannelIgnoredByDefault() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			public List<MessageChannel> resolveChannels(Message<?> message) {
				return null;
			}
		};
		Message<String> message = new StringMessage("test");
		router.onMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void nullChannelThrowsExceptionWhenResolutionRequired() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			public List<MessageChannel> resolveChannels(Message<?> message) {
				return null;
			}
		};
		router.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		router.onMessage(message);
	}

	@Test
	public void emptyChannelListIgnoredByDefault() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			public List<MessageChannel> resolveChannels(Message<?> message) {
				return Collections.emptyList();
			}
		};
		Message<String> message = new StringMessage("test");
		router.onMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void emptyChannelListThrowsExceptionWhenResolutionRequired() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			public List<MessageChannel> resolveChannels(Message<?> message) {
				return Collections.emptyList();
			}
		};
		router.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		router.onMessage(message);
	}

	@Test
	public void nullChannelNameArrayIgnoredByDefault() {
		AbstractChannelMappingMessageRouter router = new AbstractChannelMappingMessageRouter() {
			public String[] resolveChannelNames(Message<?> message) {
				return null;
			}
		};
		TestChannelMapping channelMapping = new TestChannelMapping();
		router.setChannelMapping(channelMapping);
		Message<String> message = new StringMessage("test");
		router.onMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void nullChannelNameArrayThrowsExceptionWhenResolutionRequired() {
		AbstractChannelMappingMessageRouter router = new AbstractChannelMappingMessageRouter() {
			public String[] resolveChannelNames(Message<?> message) {
				return null;
			}
		};
		TestChannelMapping channelMapping = new TestChannelMapping();
		router.setChannelMapping(channelMapping);
		router.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		router.onMessage(message);
	}


	@Test
	public void emptyChannelNameArrayIgnoredByDefault() {
		AbstractChannelMappingMessageRouter router = new AbstractChannelMappingMessageRouter() {
			public String[] resolveChannelNames(Message<?> message) {
				return new String[] {};
			}
		};
		TestChannelMapping channelMapping = new TestChannelMapping();
		router.setChannelMapping(channelMapping);
		Message<String> message = new StringMessage("test");
		router.onMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void emptyChannelNameArrayThrowsExceptionWhenResolutionRequired() {
		AbstractChannelMappingMessageRouter router = new AbstractChannelMappingMessageRouter() {
			public String[] resolveChannelNames(Message<?> message) {
				return new String[] {};
			}
		};
		TestChannelMapping channelMapping = new TestChannelMapping();
		router.setChannelMapping(channelMapping);
		router.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		router.onMessage(message);
	}

	@Test(expected = MessagingException.class)
	public void channelMappingIsRequiredWhenResolvingChannelNamesWithSingleChannelRouter() {
		AbstractSingleChannelNameRouter router = new AbstractSingleChannelNameRouter() {
			public String resolveChannelName(Message<?> message) {
				return "notImportant";
			}
		};
		router.onMessage(new StringMessage("this should fail"));
	}

	@Test(expected = MessagingException.class)
	public void channelMappingIsRequiredWhenResolvingChannelNamesWithMultiChannelRouter() {
		AbstractChannelMappingMessageRouter router = new AbstractChannelMappingMessageRouter() {
			public String[] resolveChannelNames(Message<?> message) {
				return new String[] { "notImportant" };
			}
		};
		router.onMessage(new StringMessage("this should fail"));
	}

	@Test
	public void beanFactoryWithSingleChannelRouter() {
		AbstractSingleChannelNameRouter router = new AbstractSingleChannelNameRouter() {
			public String resolveChannelName(Message<?> message) {
				return "testChannel";
			}
		};
		QueueChannel testChannel = new QueueChannel();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("testChannel", testChannel);
		router.setBeanFactory(context);
		router.onMessage(new StringMessage("test"));
		Message<?> reply = testChannel.receive(0);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void beanFactoryWithMultiChannelRouter() {
		AbstractChannelMappingMessageRouter router = new AbstractChannelMappingMessageRouter() {
			public String[] resolveChannelNames(Message<?> message) {
				return new String[] { "testChannel" };
			}
		};
		QueueChannel testChannel = new QueueChannel();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("testChannel", testChannel);
		router.setBeanFactory(context);
		router.onMessage(new StringMessage("test"));
		Message<?> reply = testChannel.receive(0);
		assertEquals("test", reply.getPayload());
	}

}
