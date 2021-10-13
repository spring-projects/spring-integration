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

package org.springframework.integration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.ClassUtils;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class SourcePollingChannelAdapterFactoryBeanTests {

	@Test
	public void testAdviceChain() {
		SourcePollingChannelAdapterFactoryBean factoryBean = new SourcePollingChannelAdapterFactoryBean();
		QueueChannel outputChannel = new QueueChannel();
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		factoryBean.setBeanFactory(context.getBeanFactory());
		factoryBean.setBeanClassLoader(ClassUtils.getDefaultClassLoader());
		factoryBean.setOutputChannel(outputChannel);
		factoryBean.setSource(() -> new GenericMessage<>("test"));
		PollerMetadata pollerMetadata = new PollerMetadata();
		List<Advice> adviceChain = new ArrayList<>();
		final AtomicBoolean adviceApplied = new AtomicBoolean(false);
		adviceChain.add((MethodInterceptor) invocation -> {
			adviceApplied.set(true);
			return invocation.proceed();
		});
		pollerMetadata.setTrigger(new PeriodicTrigger(5000));
		pollerMetadata.setMaxMessagesPerPoll(1);
		pollerMetadata.setAdviceChain(adviceChain);
		factoryBean.setPollerMetadata(pollerMetadata);
		factoryBean.setAutoStartup(true);
		factoryBean.afterPropertiesSet();
		context.registerEndpoint("testPollingEndpoint", factoryBean.getObject());
		context.refresh();
		Message<?> message = outputChannel.receive(5000);
		assertThat(message.getPayload()).isEqualTo("test");
		assertThat(adviceApplied.get()).as("adviceChain was not applied").isTrue();
		context.close();
	}

	@Test
	public void testTransactionalAdviceChain() throws Throwable {
		SourcePollingChannelAdapterFactoryBean factoryBean = new SourcePollingChannelAdapterFactoryBean();
		QueueChannel outputChannel = new QueueChannel();
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		factoryBean.setBeanFactory(context.getBeanFactory());
		factoryBean.setBeanClassLoader(ClassUtils.getDefaultClassLoader());
		factoryBean.setOutputChannel(outputChannel);
		factoryBean.setSource(() -> new GenericMessage<>("test"));
		PollerMetadata pollerMetadata = new PollerMetadata();
		List<Advice> adviceChain = new ArrayList<>();
		final AtomicBoolean adviceApplied = new AtomicBoolean(false);
		adviceChain.add((MethodInterceptor) invocation -> {
			adviceApplied.set(true);
			return invocation.proceed();
		});
		pollerMetadata.setTrigger(new PeriodicTrigger(5000));
		pollerMetadata.setMaxMessagesPerPoll(1);
		final AtomicInteger count = new AtomicInteger();
		final MethodInterceptor txAdvice = mock(MethodInterceptor.class);
		adviceChain.add((MethodInterceptor) invocation -> {
			count.incrementAndGet();
			return invocation.proceed();
		});
		when(txAdvice.invoke(any(MethodInvocation.class))).thenAnswer(invocation -> {
			count.incrementAndGet();
			return ((MethodInvocation) invocation.getArgument(0)).proceed();
		});

		pollerMetadata.setAdviceChain(adviceChain);
		factoryBean.setPollerMetadata(pollerMetadata);
		factoryBean.setAutoStartup(true);
		factoryBean.afterPropertiesSet();
		context.registerEndpoint("testPollingEndpoint", factoryBean.getObject());
		context.refresh();
		Message<?> message = outputChannel.receive(5000);
		assertThat(message.getPayload()).isEqualTo("test");
		assertThat(count.get()).isEqualTo(1);
		assertThat(adviceApplied.get()).as("adviceChain was not applied").isTrue();
		context.close();
	}

	@Test
	public void testInterrupted() throws Exception {
		final CountDownLatch startLatch = new CountDownLatch(1);

		MessageSource<Object> ms = () -> {
			startLatch.countDown();
			try {
				Thread.sleep(10000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new MessagingException("Interrupted awaiting stopLatch", e);
			}
			return null;
		};

		SourcePollingChannelAdapter pollingChannelAdapter = new SourcePollingChannelAdapter();
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
		taskScheduler.setAwaitTerminationSeconds(1);
		taskScheduler.afterPropertiesSet();
		pollingChannelAdapter.setTaskScheduler(taskScheduler);

		MessagePublishingErrorHandler errorHandler = new MessagePublishingErrorHandler();
		Log errorHandlerLogger = TestUtils.getPropertyValue(errorHandler, "logger", Log.class);
		errorHandlerLogger = spy(errorHandlerLogger);
		DirectFieldAccessor dfa = new DirectFieldAccessor(errorHandler);
		dfa.setPropertyValue("logger", errorHandlerLogger);
		pollingChannelAdapter.setErrorHandler(errorHandler);

		pollingChannelAdapter.setSource(ms);
		pollingChannelAdapter.setOutputChannel(new NullChannel());
		pollingChannelAdapter.setBeanFactory(mock(BeanFactory.class));
		pollingChannelAdapter.afterPropertiesSet();

		LogAccessor adapterLogger = TestUtils.getPropertyValue(pollingChannelAdapter, "logger", LogAccessor.class);
		adapterLogger = spy(adapterLogger);
		when(adapterLogger.isDebugEnabled()).thenReturn(true);

		dfa = new DirectFieldAccessor(pollingChannelAdapter);
		dfa.setPropertyValue("logger", adapterLogger);

		pollingChannelAdapter.start();

		assertThat(startLatch.await(10, TimeUnit.SECONDS)).isTrue();
		pollingChannelAdapter.stop();

		taskScheduler.shutdown();

		verifyNoInteractions(errorHandlerLogger);
		verify(adapterLogger)
				.debug(ArgumentMatchers.<Supplier<String>>argThat(logMessage ->
						logMessage.get().contains("Poll interrupted - during stop()?")));
	}

	@Test
	public void testStartSourceBeforeRunPollingTask() {
		TaskScheduler taskScheduler = mock(TaskScheduler.class);

		willAnswer(invocation -> {
			Runnable task = invocation.getArgument(0);
			task.run();
			return null;
		})
				.given(taskScheduler)
				.schedule(any(Runnable.class), any(Trigger.class));

		SourcePollingChannelAdapter pollingChannelAdapter = new SourcePollingChannelAdapter();
		pollingChannelAdapter.setTaskScheduler(taskScheduler);
		pollingChannelAdapter.setSource(new LifecycleMessageSource());
		pollingChannelAdapter.setMaxMessagesPerPoll(1);
		QueueChannel outputChannel = new QueueChannel();
		pollingChannelAdapter.setOutputChannel(outputChannel);
		pollingChannelAdapter.setBeanFactory(mock(BeanFactory.class));
		pollingChannelAdapter.afterPropertiesSet();
		pollingChannelAdapter.start();

		Message<?> receive = outputChannel.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(true);
		pollingChannelAdapter.stop();
	}

	@Test
	public void testZeroForMaxMessagesPerPoll() throws InterruptedException {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();

		SourcePollingChannelAdapter pollingChannelAdapter = new SourcePollingChannelAdapter();
		pollingChannelAdapter.setTaskScheduler(taskScheduler);
		pollingChannelAdapter.setSource(() -> new GenericMessage<>("test"));
		pollingChannelAdapter.setTrigger(new PeriodicTrigger(1));
		pollingChannelAdapter.setMaxMessagesPerPoll(0);
		QueueChannel outputChannel = new QueueChannel();
		pollingChannelAdapter.setOutputChannel(outputChannel);
		pollingChannelAdapter.setBeanFactory(mock(BeanFactory.class));

		LogAccessor logger = spy(TestUtils.getPropertyValue(pollingChannelAdapter, "logger", LogAccessor.class));
		new DirectFieldAccessor(pollingChannelAdapter).setPropertyValue("logger", logger);

		CountDownLatch logCalledLatch = new CountDownLatch(1);

		willAnswer(invocation -> {
			logCalledLatch.countDown();
			return invocation.callRealMethod();
		})
				.given(logger)
				.info(anyString());

		pollingChannelAdapter.afterPropertiesSet();
		pollingChannelAdapter.start();

		assertThat(logCalledLatch.await(10, TimeUnit.SECONDS)).isTrue();
		verify(logger, atLeastOnce()).info("Polling disabled while 'maxMessagesPerPoll == 0'");

		pollingChannelAdapter.setMaxMessagesPerPoll(1);

		Message<?> receive = outputChannel.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("test");
		pollingChannelAdapter.stop();
	}


	private static class LifecycleMessageSource implements MessageSource<Boolean>, Lifecycle {

		private volatile boolean running;

		LifecycleMessageSource() {
			super();
		}

		@Override
		public void start() {
			this.running = true;
		}

		@Override
		public void stop() {
			this.running = false;
		}

		@Override
		public boolean isRunning() {
			return this.running;
		}

		@Override
		public Message<Boolean> receive() {
			return new GenericMessage<>(isRunning());
		}

	}

}
