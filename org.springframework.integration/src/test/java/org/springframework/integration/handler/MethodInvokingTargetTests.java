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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.bus.DefaultMessageBus;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.SimpleEndpoint;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class MethodInvokingTargetTests {

	@Test
	public void testValidMethod() {
		MethodInvokingTarget target = new MethodInvokingTarget();
		target.setObject(new TestSink());
		target.setMethodName("validMethod");
		target.afterPropertiesSet();
		boolean result = target.send(new GenericMessage<String>("test"));
		assertTrue(result);
	}

	@Test(expected = ConfigurationException.class)
	public void testInvalidMethodWithNoArgs() {
		MethodInvokingTarget target = new MethodInvokingTarget();
		target.setObject(new TestSink());
		target.setMethodName("invalidMethodWithNoArgs");
		target.afterPropertiesSet();
	}

	@Test(expected = MessagingException.class)
	public void testMethodWithReturnValue() {
		Message<?> message = new StringMessage("test");
		try {
			MethodInvokingTarget target = new MethodInvokingTarget();
			target.setObject(new TestSink());
			target.setMethodName("methodWithReturnValue");
			target.afterPropertiesSet();
			target.send(message);
		}
		catch (MessagingException e) {
			assertEquals(e.getFailedMessage(), message);
			throw e;
		}
	}

	@Test(expected = ConfigurationException.class)
	public void testNoMatchingMethodName() {
		MethodInvokingTarget target = new MethodInvokingTarget();
		target.setObject(new TestSink());
		target.setMethodName("noSuchMethod");
		target.afterPropertiesSet();
	}

	@Test
	public void testSubscription() throws Exception {
		SynchronousQueue<String> queue = new SynchronousQueue<String>();
		TestBean testBean = new TestBean(queue);
		MethodInvokingTarget target = new MethodInvokingTarget();
		target.setObject(testBean);
		target.setMethodName("foo");
		target.afterPropertiesSet();
		QueueChannel channel = new QueueChannel();
		Message<String> message = new GenericMessage<String>("testing");
		channel.send(message);
		assertNull(queue.poll());
		MessageBus bus = new DefaultMessageBus();
		bus.registerChannel("channel", channel);
		MessageEndpoint endpoint = new SimpleEndpoint<MethodInvokingTarget>(target);
		endpoint.setBeanName("testEndpoint");
		endpoint.setSource(channel);
		bus.registerEndpoint(endpoint);
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
