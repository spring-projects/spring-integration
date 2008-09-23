/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.bus.DefaultMessageBus;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.ServiceActivatorEndpoint;
import org.springframework.integration.handler.TestSink;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.MethodInvokingConsumer;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class MethodInvokingConsumerTests {

	@Test
	public void testValidMethod() {
		MethodInvokingConsumer consumer = new MethodInvokingConsumer(new TestSink(), "validMethod");
		consumer.afterPropertiesSet();
		consumer.onMessage(new GenericMessage<String>("test"));
	}

	@Test(expected = ConfigurationException.class)
	public void testInvalidMethodWithNoArgs() {
		MethodInvokingConsumer consumer = new MethodInvokingConsumer(new TestSink(), "invalidMethodWithNoArgs");
		consumer.afterPropertiesSet();
	}

	@Test(expected = MessagingException.class)
	public void testMethodWithReturnValue() {
		Message<?> message = new StringMessage("test");
		try {
			MethodInvokingConsumer consumer = new MethodInvokingConsumer(new TestSink(), "methodWithReturnValue");
			consumer.afterPropertiesSet();
			consumer.onMessage(message);
		}
		catch (MessagingException e) {
			assertEquals(e.getFailedMessage(), message);
			throw e;
		}
	}

	@Test(expected = ConfigurationException.class)
	public void testNoMatchingMethodName() {
		MethodInvokingConsumer consumer = new MethodInvokingConsumer(new TestSink(), "noSuchMethod");
		consumer.afterPropertiesSet();
	}

	@Test
	public void testSubscription() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		SynchronousQueue<String> queue = new SynchronousQueue<String>();
		TestBean testBean = new TestBean(queue);
		MethodInvokingConsumer consumer = new MethodInvokingConsumer(testBean, "foo");
		consumer.afterPropertiesSet();
		QueueChannel channel = new QueueChannel();
		channel.setBeanName("channel");
		context.getBeanFactory().registerSingleton("channel", channel);
		Message<String> message = new GenericMessage<String>("testing");
		channel.send(message);
		assertNull(queue.poll());
		ServiceActivatorEndpoint endpoint = new ServiceActivatorEndpoint(consumer);
		endpoint.setBeanName("testEndpoint");
		endpoint.setInputChannel(channel);
		context.getBeanFactory().registerSingleton("testEndpoint", endpoint);
		DefaultMessageBus bus = new DefaultMessageBus();
		bus.setApplicationContext(context);
		bus.start();
		String result = queue.poll(1000, TimeUnit.MILLISECONDS);
		assertNotNull(result);
		assertEquals("testing", result);
		bus.stop();
	}


	public static class TestBean {

		private BlockingQueue<String> queue;

		public TestBean(BlockingQueue<String> queue) {
			this.queue = queue;
		}

		public void foo(String s) {
			try {
				this.queue.put(s);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
