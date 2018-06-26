/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.integration.aggregator;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnablePublisher;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class BarrierMessageHandlerTests {

	@Autowired
	private MessageChannel in;

	@Autowired
	private PollableChannel out;

	@Autowired
	private MessageChannel release;

	@Autowired
	private PollableChannel publisherChannel;

	@Test
	public void testRequestBeforeReply() throws Exception {
		final BarrierMessageHandler handler = new BarrierMessageHandler(10000);
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		final AtomicReference<Exception> dupCorrelation = new AtomicReference<Exception>();
		final CountDownLatch latch = new CountDownLatch(1);
		Runnable runnable = () -> {
			try {
				handler.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("foo").build());
			}
			catch (MessagingException e) {
				dupCorrelation.set(e);
			}
			latch.countDown();
		};
		ExecutorService exec = Executors.newCachedThreadPool();
		exec.execute(runnable);
		exec.execute(runnable);
		Map<?, ?> suspensions = TestUtils.getPropertyValue(handler, "suspensions", Map.class);
		int n = 0;
		while (n++ < 100 && suspensions.size() == 0) {
			Thread.sleep(100);
		}
		Map<?, ?> inProcess = TestUtils.getPropertyValue(handler, "inProcess", Map.class);
		assertEquals(1, inProcess.size());
		assertTrue("suspension did not appear in time", n < 100);
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertNotNull(dupCorrelation.get());
		assertThat(dupCorrelation.get().getMessage(), startsWith("Correlation key (foo) is already in use by"));
		handler.trigger(MessageBuilder.withPayload("bar").setCorrelationId("foo").build());
		Message<?> received = outputChannel.receive(10000);
		assertNotNull(received);
		List<?> result = (List<?>) received.getPayload();
		assertEquals("foo", result.get(0));
		assertEquals("bar", result.get(1));
		assertEquals(0, suspensions.size());
		assertEquals(0, inProcess.size());
		exec.shutdownNow();
	}

	@Test
	public void testReplyBeforeRequest() throws Exception {
		final BarrierMessageHandler handler = new BarrierMessageHandler(10000);
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> handler.trigger(MessageBuilder.withPayload("bar").setCorrelationId("foo").build()));
		Map<?, ?> suspensions = TestUtils.getPropertyValue(handler, "suspensions", Map.class);
		int n = 0;
		while (n++ < 100 && suspensions.size() == 0) {
			Thread.sleep(100);
		}
		assertTrue("suspension did not appear in time", n < 100);
		handler.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("foo").build());
		Message<?> received = outputChannel.receive(10000);
		assertNotNull(received);
		List<?> result = (ArrayList<?>) received.getPayload();
		assertEquals("foo", result.get(0));
		assertEquals("bar", result.get(1));
		assertEquals(0, suspensions.size());
		exec.shutdownNow();
	}

	@Test
	public void testLateReply() throws Exception {
		final BarrierMessageHandler handler = new BarrierMessageHandler(0);
		QueueChannel outputChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		handler.setDiscardChannelName("discards");
		handler.setChannelResolver(s -> discardChannel);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		final CountDownLatch latch = new CountDownLatch(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			handler.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("foo").build());
			latch.countDown();
		});
		Map<?, ?> suspensions = TestUtils.getPropertyValue(handler, "suspensions", Map.class);
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertEquals("suspension not removed", 0, suspensions.size());
		Log logger = spy(TestUtils.getPropertyValue(handler, "logger", Log.class));
		new DirectFieldAccessor(handler).setPropertyValue("logger", logger);
		final Message<String> triggerMessage = MessageBuilder.withPayload("bar").setCorrelationId("foo").build();
		handler.trigger(triggerMessage);
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(logger).error(captor.capture());
		assertThat(captor.getValue(),
				allOf(containsString("Suspending thread timed out or did not arrive within timeout for:"),
						containsString("payload=bar")));
		assertEquals(0, suspensions.size());
		Message<?> discard = discardChannel.receive(0);
		assertSame(discard, triggerMessage);
		handler.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("foo").build());
		assertEquals(0, suspensions.size());
		exec.shutdownNow();
	}

	@Test
	public void testRequiresReply() throws Exception {
		final BarrierMessageHandler handler = new BarrierMessageHandler(0);
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setRequiresReply(true);
		handler.afterPropertiesSet();
		try {
			handler.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("foo").build());
			fail("exception expected");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(ReplyRequiredException.class));
		}
	}

	@Test
	public void testExceptionReply() throws Exception {
		final BarrierMessageHandler handler = new BarrierMessageHandler(10000);
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		final AtomicReference<Exception> exception = new AtomicReference<Exception>();
		final CountDownLatch latch = new CountDownLatch(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			try {
				handler.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("foo").build());
			}
			catch (Exception e) {
				exception.set(e);
				latch.countDown();
			}
		});
		Map<?, ?> suspensions = TestUtils.getPropertyValue(handler, "suspensions", Map.class);
		int n = 0;
		while (n++ < 100 && suspensions.size() == 0) {
			Thread.sleep(100);
		}
		assertTrue("suspension did not appear in time", n < 100);
		Exception exc = new RuntimeException();
		handler.trigger(MessageBuilder.withPayload(exc).setCorrelationId("foo").build());
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertSame(exc, exception.get().getCause());
		assertEquals(0, suspensions.size());
		exec.shutdownNow();
	}

	@Test
	public void testJavaConfig() {
		Message<?> releasing = MessageBuilder.withPayload("bar").setCorrelationId("foo").build();
		this.release.send(releasing);
		Message<?> suspending = MessageBuilder.withPayload("foo").setCorrelationId("foo").build();
		this.in.send(suspending);
		Message<?> out = this.out.receive(10000);
		assertNotNull(out);
		assertEquals("[foo, bar]", out.getPayload().toString());

		Message<?> publisherMessage = this.publisherChannel.receive(10000);
		assertNotNull(publisherMessage);
		assertEquals("BAR", publisherMessage.getPayload());
	}

	@Configuration
	@EnableIntegration
	@EnablePublisher
	public static class Config {

		@Bean
		public MessageChannel in() {
			return new DirectChannel();
		}

		@Bean
		public MessageChannel out() {
			return new QueueChannel();
		}

		@Bean
		public MessageChannel release() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel publisherChannel() {
			return new QueueChannel();
		}

		@ServiceActivator(inputChannel = "in")
		@Bean
		public BarrierMessageHandler barrier() {
			BarrierMessageHandler barrier = new BarrierMessageHandler(10000);
			barrier.setOutputChannel(out());
			return barrier;
		}

		@ServiceActivator(inputChannel = "release", poller = @Poller(fixedDelay = "0"))
		@Bean
		public MessageHandler releaser() {
			return new MessageHandler() {

				@Override
				@Publisher(channel = "publisherChannel")
				@Payload("#args[0].payload.toUpperCase()")
				public void handleMessage(Message<?> message) throws MessagingException {
					barrier().trigger(message);
				}

			};
		}

	}

}
