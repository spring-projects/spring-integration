/*
 * Copyright 2002-2012 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
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
	public void testSendInSeparateThread() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		final DirectChannel channel = new DirectChannel();
		ThreadNameExtractingTestTarget target = new ThreadNameExtractingTestTarget(latch);
		channel.subscribe(target);
		final GenericMessage<String> message = new GenericMessage<String>("test");
		new Thread(new Runnable() {
			public void run() {
				channel.send(message);
			}
		}, "test-thread").start();
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals("test-thread", target.threadName);
	}
	
	@Test //  See INT-2434
	public void testChannelCreationWithBeanDefinitionOverrideTrue() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();
		context.setAllowBeanDefinitionOverriding(false);
		context.setConfigLocations(new String[]{"classpath:org/springframework/integration/channel/channel-override-config.xml"});
		Method method = ReflectionUtils.findMethod(ClassPathXmlApplicationContext.class, "obtainFreshBeanFactory"); 
		method.setAccessible(true);
		method.invoke(context);
		assertFalse(context.containsBean("channelA"));
		assertFalse(context.containsBean("channelB"));
		assertTrue(context.containsBean("channelC"));
		assertTrue(context.containsBean("channelD"));
		
		context.refresh();
		
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

		public void handleMessage(Message<?> message) {
			this.threadName = Thread.currentThread().getName();
			if (this.latch != null) {
				this.latch.countDown();
			}
		}
	}

}
