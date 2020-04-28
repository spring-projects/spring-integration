/*
 * Copyright 2014-2020 the original author or authors.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.Joinpoint;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.aop.CompoundTriggerAdvice;
import org.springframework.integration.aop.ReceiveMessageAdvice;
import org.springframework.integration.aop.SimpleActiveIdleReceiveMessageAdvice;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.ExpressionControlBusFactoryBean;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.scheduling.PollSkipAdvice;
import org.springframework.integration.scheduling.SimplePollSkipStrategy;
import org.springframework.integration.test.util.OnlyOnceTrigger;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.CompoundTrigger;
import org.springframework.integration.util.DynamicPeriodicTrigger;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.1
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class PollerAdviceTests {

	@Autowired
	private MessageChannel control;

	@Autowired
	private SimplePollSkipStrategy skipper;

	@Autowired
	private ThreadPoolTaskScheduler threadPoolTaskScheduler;

	@Autowired
	private BeanFactory beanFactory;

	@Test
	public void testDefaultDontSkip() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		final CountDownLatch latch = new CountDownLatch(1);
		adapter.setSource(() -> {
			latch.countDown();
			return null;
		});
		adapter.setTrigger(new OnlyOnceTrigger());
		configure(adapter);
		List<Advice> adviceChain = new ArrayList<>();
		PollSkipAdvice advice = new PollSkipAdvice();
		adviceChain.add(advice);
		adapter.setAdviceChain(adviceChain);
		adapter.afterPropertiesSet();
		adapter.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		adapter.stop();
	}

	@Test
	public void testSkipSimple() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		class LocalSource implements MessageSource<Object> {

			private final CountDownLatch latch;

			private LocalSource(CountDownLatch latch) {
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
		adapter.setTrigger(new OnlyOnceTrigger());
		AtomicBoolean ehCalled = new AtomicBoolean();
		adapter.setErrorHandler(t -> {
			ehCalled.set(true);
		});
		configure(adapter);
		List<Advice> adviceChain = new ArrayList<>();
		SimplePollSkipStrategy skipper = new SimplePollSkipStrategy();
		skipper.skipPolls();
		PollSkipAdvice advice = new PollSkipAdvice(skipper);
		adviceChain.add(advice);
		adapter.setAdviceChain(adviceChain);
		adapter.afterPropertiesSet();
		adapter.start();
		assertThat(latch.await(10, TimeUnit.MILLISECONDS)).isFalse();
		assertThat(ehCalled.get()).isFalse();
		adapter.stop();
		skipper.reset();
		latch = new CountDownLatch(1);
		adapter.setSource(new LocalSource(latch));
		adapter.setTrigger(new OnlyOnceTrigger());
		adapter.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		adapter.stop();
	}

	@Test
	public void testSkipSimpleControlBus() {
		this.control.send(new GenericMessage<>("@skipper.skipPolls()"));
		assertThat(this.skipper.skipPoll()).isTrue();
		this.control.send(new GenericMessage<>("@skipper.reset()"));
		assertThat(this.skipper.skipPoll()).isFalse();
	}

	@Test
	public void testMixedAdvice() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		final List<String> callOrder = new ArrayList<>();
		final AtomicReference<CountDownLatch> latch = new AtomicReference<>(new CountDownLatch(4));
		MessageSource<Object> source = () -> {
			callOrder.add("c");
			latch.get().countDown();
			return null;
		};
		adapter.setSource(source);
		OnlyOnceTrigger trigger = new OnlyOnceTrigger();
		adapter.setTrigger(trigger);
		configure(adapter);
		List<Advice> adviceChain = new ArrayList<>();

		adviceChain.add((MethodInterceptor) invocation -> {
			callOrder.add("a");
			latch.get().countDown();
			return invocation.proceed();
		});

		final AtomicInteger count = new AtomicInteger();
		class TestSourceAdvice implements ReceiveMessageAdvice {

			@Override
			public boolean beforeReceive(Object target) {
				count.incrementAndGet();
				callOrder.add("b");
				latch.get().countDown();
				return true;
			}

			@Override
			public Message<?> afterReceive(Message<?> result, Object target) {
				callOrder.add("d");
				latch.get().countDown();
				return result;
			}

		}
		adviceChain.add(new TestSourceAdvice());

		adapter.setAdviceChain(adviceChain);
		adapter.afterPropertiesSet();
		adapter.start();
		assertThat(latch.get().await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(callOrder).containsExactly("a", "b", "c", "d"); // advice + advice + source + advice
		adapter.stop();
		trigger.reset();
		latch.set(new CountDownLatch(4));
		adapter.start();
		assertThat(latch.get().await(10, TimeUnit.SECONDS)).isTrue();
		adapter.stop();
		assertThat(count.get()).isEqualTo(2);

		// Now test when the source is already a proxy.

		ProxyFactory pf = new ProxyFactory(source);
		pf.addAdvice((MethodInterceptor) Joinpoint::proceed);
		adapter.setSource((MessageSource<?>) pf.getProxy());
		trigger.reset();
		latch.set(new CountDownLatch(4));
		count.set(0);
		callOrder.clear();
		adapter.start();
		assertThat(latch.get().await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(callOrder).containsExactly("a", "b", "c", "d"); // advice + advice + source + advice
		adapter.stop();
		trigger.reset();
		latch.set(new CountDownLatch(4));
		adapter.start();
		assertThat(latch.get().await(10, TimeUnit.SECONDS)).isTrue();
		adapter.stop();
		assertThat(count.get()).isEqualTo(2);
		Advisor[] advisors = ((Advised) adapter.getMessageSource()).getAdvisors();
		assertThat(advisors.length).isEqualTo(2); // make sure we didn't remove the original one
	}

	@Test
	public void testActiveIdleAdvice() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		final CountDownLatch latch = new CountDownLatch(5);
		final LinkedList<Long> triggerPeriods = new LinkedList<>();
		final DynamicPeriodicTrigger trigger = new DynamicPeriodicTrigger(10);
		adapter.setSource(() -> {
			synchronized (triggerPeriods) {
				triggerPeriods.add(trigger.getDuration().toMillis());
			}
			Message<Object> m = null;
			if (latch.getCount() % 2 == 0) {
				m = new GenericMessage<>("foo");
			}
			latch.countDown();
			return m;
		});
		SimpleActiveIdleReceiveMessageAdvice toggling = new SimpleActiveIdleReceiveMessageAdvice(trigger);
		toggling.setActivePollPeriod(11);
		toggling.setIdlePollPeriod(12);
		adapter.setAdviceChain(Collections.singletonList(toggling));
		adapter.setTrigger(trigger);
		configure(adapter);
		adapter.afterPropertiesSet();
		adapter.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		adapter.stop();
		synchronized (triggerPeriods) {
			assertThat(triggerPeriods.subList(0, 5)).containsExactly(10L, 12L, 11L, 12L, 11L);
		}
	}

	@Test
	public void testActiveIdleAdviceOnQueueChannel() throws Exception {
		final CountDownLatch latch = new CountDownLatch(5);
		final LinkedList<Long> triggerPeriods = new LinkedList<>();
		final DynamicPeriodicTrigger trigger = new DynamicPeriodicTrigger(10);

		PollingConsumer pollingConsumer =
				new PollingConsumer(new PollableChannel() {

					@Override
					public Message<?> receive() {
						synchronized (triggerPeriods) {
							triggerPeriods.add(trigger.getDuration().toMillis());
						}
						Message<Object> m = null;
						if (latch.getCount() % 2 == 0) {
							m = new GenericMessage<>("foo");
						}
						latch.countDown();
						return m;
					}

					@Override
					public Message<?> receive(long timeout) {
						return receive();
					}

					@Override
					public boolean send(Message<?> message, long timeout) {
						return false;
					}

				}, m -> { });

		SimpleActiveIdleReceiveMessageAdvice toggling = new SimpleActiveIdleReceiveMessageAdvice(trigger);
		toggling.setActivePollPeriod(11);
		toggling.setIdlePollPeriod(12);
		pollingConsumer.setAdviceChain(Collections.singletonList(toggling));
		pollingConsumer.setTrigger(trigger);
		pollingConsumer.setBeanFactory(this.beanFactory);
		pollingConsumer.setTaskScheduler(this.threadPoolTaskScheduler);
		pollingConsumer.afterPropertiesSet();
		pollingConsumer.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		pollingConsumer.stop();
		synchronized (triggerPeriods) {
			assertThat(triggerPeriods.subList(0, 5)).containsExactly(10L, 12L, 11L, 12L, 11L);
		}
	}

	@Test
	public void testCompoundTriggerAdvice() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		final CountDownLatch latch = new CountDownLatch(5);
		final LinkedList<Object> overridePresent = new LinkedList<>();
		final CompoundTrigger compoundTrigger = new CompoundTrigger(new PeriodicTrigger(10));
		Trigger override = spy(new PeriodicTrigger(5));
		final CompoundTriggerAdvice advice = new CompoundTriggerAdvice(compoundTrigger, override);
		adapter.setSource(() -> {
			synchronized (overridePresent) {
				overridePresent.add(TestUtils.getPropertyValue(compoundTrigger, "override"));
			}
			Message<Object> m = null;
			if (latch.getCount() % 2 == 0) {
				m = new GenericMessage<>("foo");
			}
			latch.countDown();
			return m;
		});
		adapter.setAdviceChain(Collections.singletonList(advice));
		adapter.setTrigger(compoundTrigger);
		configure(adapter);
		adapter.afterPropertiesSet();
		adapter.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		adapter.stop();
		synchronized (overridePresent) {
			assertThat(overridePresent.subList(0, 5)).containsExactly(null, override, null, override, null);
		}
		verify(override, atLeast(2)).nextExecutionTime(any(TriggerContext.class));
	}

	private void configure(SourcePollingChannelAdapter adapter) {
		adapter.setOutputChannel(new NullChannel());
		adapter.setBeanFactory(this.beanFactory);
		adapter.setTaskScheduler(this.threadPoolTaskScheduler);
	}

	@Test
	public void testCompoundAdviceXML() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext("compound-trigger-context.xml",
				getClass());
		SourcePollingChannelAdapter adapter = ctx.getBean(SourcePollingChannelAdapter.class);
		Source source = ctx.getBean(Source.class);
		adapter.start();
		assertThat(source.latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(TestUtils.getPropertyValue(adapter, "trigger.override")).isNotNull();
		adapter.stop();
		OtherAdvice sourceAdvice = ctx.getBean(OtherAdvice.class);
		int count = sourceAdvice.calls;
		assertThat(count).isGreaterThan(0);
		((Foo) adapter.getMessageSource()).otherMethod();
		assertThat(sourceAdvice.calls).isEqualTo(count);
		ctx.close();
	}

	public interface Foo {

		void otherMethod();

	}

	public static class Source implements MessageSource<Object>, Foo {

		private final CountDownLatch latch = new CountDownLatch(5);

		@Override
		public Message<Object> receive() {
			latch.countDown();
			return null;
		}

		@Override
		public void otherMethod() {

		}

	}

	public static class OtherAdvice implements ReceiveMessageAdvice {

		private int calls;

		@Override
		public boolean beforeReceive(Object source) {
			this.calls++;
			return true;
		}

		@Override
		public Message<?> afterReceive(Message<?> result, Object source) {
			return result;
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
