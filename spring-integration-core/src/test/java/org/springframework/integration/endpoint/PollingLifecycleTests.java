/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.config.TestErrorHandler;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 */
public class PollingLifecycleTests {

	private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

	private final TestErrorHandler errorHandler = new TestErrorHandler();

	@BeforeEach
	public void init() {
		this.taskScheduler.afterPropertiesSet();
	}

	@AfterEach
	public void tearDown() {
		this.taskScheduler.destroy();
	}

	@Test
	public void ensurePollerTaskStops() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<>("foo"));

		//Has to be an explicit implementation - Mockito cannot mock/spy lambdas
		MessageHandler handler = Mockito.spy(new MessageHandler() {

			@Override
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
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		Mockito.verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
		consumer.stop();
		Mockito.reset(handler);
		for (int i = 0; i < 10; i++) {
			channel.send(new GenericMessage<>("foo"));
		}
		Mockito.verify(handler, atMost(1)).handleMessage(Mockito.any(Message.class));
	}

	@Test
	public void ensurePollerTaskStopsForAdapter() {
		QueueChannel channel = new QueueChannel();

		SourcePollingChannelAdapterFactoryBean adapterFactory = new SourcePollingChannelAdapterFactoryBean();
		PollerMetadata pollerMetadata = new PollerMetadata();
		pollerMetadata.setTrigger(new PeriodicTrigger(2000));
		adapterFactory.setPollerMetadata(pollerMetadata);

		//Has to be an explicit implementation - Mockito cannot mock/spy lambdas
		MessageSource<String> source = spy(new MessageSource<String>() {

			@Override
			public Message<String> receive() {
				return new GenericMessage<>("hello");
			}

		});
		adapterFactory.setSource(source);
		adapterFactory.setOutputChannel(channel);
		adapterFactory.setBeanFactory(mock(ConfigurableBeanFactory.class));
		SourcePollingChannelAdapter adapter = adapterFactory.getObject();
		adapter.setTaskScheduler(this.taskScheduler);
		adapter.afterPropertiesSet();
		adapter.start();
		assertThat(channel.receive(10000)).isNotNull();
		adapter.stop();
		assertThat(channel.receive(10)).isNull();
		Mockito.verify(source, times(1)).receive();
	}

	@Test
	public void ensurePollerTaskStopsForAdapterWithInterruptible() throws Exception {
		final CountDownLatch latch = new CountDownLatch(2);
		QueueChannel channel = new QueueChannel();

		SourcePollingChannelAdapterFactoryBean adapterFactory = new SourcePollingChannelAdapterFactoryBean();
		PollerMetadata pollerMetadata = new PollerMetadata();
		pollerMetadata.setMaxMessagesPerPoll(-1);
		pollerMetadata.setTrigger(new PeriodicTrigger(2000));
		adapterFactory.setPollerMetadata(pollerMetadata);
		final Runnable caughtInterrupted = mock(Runnable.class);
		final CountDownLatch interruptedLatch = new CountDownLatch(1);
		MessageSource<String> source = () -> {

			try {
				for (int i = 0; i < 10; i++) {
					Thread.sleep(latch.getCount() > 0 ? 10 : 1000);
					latch.countDown();
				}
			}
			catch (InterruptedException e) {
				caughtInterrupted.run();
				interruptedLatch.countDown();
			}

			return new GenericMessage<>("hello");
		};
		adapterFactory.setSource(source);
		adapterFactory.setOutputChannel(channel);
		adapterFactory.setBeanFactory(mock(ConfigurableBeanFactory.class));
		SourcePollingChannelAdapter adapter = adapterFactory.getObject();
		adapter.setTaskScheduler(this.taskScheduler);
		adapter.afterPropertiesSet();
		adapter.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		//
		adapter.stop();

		assertThat(interruptedLatch.await(10, TimeUnit.SECONDS)).isTrue();
		Mockito.verify(caughtInterrupted, times(1)).run();
	}

	@Test
	public void testAdapterLifecycleIsPropagatedToMessageSource() {
		SourcePollingChannelAdapterFactoryBean adapterFactory = new SourcePollingChannelAdapterFactoryBean();
		adapterFactory.setOutputChannel(new NullChannel());
		adapterFactory.setBeanFactory(mock(ConfigurableBeanFactory.class));
		PollerMetadata pollerMetadata = new PollerMetadata();
		pollerMetadata.setTrigger(new PeriodicTrigger(2000));
		adapterFactory.setPollerMetadata(pollerMetadata);

		final AtomicBoolean startInvoked = new AtomicBoolean();

		final AtomicBoolean stopInvoked = new AtomicBoolean();

		MethodInvokingMessageSource source = new MethodInvokingMessageSource();
		source.setObject(new Lifecycle() {

			@Override
			public void start() {
				startInvoked.set(true);
			}

			@Override
			public void stop() {
				stopInvoked.set(true);
			}

			@Override
			public boolean isRunning() {
				return false;
			}

		});
		source.setMethodName("isRunning");

		adapterFactory.setSource(source);

		SourcePollingChannelAdapter adapter = adapterFactory.getObject();
		adapter.setTaskScheduler(this.taskScheduler);
		adapter.start();
		adapter.stop();

		assertThat(startInvoked.get()).isTrue();
		assertThat(stopInvoked.get()).isTrue();
	}

}
