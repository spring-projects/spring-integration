/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.channel.reactive;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ReactiveChannel;
import org.springframework.integration.endpoint.ReactiveEndpoint;
import org.springframework.integration.handler.MethodInvokingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

import reactor.core.publisher.EmitterProcessor;
import reactor.core.test.TestSubscriber;

/**
 * @author Artem Bilan
 * @since 5.0
 */
public class ReactiveEndpointTests {

	@Test
	public void testReactiveEndpointReactiveChannel() throws InterruptedException {
		ReactiveChannel testChannel =
				new ReactiveChannel(EmitterProcessor.create(false));

		List<Message<?>> result = new LinkedList<>();
		CountDownLatch stopLatch = new CountDownLatch(2);

		MessageHandler messageHandler = m -> {
			result.add(m);
			stopLatch.countDown();
		};

		MethodInvokingMessageHandler testSubscriber = new MethodInvokingMessageHandler(messageHandler, (String) null);

		ReactiveEndpoint reactiveEndpoint = new ReactiveEndpoint(testChannel, testSubscriber);
		reactiveEndpoint.setBeanFactory(mock(BeanFactory.class));
		reactiveEndpoint.afterPropertiesSet();
		reactiveEndpoint.start();

		Message<?> testMessage = new GenericMessage<>("test");
		testChannel.send(testMessage);

		reactiveEndpoint.stop();

		testChannel.send(testMessage);

		reactiveEndpoint.start();

		Message<?> testMessage2 = new GenericMessage<>("test2");
		testChannel.send(testMessage2);

		assertTrue(stopLatch.await(10, TimeUnit.SECONDS));
		assertThat(result, Matchers.<Message<?>>contains(testMessage, testMessage2));
	}


	@Test
	public void testReactiveEndpointDirectChannel() {
		DirectChannel testChannel = new DirectChannel();

		TestSubscriber<Message<?>> testSubscriber = new TestSubscriber<>();

		ReactiveEndpoint reactiveEndpoint = new ReactiveEndpoint(testChannel, testSubscriber);
		reactiveEndpoint.setBeanFactory(mock(BeanFactory.class));
		reactiveEndpoint.afterPropertiesSet();
		reactiveEndpoint.start();

		Message<?> testMessage = new GenericMessage<>("test");
		testChannel.send(testMessage);

		testSubscriber.assertSubscribed();
		testSubscriber.assertNoError();
		testSubscriber.assertNotComplete();

		testSubscriber.assertValues(testMessage);

		reactiveEndpoint.stop();

		try {
			testChannel.send(testMessage);
			fail("MessageDeliveryException");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageDeliveryException.class));
		}

		new DirectFieldAccessor(testSubscriber).setPropertyValue("s", null);
		TestUtils.getPropertyValue(testSubscriber, "values", List.class).clear();

		reactiveEndpoint.start();

		testSubscriber.request(1);

		testMessage = new GenericMessage<>("test2");

		testChannel.send(testMessage);

		testSubscriber.assertValues(testMessage);

		testChannel.send(testMessage);

		testSubscriber.assertError(IllegalStateException.class);
		testSubscriber.assertErrorMessage("Can't deliver value due to lack of requests");

	}

}
