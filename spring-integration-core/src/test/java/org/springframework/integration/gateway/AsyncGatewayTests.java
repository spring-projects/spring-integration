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

package org.springframework.integration.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.GatewayHeader;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import reactor.core.publisher.Mono;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class AsyncGatewayTests {

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
		Object result = f.get(10000, TimeUnit.MILLISECONDS);
		assertNotNull(result);
		assertEquals("foobar", ((Message<?>) result).getPayload());
	}

	@Test
	public void futureWithError() throws Exception {
		final Error error = new Error("error");
		DirectChannel channel = new DirectChannel() {

			@Override
			protected boolean doSend(Message<?> message, long timeout) {
				throw error;
			}

		};
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(channel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		Future<Message<?>> f = service.returnMessage("foo");
		try {
			f.get(10000, TimeUnit.MILLISECONDS);
			fail("Expected Exception");
		}
		catch (ExecutionException e) {
			assertEquals(error, e.getCause());
		}
	}

	@Test
	public void listenableFutureWithMessageReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		addThreadEnricher(requestChannel);
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		ListenableFuture<Message<?>> f = service.returnMessageListenable("foo");
		long start = System.currentTimeMillis();
		final AtomicReference<Message<?>> result = new AtomicReference<Message<?>>();
		final CountDownLatch latch = new CountDownLatch(1);
		f.addCallback(new ListenableFutureCallback<Message<?>>() {

			@Override
			public void onSuccess(Message<?> msg) {
				result.set(msg);
				latch.countDown();
			}

			@Override
			public void onFailure(Throwable t) {
			}

		});
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		long elapsed = System.currentTimeMillis() - start;
		assertTrue(elapsed >= 200);
		assertEquals("foobar", result.get().getPayload());
		Object thread = result.get().getHeaders().get("thread");
		assertNotEquals(Thread.currentThread(), thread);
	}

	@Test
	public void customFutureReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		addThreadEnricher(requestChannel);
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		CustomFuture f = service.returnCustomFuture("foo");
		String result = f.get(10000, TimeUnit.MILLISECONDS);
		assertEquals("foobar", result);
		assertEquals(Thread.currentThread(), f.thread);
	}

	@Test
	public void nonAsyncFutureReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		addThreadEnricher(requestChannel);
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));

		proxyFactory.setAsyncExecutor(null);    // Not async - user flow returns Future<?>

		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		CustomFuture f = (CustomFuture) service.returnCustomFutureWithTypeFuture("foo");
		String result = f.get(10000, TimeUnit.MILLISECONDS);
		assertEquals("foobar", result);
		assertEquals(Thread.currentThread(), f.thread);
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
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		Future<String> f = service.returnString("foo");
		Object result = f.get(10000, TimeUnit.MILLISECONDS);
		assertNotNull(result);
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
		Object result = f.get(10000, TimeUnit.MILLISECONDS);
		assertTrue(result instanceof String);
		assertEquals("foobar", result);
	}


	@Test
	public void monoWithMessageReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setBeanName("testGateway");
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		Mono<Message<?>> mono = service.returnMessagePromise("foo");
		Object result = mono.block(Duration.ofSeconds(10));
		assertEquals("foobar", ((Message<?>) result).getPayload());
	}

	@Test
	public void monoWithPayloadReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setBeanName("testGateway");
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		Mono<String> mono = service.returnStringPromise("foo");
		Object result = mono.block(Duration.ofSeconds(10));
		assertEquals("foobar", result);
	}

	@Test
	public void monoWithWildcardReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setBeanName("testGateway");
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		Mono<?> mono = service.returnSomethingPromise("foo");
		Object result = mono.block(Duration.ofSeconds(10));
		assertNotNull(result);
		assertEquals("foobar", result);
	}

	@Test
	public void monoWithConsumer() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setBeanName("testGateway");
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		Mono<String> mono = service.returnStringPromise("foo");

		final AtomicReference<String> result = new AtomicReference<String>();
		final CountDownLatch latch = new CountDownLatch(1);

		mono.subscribe(s -> {
			result.set(s);
			latch.countDown();
		});

		latch.await(10, TimeUnit.SECONDS);
		assertEquals("foobar", result.get());
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

		ListenableFuture<Message<?>> returnMessageListenable(String s);

		@Gateway(headers = @GatewayHeader(name = "method", expression = "#gatewayMethod.name"))
		CustomFuture returnCustomFuture(String s);

		@Gateway(headers = @GatewayHeader(name = "method", expression = "#gatewayMethod.name"))
		Future<?> returnCustomFutureWithTypeFuture(String s);

		Mono<String> returnStringPromise(String s);

		Mono<Message<?>> returnMessagePromise(String s);

		Mono<?> returnSomethingPromise(String s);

	}

	private static class CustomFuture implements Future<String> {

		private final String result;

		private final Thread thread;

		private CustomFuture(String result, Thread thread) {
			this.result = result;
			this.thread = thread;
		}

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
		public String get() throws InterruptedException, ExecutionException {
			return result;
		}

		@Override
		public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
				TimeoutException {
			return result;
		}

	}

}
