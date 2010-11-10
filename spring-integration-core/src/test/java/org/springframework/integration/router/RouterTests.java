/*
 * Copyright 2002-2010 the original author or authors.
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
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.TestChannelResolver;
import org.springframework.integration.message.GenericMessage;
import org.springframework.util.CollectionUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class RouterTests {

	@Test
	public void nullChannelIgnoredByDefault() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			@Override
			protected List<Object> getChannelIdentifiers(Message<?> message) {
				return null;
			}		
		};
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void nullChannelThrowsExceptionWhenResolutionRequired() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			@Override
			protected List<Object> getChannelIdentifiers(Message<?> message) {
				return null;
			}
		};
		router.setResolutionRequired(true);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}

	@Test
	public void emptyChannelListIgnoredByDefault() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			@Override
			protected List<Object> getChannelIdentifiers(Message<?> message) {
				return null;
			}
		};
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void emptyChannelListThrowsExceptionWhenResolutionRequired() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			@Override
			protected List<Object> getChannelIdentifiers(Message<?> message) {
				return null;
			}
		};
		router.setResolutionRequired(true);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}

	@Test
	public void nullChannelNameArrayIgnoredByDefault() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			@Override
			protected List<Object> getChannelIdentifiers(Message<?> message) {
				return null;
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void nullChannelNameArrayThrowsExceptionWhenResolutionRequired() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			protected List<Object> getChannelIdentifiers(Message<?> message)  {
				return null;
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		router.setResolutionRequired(true);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}


	@Test
	public void emptyChannelNameArrayIgnoredByDefault() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			protected List<Object> getChannelIdentifiers(Message<?> message) {
				return new ArrayList<Object>();
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void emptyChannelNameArrayThrowsExceptionWhenResolutionRequired() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelIdentifiers(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] {});
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		router.setResolutionRequired(true);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}

	@Test(expected = MessagingException.class)
	public void channelMappingIsRequiredWhenResolvingChannelNamesWithSingleChannelRouter() {
		AbstractSingleChannelNameRouter router = new AbstractSingleChannelNameRouter() {
			public String determineTargetChannelName(Message<?> message) {
				return "notImportant";
			}
		};
		router.setBeanFactory(mock(BeanFactory.class));
		router.handleMessage(new GenericMessage<String>("this should fail"));
	}

	@Test(expected = MessagingException.class)
	public void channelMappingIsRequiredWhenResolvingChannelNamesWithMultiChannelRouter() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelIdentifiers(Message<?> message){
				return CollectionUtils.arrayToList(new String[] { "notImportant" });
			}
		};
		router.setBeanFactory(mock(BeanFactory.class));
		router.handleMessage(new GenericMessage<String>("this should fail"));
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
		router.handleMessage(new GenericMessage<String>("test"));
		Message<?> reply = testChannel.receive(0);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void beanFactoryWithMultiChannelRouter() {
		AbstractMessageRouter router = new AbstractMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelIdentifiers(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] { "testChannel" });
			}
		};
		QueueChannel testChannel = new QueueChannel();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("testChannel", testChannel);
		router.setBeanFactory(context);
		router.handleMessage(new GenericMessage<String>("test"));
		Message<?> reply = testChannel.receive(0);
		assertEquals("test", reply.getPayload());
	}

}
