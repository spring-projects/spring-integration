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

import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class MessagePublishingInterceptorTests {

	@Test
	public void testNonNullReturnValuePublishedWithDefaultChannel() {
		QueueChannel channel = new QueueChannel();
		MessagePublishingInterceptor interceptor = new MessagePublishingInterceptor();
		interceptor.setOutputChannel(channel);
		TestService proxy = (TestService) this.createProxy(new TestServiceImpl("hello world"), interceptor);
		proxy.messageTest();
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertEquals("hello world", message.getPayload());
	}

	@Test
	public void testNullReturnValueNotPublished() {
		QueueChannel channel = new QueueChannel();
		MessagePublishingInterceptor interceptor = new MessagePublishingInterceptor();
		interceptor.setOutputChannel(channel);
		TestService proxy = (TestService) this.createProxy(new TestServiceImpl(null), interceptor);
		proxy.messageTest();
		assertNull(channel.receive(0));
	}

	@Test
	public void testVoidReturnValueNotPublished() {
		QueueChannel channel = new QueueChannel();
		MessagePublishingInterceptor interceptor = new MessagePublishingInterceptor();
		interceptor.setOutputChannel(channel);
		TestService proxy = (TestService) this.createProxy(new TestServiceImpl(null), interceptor);
		proxy.voidTest();
		assertNull(channel.receive(0));
	}


	private Object createProxy(Object target, MessagePublishingInterceptor interceptor) {
		ProxyFactory factory = new ProxyFactory(target);
		factory.addAdvice(interceptor);
		return factory.getProxy();
	}


	private static interface TestService {
		String messageTest();
		void voidTest();
	}


	private static class TestServiceImpl implements TestService {

		private String message;

		public TestServiceImpl(String message) {
			this.message = message;
		}

		public String messageTest() {
			return this.message;
		}

		public void voidTest() {
			return;
		}

	}

}
