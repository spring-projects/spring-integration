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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.messaging.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.ClassUtils;

/**
 * @author Mark Fisher
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
				return ((MethodInvocation)invocation.getArguments()[0]).proceed();
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

	private static class TestSource implements MessageSource<String> {

		public Message<String> receive() {
			return new GenericMessage<String>("test");
		}
	}

}
