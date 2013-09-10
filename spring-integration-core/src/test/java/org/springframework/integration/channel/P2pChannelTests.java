/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 *
 */
public class P2pChannelTests {
	
	@Test
	public void testDirectChannelLoggingWithMoreThenOneSubscriber() {
		final DirectChannel channel = new DirectChannel();
		channel.setBeanName("directChannel");

		verifySubscriptions(channel);
	}

	@Test
	public void testCustomChannelLoggingWithMoreThenOneSubscriberNotAbstractDispatcher() {
		final MessageDispatcher mockDispatcher = mock(MessageDispatcher.class);
		when(mockDispatcher.addHandler(Mockito.any(MessageHandler.class))).thenReturn(true);
		when(mockDispatcher.removeHandler(Mockito.any(MessageHandler.class))).thenReturn(true).thenReturn(false).thenReturn(true);

		final AbstractSubscribableChannel channel = new AbstractSubscribableChannel() {
			@Override
			protected MessageDispatcher getDispatcher() {
				return mockDispatcher;
			}
		};
		channel.setBeanName("customChannel");

		verifySubscriptions(channel);
	}

	/**
	 * @param channel
	 */
	private void verifySubscriptions(final AbstractSubscribableChannel channel) {
		final Log logger = mock(Log.class);
		when(logger.isInfoEnabled()).thenReturn(true);
		final List<String> logs = new ArrayList<String>();
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				logs.add((String) invocation.getArguments()[0]);
				return null;
			}}).when(logger).info(Mockito.anyString());
		ReflectionUtils.doWithFields(AbstractMessageChannel.class, new FieldCallback() {
			public void doWith(Field field) throws IllegalArgumentException,
					IllegalAccessException {
				if ("logger".equals(field.getName())){
					field.setAccessible(true);
					field.set(channel, logger);
				}
			}
		});
		String log = "Channel '"
				+ channel.getComponentName()
				+ "' has "
				+ "%d subscriber(s).";

		MessageHandler handler1 = mock(MessageHandler.class);
		channel.subscribe(handler1);
		assertEquals(String.format(log, 1), logs.remove(0));
		MessageHandler handler2 = mock(MessageHandler.class);
		channel.subscribe(handler2);
		assertEquals(String.format(log, 2), logs.remove(0));
		channel.unsubscribe(handler1);
		assertEquals(String.format(log, 1), logs.remove(0));
		channel.unsubscribe(handler1);
		assertEquals(0, logs.size());
		channel.unsubscribe(handler2);
		assertEquals(String.format(log, 0), logs.remove(0));
		verify(logger, times(4)).info(Mockito.anyString());
	}
	
	@Test
	public void testExecutorChannelLoggingWithMoreThenOneSubscriber() {
		final ExecutorChannel channel = new ExecutorChannel(mock(Executor.class));
		channel.setBeanName("executorChannel");

		final Log logger = mock(Log.class);
		when(logger.isInfoEnabled()).thenReturn(true);
		ReflectionUtils.doWithFields(AbstractMessageChannel.class, new FieldCallback() {

			public void doWith(Field field) throws IllegalArgumentException,
					IllegalAccessException {
				if ("logger".equals(field.getName())){
					field.setAccessible(true);
					field.set(channel, logger);
				}
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

		final Log logger = mock(Log.class);
		when(logger.isInfoEnabled()).thenReturn(true);
		ReflectionUtils.doWithFields(AbstractMessageChannel.class, new FieldCallback() {
			
			public void doWith(Field field) throws IllegalArgumentException,
					IllegalAccessException {
				if ("logger".equals(field.getName())){
					field.setAccessible(true);
					field.set(channel, logger);
				}
			}
		});
		channel.subscribe(mock(MessageHandler.class));
		channel.subscribe(mock(MessageHandler.class));
		verify(logger, times(2)).info(Mockito.anyString());
	}
}
