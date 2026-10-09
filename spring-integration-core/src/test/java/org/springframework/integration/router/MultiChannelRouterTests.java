/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.TestChannelResolver;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MultiChannelRouterTests implements TestApplicationContextAware {

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
		router.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		router.afterPropertiesSet();

		Message<String> message = new GenericMessage<>("test");
		router.handleMessage(message);
		Message<?> result1 = channel1.receive(25);
		assertThat(result1).isNotNull();
		assertThat(result1.getPayload()).isEqualTo("test");
		Message<?> result2 = channel2.receive(25);
		assertThat(result2).isNotNull();
		assertThat(result2.getPayload()).isEqualTo("test");
	}

	@Test
	public void channelNameLookupFailure() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {

			public List<Object> getChannelKeys(Message<?> message) {
				return Collections.singletonList("noSuchChannel");
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		router.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		router.afterPropertiesSet();

		Message<String> message = new GenericMessage<>("test");
		assertThatThrownBy(() -> router.handleMessage(message)).isInstanceOf(MessagingException.class);
	}

	@Test
	public void channelMappingNotAvailable() {
		AbstractMappingMessageRouter router =
				new AbstractMappingMessageRouter() {

					public List<Object> getChannelKeys(Message<?> message) {
						return Collections.singletonList("noSuchChannel");
					}
				};
		router.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		router.afterPropertiesSet();

		Message<String> message = new GenericMessage<>("test");
		assertThatThrownBy(() -> router.handleMessage(message))
				.isInstanceOf(MessagingException.class);
	}

}
