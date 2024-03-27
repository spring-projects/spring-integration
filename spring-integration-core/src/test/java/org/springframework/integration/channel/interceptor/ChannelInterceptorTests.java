/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.channel.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
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
import org.springframework.messaging.support.InterceptableChannel;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test");
		assertThat(result.getHeaders().get(PreSendReturnsMessageInterceptor.class.getSimpleName())).isEqualTo(1);
		assertThat(interceptor.wasAfterCompletionInvoked()).isTrue();
	}

	@Test
	public void testPreSendInterceptorReturnsNull() {
		PreSendReturnsNullInterceptor interceptor = new PreSendReturnsNullInterceptor();
		channel.addInterceptor(interceptor);
		Message<?> message = new GenericMessage<String>("test");
		channel.send(message);
		assertThat(interceptor.getCount()).isEqualTo(1);

		assertThat(channel.removeInterceptor(interceptor)).isTrue();

		channel.send(new GenericMessage<String>("TEST"));
		assertThat(interceptor.getCount()).isEqualTo(1);

		Message<?> result = channel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
	}

	@Test
	public void testPostSendInterceptorWithSentMessage() {
		final AtomicBoolean invoked = new AtomicBoolean(false);
		channel.addInterceptor(new ChannelInterceptor() {

			@Override
			public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
				assertThat(message).isNotNull();
				assertThat(channel).isNotNull();
				assertThat(channel).isSameAs(ChannelInterceptorTests.this.channel);
				assertThat(sent).isTrue();
				invoked.set(true);
			}

		});
		channel.send(new GenericMessage<String>("test"));
		assertThat(invoked.get()).isTrue();
	}

	@Test
	public void testPostSendInterceptorWithUnsentMessage() {
		final AtomicInteger invokedCounter = new AtomicInteger(0);
		final AtomicInteger sentCounter = new AtomicInteger(0);
		final QueueChannel singleItemChannel = new QueueChannel(1);
		singleItemChannel.addInterceptor(new ChannelInterceptor() {

			@Override
			public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
				assertThat(message).isNotNull();
				assertThat(channel).isNotNull();
				assertThat(channel).isSameAs(singleItemChannel);
				if (sent) {
					sentCounter.incrementAndGet();
				}
				invokedCounter.incrementAndGet();
			}

		});
		assertThat(invokedCounter.get()).isEqualTo(0);
		assertThat(sentCounter.get()).isEqualTo(0);
		singleItemChannel.send(new GenericMessage<String>("test1"));
		assertThat(invokedCounter.get()).isEqualTo(1);
		assertThat(sentCounter.get()).isEqualTo(1);
		singleItemChannel.send(new GenericMessage<String>("test2"), 0);
		assertThat(invokedCounter.get()).isEqualTo(2);
		assertThat(sentCounter.get()).isEqualTo(1);

		assertThat(singleItemChannel.removeInterceptor(0)).isNotNull();
		singleItemChannel.send(new GenericMessage<String>("test2"), 0);
		assertThat(invokedCounter.get()).isEqualTo(2);
		assertThat(sentCounter.get()).isEqualTo(1);
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
			assertThat(ex.getCause().getMessage()).isEqualTo("Simulated exception");
		}
		assertThat(interceptor1.wasAfterCompletionInvoked()).isTrue();
		assertThat(interceptor2.wasAfterCompletionInvoked()).isTrue();
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
			assertThat(ex.getCause().getMessage()).isEqualTo("Simulated exception");
		}
		assertThat(interceptor1.wasAfterCompletionInvoked()).isTrue();
		assertThat(interceptor2.wasAfterCompletionInvoked()).isFalse();
	}

	@Test
	public void testPreReceiveInterceptorReturnsTrue() {
		PreReceiveReturnsTrueInterceptor interceptor = new PreReceiveReturnsTrueInterceptor();
		channel.addInterceptor(interceptor);
		Message<?> message = new GenericMessage<String>("test");
		channel.send(message);
		Message<?> result = channel.receive(0);
		assertThat(interceptor.getCounter().get()).isEqualTo(1);
		assertThat(result).isNotNull();
		assertThat(interceptor.wasAfterCompletionInvoked()).isTrue();
	}

	@Test
	public void testPreReceiveInterceptorReturnsFalse() {
		channel.addInterceptor(new PreReceiveReturnsFalseInterceptor());
		Message<?> message = new GenericMessage<String>("test");
		channel.send(message);
		Message<?> result = channel.receive(0);
		assertThat(PreReceiveReturnsFalseInterceptor.counter.get()).isEqualTo(1);
		assertThat(result).isNull();
	}

	@Test
	public void testPostReceiveInterceptor() {
		final AtomicInteger messageCount = new AtomicInteger();
		channel.addInterceptor(new ChannelInterceptor() {

			@Override
			public Message<?> postReceive(Message<?> message, MessageChannel channel) {
				assertThat(channel).isNotNull();
				assertThat(channel).isSameAs(ChannelInterceptorTests.this.channel);
				messageCount.incrementAndGet();
				return message;
			}

		});
		channel.receive(0);
		assertThat(messageCount.get()).isEqualTo(0);
		channel.send(new GenericMessage<String>("test"));
		Message<?> result = channel.receive(0);
		assertThat(result).isNotNull();
		assertThat(messageCount.get()).isEqualTo(1);
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
			assertThat(ex.getMessage()).isEqualTo("Simulated exception");
		}
		assertThat(interceptor1.wasAfterCompletionInvoked()).isTrue();
		assertThat(interceptor2.wasAfterCompletionInvoked()).isFalse();
	}

	@Test
	public void testInterceptorBeanWithPNamespace() {
		ConfigurableApplicationContext ac =
				new ClassPathXmlApplicationContext("ChannelInterceptorTests-context.xml", ChannelInterceptorTests.class);
		InterceptableChannel channel = ac.getBean("input", AbstractMessageChannel.class);
		List<ChannelInterceptor> interceptors = channel.getInterceptors();
		ChannelInterceptor channelInterceptor = interceptors.get(0);
		assertThat(channelInterceptor).isInstanceOf(PreSendReturnsMessageInterceptor.class);
		String foo = ((PreSendReturnsMessageInterceptor) channelInterceptor).getFoo();
		assertThat(StringUtils.hasText(foo)).isTrue();
		assertThat(foo).isEqualTo("foo");
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

		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();

		channel.addInterceptor(new TestExecutorInterceptor());
		channel.send(new GenericMessage<>("foo"));

		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(messages.size()).isEqualTo(2);

		assertThat(messages.get(0).getPayload()).isEqualTo("foo");
		assertThat(messages.get(1).getPayload()).isEqualTo("FOO");

		testApplicationContext.close();
	}

	public static class PreSendReturnsMessageInterceptor implements ChannelInterceptor {

		private String foo;

		private static AtomicInteger counter = new AtomicInteger();

		private volatile boolean afterCompletionInvoked;

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			assertThat(message).isNotNull();
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
			assertThat(message).isNotNull();
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
			assertThat(message).isNotNull();
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
