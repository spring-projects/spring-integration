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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.TestChannelResolver;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.StringMessage;
import org.springframework.util.CollectionUtils;

/**
 * @author Mark Fisher
 */
public class RouterTests {

	@Test
	public void nullChannelIgnoredByDefault() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			public List<MessageChannel> determineTargetChannels(Message<?> message) {
				return null;
			}
		};
		Message<String> message = new StringMessage("test");
		router.handleMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void nullChannelThrowsExceptionWhenResolutionRequired() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			public List<MessageChannel> determineTargetChannels(Message<?> message) {
				return null;
			}
		};
		router.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		router.handleMessage(message);
	}

	@Test
	public void emptyChannelListIgnoredByDefault() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			public List<MessageChannel> determineTargetChannels(Message<?> message) {
				return Collections.emptyList();
			}
		};
		Message<String> message = new StringMessage("test");
		router.handleMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void emptyChannelListThrowsExceptionWhenResolutionRequired() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			public List<MessageChannel> determineTargetChannels(Message<?> message) {
				return Collections.emptyList();
			}
		};
		router.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		router.handleMessage(message);
	}

	@Test
	public void nullChannelNameArrayIgnoredByDefault() {
		AbstractChannelNameResolvingMessageRouter router = new AbstractChannelNameResolvingMessageRouter() {
			protected List<Object> getChannelIndicatorList(Message<?> message)  {
				return null;
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		Message<String> message = new StringMessage("test");
		router.handleMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void nullChannelNameArrayThrowsExceptionWhenResolutionRequired() {
		AbstractChannelNameResolvingMessageRouter router = new AbstractChannelNameResolvingMessageRouter() {
			protected List<Object> getChannelIndicatorList(Message<?> message)  {
				return null;
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		router.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		router.handleMessage(message);
	}


	@Test
	public void emptyChannelNameArrayIgnoredByDefault() {
		AbstractChannelNameResolvingMessageRouter router = new AbstractChannelNameResolvingMessageRouter() {
			protected List<Object> getChannelIndicatorList(Message<?> message) {
				return new ArrayList<Object>();
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		Message<String> message = new StringMessage("test");
		router.handleMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void emptyChannelNameArrayThrowsExceptionWhenResolutionRequired() {
		AbstractChannelNameResolvingMessageRouter router = new AbstractChannelNameResolvingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelIndicatorList(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] {});
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		router.setResolutionRequired(true);
		Message<String> message = new StringMessage("test");
		router.handleMessage(message);
	}

	@Test(expected = MessagingException.class)
	public void channelMappingIsRequiredWhenResolvingChannelNamesWithSingleChannelRouter() {
		AbstractSingleChannelNameRouter router = new AbstractSingleChannelNameRouter() {
			public String determineTargetChannelName(Message<?> message) {
				return "notImportant";
			}
		};
		router.handleMessage(new StringMessage("this should fail"));
	}

	@Test(expected = MessagingException.class)
	public void channelMappingIsRequiredWhenResolvingChannelNamesWithMultiChannelRouter() {
		AbstractChannelNameResolvingMessageRouter router = new AbstractChannelNameResolvingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelIndicatorList(Message<?> message){
				return CollectionUtils.arrayToList(new String[] { "notImportant" });
			}
		};
		router.handleMessage(new StringMessage("this should fail"));
	}

	@Test
	public void beanFactoryWithSingleChannelRouter() {
		AbstractSingleChannelNameRouter router = new AbstractSingleChannelNameRouter() {
			public String determineTargetChannelName(Message<?> message) {
				return "testChannel";
			}
		};
		QueueChannel testChannel = new QueueChannel();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("testChannel", testChannel);
		router.setBeanFactory(context);
		router.handleMessage(new StringMessage("test"));
		Message<?> reply = testChannel.receive(0);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void beanFactoryWithMultiChannelRouter() {
		AbstractChannelNameResolvingMessageRouter router = new AbstractChannelNameResolvingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelIndicatorList(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] { "testChannel" });
			}
		};
		QueueChannel testChannel = new QueueChannel();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("testChannel", testChannel);
		router.setBeanFactory(context);
		router.handleMessage(new StringMessage("test"));
		Message<?> reply = testChannel.receive(0);
		assertEquals("test", reply.getPayload());
	}

}
