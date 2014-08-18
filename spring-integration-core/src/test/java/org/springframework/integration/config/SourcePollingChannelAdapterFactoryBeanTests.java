/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.ClassUtils;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class SourcePollingChannelAdapterFactoryBeanTests {

	@Test
	public void testAdviceChain() throws Exception {
		SourcePollingChannelAdapterFactoryBean factoryBean = new SourcePollingChannelAdapterFactoryBean();
		QueueChannel outputChannel = new QueueChannel();
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		factoryBean.setBeanFactory(context.getBeanFactory());
		factoryBean.setBeanClassLoader(ClassUtils.getDefaultClassLoader());
		factoryBean.setOutputChannel(outputChannel);
		factoryBean.setSource(new TestSource());
		PollerMetadata pollerMetadata = new PollerMetadata();
		List<Advice> adviceChain = new ArrayList<Advice>();
		final AtomicBoolean adviceApplied = new AtomicBoolean(false);
		adviceChain.add(new MethodInterceptor() {
			public Object invoke(MethodInvocation invocation) throws Throwable {
				adviceApplied.set(true);
				return invocation.proceed();
			}
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
		assertEquals("test", message.getPayload());
		assertTrue("adviceChain was not applied", adviceApplied.get());
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testTransactionalAdviceChain() throws Throwable {
		SourcePollingChannelAdapterFactoryBean factoryBean = new SourcePollingChannelAdapterFactoryBean();
		QueueChannel outputChannel = new QueueChannel();
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		factoryBean.setBeanFactory(context.getBeanFactory());
		factoryBean.setBeanClassLoader(ClassUtils.getDefaultClassLoader());
		factoryBean.setOutputChannel(outputChannel);
		factoryBean.setSource(new TestSource());
		PollerMetadata pollerMetadata = new PollerMetadata();
		List<Advice> adviceChain = new ArrayList<Advice>();
		final AtomicBoolean adviceApplied = new AtomicBoolean(false);
		adviceChain.add(new MethodInterceptor() {
			public Object invoke(MethodInvocation invocation) throws Throwable {
				adviceApplied.set(true);
				return invocation.proceed();
			}
		});
		pollerMetadata.setTrigger(new PeriodicTrigger(5000));
		pollerMetadata.setMaxMessagesPerPoll(1);
		final AtomicInteger count = new AtomicInteger();
		final MethodInterceptor txAdvice = mock(MethodInterceptor.class);
		adviceChain.add(new MethodInterceptor() {
			public Object invoke(MethodInvocation invocation) throws Throwable {
				count.incrementAndGet();
				return invocation.proceed();
			}
		});
		when(txAdvice.invoke(Mockito.any(MethodInvocation.class))).thenAnswer(new Answer() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				count.incrementAndGet();
				return ((MethodInvocation) invocation.getArguments()[0]).proceed();
			}
		});

		pollerMetadata.setAdviceChain(adviceChain);
		factoryBean.setPollerMetadata(pollerMetadata);
		factoryBean.setAutoStartup(true);
		factoryBean.afterPropertiesSet();
		context.registerEndpoint("testPollingEndpoint", factoryBean.getObject());
		context.refresh();
		Message<?> message = outputChannel.receive(5000);
		assertEquals("test", message.getPayload());
		assertEquals(1, count.get());
		assertTrue("adviceChain was not applied", adviceApplied.get());
	}

	@Test
	public void testInterrupted() throws Exception {
		final CountDownLatch startLatch = new CountDownLatch(1);

		MessageSource<Object> ms = new MessageSource<Object>() {
			@Override
			public Message<Object> receive() {
				startLatch.countDown();
				try {
					Thread.sleep(10000);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new MessagingException("Interrupted awaiting stopLatch", e);
				}
				return null;
			}
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

		Log adapterLogger = TestUtils.getPropertyValue(pollingChannelAdapter, "logger", Log.class);
		adapterLogger = spy(adapterLogger);
		when(adapterLogger.isDebugEnabled()).thenReturn(true);

		dfa = new DirectFieldAccessor(pollingChannelAdapter);
		dfa.setPropertyValue("logger", adapterLogger);

		pollingChannelAdapter.start();

		assertTrue(startLatch.await(10, TimeUnit.SECONDS));
		pollingChannelAdapter.stop();

		taskScheduler.shutdown();

		verifyZeroInteractions(errorHandlerLogger);
		verify(adapterLogger).debug(contains("The interruption error occurred during stop:"));
	}

	private static class TestSource implements MessageSource<String> {

		public Message<String> receive() {
			return new GenericMessage<String>("test");
		}
	}

}
