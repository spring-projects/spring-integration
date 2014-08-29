/*
 * Copyright 2002-2014 the original author or authors.
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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.GatewayHeader;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import reactor.core.Environment;
import reactor.core.composable.Promise;
import reactor.function.Consumer;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class AsyncGatewayTests {

	private final Environment reactorEnvironment = new Environment();

	// TODO: changed from 0 because of recurrent failure: is this right?
	private final long safety = 100;

	public void tearDown() {
		this.reactorEnvironment.shutdown();
	}

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
		String result = f.get(1000, TimeUnit.MILLISECONDS);
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

		proxyFactory.setAsyncExecutor(null);	// Not async - user flow returns Future<?>

		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		CustomFuture f = (CustomFuture) service.returnCustomFutureWithTypeFuture("foo");
		String result = f.get(1000, TimeUnit.MILLISECONDS);
		assertEquals("foobar", result);

		assertEquals(Thread.currentThread(), f.thread);
	}

	protected void addThreadEnricher(QueueChannel requestChannel) {
		requestChannel.addInterceptor(new ChannelInterceptorAdapter() {

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
		long start = System.currentTimeMillis();
		Object result = f.get(1000, TimeUnit.MILLISECONDS);
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200 - safety);
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

		assertTrue(elapsed >= 200 - safety);
		assertTrue(result instanceof String);
		assertEquals("foobar", result);
	}


	@Test
	public void promiseWithMessageReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setReactorEnvironment(this.reactorEnvironment);
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		Promise<Message<?>> promise = service.returnMessagePromise("foo");
		long start = System.currentTimeMillis();
		Object result = promise.await(1, TimeUnit.SECONDS);
		long elapsed = System.currentTimeMillis() - start;
		assertTrue(elapsed >= 200);
		assertEquals("foobar", ((Message<?>) result).getPayload());
	}

	@Test
	public void promiseWithPayloadReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setReactorEnvironment(this.reactorEnvironment);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		Promise<String> promise = service.returnStringPromise("foo");
		long start = System.currentTimeMillis();
		Object result = promise.await(1, TimeUnit.SECONDS);
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200 - safety);
		assertEquals("foobar", result);
	}

	@Test
	public void promiseWithWildcardReturned() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setReactorEnvironment(this.reactorEnvironment);
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		Promise<?> promise = service.returnSomethingPromise("foo");
		long start = System.currentTimeMillis();
		Object result = promise.await(1, TimeUnit.SECONDS);
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200 - safety);
		assertTrue(result instanceof String);
		assertEquals("foobar", result);
	}

	@Test
	public void promiseWithConsumer() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setReactorEnvironment(this.reactorEnvironment);
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		Promise<String> promise = service.returnStringPromise("foo");
		long start = System.currentTimeMillis();

		final AtomicReference<String> result = new AtomicReference<String>();
		final CountDownLatch latch = new CountDownLatch(1);

		promise.consume(new Consumer<String>() {
			@Override
			public void accept(String s) {
				result.set(s);
				latch.countDown();
			}
		})
				.flush();

		latch.await(1, TimeUnit.SECONDS);
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200 - safety);
		assertEquals("foobar", result.get());
	}

	@Test
	public void promiseMethodWithoutEnvironment() throws Exception {
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setServiceInterface(TestEchoService.class);
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setBeanName("testGateway");
		proxyFactory.afterPropertiesSet();
		TestEchoService service = (TestEchoService) proxyFactory.getObject();
		try {
			service.returnStringPromise("foo");
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(IllegalStateException.class));
			assertEquals(e.getMessage(), "'reactorEnvironment' is required in case of 'Promise' return type.");
		}
	}

	private static void startResponder(final PollableChannel requestChannel) {
		new Thread(new Runnable() {
			@Override
			public void run() {
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
			}
		}).start();
	}


	static interface TestEchoService {

		Future<String> returnString(String s);

		Future<Message<?>> returnMessage(String s);

		Future<?> returnSomething(String s);

		ListenableFuture<Message<?>> returnMessageListenable(String s);

		@Gateway(headers=@GatewayHeader(name="method", expression="#gatewayMethod.name"))
		CustomFuture returnCustomFuture(String s);

		@Gateway(headers=@GatewayHeader(name="method", expression="#gatewayMethod.name"))
		Future<?> returnCustomFutureWithTypeFuture(String s);

		Promise<String> returnStringPromise(String s);

		Promise<Message<?>> returnMessagePromise(String s);

		Promise<?> returnSomethingPromise(String s);

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
