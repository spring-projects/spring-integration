/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.endpoint;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.config.TestErrorHandler;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 *
 */
public class PollingLifecycleTests {
	private ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
	private TestErrorHandler errorHandler = new TestErrorHandler();

	@Before
	public void init() throws Exception {
		taskScheduler.afterPropertiesSet();
	}

	@Test
	public void ensurePollerTaskStops() throws Exception{
		final CountDownLatch latch = new CountDownLatch(1);
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<String>("foo"));

		MessageHandler handler = Mockito.spy(new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				latch.countDown();
			}
		});
		PollingConsumer consumer = new PollingConsumer(channel, handler);
		consumer.setTrigger(new PeriodicTrigger(0));
		consumer.setErrorHandler(errorHandler);
		consumer.setTaskScheduler(taskScheduler);
		consumer.setBeanFactory(mock(BeanFactory.class));
		consumer.afterPropertiesSet();
		consumer.start();
		assertTrue(latch.await(2, TimeUnit.SECONDS));
		Mockito.verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
		consumer.stop();
		for (int i = 0; i < 10; i++) {
			channel.send(new GenericMessage<String>("foo"));
		}
		Thread.sleep(2000); // give enough time for poller to kick in if it didn't stop properly
		// we'll still have a natural race condition between call to stop() and poller polling
		// so what we really have to assert is that it doesn't poll for more then once after stop() was called
		Mockito.reset(handler);
		Mockito.verify(handler, atMost(1)).handleMessage(Mockito.any(Message.class));
	}

	@Test
	public void ensurePollerTaskStopsForAdapter() throws Exception{
		final CountDownLatch latch = new CountDownLatch(1);
		QueueChannel channel = new QueueChannel();

		SourcePollingChannelAdapterFactoryBean adapterFactory = new SourcePollingChannelAdapterFactoryBean();
		PollerMetadata pollerMetadata = new PollerMetadata();
		pollerMetadata.setTrigger(new PeriodicTrigger(2000));
		adapterFactory.setPollerMetadata(pollerMetadata);
		MessageSource<String> source = spy(new MessageSource<String>() {
			public Message<String> receive() {
				latch.countDown();
				return new GenericMessage<String>("hello");
			}
		});
		adapterFactory.setSource(source);
		adapterFactory.setOutputChannel(channel);
		adapterFactory.setBeanFactory(mock(ConfigurableBeanFactory.class));
		SourcePollingChannelAdapter adapter = adapterFactory.getObject();
		adapter.setTaskScheduler(taskScheduler);
		adapter.afterPropertiesSet();
		adapter.start();
		assertTrue(latch.await(20, TimeUnit.SECONDS));
		assertNotNull(channel.receive(100));
		adapter.stop();
		assertNull(channel.receive(1000));
		Mockito.verify(source, times(1)).receive();
	}

	@Test
	public void ensurePollerTaskStopsForAdapterWithInterruptible() throws Exception{
		final CountDownLatch latch = new CountDownLatch(2);
		QueueChannel channel = new QueueChannel();

		SourcePollingChannelAdapterFactoryBean adapterFactory = new SourcePollingChannelAdapterFactoryBean();
		PollerMetadata pollerMetadata = new PollerMetadata();
		pollerMetadata.setMaxMessagesPerPoll(-1);
		pollerMetadata.setTrigger(new PeriodicTrigger(2000));
		adapterFactory.setPollerMetadata(pollerMetadata);
		final Runnable coughtInterrupted = mock(Runnable.class);
		MessageSource<String> source = new MessageSource<String>() {
			public Message<String> receive() {

				try {
					for (int i = 0; i < 10; i++) {
						Thread.sleep(1000);
						latch.countDown();
					}
				} catch (InterruptedException e) {
					coughtInterrupted.run();
				}

				return new GenericMessage<String>("hello");
			}
		};
		adapterFactory.setSource(source);
		adapterFactory.setOutputChannel(channel);
		adapterFactory.setBeanFactory(mock(ConfigurableBeanFactory.class));
		SourcePollingChannelAdapter adapter = adapterFactory.getObject();
		adapter.setTaskScheduler(taskScheduler);
		adapter.afterPropertiesSet();
		adapter.start();
		assertTrue(latch.await(3000, TimeUnit.SECONDS));
		//
		adapter.stop();
		Thread.sleep(1000);
		Mockito.verify(coughtInterrupted, times(1)).run();
	}
}
