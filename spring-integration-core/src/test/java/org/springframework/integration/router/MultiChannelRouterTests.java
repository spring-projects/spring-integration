/*
 * Copyright 2002-2011 the original author or authors.
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
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.TestChannelResolver;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.CollectionUtils;

/**
 * @author Mark Fisher
 */
public class MultiChannelRouterTests {

	@Test
	public void routeWithChannelMapping() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			public List<Object> getChannelKeys(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] {"channel1", "channel2"});
			}
		};
		QueueChannel channel1 = new QueueChannel();
		QueueChannel channel2 = new QueueChannel();
		TestChannelResolver channelResolver = new TestChannelResolver();
		channelResolver.addChannel("channel1", channel1);
		channelResolver.addChannel("channel2", channel2);
		router.setChannelResolver(channelResolver);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
		Message<?> result1 = channel1.receive(25);
		assertNotNull(result1);
		assertEquals("test", result1.getPayload());
		Message<?> result2 = channel2.receive(25);
		assertNotNull(result2);
		assertEquals("test", result2.getPayload());
	}

	@Test(expected = MessagingException.class)
	public void channelNameLookupFailure() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			public List<Object> getChannelKeys(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] {"noSuchChannel"} );
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}

	@Test(expected = MessagingException.class)
	public void channelMappingNotAvailable() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			public List<Object> getChannelKeys(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] {"noSuchChannel"});
			}
		};
		router.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}

}
