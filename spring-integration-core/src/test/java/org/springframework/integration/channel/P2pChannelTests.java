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
package org.springframework.integration.channel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.integration.core.MessageHandler;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * @author Oleg Zhurakousky
 *
 */
public class P2pChannelTests {
	
	@Test
	public void testDirectChannelLoggingWithMoreThenOneSubscriber() {
		final DirectChannel channel = new DirectChannel();
		channel.setBeanName("directChannel");
		
	
		final Log logger = mock(Log.class);
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
		verify(logger, times(0)).info(Mockito.anyString());
		channel.subscribe(mock(MessageHandler.class));
		verify(logger, times(1)).info(Mockito.anyString());
	}
	
	@Test
	public void testExecutorChannelLoggingWithMoreThenOneSubscriber() {
		final ExecutorChannel channel = new ExecutorChannel(mock(Executor.class));
		channel.setBeanName("executorChannel");
		
		final Log logger = mock(Log.class);
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
		verify(logger, times(0)).info(Mockito.anyString());
		channel.subscribe(mock(MessageHandler.class));
		verify(logger, times(1)).info(Mockito.anyString());
	}
	
	@Test
	public void testPubSubChannelLoggingWithMoreThenOneSubscriber() {
		final PublishSubscribeChannel channel = new PublishSubscribeChannel();
		channel.setBeanName("pubSubChannel");
		
		final Log logger = mock(Log.class);
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
		verify(logger, times(0)).info(Mockito.anyString());
		channel.subscribe(mock(MessageHandler.class));
		verify(logger, times(0)).info(Mockito.anyString());
	}
}
