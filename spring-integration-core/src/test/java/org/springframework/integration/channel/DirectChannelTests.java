/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.ReflectionUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
class DirectChannelTests {

	@Test
	void testSend() {
		DirectChannel channel = new DirectChannel();
		LogAccessor logger = spy(TestUtils.getPropertyValue(channel, "logger", LogAccessor.class));
		when(logger.isDebugEnabled()).thenReturn(true);
		new DirectFieldAccessor(channel).setPropertyValue("logger", logger);
		ThreadNameExtractingTestTarget target = new ThreadNameExtractingTestTarget();
		channel.subscribe(target);
		GenericMessage<String> message = new GenericMessage<>("test");
		assertThat(channel.send(message)).isTrue();
		assertThat(target.threadName).isEqualTo(Thread.currentThread().getName());
		DirectFieldAccessor channelAccessor = new DirectFieldAccessor(channel);
		UnicastingDispatcher dispatcher = (UnicastingDispatcher) channelAccessor.getPropertyValue("dispatcher");
		DirectFieldAccessor dispatcherAccessor = new DirectFieldAccessor(dispatcher);
		Object loadBalancingStrategy = dispatcherAccessor.getPropertyValue("loadBalancingStrategy");
		assertThat(loadBalancingStrategy instanceof RoundRobinLoadBalancingStrategy).isTrue();
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(logger, times(2)).debug(captor.capture());
		List<String> logs = captor.getAllValues();
		assertThat(logs.size()).isEqualTo(2);
		assertThat(logs.get(0)).startsWith("preSend");
		assertThat(logs.get(1)).startsWith("postSend");
	}

	@Test
	void testSendPerfOneHandler() {
		/*
		 *  INT-3308 - used to run 12 million/sec
		 *  1. optimize for single handler 20 million/sec
		 *  2. Don't iterate over empty datatypes 23 million/sec
		 *  3. Don't iterate over empty interceptors 31 million/sec
		 *  4. Move single handler optimization to dispatcher 34 million/sec
		 *
		 *  29 million per second with increment counter in the handler
		 */
		DirectChannel channel = new DirectChannel();
		final AtomicInteger count = new AtomicInteger();
		channel.subscribe(message -> count.incrementAndGet());
		GenericMessage<String> message = new GenericMessage<>("test");
		assertThat(channel.send(message)).isTrue();
		for (int i = 0; i < 10000000; i++) {
			channel.send(message);
		}
	}

	@Test
	void testSendPerfTwoHandlers() {
		/*
		 *  INT-3308 - used to run 6.4 million/sec
		 *  1. Skip empty iterators as above 7.2 million/sec
		 *  2. optimize for single handler 6.7 million/sec (small overhead added)
		 *  3. remove LB rwlock from UnicastingDispatcher 7.2 million/sec
		 *  4. Move single handler optimization to dispatcher 7.3 million/sec
		 */
		DirectChannel channel = new DirectChannel();
		final AtomicInteger count1 = new AtomicInteger();
		final AtomicInteger count2 = new AtomicInteger();
		channel.subscribe(message -> count1.incrementAndGet());
		channel.subscribe(message -> count2.getAndIncrement());
		GenericMessage<String> message = new GenericMessage<>("test");
		assertThat(channel.send(message)).isTrue();
		for (int i = 0; i < 10000000; i++) {
			channel.send(message);
		}
		assertThat(count1.get()).isEqualTo(5000001);
		assertThat(count2.get()).isEqualTo(5000000);
	}

	@Test
	void testSendPerfFixedSubscriberChannel() {
		/*
		 *  INT-3308 - 96 million/sec
		 *  NOTE: in order to get a measurable time, I had to add some code to the handler -
		 *  presumably the JIT compiler short circuited the call becaues it's a final field
		 *  and he knows the method does nothing.
		 *  Added the same code to the other tests for comparison.
		 */
		final AtomicInteger count = new AtomicInteger();
		FixedSubscriberChannel channel = new FixedSubscriberChannel(message -> count.incrementAndGet());
		GenericMessage<String> message = new GenericMessage<>("test");
		assertThat(channel.send(message)).isTrue();
		for (int i = 0; i < 100000000; i++) {
			channel.send(message, 0);
		}
	}

	@Test
	void testSendInSeparateThread() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		final DirectChannel channel = new DirectChannel();
		ThreadNameExtractingTestTarget target = new ThreadNameExtractingTestTarget(latch);
		channel.subscribe(target);
		final GenericMessage<String> message = new GenericMessage<>("test");
		new Thread((Runnable) () -> channel.send(message), "test-thread").start();
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertThat(target.threadName).isEqualTo("test-thread");
	}

	@Test
	void testChannelCreationWithBeanDefinitionOverrideTrue() throws Exception {
		ClassPathXmlApplicationContext parentContext =
				new ClassPathXmlApplicationContext("parent-config.xml", getClass());
		MessageChannel parentChannelA = parentContext.getBean("parentChannelA", MessageChannel.class);
		MessageChannel parentChannelB = parentContext.getBean("parentChannelB", MessageChannel.class);

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();
		context.setAllowBeanDefinitionOverriding(false);
		context.setConfigLocations("classpath:org/springframework/integration/channel/channel-override-config.xml");
		context.setParent(parentContext);
		Method method = ReflectionUtils.findMethod(ClassPathXmlApplicationContext.class, "obtainFreshBeanFactory");
		method.setAccessible(true);
		method.invoke(context);
		assertThat(context.containsBean("channelA")).isFalse();
		assertThat(context.containsBean("channelB")).isFalse();
		assertThat(context.containsBean("channelC")).isTrue();
		assertThat(context.containsBean("channelD")).isTrue();

		context.refresh();

		PublishSubscribeChannel channelEarly = context.getBean("channelEarly", PublishSubscribeChannel.class);

		assertThat(context.containsBean("channelA")).isTrue();
		assertThat(context.containsBean("channelB")).isTrue();
		assertThat(context.containsBean("channelC")).isTrue();
		assertThat(context.containsBean("channelD")).isTrue();
		EventDrivenConsumer consumerA = context.getBean("serviceA", EventDrivenConsumer.class);
		assertThat(TestUtils.getPropertyValue(consumerA, "inputChannel")).isEqualTo(context.getBean("channelA"));
		assertThat(TestUtils.getPropertyValue(consumerA, "handler.outputChannelName")).isEqualTo("channelB");

		EventDrivenConsumer consumerB = context.getBean("serviceB", EventDrivenConsumer.class);
		assertThat(TestUtils.getPropertyValue(consumerB, "inputChannel")).isEqualTo(context.getBean("channelB"));
		assertThat(TestUtils.getPropertyValue(consumerB, "handler.outputChannelName")).isEqualTo("channelC");

		EventDrivenConsumer consumerC = context.getBean("serviceC", EventDrivenConsumer.class);
		assertThat(TestUtils.getPropertyValue(consumerC, "inputChannel")).isEqualTo(context.getBean("channelC"));
		assertThat(TestUtils.getPropertyValue(consumerC, "handler.outputChannelName")).isEqualTo("channelD");

		EventDrivenConsumer consumerD = context.getBean("serviceD", EventDrivenConsumer.class);
		assertThat(TestUtils.getPropertyValue(consumerD, "inputChannel")).isEqualTo(parentChannelA);
		assertThat(TestUtils.getPropertyValue(consumerD, "handler.outputChannelName")).isEqualTo("parentChannelB");

		EventDrivenConsumer consumerE = context.getBean("serviceE", EventDrivenConsumer.class);
		assertThat(TestUtils.getPropertyValue(consumerE, "inputChannel")).isEqualTo(parentChannelB);

		EventDrivenConsumer consumerF = context.getBean("serviceF", EventDrivenConsumer.class);
		assertThat(TestUtils.getPropertyValue(consumerF, "inputChannel")).isEqualTo(channelEarly);

		context.close();
		parentContext.close();
	}


	private static class ThreadNameExtractingTestTarget implements MessageHandler {

		private String threadName;

		private final CountDownLatch latch;


		ThreadNameExtractingTestTarget() {
			this(null);
		}

		ThreadNameExtractingTestTarget(@Nullable CountDownLatch latch) {
			this.latch = latch;
		}

		@Override
		public void handleMessage(Message<?> message) {
			this.threadName = Thread.currentThread().getName();
			if (this.latch != null) {
				this.latch.countDown();
			}
		}

	}

}
