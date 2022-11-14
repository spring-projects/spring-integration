/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.router;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.TestChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MultiChannelRouterTests {

	@Test
	public void routeWithChannelMapping() {
		AbstractMappingMessageRouter router =
				new AbstractMappingMessageRouter() {

					public List<Object> getChannelKeys(Message<?> message) {
						return Arrays.asList("channel1", "channel2");
					}
				};
		QueueChannel channel1 = new QueueChannel();
		QueueChannel channel2 = new QueueChannel();
		TestChannelResolver channelResolver = new TestChannelResolver();
		channelResolver.addChannel("channel1", channel1);
		channelResolver.addChannel("channel2", channel2);
		router.setChannelResolver(channelResolver);
		Message<String> message = new GenericMessage<>("test");
		router.handleMessage(message);
		Message<?> result1 = channel1.receive(25);
		assertThat(result1).isNotNull();
		assertThat(result1.getPayload()).isEqualTo("test");
		Message<?> result2 = channel2.receive(25);
		assertThat(result2).isNotNull();
		assertThat(result2.getPayload()).isEqualTo("test");
	}

	@Test(expected = MessagingException.class)
	public void channelNameLookupFailure() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {

			public List<Object> getChannelKeys(Message<?> message) {
				return Collections.singletonList("noSuchChannel");
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		Message<String> message = new GenericMessage<>("test");
		router.handleMessage(message);
	}

	@Test(expected = MessagingException.class)
	public void channelMappingNotAvailable() {
		AbstractMappingMessageRouter router =
				new AbstractMappingMessageRouter() {

					public List<Object> getChannelKeys(Message<?> message) {
						return Collections.singletonList("noSuchChannel");
					}
				};
		router.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = new GenericMessage<>("test");
		router.handleMessage(message);
	}

}
