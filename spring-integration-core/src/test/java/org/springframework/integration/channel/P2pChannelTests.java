/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.core.log.LogAccessor;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.ReflectionUtils;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
public class P2pChannelTests {

	@Test
	public void testDirectChannelLoggingWithMoreThenOneSubscriber() {
		final DirectChannel channel = new DirectChannel();
		channel.setBeanName("directChannel");

		verifySubscriptions(channel);
	}

	/**
	 * @param channel
	 */
	private void verifySubscriptions(final AbstractSubscribableChannel channel) {
		final LogAccessor logger = mock(LogAccessor.class);
		when(logger.isInfoEnabled()).thenReturn(true);
		final List<String> logs = new ArrayList<>();
		doAnswer(invocation -> {
			logs.add(invocation.getArgument(0));
			return null;
		}).when(logger).info(Mockito.anyString());
		ReflectionUtils.doWithFields(AbstractMessageChannel.class, field -> {
			if ("logger".equals(field.getName())) {
				field.setAccessible(true);
				field.set(channel, logger);
			}
		});
		String log = "Channel '"
				+ channel.getComponentName()
				+ "' has "
				+ "%d subscriber(s).";

		MessageHandler handler1 = mock(MessageHandler.class);
		channel.subscribe(handler1);
		assertThat(channel.getSubscriberCount()).isEqualTo(1);
		assertThat(logs.remove(0)).isEqualTo(String.format(log, 1));
		MessageHandler handler2 = mock(MessageHandler.class);
		channel.subscribe(handler2);
		assertThat(channel.getSubscriberCount()).isEqualTo(2);
		assertThat(logs.remove(0)).isEqualTo(String.format(log, 2));
		channel.unsubscribe(handler1);
		assertThat(channel.getSubscriberCount()).isEqualTo(1);
		assertThat(logs.remove(0)).isEqualTo(String.format(log, 1));
		channel.unsubscribe(handler1);
		assertThat(channel.getSubscriberCount()).isEqualTo(1);
		assertThat(logs.size()).isEqualTo(0);
		channel.unsubscribe(handler2);
		assertThat(channel.getSubscriberCount()).isEqualTo(0);
		assertThat(logs.remove(0)).isEqualTo(String.format(log, 0));
		verify(logger, times(4)).info(Mockito.anyString());
	}

	@Test
	public void testExecutorChannelLoggingWithMoreThenOneSubscriber() {
		final ExecutorChannel channel = new ExecutorChannel(mock(Executor.class));
		channel.setBeanName("executorChannel");

		final LogAccessor logger = mock(LogAccessor.class);
		when(logger.isInfoEnabled()).thenReturn(true);
		ReflectionUtils.doWithFields(AbstractMessageChannel.class, field -> {
			if ("logger".equals(field.getName())) {
				field.setAccessible(true);
				field.set(channel, logger);
			}
		});
		channel.subscribe(mock(MessageHandler.class));
		channel.subscribe(mock(MessageHandler.class));
		verify(logger, times(2)).info(Mockito.anyString());
	}

	@Test
	public void testPubSubChannelLoggingWithMoreThenOneSubscriber() {
		final PublishSubscribeChannel channel = new PublishSubscribeChannel();
		channel.setBeanName("pubSubChannel");

		final LogAccessor logger = mock(LogAccessor.class);
		when(logger.isInfoEnabled()).thenReturn(true);
		ReflectionUtils.doWithFields(AbstractMessageChannel.class, field -> {
			if ("logger".equals(field.getName())) {
				field.setAccessible(true);
				field.set(channel, logger);
			}
		});
		channel.subscribe(mock(MessageHandler.class));
		channel.subscribe(mock(MessageHandler.class));
		verify(logger, times(2)).info(Mockito.anyString());
	}
}
