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

package org.springframework.integration.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.MethodInvokingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class MethodInvokingMessageHandlerTests {

	@Test
	public void validMethod() {
		MethodInvokingMessageHandler handler = new MethodInvokingMessageHandler(new TestSink(), "validMethod");
		handler.handleMessage(new GenericMessage<String>("test"));
	}

	@Test
	public void validMethodWithNoArgs() {
		new MethodInvokingMessageHandler(new TestSink(), "validMethodWithNoArgs");
	}

	@Test(expected = MessagingException.class)
	public void methodWithReturnValue() {
		Message<?> message = new GenericMessage<String>("test");
		try {
			MethodInvokingMessageHandler handler = new MethodInvokingMessageHandler(new TestSink(), "methodWithReturnValue");
			handler.handleMessage(message);
		}
		catch (MessagingException e) {
			assertEquals(e.getFailedMessage(), message);
			throw e;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void noMatchingMethodName() {
		new MethodInvokingMessageHandler(new TestSink(), "noSuchMethod");
	}

	@Test
	public void subscription() throws Exception {
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		SynchronousQueue<String> queue = new SynchronousQueue<String>();
		TestBean testBean = new TestBean(queue);
		QueueChannel channel = new QueueChannel();
		context.registerChannel("channel", channel);
		Message<String> message = new GenericMessage<String>("testing");
		channel.send(message);
		assertNull(queue.poll());
		MethodInvokingMessageHandler handler = new MethodInvokingMessageHandler(testBean, "foo");
		PollingConsumer endpoint = new PollingConsumer(channel, handler);
		endpoint.setTrigger(new PeriodicTrigger(10));
		context.registerEndpoint("testEndpoint", endpoint);
		context.refresh();
		String result = queue.poll(2000, TimeUnit.MILLISECONDS);
		assertNotNull(result);
		assertEquals("testing", result);
		context.stop();
	}


	private static class TestBean {

		private final BlockingQueue<String> queue;

		public TestBean(BlockingQueue<String> queue) {
			this.queue = queue;
		}

		@SuppressWarnings("unused")
		public void foo(String s) {
			try {
				this.queue.put(s);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}


	@SuppressWarnings("unused")
	private static class TestSink {

		private String result;


		public void validMethod(String s) {
		}

		public void validMethodWithNoArgs() {
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
