/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
