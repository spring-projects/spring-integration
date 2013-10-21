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

package org.springframework.integration.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
public class AsyncGatewayTests {

	// TODO: changed from 0 because of recurrent failure: is this right?
	private final long safety = 100;

	@Test
	public void futureWithMessageReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		Future<Message<?>> f = service.returnMessage("foo");
		long start = System.currentTimeMillis();
		Object result = f.get(1000, TimeUnit.MILLISECONDS);
		long elapsed = System.currentTimeMillis() - start;
		assertTrue(elapsed >= 200);
		assertTrue(result instanceof Message<?>);
		assertEquals("foobar", ((Message<?>) result).getPayload());
	}

	@Test
	public void futureWithPayloadReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		Future<String> f = service.returnString("foo");
		long start = System.currentTimeMillis();
		Object result = f.get(1000, TimeUnit.MILLISECONDS);
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200-safety);
		assertTrue(result instanceof String);
		assertEquals("foobar", result);
	}

	@Test
	public void futureWithWildcardReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		Future<?> f = service.returnSomething("foo");
		long start = System.currentTimeMillis();
		Object result = f.get(1000, TimeUnit.MILLISECONDS);
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200-safety);
		assertTrue(result instanceof String);
		assertEquals("foobar", result);
	}


	private static void startResponder(final PollableChannel requestChannel) {
		new Thread(new Runnable() {
			public void run() {
				Message<?> input = requestChannel.receive();
				GenericMessage<String> reply = new GenericMessage<String>(input.getPayload() + "bar");
				try {
					Thread.sleep(200);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				((MessageChannel) input.getHeaders().getReplyChannel()).send(reply);
			}
		}).start();
	}


	static interface TestEchoService {

		Future<String> returnString(String s);

		Future<Message<?>> returnMessage(String s);

		Future<?> returnSomething(String s);

	}

}
