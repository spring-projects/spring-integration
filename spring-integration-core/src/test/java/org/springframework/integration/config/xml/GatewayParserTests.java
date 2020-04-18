/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.config.xml;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.expression.Expression;
import org.springframework.integration.channel.QueueChannel;
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
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.test.StepVerifier;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
@SpringJUnitConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GatewayParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private SubscribableChannel errorChannel;

	@Test
	public void testOneWay() {
		TestService service = (TestService) context.getBean("oneWay");
		service.oneWay("foo");
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		Message<?> result = channel.receive(10000);
		assertThat(result.getPayload()).isEqualTo("foo");

		MonoProcessor<Object> defaultMethodHandler = MonoProcessor.create();

		this.errorChannel.subscribe(message -> defaultMethodHandler.onNext(message.getPayload()));

		String defaultMethodPayload = "defaultMethodPayload";
		service.defaultMethodGateway(defaultMethodPayload);

		StepVerifier.create(defaultMethodHandler)
				.expectNext(defaultMethodPayload)
				.verifyComplete();
	}

	@Test
	public void testOneWayOverride() {
		TestService service = (TestService) context.getBean("methodOverride");
		service.oneWay("foo");
		PollableChannel channel = (PollableChannel) context.getBean("otherRequestChannel");
		Message<?> result = channel.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("fiz");
		assertThat(result.getHeaders().get("foo")).isEqualTo("bar");
		assertThat(result.getHeaders().get("baz")).isEqualTo("qux");
		GatewayProxyFactoryBean fb = context.getBean("&methodOverride", GatewayProxyFactoryBean.class);
		assertThat(TestUtils.getPropertyValue(fb, "defaultRequestTimeout", Expression.class).getValue())
				.isEqualTo(1000L);
		assertThat(TestUtils.getPropertyValue(fb, "defaultReplyTimeout", Expression.class).getValue()).isEqualTo(2000L);
		Map<?, ?> methods = TestUtils.getPropertyValue(fb, "methodMetadataMap", Map.class);
		GatewayMethodMetadata meta = (GatewayMethodMetadata) methods.get("oneWay");
		assertThat(meta).isNotNull();
		assertThat(meta.getRequestTimeout()).isEqualTo("456");
		assertThat(meta.getReplyTimeout()).isEqualTo("123");
		assertThat(meta.getReplyChannelName()).isEqualTo("foo");
		meta = (GatewayMethodMetadata) methods.get("oneWayWithTimeouts");
		assertThat(meta).isNotNull();
		assertThat(meta.getRequestTimeout()).isEqualTo("args[1]");
		assertThat(meta.getReplyTimeout()).isEqualTo("args[2]");
		service.oneWayWithTimeouts("foo", 100L, 200L);
		result = channel.receive(10000);
		assertThat(result).isNotNull();
	}

	@Test
	public void testSolicitResponse() {
		PollableChannel channel = (PollableChannel) context.getBean("replyChannel");
		channel.send(new GenericMessage<>("foo"));
		TestService service = (TestService) context.getBean("solicitResponse");
		String result = service.solicitResponse();
		assertThat(result).isEqualTo("foo");
	}

	@Test
	public void testRequestReply() {
		PollableChannel requestChannel = (PollableChannel) context.getBean("requestChannel");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = (TestService) context.getBean("requestReply");
		String result = service.requestReply("foo");
		assertThat(result).isEqualTo("foo");
	}

	@Test
	public void testAsyncGateway() throws Exception {
		PollableChannel requestChannel = (PollableChannel) context.getBean("requestChannel");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("async", TestService.class);
		Future<Message<?>> result = service.async("foo");
		Message<?> reply = result.get(10, TimeUnit.SECONDS);
		assertThat(reply.getPayload()).isEqualTo("foo");
		assertThat(reply.getHeaders().get("executor")).isEqualTo("testExecutor");
		assertThat(TestUtils.getPropertyValue(context.getBean("&async"), "asyncExecutor")).isNotNull();
	}

	@Test
	public void testAsyncDisabledGateway() throws Exception {
		PollableChannel requestChannel = (PollableChannel) context.getBean("requestChannel");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("asyncOff", TestService.class);
		Future<Message<?>> result = service.async("futureSync");
		Message<?> reply = result.get(10, TimeUnit.SECONDS);
		assertThat(reply.getPayload()).isEqualTo("futureSync");
		Object serviceBean = context.getBean("&asyncOff");
		assertThat(TestUtils.getPropertyValue(serviceBean, "asyncExecutor")).isNull();
	}

	@Test
	public void testFactoryBeanObjectTypeWithServiceInterface() {
		ConfigurableListableBeanFactory beanFactory = ((GenericApplicationContext) context).getBeanFactory();
		Object attribute =
				beanFactory.getMergedBeanDefinition("&oneWay").getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
		assertThat(attribute).isEqualTo(TestService.class.getName());
	}

	@Test
	public void testFactoryBeanObjectTypeWithNoServiceInterface() {
		ConfigurableListableBeanFactory beanFactory = ((GenericApplicationContext) context).getBeanFactory();
		Object attribute =
				beanFactory.getMergedBeanDefinition("&defaultConfig").getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
		assertThat(attribute).isEqualTo(RequestReplyExchanger.class.getName());
	}

	@Test
	public void testMonoGateway() {
		PollableChannel requestChannel = context.getBean("requestChannel", PollableChannel.class);
		MessageChannel replyChannel = context.getBean("replyChannel", MessageChannel.class);
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("promise", TestService.class);
		Mono<Message<?>> result = service.promise("foo");
		Message<?> reply = result.block(Duration.ofSeconds(10));
		assertThat(reply.getPayload()).isEqualTo("foo");
		assertThat(TestUtils.getPropertyValue(context.getBean("&promise"), "asyncExecutor")).isNotNull();
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
		assertThat(reply).isEqualTo("FOO");
		assertThat(thread.get().getName()).startsWith("testExec-");
		assertThat(TestUtils.getPropertyValue(context.getBean("&asyncCompletable"), "asyncExecutor")).isNotNull();
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
		assertThat(reply).isEqualTo("SYNC_COMPLETABLE");
		assertThat(thread.get()).isEqualTo(Thread.currentThread());
		assertThat(TestUtils.getPropertyValue(context.getBean("&completableNoAsync"), "asyncExecutor")).isNull();
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
		assertThat(reply).isEqualTo("SYNC_CUSTOM_COMPLETABLE");
		assertThat(thread.get()).isEqualTo(Thread.currentThread());
		assertThat(TestUtils.getPropertyValue(context.getBean("&completableNoAsync"), "asyncExecutor")).isNull();
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
		assertThat(reply).isEqualTo("SYNC_CUSTOM_COMPLETABLE");
		assertThat(thread.get()).isEqualTo(Thread.currentThread());
		assertThat(TestUtils.getPropertyValue(gateway, "asyncExecutor")).isNotNull();
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
		assertThat(reply.getPayload()).isEqualTo("foo");
		assertThat(thread.get().getName()).startsWith("testExec-");
		assertThat(TestUtils.getPropertyValue(context.getBean("&asyncCompletable"), "asyncExecutor")).isNotNull();
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
		assertThat(reply.getPayload()).isEqualTo("flowCompletableM");
		assertThat(thread.get()).isEqualTo(Thread.currentThread());
		assertThat(TestUtils.getPropertyValue(context.getBean("&completableNoAsync"), "asyncExecutor")).isNull();
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
		assertThat(reply.getPayload()).isEqualTo("flowCustomCompletableM");
		assertThat(thread.get()).isEqualTo(Thread.currentThread());
		assertThat(TestUtils.getPropertyValue(context.getBean("&completableNoAsync"), "asyncExecutor")).isNull();
	}

	private void startResponder(final PollableChannel requestChannel, final MessageChannel replyChannel) {
		Executors.newSingleThreadExecutor().execute(() -> {
			Message<?> request = requestChannel.receive(60000);
			assertThat(request).as("Request not received").isNotNull();
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
		@SuppressWarnings({ "rawtypes", "unchecked" })
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
