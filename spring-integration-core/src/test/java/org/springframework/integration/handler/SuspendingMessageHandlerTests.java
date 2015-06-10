/*
 * Copyright 2015 the original author or authors.
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.SuspendingMessageHandler.ReleasingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;


/**
 * @author Gary Russell
 * @since 4.2
 *
 */
public class SuspendingMessageHandlerTests {

	@Test
	public void testRequestBeforeReply() throws Exception {
		ReleasingMessageHandler releaser = new ReleasingMessageHandler();
		final SuspendingMessageHandler handler = new SuspendingMessageHandler(releaser, 10000);
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
				handler.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("foo").build());
			}
		});
		Map<?, ?> suspensions = TestUtils.getPropertyValue(handler, "suspensions", Map.class);
		int n = 0;
		while (n++ < 100 && suspensions.size() == 0) {
			Thread.sleep(100);
		}
		assertTrue("suspension did not appear in time", n < 100);
		releaser.handleMessage(MessageBuilder.withPayload("bar").setCorrelationId("foo").build());
		Message<?> received = outputChannel.receive(10000);
		assertNotNull(received);
		Object[] result = (Object[]) received.getPayload();
		assertEquals("foo", result[0]);
		assertEquals("bar", ((Message<?>) result[1]).getPayload());
		assertEquals(0, suspensions.size());
	}

	@Test
	public void testReplyBeforeRequest() throws Exception {
		ReleasingMessageHandler releaser = new ReleasingMessageHandler();
		final SuspendingMessageHandler handler = new SuspendingMessageHandler(releaser, 10000);
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
				ReleasingMessageHandler releaser = TestUtils.getPropertyValue(handler, "releasingHandler",
						ReleasingMessageHandler.class);
				releaser.handleMessage(MessageBuilder.withPayload("bar").setCorrelationId("foo").build());
			}
		});
		Map<?, ?> suspensions = TestUtils.getPropertyValue(handler, "suspensions", Map.class);
		int n = 0;
		while (n++ < 100 && suspensions.size() == 0) {
			Thread.sleep(100);
		}
		assertTrue("suspension did not appear in time", n < 100);
		handler.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("foo").build());
		Message<?> received = outputChannel.receive(10000);
		assertNotNull(received);
		Object[] result = (Object[]) received.getPayload();
		assertEquals("foo", result[0]);
		assertEquals("bar", ((Message<?>) result[1]).getPayload());
		assertEquals(0, suspensions.size());
	}

	@Test
	public void testLateReply() throws Exception {
		ReleasingMessageHandler releaser = new ReleasingMessageHandler();
		final SuspendingMessageHandler handler = new SuspendingMessageHandler(releaser, 0);
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
				handler.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("foo").build());
			}
		});
		Map<?, ?> suspensions = TestUtils.getPropertyValue(handler, "suspensions", Map.class);
		int n = 0;
		while (n++ < 100 && suspensions.size() == 0) {
			Thread.sleep(100);
		}
		assertTrue("suspension did not appear in time", n < 100);
		Log logger = spy(TestUtils.getPropertyValue(handler, "releasingHandler.logger", Log.class));
		new DirectFieldAccessor(releaser).setPropertyValue("logger", logger);
		releaser.handleMessage(MessageBuilder.withPayload("bar").setCorrelationId("foo").build());
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(logger).error(captor.capture());
		assertThat(captor.getValue(),
				Matchers.allOf(containsString("Release message arrived too late"),
						containsString("payload=foo"), containsString("payload=bar")));
		assertEquals(0, suspensions.size());
	}

	@Test
	public void testExceptionReply() throws Exception {
		ReleasingMessageHandler releaser = new ReleasingMessageHandler();
		final SuspendingMessageHandler handler = new SuspendingMessageHandler(releaser, 10000);
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		final AtomicReference<Exception> exception = new AtomicReference<Exception>();
		final CountDownLatch latch = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
				try {
					handler.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("foo").build());
				}
				catch (Exception e) {
					exception.set(e);
					latch.countDown();
				}
			}
		});
		Map<?, ?> suspensions = TestUtils.getPropertyValue(handler, "suspensions", Map.class);
		int n = 0;
		while (n++ < 100 && suspensions.size() == 0) {
			Thread.sleep(100);
		}
		assertTrue("suspension did not appear in time", n < 100);
		Exception exc = new RuntimeException();
		releaser.handleMessage(MessageBuilder.withPayload(exc).setCorrelationId("foo").build());
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertSame(exc, exception.get().getCause());
		assertEquals(0, suspensions.size());
	}

}
