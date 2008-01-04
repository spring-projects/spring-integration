/*
 * Copyright 2002-2007 the original author or authors.
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

import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PointToPointChannel;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class PublisherAnnotationAdvisorTests {

	@Test
	public void testPublisherAnnotation() {
		final MessageChannel channel = new PointToPointChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("testChannel", channel);
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor(channelRegistry);
		TestService proxy = (TestService) this.createProxy(new TestServiceImpl("hello world"), advisor);
		proxy.publisherTest();
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertEquals("hello world", message.getPayload());
	}

	@Test
	public void testNoPublisherAnnotation() {
		final MessageChannel channel = new PointToPointChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("testChannel", channel);
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor(channelRegistry);
		TestService proxy = (TestService) this.createProxy(new TestServiceImpl("hello world"), advisor);
		proxy.noPublisherTest();
		Message<?> message = channel.receive(0);
		assertNull(message);
	}


	private Object createProxy(Object target, PublisherAnnotationAdvisor advisor) {
		ProxyFactory factory = new ProxyFactory(target);
		factory.addAdvisor(advisor);
		return factory.getProxy();
	}


	private static interface TestService {

		String publisherTest();

		String noPublisherTest();
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

	}

}
