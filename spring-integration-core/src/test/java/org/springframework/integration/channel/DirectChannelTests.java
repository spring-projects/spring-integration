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

package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class DirectChannelTests {

	@Test
	public void testSend() {
		DirectChannel channel = new DirectChannel();
		ThreadNameExtractingTestTarget target = new ThreadNameExtractingTestTarget();
		channel.subscribe(target);
		GenericMessage<String> message = new GenericMessage<String>("test");
		assertTrue(channel.send(message));
		assertEquals(Thread.currentThread().getName(), target.threadName);
		DirectFieldAccessor channelAccessor = new DirectFieldAccessor(channel);
		UnicastingDispatcher dispatcher = (UnicastingDispatcher) channelAccessor.getPropertyValue("dispatcher");
		DirectFieldAccessor dispatcherAccessor = new DirectFieldAccessor(dispatcher);
		Object loadBalancingStrategy = dispatcherAccessor.getPropertyValue("loadBalancingStrategy");
		assertTrue(loadBalancingStrategy instanceof RoundRobinLoadBalancingStrategy);
	}
	
	@Test
	public void testWithMoreThenOneSubscriber() {
		final DirectChannel channel = new DirectChannel();
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
	public void testSendInSeparateThread() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		final DirectChannel channel = new DirectChannel();
		ThreadNameExtractingTestTarget target = new ThreadNameExtractingTestTarget(latch);
		channel.subscribe(target);
		final GenericMessage<String> message = new GenericMessage<String>("test");
		new Thread(new Runnable() {
			public void run() {
				channel.send(message);
			}
		}, "test-thread").start();
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals("test-thread", target.threadName);
	}


	private static class ThreadNameExtractingTestTarget implements MessageHandler {

		private String threadName;

		private final CountDownLatch latch;


		ThreadNameExtractingTestTarget() {
			this(null);
		}

		ThreadNameExtractingTestTarget(CountDownLatch latch) {
			this.latch = latch;
		}

		public void handleMessage(Message<?> message) {
			this.threadName = Thread.currentThread().getName();
			if (this.latch != null) {
				this.latch.countDown();
			}
		}
	}

}
