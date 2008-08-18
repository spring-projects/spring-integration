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

package org.springframework.integration.aop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class PublisherAnnotationAdvisorTests {

	@Test
	public void testPublisherAnnotation() {
		final QueueChannel channel = new QueueChannel();
		channel.setBeanName("testChannel");
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel(channel);
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor(channelRegistry);
		TestService proxy = (TestService) this.createProxy(new TestServiceImpl("hello world"), advisor);
		proxy.publisherTest();
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertEquals("hello world", message.getPayload());
	}

	@Test
	public void testNoPublisherAnnotation() {
		final QueueChannel channel = new QueueChannel();
		channel.setBeanName("testChannel");
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel(channel);
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor(channelRegistry);
		TestService proxy = (TestService) this.createProxy(new TestServiceImpl("hello world"), advisor);
		proxy.noPublisherTest();
		Message<?> message = channel.receive(0);
		assertNull(message);
	}

	@Test
	public void testPublishArguments() {
		final QueueChannel channel = new QueueChannel();
		channel.setBeanName("testChannel");
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel(channel);
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor(channelRegistry);
		TestService proxy = (TestService) this.createProxy(new TestServiceImpl("hello world"), advisor);
		proxy.publishArguments("foo", 99);
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof Object[]);
		Object[] args = (Object[]) message.getPayload();
		assertEquals(2, args.length);
		assertEquals("foo", args[0]);
		assertEquals(99, args[1]);
	}

	@Test
	public void testPublishException() {
		final QueueChannel channel = new QueueChannel();
		channel.setBeanName("testChannel");
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel(channel);
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor(channelRegistry);
		TestService proxy = (TestService) this.createProxy(new TestServiceImpl("hello world"), advisor);
		RuntimeException caughtException = null;
		try {
			proxy.publishException();
		}
		catch (RuntimeException e) {
			caughtException = e;
		}
		assertNotNull(caughtException);
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof RuntimeException);
		RuntimeException publishedException = (RuntimeException) message.getPayload();
		assertEquals(caughtException, publishedException);
	}

	@Test
	public void testPublishReturnValue() {
		final QueueChannel channel = new QueueChannel();
		channel.setBeanName("testChannel");
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel(channel);
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor(channelRegistry);
		TestService proxy = (TestService) this.createProxy(new TestServiceImpl("hello world"), advisor);
		Integer actualReturnValue = proxy.publishReturnValue();
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertEquals(actualReturnValue, message.getPayload());
	}


	private Object createProxy(Object target, PublisherAnnotationAdvisor advisor) {
		ProxyFactory factory = new ProxyFactory(target);
		factory.addAdvisor(advisor);
		return factory.getProxy();
	}


	private static interface TestService {

		String publisherTest();

		String noPublisherTest();

		void publishArguments(String s, Integer n);

		Integer publishReturnValue();

		void publishException();

	}


	private static class TestServiceImpl implements TestService {

		private String message;

		public TestServiceImpl(String message) {
			this.message = message;
		}

		@Publisher(channel="testChannel")
		public String publisherTest() {
			return this.message;
		}

		public String noPublisherTest() {
			return this.message;
		}

		@Publisher(channel="testChannel", payloadType=MessagePublishingInterceptor.PayloadType.ARGUMENTS)
		public void publishArguments(String s, Integer n) {
		}

		@Publisher(channel="testChannel", payloadType=MessagePublishingInterceptor.PayloadType.EXCEPTION)
		public void publishException() {
			throw new RuntimeException("test failure");
		}

		@Publisher(channel="testChannel", payloadType=MessagePublishingInterceptor.PayloadType.RETURN_VALUE)
		public Integer publishReturnValue() {
			return 123;
		}

	}

}
