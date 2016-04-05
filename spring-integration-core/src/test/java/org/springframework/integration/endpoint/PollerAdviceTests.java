/*
 * Copyright 2014-2016 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.aop.AbstractMessageSourceAdvice;
import org.springframework.integration.aop.CompoundTriggerAdvice;
import org.springframework.integration.aop.SimpleActiveIdleMessageSourceAdvice;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.ExpressionControlBusFactoryBean;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.scheduling.PollSkipAdvice;
import org.springframework.integration.scheduling.SimplePollSkipStrategy;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.CompoundTrigger;
import org.springframework.integration.util.DynamicPeriodicTrigger;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.1
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class PollerAdviceTests {

	public Message<?> receiveAdviceResult;

	@Autowired
	private MessageChannel control;

	@Autowired
	private SimplePollSkipStrategy skipper;

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
	public void testSkipSimple() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		class LocalSource implements MessageSource<Object> {

			private final CountDownLatch latch;

			LocalSource(CountDownLatch latch) {
				this.latch = latch;
			}

			@Override
			public Message<Object> receive() {
				latch.countDown();
				return null;
			}

		}
		CountDownLatch latch = new CountDownLatch(1);
		adapter.setSource(new LocalSource(latch));
		class OneAndDone10msTrigger implements Trigger {

			private boolean done;

			@Override
			public Date nextExecutionTime(TriggerContext triggerContext) {
				Date date = done ? null : new Date(System.currentTimeMillis() + 10);
				done = true;
				return date;
			}
		}
		adapter.setTrigger(new OneAndDone10msTrigger());
		configure(adapter);
		List<Advice> adviceChain = new ArrayList<Advice>();
		SimplePollSkipStrategy skipper = new SimplePollSkipStrategy();
		skipper.skipPolls();
		PollSkipAdvice advice = new PollSkipAdvice(skipper);
		adviceChain.add(advice);
		adapter.setAdviceChain(adviceChain);
		adapter.afterPropertiesSet();
		adapter.start();
		assertFalse(latch.await(1, TimeUnit.SECONDS));
		adapter.stop();
		skipper.reset();
		latch = new CountDownLatch(1);
		adapter.setSource(new LocalSource(latch));
		adapter.setTrigger(new OneAndDone10msTrigger());
		adapter.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		adapter.stop();
	}

	@Test
	public void testSkipSimpleControlBus() {
		this.control.send(new GenericMessage<String>("@skipper.skipPolls()"));
		assertTrue(this.skipper.skipPoll());
		this.control.send(new GenericMessage<String>("@skipper.reset()"));
		assertFalse(this.skipper.skipPoll());
	}

	@Test
	public void testMixedAdvice() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		final List<String> callOrder = new ArrayList<String>();
		final CountDownLatch latch = new CountDownLatch(4); // advice + advice + source + advice
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
		SimpleActiveIdleMessageSourceAdvice toggling = new SimpleActiveIdleMessageSourceAdvice(trigger);
		toggling.setActivePollPeriod(11);
		toggling.setIdlePollPeriod(12);
		adapter.setAdviceChain(Collections.singletonList(toggling));
		adapter.setTrigger(trigger);
		configure(adapter);
		adapter.afterPropertiesSet();
		adapter.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		adapter.stop();
		while (triggerPeriods.size() > 5) {
			triggerPeriods.removeLast();
		}
		assertThat(triggerPeriods, contains(10L, 12L, 11L, 12L, 11L));
	}

	@Test
	public void testCompoundTriggerAdvice() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		final CountDownLatch latch = new CountDownLatch(5);
		final LinkedList<Object> overridePresent = new LinkedList<Object>();
		final CompoundTrigger compoundTrigger = new CompoundTrigger(new PeriodicTrigger(10));
		Trigger override = spy(new PeriodicTrigger(5));
		final CompoundTriggerAdvice advice = new CompoundTriggerAdvice(compoundTrigger, override);
		adapter.setSource(new MessageSource<Object>() {

			@Override
			public Message<Object> receive() {
				overridePresent.add(TestUtils.getPropertyValue(compoundTrigger, "override"));
				Message<Object> m = null;
				if (latch.getCount() % 2 == 0) {
					m = new GenericMessage<Object>("foo");
				}
				latch.countDown();
				return m;
			}
		});
		adapter.setAdviceChain(Collections.singletonList(advice));
		adapter.setTrigger(compoundTrigger);
		configure(adapter);
		adapter.afterPropertiesSet();
		adapter.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		adapter.stop();
		while (overridePresent.size() > 5) {
			overridePresent.removeLast();
		}
		assertThat(overridePresent, contains(null, override, null, override, null));
		verify(override, atLeast(2)).nextExecutionTime(any(TriggerContext.class));
	}

	private void configure(SourcePollingChannelAdapter adapter) {
		adapter.setOutputChannel(new NullChannel());
		adapter.setBeanFactory(mock(BeanFactory.class));
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.afterPropertiesSet();
		adapter.setTaskScheduler(scheduler);
	}

	@Test
	public void testCompoundAdviceXML() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext("compound-trigger-context.xml",
				getClass());
		SourcePollingChannelAdapter adapter = ctx.getBean(SourcePollingChannelAdapter.class);
		Source source = ctx.getBean(Source.class);
		adapter.start();
		assertTrue(source.latch.await(10, TimeUnit.SECONDS));
		assertNotNull(TestUtils.getPropertyValue(adapter, "trigger.override"));
		adapter.stop();
		ctx.close();
	}

	public static class Source implements MessageSource<Object> {

		private final CountDownLatch latch = new CountDownLatch(5);

		@Override
		public Message<Object> receive() {
			latch.countDown();
			return null;
		}

	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public SimplePollSkipStrategy skipper() {
			return new SimplePollSkipStrategy();
		}

		@Bean
		public MessageChannel control() {
			return new DirectChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "control")
		public ExpressionControlBusFactoryBean controlBus() {
			return new ExpressionControlBusFactoryBean();
		}

	}

}
