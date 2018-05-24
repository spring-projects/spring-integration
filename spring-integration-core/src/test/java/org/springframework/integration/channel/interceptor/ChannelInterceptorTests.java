/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.channel.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 */
public class ChannelInterceptorTests {

	private final QueueChannel channel = new QueueChannel();


	@Test
	public void testPreSendInterceptorReturnsMessage() {
		PreSendReturnsMessageInterceptor interceptor = new PreSendReturnsMessageInterceptor();
		channel.addInterceptor(interceptor);
		channel.send(new GenericMessage<String>("test"));
		Message<?> result = channel.receive(0);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
		assertEquals(1, result.getHeaders().get(PreSendReturnsMessageInterceptor.class.getSimpleName()));
		assertTrue(interceptor.wasAfterCompletionInvoked());
	}

	@Test
	public void testPreSendInterceptorReturnsNull() {
		PreSendReturnsNullInterceptor interceptor = new PreSendReturnsNullInterceptor();
		channel.addInterceptor(interceptor);
		Message<?> message = new GenericMessage<String>("test");
		channel.send(message);
		assertEquals(1, interceptor.getCount());

		assertTrue(channel.removeInterceptor(interceptor));

		channel.send(new GenericMessage<String>("TEST"));
		assertEquals(1, interceptor.getCount());

		Message<?> result = channel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
	}

	@Test
	public void testPostSendInterceptorWithSentMessage() {
		final AtomicBoolean invoked = new AtomicBoolean(false);
		channel.addInterceptor(new ChannelInterceptor() {

			@Override
			public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
				assertNotNull(message);
				assertNotNull(channel);
				assertSame(ChannelInterceptorTests.this.channel, channel);
				assertTrue(sent);
				invoked.set(true);
			}

		});
		channel.send(new GenericMessage<String>("test"));
		assertTrue(invoked.get());
	}

	@Test
	public void testPostSendInterceptorWithUnsentMessage() {
		final AtomicInteger invokedCounter = new AtomicInteger(0);
		final AtomicInteger sentCounter = new AtomicInteger(0);
		final QueueChannel singleItemChannel = new QueueChannel(1);
		singleItemChannel.addInterceptor(new ChannelInterceptor() {

			@Override
			public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
				assertNotNull(message);
				assertNotNull(channel);
				assertSame(singleItemChannel, channel);
				if (sent) {
					sentCounter.incrementAndGet();
				}
				invokedCounter.incrementAndGet();
			}

		});
		assertEquals(0, invokedCounter.get());
		assertEquals(0, sentCounter.get());
		singleItemChannel.send(new GenericMessage<String>("test1"));
		assertEquals(1, invokedCounter.get());
		assertEquals(1, sentCounter.get());
		singleItemChannel.send(new GenericMessage<String>("test2"), 0);
		assertEquals(2, invokedCounter.get());
		assertEquals(1, sentCounter.get());

		assertNotNull(singleItemChannel.removeInterceptor(0));
		singleItemChannel.send(new GenericMessage<String>("test2"), 0);
		assertEquals(2, invokedCounter.get());
		assertEquals(1, sentCounter.get());
	}

	@Test
	public void afterCompletionWithSendException() {
		final AbstractMessageChannel testChannel = new AbstractMessageChannel() {

			@Override
			protected boolean doSend(Message<?> message, long timeout) {
				throw new RuntimeException("Simulated exception");
			}
		};
		AfterCompletionTestInterceptor interceptor1 = new AfterCompletionTestInterceptor();
		AfterCompletionTestInterceptor interceptor2 = new AfterCompletionTestInterceptor();
		testChannel.addInterceptor(interceptor1);
		testChannel.addInterceptor(interceptor2);
		try {
			testChannel.send(MessageBuilder.withPayload("test").build());
		}
		catch (Exception ex) {
			assertEquals("Simulated exception", ex.getCause().getMessage());
		}
		assertTrue(interceptor1.wasAfterCompletionInvoked());
		assertTrue(interceptor2.wasAfterCompletionInvoked());
	}

	@Test
	public void afterCompletionWithPreSendException() {
		AfterCompletionTestInterceptor interceptor1 = new AfterCompletionTestInterceptor();
		AfterCompletionTestInterceptor interceptor2 = new AfterCompletionTestInterceptor();
		interceptor2.setExceptionToRaise(new RuntimeException("Simulated exception"));
		this.channel.addInterceptor(interceptor1);
		this.channel.addInterceptor(interceptor2);
		try {
			this.channel.send(MessageBuilder.withPayload("test").build());
		}
		catch (Exception ex) {
			assertEquals("Simulated exception", ex.getCause().getMessage());
		}
		assertTrue(interceptor1.wasAfterCompletionInvoked());
		assertFalse(interceptor2.wasAfterCompletionInvoked());
	}

	@Test
	public void testPreReceiveInterceptorReturnsTrue() {
		PreReceiveReturnsTrueInterceptor interceptor = new PreReceiveReturnsTrueInterceptor();
		channel.addInterceptor(interceptor);
		Message<?> message = new GenericMessage<String>("test");
		channel.send(message);
		Message<?> result = channel.receive(0);
		assertEquals(1, interceptor.getCounter().get());
		assertNotNull(result);
		assertTrue(interceptor.wasAfterCompletionInvoked());
	}

	@Test
	public void testPreReceiveInterceptorReturnsFalse() {
		channel.addInterceptor(new PreReceiveReturnsFalseInterceptor());
		Message<?> message = new GenericMessage<String>("test");
		channel.send(message);
		Message<?> result = channel.receive(0);
		assertEquals(1, PreReceiveReturnsFalseInterceptor.counter.get());
		assertNull(result);
	}

	@Test
	public void testPostReceiveInterceptor() {
		final AtomicInteger invokedCount = new AtomicInteger();
		final AtomicInteger messageCount = new AtomicInteger();
		channel.addInterceptor(new ChannelInterceptor() {

			@Override
			public Message<?> postReceive(Message<?> message, MessageChannel channel) {
				assertNotNull(channel);
				assertSame(ChannelInterceptorTests.this.channel, channel);
				if (message != null) {
					messageCount.incrementAndGet();
				}
				invokedCount.incrementAndGet();
				return message;
			}

		});
		channel.receive(0);
		assertEquals(1, invokedCount.get());
		assertEquals(0, messageCount.get());
		channel.send(new GenericMessage<String>("test"));
		Message<?> result = channel.receive(0);
		assertNotNull(result);
		assertEquals(2, invokedCount.get());
		assertEquals(1, messageCount.get());
	}

	@Test
	public void afterCompletionWithReceiveException() {
		PreReceiveReturnsTrueInterceptor interceptor1 = new PreReceiveReturnsTrueInterceptor();
		PreReceiveReturnsTrueInterceptor interceptor2 = new PreReceiveReturnsTrueInterceptor();
		interceptor2.setExceptionToRaise(new RuntimeException("Simulated exception"));
		channel.addInterceptor(interceptor1);
		channel.addInterceptor(interceptor2);

		try {
			channel.receive(0);
		}
		catch (Exception ex) {
			assertEquals("Simulated exception", ex.getMessage());
		}
		assertTrue(interceptor1.wasAfterCompletionInvoked());
		assertFalse(interceptor2.wasAfterCompletionInvoked());
	}

	@Test
	public void testInterceptorBeanWithPNamespace() {
		ConfigurableApplicationContext ac =
				new ClassPathXmlApplicationContext("ChannelInterceptorTests-context.xml", ChannelInterceptorTests.class);
		ChannelInterceptorAware channel = ac.getBean("input", AbstractMessageChannel.class);
		List<ChannelInterceptor> interceptors = channel.getChannelInterceptors();
		ChannelInterceptor channelInterceptor = interceptors.get(0);
		assertThat(channelInterceptor, Matchers.instanceOf(PreSendReturnsMessageInterceptor.class));
		String foo = ((PreSendReturnsMessageInterceptor) channelInterceptor).getFoo();
		assertTrue(StringUtils.hasText(foo));
		assertEquals("foo", foo);
		ac.close();
	}

	@Test
	public void testPollingConsumerWithExecutorInterceptor() throws InterruptedException {
		TestUtils.TestApplicationContext testApplicationContext = TestUtils.createTestApplicationContext();

		QueueChannel channel = new QueueChannel();

		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(2);
		final List<Message<?>> messages = new ArrayList<>();

		PollingConsumer consumer = new PollingConsumer(channel, message -> {
			messages.add(message);
			latch1.countDown();
			latch2.countDown();
		});

		testApplicationContext.registerBean("consumer", consumer);
		testApplicationContext.refresh();

		channel.send(new GenericMessage<>("foo"));

		assertTrue(latch1.await(10, TimeUnit.SECONDS));

		channel.addInterceptor(new TestExecutorInterceptor());
		channel.send(new GenericMessage<>("foo"));

		assertTrue(latch2.await(10, TimeUnit.SECONDS));
		assertEquals(2, messages.size());

		assertEquals("foo", messages.get(0).getPayload());
		assertEquals("FOO", messages.get(1).getPayload());

		testApplicationContext.close();
	}

	public static class PreSendReturnsMessageInterceptor implements ChannelInterceptor {
		private String foo;

		private static AtomicInteger counter = new AtomicInteger();

		private volatile boolean afterCompletionInvoked;

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			assertNotNull(message);
			return MessageBuilder.fromMessage(message)
					.setHeader(this.getClass().getSimpleName(), counter.incrementAndGet())
					.build();
		}
		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public boolean wasAfterCompletionInvoked() {
			return this.afterCompletionInvoked;
		}

		@Override
		public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
			this.afterCompletionInvoked = true;
		}

	}


	private static class PreSendReturnsNullInterceptor implements ChannelInterceptor {

		private static AtomicInteger counter = new AtomicInteger();

		PreSendReturnsNullInterceptor() {
			super();
		}

		protected int getCount() {
			return counter.get();
		}

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			assertNotNull(message);
			counter.incrementAndGet();
			return null;
		}
	}

	private static class AfterCompletionTestInterceptor implements ChannelInterceptor {

		private final AtomicInteger counter = new AtomicInteger();

		private volatile boolean afterCompletionInvoked;

		private RuntimeException exceptionToRaise;

		AfterCompletionTestInterceptor() {
			super();
		}

		public void setExceptionToRaise(RuntimeException exception) {
			this.exceptionToRaise = exception;
		}

		@SuppressWarnings("unused")
		public AtomicInteger getCounter() {
			return this.counter;
		}

		public boolean wasAfterCompletionInvoked() {
			return this.afterCompletionInvoked;
		}

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			assertNotNull(message);
			counter.incrementAndGet();
			if (this.exceptionToRaise != null) {
				throw this.exceptionToRaise;
			}
			return message;
		}

		@Override
		public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
			this.afterCompletionInvoked = true;
		}

	}

	private static class PreReceiveReturnsTrueInterceptor implements ChannelInterceptor {

		private final AtomicInteger counter = new AtomicInteger();

		private volatile boolean afterCompletionInvoked;

		private RuntimeException exceptionToRaise;

		PreReceiveReturnsTrueInterceptor() {
			super();
		}

		public void setExceptionToRaise(RuntimeException exception) {
			this.exceptionToRaise = exception;
		}

		public AtomicInteger getCounter() {
			return this.counter;
		}

		@Override
		public boolean preReceive(MessageChannel channel) {
			counter.incrementAndGet();
			if (this.exceptionToRaise != null) {
				throw this.exceptionToRaise;
			}
			return true;
		}

		public boolean wasAfterCompletionInvoked() {
			return this.afterCompletionInvoked;
		}

		@Override
		public void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {
			this.afterCompletionInvoked = true;
		}

	}


	private static class PreReceiveReturnsFalseInterceptor implements ChannelInterceptor {

		private static AtomicInteger counter = new AtomicInteger();

		PreReceiveReturnsFalseInterceptor() {
			super();
		}

		@Override
		public boolean preReceive(MessageChannel channel) {
			counter.incrementAndGet();
			return false;
		}

	}

	private static class TestExecutorInterceptor implements ExecutorChannelInterceptor {

		TestExecutorInterceptor() {
			super();
		}

		@Override
		public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
			return MessageBuilder.withPayload(((String) message.getPayload()).toUpperCase())
					.copyHeaders(message.getHeaders())
					.build();
		}

		@Override
		public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler,
										Exception ex) {

		}

	}


}
