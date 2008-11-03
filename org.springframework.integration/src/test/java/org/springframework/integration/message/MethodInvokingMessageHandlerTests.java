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
import org.springframework.integration.bus.ApplicationContextMessageBus;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.consumer.MethodInvokingMessageHandler;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.endpoint.PollingConsumerEndpoint;
import org.springframework.integration.util.TestUtils;

/**
 * @author Mark Fisher
 */
public class MethodInvokingMessageHandlerTests {

	@Test
	public void validMethod() {
		MethodInvokingMessageHandler handler = new MethodInvokingMessageHandler(new TestSink(), "validMethod");
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<String>("test"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidMethodWithNoArgs() {
		MethodInvokingMessageHandler handler = new MethodInvokingMessageHandler(new TestSink(), "invalidMethodWithNoArgs");
		handler.afterPropertiesSet();
	}

	@Test(expected = MessagingException.class)
	public void methodWithReturnValue() {
		Message<?> message = new StringMessage("test");
		try {
			MethodInvokingMessageHandler handler = new MethodInvokingMessageHandler(new TestSink(), "methodWithReturnValue");
			handler.afterPropertiesSet();
			handler.handleMessage(message);
		}
		catch (MessagingException e) {
			assertEquals(e.getFailedMessage(), message);
			throw e;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void noMatchingMethodName() {
		MethodInvokingMessageHandler handler = new MethodInvokingMessageHandler(new TestSink(), "noSuchMethod");
		handler.afterPropertiesSet();
	}

	@Test
	public void subscription() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		SynchronousQueue<String> queue = new SynchronousQueue<String>();
		TestBean testBean = new TestBean(queue);
		QueueChannel channel = new QueueChannel();
		channel.setBeanName("channel");
		context.getBeanFactory().registerSingleton("channel", channel);
		Message<String> message = new GenericMessage<String>("testing");
		channel.send(message);
		assertNull(queue.poll());
		MethodInvokingMessageHandler handler = new MethodInvokingMessageHandler(testBean, "foo");
		PollingConsumerEndpoint endpoint = new PollingConsumerEndpoint(channel, handler);
		context.getBeanFactory().registerSingleton("testEndpoint", endpoint);
		ApplicationContextMessageBus bus = new ApplicationContextMessageBus();
		bus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		bus.setApplicationContext(context);
		context.refresh();
		bus.start();
		String result = queue.poll(1000, TimeUnit.MILLISECONDS);
		assertNotNull(result);
		assertEquals("testing", result);
		bus.stop();
	}


	private static class TestBean {

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


	private static class TestSink {

		private String result;


		public void validMethod(String s) {
		}

		public void invalidMethodWithNoArgs() {
		}

		public String methodWithReturnValue(String s) {
			return "value";
		}

		public void store(String s) {
			this.result = s;
		}

		public String get() {
			return this.result;
		}
	}

}
