/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.channel.config;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Executor;

import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.TestChannelInterceptor;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.PollableChannel;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gunnar Hillert
 *
 * @see ChannelWithCustomQueueParserTests
 */
public class ChannelParserTests {

	@Test(expected = FatalBeanException.class)
	public void testChannelWithoutId() {
		new ClassPathXmlApplicationContext("channelWithoutId.xml", this.getClass());
	}

	@Test
	public void testChannelWithCapacity() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("channelParserTests.xml", this
				.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("capacityChannel");
		for (int i = 0; i < 10; i++) {
			boolean result = channel.send(new GenericMessage<String>("test"), 10);
			assertTrue(result);
		}
		assertFalse(channel.send(new GenericMessage<String>("test"), 3));
	}

	@Test
	public void testDirectChannelByDefault() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("channelParserTests.xml", this
				.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("defaultChannel");
		assertEquals(DirectChannel.class, channel.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		Object dispatcher = accessor.getPropertyValue("dispatcher");
		assertThat(dispatcher, is(instanceOf(UnicastingDispatcher.class)));
		assertThat(new DirectFieldAccessor(dispatcher).getPropertyValue("loadBalancingStrategy"),
				is(instanceOf(RoundRobinLoadBalancingStrategy.class)));
	}

	@Test
	public void channelWithFailoverDispatcherAttribute() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("channelParserTests.xml", this
				.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("channelWithFailoverAttribute");
		assertEquals(DirectChannel.class, channel.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		Object dispatcher = accessor.getPropertyValue("dispatcher");
		assertThat(dispatcher, is(instanceOf(UnicastingDispatcher.class)));
		assertNull(new DirectFieldAccessor(dispatcher).getPropertyValue("loadBalancingStrategy"));
	}

	@Test
	public void testPublishSubscribeChannel() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("channelParserTests.xml", this
				.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("publishSubscribeChannel");
		assertEquals(PublishSubscribeChannel.class, channel.getClass());
	}

	@Test
	public void testPublishSubscribeChannelWithTaskExecutorReference() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("channelParserTests.xml", this
				.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("publishSubscribeChannelWithTaskExecutorRef");
		assertEquals(PublishSubscribeChannel.class, channel.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("dispatcher"));
		Object executorProperty = accessor.getPropertyValue("executor");
		assertNotNull(executorProperty);
		assertEquals(ErrorHandlingTaskExecutor.class, executorProperty.getClass());
		DirectFieldAccessor executorAccessor = new DirectFieldAccessor(executorProperty);
		Executor innerExecutor = (Executor) executorAccessor.getPropertyValue("executor");
		Object executorBean = context.getBean("taskExecutor");
		assertEquals(executorBean, innerExecutor);
	}

	@Test
	public void channelWithCustomQueue() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"channelParserTests.xml", this.getClass());
		Object customQueue = context.getBean("customQueue");
		Object channelWithCustomQueue = context.getBean("channelWithCustomQueue");
		assertEquals(QueueChannel.class, channelWithCustomQueue.getClass());
		Object actualQueue = new DirectFieldAccessor(channelWithCustomQueue).getPropertyValue("queue");
		assertSame(customQueue, actualQueue);
	}

	@Test
	public void testDatatypeChannelWithCorrectType() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("channelParserTests.xml", this
				.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("integerChannel");
		assertTrue(channel.send(new GenericMessage<Integer>(123)));
	}

	@Test(expected = MessageDeliveryException.class)
	public void testDatatypeChannelWithIncorrectType() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("channelParserTests.xml", this
				.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("integerChannel");
		channel.send(new GenericMessage<String>("incorrect type"));
	}

	@Test
	public void testDatatypeChannelWithAssignableSubTypes() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("channelParserTests.xml", this
				.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("numberChannel");
		assertTrue(channel.send(new GenericMessage<Integer>(123)));
		assertTrue(channel.send(new GenericMessage<Double>(123.45)));
	}

	@Test
	public void testMultipleDatatypeChannelWithCorrectTypes() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("channelParserTests.xml", this
				.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("stringOrNumberChannel");
		assertTrue(channel.send(new GenericMessage<Integer>(123)));
		assertTrue(channel.send(new GenericMessage<String>("accepted type")));
	}

	@Test(expected = MessageDeliveryException.class)
	public void testMultipleDatatypeChannelWithIncorrectType() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("channelParserTests.xml", this
				.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("stringOrNumberChannel");
		channel.send(new GenericMessage<Boolean>(true));
	}

	@Test
	public void testChannelInteceptorRef() {
		ApplicationContext context = new ClassPathXmlApplicationContext("channelInterceptorParserTests.xml", this
				.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("channelWithInterceptorRef");
		TestChannelInterceptor interceptor = (TestChannelInterceptor) context.getBean("interceptor");
		assertEquals(0, interceptor.getSendCount());
		channel.send(new GenericMessage<String>("test"));
		assertEquals(1, interceptor.getSendCount());
		assertEquals(0, interceptor.getReceiveCount());
		channel.receive();
		assertEquals(1, interceptor.getReceiveCount());
	}

	@Test
	public void testChannelInteceptorInnerBean() {
		ApplicationContext context = new ClassPathXmlApplicationContext("channelInterceptorParserTests.xml", this
				.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("channelWithInterceptorInnerBean");
		channel.send(new GenericMessage<String>("test"));
		Message<?> transformed = channel.receive(1000);
		assertEquals("TEST", transformed.getPayload());
	}

	@Test
	public void testPriorityChannelWithDefaultComparator() {
		ApplicationContext context = new ClassPathXmlApplicationContext("priorityChannelParserTests.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("priorityChannelWithDefaultComparator");
		Message<String> lowPriorityMessage = MessageBuilder.withPayload("low").setPriority(-14).build();
		Message<String> midPriorityMessage = MessageBuilder.withPayload("mid").setPriority(0).build();
		Message<String> highPriorityMessage = MessageBuilder.withPayload("high").setPriority(99).build();
		channel.send(lowPriorityMessage);
		channel.send(highPriorityMessage);
		channel.send(midPriorityMessage);
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		Message<?> reply3 = channel.receive(0);
		assertEquals("high", reply1.getPayload());
		assertEquals("mid", reply2.getPayload());
		assertEquals("low", reply3.getPayload());
	}

	@Test
	public void testPriorityChannelWithCustomComparator() {
		ApplicationContext context = new ClassPathXmlApplicationContext("priorityChannelParserTests.xml", this
				.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("priorityChannelWithCustomComparator");
		channel.send(new GenericMessage<String>("C"));
		channel.send(new GenericMessage<String>("A"));
		channel.send(new GenericMessage<String>("D"));
		channel.send(new GenericMessage<String>("B"));
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		Message<?> reply3 = channel.receive(0);
		Message<?> reply4 = channel.receive(0);
		assertEquals("A", reply1.getPayload());
		assertEquals("B", reply2.getPayload());
		assertEquals("C", reply3.getPayload());
		assertEquals("D", reply4.getPayload());
	}

	@Test
	public void testPriorityChannelWithIntegerDatatypeEnforced() {
		ApplicationContext context = new ClassPathXmlApplicationContext("priorityChannelParserTests.xml", this
				.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("integerOnlyPriorityChannel");
		channel.send(new GenericMessage<Integer>(3));
		channel.send(new GenericMessage<Integer>(2));
		channel.send(new GenericMessage<Integer>(1));
		assertEquals(1, channel.receive(0).getPayload());
		assertEquals(2, channel.receive(0).getPayload());
		assertEquals(3, channel.receive(0).getPayload());
		boolean threwException = false;
		try {
			channel.send(new GenericMessage<String>("wrong type"));
		}
		catch (MessageDeliveryException e) {
			assertEquals("wrong type", e.getFailedMessage().getPayload());
			threwException = true;
		}
		assertTrue(threwException);
	}

}
