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

package org.springframework.integration.config.xml;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.expression.Expression;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.IntegrationConfigUtils;
import org.springframework.integration.gateway.GatewayMethodMetadata;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.gateway.TestService;
import org.springframework.integration.gateway.TestService.MyCompletableFuture;
import org.springframework.integration.gateway.TestService.MyCompletableMessageFuture;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import reactor.core.publisher.Mono;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GatewayParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testOneWay() {
		TestService service = (TestService) context.getBean("oneWay");
		service.oneWay("foo");
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		Message<?> result = channel.receive(10000);
		assertEquals("foo", result.getPayload());
	}

	@Test
	public void testOneWayOverride() {
		TestService service = (TestService) context.getBean("methodOverride");
		service.oneWay("foo");
		PollableChannel channel = (PollableChannel) context.getBean("otherRequestChannel");
		Message<?> result = channel.receive(10000);
		assertNotNull(result);
		assertEquals("fiz", result.getPayload());
		assertEquals("bar", result.getHeaders().get("foo"));
		assertEquals("qux", result.getHeaders().get("baz"));
		GatewayProxyFactoryBean fb = context.getBean("&methodOverride", GatewayProxyFactoryBean.class);
		assertEquals(1000L, TestUtils.getPropertyValue(fb, "defaultRequestTimeout", Expression.class).getValue());
		assertEquals(2000L, TestUtils.getPropertyValue(fb, "defaultReplyTimeout", Expression.class).getValue());
		Map<?, ?> methods = TestUtils.getPropertyValue(fb, "methodMetadataMap", Map.class);
		GatewayMethodMetadata meta = (GatewayMethodMetadata) methods.get("oneWay");
		assertNotNull(meta);
		assertEquals("456", meta.getRequestTimeout());
		assertEquals("123", meta.getReplyTimeout());
		assertEquals("foo", meta.getReplyChannelName());
		meta = (GatewayMethodMetadata) methods.get("oneWayWithTimeouts");
		assertNotNull(meta);
		assertEquals("#args[1]", meta.getRequestTimeout());
		assertEquals("#args[2]", meta.getReplyTimeout());
		service.oneWayWithTimeouts("foo", 100L, 200L);
		result = channel.receive(10000);
		assertNotNull(result);
	}

	@Test
	public void testSolicitResponse() {
		PollableChannel channel = (PollableChannel) context.getBean("replyChannel");
		channel.send(new GenericMessage<String>("foo"));
		TestService service = (TestService) context.getBean("solicitResponse");
		String result = service.solicitResponse();
		assertEquals("foo", result);
	}

	@Test
	public void testRequestReply() {
		PollableChannel requestChannel = (PollableChannel) context.getBean("requestChannel");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = (TestService) context.getBean("requestReply");
		String result = service.requestReply("foo");
		assertEquals("foo", result);
	}

	@Test
	public void testAsyncGateway() throws Exception {
		PollableChannel requestChannel = (PollableChannel) context.getBean("requestChannel");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("async", TestService.class);
		Future<Message<?>> result = service.async("foo");
		Message<?> reply = result.get(10, TimeUnit.SECONDS);
		assertEquals("foo", reply.getPayload());
		assertEquals("testExecutor", reply.getHeaders().get("executor"));
		assertNotNull(TestUtils.getPropertyValue(context.getBean("&async"), "asyncExecutor"));
	}

	@Test
	public void testAsyncDisabledGateway() throws Exception {
		PollableChannel requestChannel = (PollableChannel) context.getBean("requestChannel");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("asyncOff", TestService.class);
		Future<Message<?>> result = service.async("futureSync");
		Message<?> reply = result.get(10, TimeUnit.SECONDS);
		assertEquals("futureSync", reply.getPayload());
		Object serviceBean = context.getBean("&asyncOff");
		assertNull(TestUtils.getPropertyValue(serviceBean, "asyncExecutor"));
	}

	@Test
	public void testFactoryBeanObjectTypeWithServiceInterface() throws Exception {
		ConfigurableListableBeanFactory beanFactory = ((GenericApplicationContext) context).getBeanFactory();
		Object attribute = beanFactory.getMergedBeanDefinition("&oneWay").getAttribute(
				IntegrationConfigUtils.FACTORY_BEAN_OBJECT_TYPE);
		assertEquals(TestService.class.getName(), attribute);
	}

	@Test
	public void testFactoryBeanObjectTypeWithNoServiceInterface() throws Exception {
		ConfigurableListableBeanFactory beanFactory = ((GenericApplicationContext) context).getBeanFactory();
		Object attribute = beanFactory.getMergedBeanDefinition("&defaultConfig").getAttribute(
				IntegrationConfigUtils.FACTORY_BEAN_OBJECT_TYPE);
		assertEquals(RequestReplyExchanger.class.getName(), attribute);
	}

	@Test
	public void testMonoGateway() throws Exception {
		PollableChannel requestChannel = context.getBean("requestChannel", PollableChannel.class);
		MessageChannel replyChannel = context.getBean("replyChannel", MessageChannel.class);
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("promise", TestService.class);
		Mono<Message<?>> result = service.promise("foo");
		Message<?> reply = result.block(Duration.ofSeconds(1));
		assertEquals("foo", reply.getPayload());
		assertNotNull(TestUtils.getPropertyValue(context.getBean("&promise"), "asyncExecutor"));
	}

	@Test
	public void testAsyncCompletable() throws Exception {
		QueueChannel requestChannel = (QueueChannel) context.getBean("requestChannel");
		final AtomicReference<Thread> thread = new AtomicReference<>();
		requestChannel.addInterceptor(new ChannelInterceptor() {

			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				thread.set(Thread.currentThread());
				return message;
			}

		});
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("asyncCompletable", TestService.class);
		CompletableFuture<String> result = service.completable("foo").thenApply(String::toUpperCase);
		String reply = result.get(10, TimeUnit.SECONDS);
		assertEquals("FOO", reply);
		assertThat(thread.get().getName(), startsWith("testExec-"));
		assertNotNull(TestUtils.getPropertyValue(context.getBean("&asyncCompletable"), "asyncExecutor"));
	}

	@Test
	public void testAsyncCompletableNoAsync() throws Exception {
		QueueChannel requestChannel = (QueueChannel) context.getBean("requestChannel");
		final AtomicReference<Thread> thread = new AtomicReference<>();
		requestChannel.addInterceptor(new ChannelInterceptor() {

			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				thread.set(Thread.currentThread());
				return message;
			}

		});
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("completableNoAsync", TestService.class);
		CompletableFuture<String> result = service.completable("flowCompletable");
		String reply = result.get(10, TimeUnit.SECONDS);
		assertEquals("SYNC_COMPLETABLE", reply);
		assertEquals(Thread.currentThread(), thread.get());
		assertNull(TestUtils.getPropertyValue(context.getBean("&completableNoAsync"), "asyncExecutor"));
	}

	@Test
	public void testCustomCompletableNoAsync() throws Exception {
		QueueChannel requestChannel = (QueueChannel) context.getBean("requestChannel");
		final AtomicReference<Thread> thread = new AtomicReference<>();
		requestChannel.addInterceptor(new ChannelInterceptor() {

			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				thread.set(Thread.currentThread());
				return message;
			}

		});
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("completableNoAsync", TestService.class);
		MyCompletableFuture result = service.customCompletable("flowCustomCompletable");
		String reply = result.get(10, TimeUnit.SECONDS);
		assertEquals("SYNC_CUSTOM_COMPLETABLE", reply);
		assertEquals(Thread.currentThread(), thread.get());
		assertNull(TestUtils.getPropertyValue(context.getBean("&completableNoAsync"), "asyncExecutor"));
	}

	@Test
	public void testCustomCompletableNoAsyncAttemptAsync() throws Exception {
		Object gateway = context.getBean("&customCompletableAttemptAsync");
		Log logger = spy(TestUtils.getPropertyValue(gateway, "logger", Log.class));
		when(logger.isDebugEnabled()).thenReturn(true);
		new DirectFieldAccessor(gateway).setPropertyValue("logger", logger);
		QueueChannel requestChannel = (QueueChannel) context.getBean("requestChannel");
		final AtomicReference<Thread> thread = new AtomicReference<>();
		requestChannel.addInterceptor(new ChannelInterceptor() {

			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				thread.set(Thread.currentThread());
				return message;
			}

		});
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("customCompletableAttemptAsync", TestService.class);
		MyCompletableFuture result = service.customCompletable("flowCustomCompletable");
		String reply = result.get(10, TimeUnit.SECONDS);
		assertEquals("SYNC_CUSTOM_COMPLETABLE", reply);
		assertEquals(Thread.currentThread(), thread.get());
		assertNotNull(TestUtils.getPropertyValue(gateway, "asyncExecutor"));
		verify(logger).debug("AsyncTaskExecutor submit*() return types are incompatible with the method return type; "
							+ "running on calling thread; the downstream flow must return the required Future: "
							+ "MyCompletableFuture");
	}

	@Test
	public void testAsyncCompletableMessage() throws Exception {
		QueueChannel requestChannel = (QueueChannel) context.getBean("requestChannel");
		final AtomicReference<Thread> thread = new AtomicReference<>();
		requestChannel.addInterceptor(new ChannelInterceptor() {

			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				thread.set(Thread.currentThread());
				return message;
			}

		});
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("asyncCompletable", TestService.class);
		CompletableFuture<Message<?>> result = service.completableReturnsMessage("foo");
		Message<?> reply = result.get(10, TimeUnit.SECONDS);
		assertEquals("foo", reply.getPayload());
		assertThat(thread.get().getName(), startsWith("testExec-"));
		assertNotNull(TestUtils.getPropertyValue(context.getBean("&asyncCompletable"), "asyncExecutor"));
	}

	@Test
	public void testAsyncCompletableNoAsyncMessage() throws Exception {
		QueueChannel requestChannel = (QueueChannel) context.getBean("requestChannel");
		final AtomicReference<Thread> thread = new AtomicReference<>();
		requestChannel.addInterceptor(new ChannelInterceptor() {

			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				thread.set(Thread.currentThread());
				return message;
			}

		});
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("completableNoAsync", TestService.class);
		CompletableFuture<Message<?>> result = service.completableReturnsMessage("flowCompletableM");
		Message<?> reply = result.get(10, TimeUnit.SECONDS);
		assertEquals("flowCompletableM", reply.getPayload());
		assertEquals(Thread.currentThread(), thread.get());
		assertNull(TestUtils.getPropertyValue(context.getBean("&completableNoAsync"), "asyncExecutor"));
	}

	@Test
	public void testCustomCompletableNoAsyncMessage() throws Exception {
		QueueChannel requestChannel = (QueueChannel) context.getBean("requestChannel");
		final AtomicReference<Thread> thread = new AtomicReference<>();
		requestChannel.addInterceptor(new ChannelInterceptor() {

			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				thread.set(Thread.currentThread());
				return message;
			}

		});
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("completableNoAsync", TestService.class);
		MyCompletableMessageFuture result = service.customCompletableReturnsMessage("flowCustomCompletableM");
		Message<?> reply = result.get(10, TimeUnit.SECONDS);
		assertEquals("flowCustomCompletableM", reply.getPayload());
		assertEquals(Thread.currentThread(), thread.get());
		assertNull(TestUtils.getPropertyValue(context.getBean("&completableNoAsync"), "asyncExecutor"));
	}

	private void startResponder(final PollableChannel requestChannel, final MessageChannel replyChannel) {
		Executors.newSingleThreadExecutor().execute(() -> {
			Message<?> request = requestChannel.receive(60000);
			assertNotNull("Request not received", request);
			Message<?> reply = MessageBuilder.fromMessage(request)
					.setCorrelationId(request.getHeaders().getId()).build();
			Object payload = null;
			if (request.getPayload().equals("futureSync")) {
				payload = new AsyncResult<Message<?>>(reply);
			}
			else if (request.getPayload().equals("flowCompletable")) {
				payload = CompletableFuture.<String>completedFuture("SYNC_COMPLETABLE");
			}
			else if (request.getPayload().equals("flowCustomCompletable")) {
				MyCompletableFuture myCompletableFuture1 = new MyCompletableFuture();
				myCompletableFuture1.complete("SYNC_CUSTOM_COMPLETABLE");
				payload = myCompletableFuture1;
			}
			else if (request.getPayload().equals("flowCompletableM")) {
				payload = CompletableFuture.<Message<?>>completedFuture(reply);
			}
			else if (request.getPayload().equals("flowCustomCompletableM")) {
				MyCompletableMessageFuture myCompletableFuture2 = new MyCompletableMessageFuture();
				myCompletableFuture2.complete(reply);
				payload = myCompletableFuture2;
			}
			if (payload != null) {
				reply = MessageBuilder.withPayload(payload)
						.copyHeaders(reply.getHeaders())
						.build();
			}
			replyChannel.send(reply);
		});
	}


	@SuppressWarnings("unused")
	private static class TestExecutor extends SimpleAsyncTaskExecutor implements BeanNameAware {

		private static final long serialVersionUID = 1L;

		private volatile String beanName;

		TestExecutor() {
			setThreadNamePrefix("testExec-");
		}

		@Override
		public void setBeanName(String beanName) {
			this.beanName = beanName;
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public <T> Future<T> submit(Callable<T> task) {
			try {
				Future<?> result = super.submit(task);
				Message<?> message = (Message<?>) result.get(10, TimeUnit.SECONDS);
				Message<?> modifiedMessage;
				if (message == null) {
					modifiedMessage = MessageBuilder.withPayload("foo")
							.setHeader("executor", this.beanName).build();
				}
				else {
					modifiedMessage = MessageBuilder.fromMessage(message)
						.setHeader("executor", this.beanName).build();
				}
				return new AsyncResult(modifiedMessage);
			}
			catch (Exception e) {
				throw new IllegalStateException("unexpected exception in testExecutor", e);
			}
		}

	}

}
