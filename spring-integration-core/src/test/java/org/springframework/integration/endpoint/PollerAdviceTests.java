/*
 * Copyright 2014-2015 the original author or authors.
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

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.aop.AbstractMessageSourceAdvice;
import org.springframework.integration.aop.SimpleActiveIdleMessageSourceAdvice;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.scheduling.PollSkipAdvice;
import org.springframework.integration.scheduling.PollSkipStrategy;
import org.springframework.integration.util.DynamicPeriodicTrigger;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 * @since 4.1
 *
 */
public class PollerAdviceTests {

	public Message<?> receiveAdviceResult;

	@Test
	public void testDefaultDontSkip() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		final CountDownLatch latch = new CountDownLatch(1);
		adapter.setSource(new MessageSource<Object>() {

			@Override
			public Message<Object> receive() {
				latch.countDown();
				return null;
			}
		});
		adapter.setTrigger(new Trigger() {

			private boolean done;

			@Override
			public Date nextExecutionTime(TriggerContext triggerContext) {
				Date date = done ? null : new Date(System.currentTimeMillis() + 10);
				done = true;
				return date;
			}
		});
		configure(adapter);
		List<Advice> adviceChain = new ArrayList<Advice>();
		PollSkipAdvice advice = new PollSkipAdvice();
		adviceChain.add(advice);
		adapter.setAdviceChain(adviceChain);
		adapter.afterPropertiesSet();
		adapter.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		adapter.stop();
	}

	@Test
	public void testSkipAll() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		final CountDownLatch latch = new CountDownLatch(1);
		adapter.setSource(new MessageSource<Object>() {

			@Override
			public Message<Object> receive() {
				latch.countDown();
				return null;
			}
		});
		adapter.setTrigger(new Trigger() {

			private boolean done;

			@Override
			public Date nextExecutionTime(TriggerContext triggerContext) {
				Date date = done ? null : new Date(System.currentTimeMillis() + 10);
				done = true;
				return date;
			}
		});
		configure(adapter);
		List<Advice> adviceChain = new ArrayList<Advice>();
		PollSkipAdvice advice = new PollSkipAdvice(new PollSkipStrategy() {

			@Override
			public boolean skipPoll() {
				return true;
			}

		});
		adviceChain.add(advice);
		adapter.setAdviceChain(adviceChain);
		adapter.afterPropertiesSet();
		adapter.start();
		assertFalse(latch.await(1, TimeUnit.SECONDS));
		adapter.stop();
	}

	@Test
	public void testMixedAdvice() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		final List<String> callOrder = new ArrayList<String>();
		final CountDownLatch latch = new CountDownLatch(4);// advice + advice + source + advice
		adapter.setSource(new MessageSource<Object>() {

			@Override
			public Message<Object> receive() {
				callOrder.add("c");
				latch.countDown();
				return null;
			}
		});
		adapter.setTrigger(new Trigger() {

			private boolean done;

			@Override
			public Date nextExecutionTime(TriggerContext triggerContext) {
				Date date = done ? null : new Date(System.currentTimeMillis() + 10);
				done = true;
				return date;
			}
		});
		configure(adapter);
		List<Advice> adviceChain = new ArrayList<Advice>();

		class TestGeneralAdvice implements MethodInterceptor {

			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				callOrder.add("a");
				latch.countDown();
				return invocation.proceed();
			}

		}
		adviceChain.add(new TestGeneralAdvice());

		class TestSourceAdvice extends AbstractMessageSourceAdvice {

			@Override
			public boolean beforeReceive(MessageSource<?> target) {
				callOrder.add("b");
				latch.countDown();
				return true;
			}

			@Override
			public Message<?> afterReceive(Message<?> result, MessageSource<?> target) {
				callOrder.add("d");
				latch.countDown();
				return result;
			}

		}
		adviceChain.add(new TestSourceAdvice());

		adapter.setAdviceChain(adviceChain);
		adapter.afterPropertiesSet();
		adapter.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertThat(callOrder, contains("a", "b", "c", "d")); // advice + advice + source + advice
		adapter.stop();
	}

	@Test
	public void testActiveIdleAdvice() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		final CountDownLatch latch = new CountDownLatch(5);
		final LinkedList<Long> triggerPeriods = new LinkedList<Long>();
		final DynamicPeriodicTrigger trigger = new DynamicPeriodicTrigger(10);
		adapter.setSource(new MessageSource<Object>() {

			@Override
			public Message<Object> receive() {
				triggerPeriods.add(trigger.getPeriod());
				Message<Object> m = null;
				if (latch.getCount() % 2 == 0) {
					m = new GenericMessage<Object>("foo");
				}
				latch.countDown();
				return m;
			}
		});
		QueueChannel channel = new QueueChannel();
		SimpleActiveIdleMessageSourceAdvice toggling = new SimpleActiveIdleMessageSourceAdvice(trigger);
		toggling.setActivePollPeriod(11);
		toggling.setIdlePollPeriod(12);
		adapter.setAdviceChain(Collections.singletonList(toggling));
		configure(adapter);
		adapter.afterPropertiesSet();
		adapter.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		adapter.stop();
		while (triggerPeriods.size() > 5) {
			triggerPeriods.removeLast();
		}
		assertThat(triggerPeriods, Matchers.contains(10L, 12L, 11L, 12L, 11L));
	}

	private void configure(SourcePollingChannelAdapter adapter) {
		adapter.setOutputChannel(new NullChannel());
		adapter.setBeanFactory(mock(BeanFactory.class));
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.afterPropertiesSet();
		adapter.setTaskScheduler(scheduler);
	}

}
