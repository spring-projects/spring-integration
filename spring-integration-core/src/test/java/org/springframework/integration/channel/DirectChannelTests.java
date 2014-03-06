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

package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.ReflectionUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
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
	public void testSendPerfOneHandler() {
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
		channel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				count.incrementAndGet();
			}
		});
		GenericMessage<String> message = new GenericMessage<String>("test");
		assertTrue(channel.send(message));
		for (int i = 0; i < 10000000; i++) {
			channel.send(message);
		}
	}

	@Test
	public void testSendPerfTwoHandlers() {
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
		channel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				count1.incrementAndGet();
			}
		});
		channel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				count2.getAndIncrement();
			}
		});
		GenericMessage<String> message = new GenericMessage<String>("test");
		assertTrue(channel.send(message));
		for (int i = 0; i < 10000000; i++) {
			channel.send(message);
		}
		assertEquals(5000001, count1.get());
		assertEquals(5000000, count2.get());
	}

	@Test
	public void testSendPerfFixedSubscriberChannel() {
		/*
		 *  INT-3308 - 96 million/sec
		 *  NOTE: in order to get a measurable time, I had to add some code to the handler -
		 *  presumably the JIT compiler short circuited the call becaues it's a final field
		 *  and he knows the method does nothing.
		 *  Added the same code to the other tests for comparison.
		 */
		final AtomicInteger count = new AtomicInteger();
		FixedSubscriberChannel channel = new FixedSubscriberChannel(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				count.incrementAndGet();
			}
		});
		GenericMessage<String> message = new GenericMessage<String>("test");
		assertTrue(channel.send(message));
		for (int i = 0; i < 100000000; i++) {
			channel.send(message, 0);
		}
	}

	@Test
	public void testSendInSeparateThread() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		final DirectChannel channel = new DirectChannel();
		ThreadNameExtractingTestTarget target = new ThreadNameExtractingTestTarget(latch);
		channel.subscribe(target);
		final GenericMessage<String> message = new GenericMessage<String>("test");
		new Thread(new Runnable() {
			@Override
			public void run() {
				channel.send(message);
			}
		}, "test-thread").start();
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals("test-thread", target.threadName);
	}

	@Test //  See INT-2434
	public void testChannelCreationWithBeanDefinitionOverrideTrue() throws Exception {
		ClassPathXmlApplicationContext parentContext = new ClassPathXmlApplicationContext("parent-config.xml", this.getClass());
		MessageChannel parentChannelA = parentContext.getBean("parentChannelA", MessageChannel.class);
		MessageChannel parentChannelB = parentContext.getBean("parentChannelB", MessageChannel.class);

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();
		context.setAllowBeanDefinitionOverriding(false);
		context.setConfigLocations(new String[]{"classpath:org/springframework/integration/channel/channel-override-config.xml"});
		context.setParent(parentContext);
		Method method = ReflectionUtils.findMethod(ClassPathXmlApplicationContext.class, "obtainFreshBeanFactory");
		method.setAccessible(true);
		method.invoke(context);
		assertFalse(context.containsBean("channelA"));
		assertFalse(context.containsBean("channelB"));
		assertTrue(context.containsBean("channelC"));
		assertTrue(context.containsBean("channelD"));

		context.refresh();

		PublishSubscribeChannel channelEarly = context.getBean("channelEarly", PublishSubscribeChannel.class);

		assertTrue(context.containsBean("channelA"));
		assertTrue(context.containsBean("channelB"));
		assertTrue(context.containsBean("channelC"));
		assertTrue(context.containsBean("channelD"));
		EventDrivenConsumer consumerA = context.getBean("serviceA", EventDrivenConsumer.class);
		assertEquals(context.getBean("channelA"), TestUtils.getPropertyValue(consumerA, "inputChannel"));
		assertEquals(context.getBean("channelB"), TestUtils.getPropertyValue(consumerA, "handler.outputChannel"));

		EventDrivenConsumer consumerB = context.getBean("serviceB", EventDrivenConsumer.class);
		assertEquals(context.getBean("channelB"), TestUtils.getPropertyValue(consumerB, "inputChannel"));
		assertEquals(context.getBean("channelC"), TestUtils.getPropertyValue(consumerB, "handler.outputChannel"));

		EventDrivenConsumer consumerC = context.getBean("serviceC", EventDrivenConsumer.class);
		assertEquals(context.getBean("channelC"), TestUtils.getPropertyValue(consumerC, "inputChannel"));
		assertEquals(context.getBean("channelD"), TestUtils.getPropertyValue(consumerC, "handler.outputChannel"));

		EventDrivenConsumer consumerD = context.getBean("serviceD", EventDrivenConsumer.class);
		assertEquals(parentChannelA, TestUtils.getPropertyValue(consumerD, "inputChannel"));
		assertEquals(parentChannelB, TestUtils.getPropertyValue(consumerD, "handler.outputChannel"));

		EventDrivenConsumer consumerE = context.getBean("serviceE", EventDrivenConsumer.class);
		assertEquals(parentChannelB, TestUtils.getPropertyValue(consumerE, "inputChannel"));

		EventDrivenConsumer consumerF = context.getBean("serviceF", EventDrivenConsumer.class);
		assertEquals(channelEarly, TestUtils.getPropertyValue(consumerF, "inputChannel"));
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

		@Override
		public void handleMessage(Message<?> message) {
			this.threadName = Thread.currentThread().getName();
			if (this.latch != null) {
				this.latch.countDown();
			}
		}
	}

}
