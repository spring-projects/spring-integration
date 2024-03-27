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

package org.springframework.integration.gateway;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.GatewayHeader;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class AsyncGatewayTests {

	@Test
	public void futureWithMessageReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean<TestEchoService> proxyFactory = new GatewayProxyFactoryBean<>(TestEchoService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = proxyFactory.getObject();
		Future<Message<?>> f = service.returnMessage("foo");
		Object result = f.get(10000, TimeUnit.MILLISECONDS);
		assertThat(result).isNotNull();
		assertThat(((Message<?>) result).getPayload()).isEqualTo("foobar");
	}

	@Test
	public void futureWithError() {
		final Error error = new Error("error");
		DirectChannel channel = new DirectChannel() {

			@Override
			protected boolean doSend(Message<?> message, long timeout) {
				throw error;
			}

		};
		GatewayProxyFactoryBean<TestEchoService> proxyFactory = new GatewayProxyFactoryBean<>(TestEchoService.class);
		proxyFactory.setDefaultRequestChannel(channel);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = proxyFactory.getObject();
		Future<Message<?>> f = service.returnMessage("foo");

		assertThatExceptionOfType(ExecutionException.class)
				.isThrownBy(() -> f.get(10000, TimeUnit.MILLISECONDS))
				.withCause(error);
	}

	@Test
	public void listenableFutureWithMessageReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		addThreadEnricher(requestChannel);
		startResponder(requestChannel);
		GatewayProxyFactoryBean<TestEchoService> proxyFactory = new GatewayProxyFactoryBean<>(TestEchoService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = proxyFactory.getObject();
		CompletableFuture<Message<?>> f = service.returnMessageListenable("foo");
		long start = System.currentTimeMillis();
		final AtomicReference<Message<?>> result = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		f.whenComplete((message, throwable) -> {
			if (throwable == null) {
				result.set(message);
				latch.countDown();
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		long elapsed = System.currentTimeMillis() - start;
		assertThat(elapsed >= 200).isTrue();
		assertThat(result.get().getPayload()).isEqualTo("foobar");
		Object thread = result.get().getHeaders().get("thread");
		assertThat(thread).isNotEqualTo(Thread.currentThread());
	}

	@Test
	public void customFutureReturned() {
		QueueChannel requestChannel = new QueueChannel();
		addThreadEnricher(requestChannel);
		startResponder(requestChannel);
		GatewayProxyFactoryBean<TestEchoService> proxyFactory = new GatewayProxyFactoryBean<>(TestEchoService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = proxyFactory.getObject();
		CustomFuture f = service.returnCustomFuture("foo");
		String result = f.get(10000, TimeUnit.MILLISECONDS);
		assertThat(result).isEqualTo("foobar");
		assertThat(f.thread).isEqualTo(Thread.currentThread());
	}

	@Test
	public void nonAsyncFutureReturned() {
		QueueChannel requestChannel = new QueueChannel();
		addThreadEnricher(requestChannel);
		startResponder(requestChannel);
		GatewayProxyFactoryBean<TestEchoService> proxyFactory = new GatewayProxyFactoryBean<>(TestEchoService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));

		proxyFactory.setAsyncExecutor(null);    // Not async - user flow returns Future<?>

		proxyFactory.afterPropertiesSet();
		TestEchoService service = proxyFactory.getObject();
		CustomFuture f = (CustomFuture) service.returnCustomFutureWithTypeFuture("foo");
		String result = f.get(10000, TimeUnit.MILLISECONDS);
		assertThat(result).isEqualTo("foobar");
		assertThat(f.thread).isEqualTo(Thread.currentThread());
	}

	protected void addThreadEnricher(QueueChannel requestChannel) {
		requestChannel.addInterceptor(new ChannelInterceptor() {

			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				return MessageBuilder.fromMessage(message)
						.setHeader("thread", Thread.currentThread())
						.build();
			}

		});
	}

	@Test
	public void futureWithPayloadReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean<TestEchoService> proxyFactory = new GatewayProxyFactoryBean<>(TestEchoService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = proxyFactory.getObject();
		Future<String> f = service.returnString("foo");
		Object result = f.get(10000, TimeUnit.MILLISECONDS);
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo("foobar");
	}

	@Test
	public void futureWithWildcardReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean<TestEchoService> proxyFactory = new GatewayProxyFactoryBean<>(TestEchoService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = proxyFactory.getObject();
		Future<?> f = service.returnSomething("foo");
		Object result = f.get(10000, TimeUnit.MILLISECONDS);
		assertThat(result instanceof String).isTrue();
		assertThat(result).isEqualTo("foobar");
	}

	@Test
	public void futureVoid() throws Exception {
		GatewayProxyFactoryBean<TestEchoService> proxyFactory = new GatewayProxyFactoryBean<>(TestEchoService.class);
		proxyFactory.setDefaultRequestChannel(new NullChannel());
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = proxyFactory.getObject();
		Future<Void> f = service.asyncSendAndForget("test1");
		Object result = f.get(10, TimeUnit.SECONDS);
		assertThat(result).isNull();

		new DirectFieldAccessor(proxyFactory).setPropertyValue("initialized", false);
		proxyFactory.setDefaultRequestChannel((message, timeout) -> {
			throw new MessageDispatchingException(message, "intentional dispatcher error");
		});
		proxyFactory.afterPropertiesSet();

		Future<Void> futureError = service.asyncSendAndForget("test2");
		assertThatExceptionOfType(ExecutionException.class)
				.isThrownBy(() -> futureError.get(10, TimeUnit.SECONDS))
				.withCauseInstanceOf(MessageDispatchingException.class)
				.withMessageContaining("intentional dispatcher error");
	}

	@Test
	public void futureVoidReply() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		CountDownLatch readyForReplyLatch = new CountDownLatch(1);
		new Thread(() -> {
			try {
				Message<?> input = requestChannel.receive();
				CompletableFuture<Void> reply = new CompletableFuture<>();
				((MessageChannel) input.getHeaders().getReplyChannel()).send(new GenericMessage<>(reply));
				readyForReplyLatch.await(10, TimeUnit.SECONDS);
				reply.complete(null);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				ReflectionUtils.rethrowRuntimeException(ex);
			}
		}).start();
		GatewayProxyFactoryBean<TestEchoService> proxyFactory = new GatewayProxyFactoryBean<>(TestEchoService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setAsyncExecutor(null);
		proxyFactory.afterPropertiesSet();
		TestEchoService service = proxyFactory.getObject();
		Future<Void> f = service.sendAndReceiveFutureVoid("test");
		readyForReplyLatch.countDown();
		Object result = f.get(10, TimeUnit.SECONDS);
		assertThat(result).isNull();
	}

	@Test
	public void monoWithMessageReturned() {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean<TestEchoService> proxyFactory = new GatewayProxyFactoryBean<>(TestEchoService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setBeanName("testGateway");
		proxyFactory.afterPropertiesSet();
		TestEchoService service = proxyFactory.getObject();
		Mono<Message<?>> mono = service.returnMessagePromise("foo");
		Object result = mono.block(Duration.ofSeconds(10));
		assertThat(((Message<?>) result).getPayload()).isEqualTo("foobar");
	}

	@Test
	public void monoWithPayloadReturned() {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean<TestEchoService> proxyFactory = new GatewayProxyFactoryBean<>(TestEchoService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setBeanName("testGateway");
		proxyFactory.afterPropertiesSet();
		TestEchoService service = proxyFactory.getObject();
		Mono<String> mono = service.returnStringPromise("foo");
		Object result = mono.block(Duration.ofSeconds(10));
		assertThat(result).isEqualTo("foobar");
	}

	@Test
	public void monoWithWildcardReturned() {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean<TestEchoService> proxyFactory = new GatewayProxyFactoryBean<>(TestEchoService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setBeanName("testGateway");
		proxyFactory.afterPropertiesSet();
		TestEchoService service = proxyFactory.getObject();
		Mono<?> mono = service.returnSomethingPromise("foo");
		Object result = mono.block(Duration.ofSeconds(10));
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo("foobar");
	}

	@Test
	public void monoWithConsumer() {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean<TestEchoService> proxyFactory = new GatewayProxyFactoryBean<>(TestEchoService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setBeanName("testGateway");
		proxyFactory.afterPropertiesSet();
		TestEchoService service = proxyFactory.getObject();
		Mono<String> mono = service.returnStringPromise("foo");

		StepVerifier.create(mono)
				.expectNext("foobar")
				.verifyComplete();
	}

	@Test
	public void monoVoid() throws InterruptedException {
		GatewayProxyFactoryBean<TestEchoService> proxyFactory = new GatewayProxyFactoryBean<>(TestEchoService.class);
		proxyFactory.setDefaultRequestChannel(new NullChannel());
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = proxyFactory.getObject();
		Mono<Void> mono = service.monoVoid("test1");

		CountDownLatch emptyMonoLatch = new CountDownLatch(1);
		mono.switchIfEmpty(Mono.empty().doOnSuccess(v -> emptyMonoLatch.countDown()).then()).subscribe();

		assertThat(emptyMonoLatch.await(10, TimeUnit.SECONDS)).isTrue();

		new DirectFieldAccessor(proxyFactory).setPropertyValue("initialized", false);
		proxyFactory.setDefaultRequestChannel((message, timeout) -> {
			throw new MessageDispatchingException(message, "intentional dispatcher error");
		});
		proxyFactory.afterPropertiesSet();

		Mono<Void> monoError = service.monoVoid("test2");

		StepVerifier.create(monoError)
				.expectSubscription()
				.expectError(MessageDispatchingException.class)
				.verify(Duration.ofSeconds(10));
	}

	private static void startResponder(final PollableChannel requestChannel) {
		new Thread(() -> {
			Message<?> input = requestChannel.receive();
			String payload = input.getPayload() + "bar";
			Message<?> reply = MessageBuilder.withPayload(payload)
					.copyHeaders(input.getHeaders())
					.build();
			try {
				Thread.sleep(200);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			String header = (String) input.getHeaders().get("method");
			if (header != null && header.startsWith("returnCustomFuture")) {
				reply = MessageBuilder.withPayload(new CustomFuture(payload,
								(Thread) input.getHeaders().get("thread")))
						.copyHeaders(input.getHeaders())
						.build();
			}
			((MessageChannel) input.getHeaders().getReplyChannel()).send(reply);
		}).start();
	}

	private interface TestEchoService {

		Future<String> returnString(String s);

		Future<Message<?>> returnMessage(String s);

		Future<?> returnSomething(String s);

		CompletableFuture<Message<?>> returnMessageListenable(String s);

		@Gateway(headers = @GatewayHeader(name = "method", expression = "method.name"))
		CustomFuture returnCustomFuture(String s);

		@Gateway(headers = @GatewayHeader(name = "method", expression = "method.name"))
		Future<?> returnCustomFutureWithTypeFuture(String s);

		Mono<String> returnStringPromise(String s);

		Mono<Message<?>> returnMessagePromise(String s);

		Mono<?> returnSomethingPromise(String s);

		Future<Void> asyncSendAndForget(String s);

		Future<Void> sendAndReceiveFutureVoid(String s);

		Mono<Void> monoVoid(String s);

	}

	private record CustomFuture(String result, Thread thread) implements Future<String> {

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public String get() {
			return this.result;
		}

		@Override
		public String get(long timeout, TimeUnit unit) {
			return this.result;
		}

	}

}
