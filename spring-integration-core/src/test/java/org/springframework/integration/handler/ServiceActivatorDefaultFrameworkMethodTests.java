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

package org.springframework.integration.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.integration.util.StackTraceUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * See INT-1688 for background.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0.1
 */
@SpringJUnitConfig
public class ServiceActivatorDefaultFrameworkMethodTests {

	@Autowired
	private MessageChannel gatewayTestInputChannel;

	@Autowired
	private MessageChannel replyingHandlerTestInputChannel;

	@Autowired
	private MessageChannel optimizedRefReplyingHandlerTestInputChannel;

	@Autowired
	private MessageChannel replyingHandlerWithStandardMethodTestInputChannel;

	@Autowired
	private MessageChannel replyingHandlerWithOtherMethodTestInputChannel;

	@Autowired
	private MessageChannel handlerTestInputChannel;

	@Autowired
	private MessageChannel processorTestInputChannel;

	@Autowired
	private EventDrivenConsumer processorTestService;

	@Autowired
	private TestMessageProcessor testMessageProcessor;

	@Autowired
	private MessageChannel asyncIn;

	@Autowired
	private AsyncService asyncService;

	@Autowired
	private PollableChannel errorChannel;

	@Autowired
	private MessageChannel processorViaFunctionChannel;

	@Test
	public void testGateway() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.gatewayTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply.getHeaders().get("history").toString())
				.isEqualTo("gatewayTestInputChannel,gatewayTestService," +
						"gateway#exchange(Message),requestChannel,replyChannel");

		Message<?> message2 = MessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build();

		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> this.gatewayTestInputChannel.send(message2))
				.withCauseInstanceOf(MessageHandlingException.class)
				.withRootCauseInstanceOf(IllegalStateException.class)
				.withMessageContaining("Expression evaluation failed")
				.withMessageContaining("Wrong payload");
	}

	@Test
	public void testReplyingMessageHandler() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.replyingHandlerTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply.getPayload()).isEqualTo("TEST");
		assertThat(reply.getHeaders().get("history").toString())
				.isEqualTo("replyingHandlerTestInputChannel,replyingHandlerTestService");
		StackTraceElement[] st = (StackTraceElement[]) reply.getHeaders().get("callStack");
		assertThat(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel",
				"MethodInvokerHelper", st)).isTrue(); // close to the metal
	}

	@Test
	public void testOptimizedReplyingMessageHandler() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.optimizedRefReplyingHandlerTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply.getPayload()).isEqualTo("TEST");
		assertThat(reply.getHeaders().get("history").toString())
				.isEqualTo("optimizedRefReplyingHandlerTestInputChannel,optimizedRefReplyingHandlerTestService");
		StackTraceElement[] st = (StackTraceElement[]) reply.getHeaders().get("callStack");
		assertThat(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel",
				"MethodInvokerHelper", st)).isTrue(); // close to the metal
	}

	@Test
	public void testReplyingMessageHandlerWithStandardMethod() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.replyingHandlerWithStandardMethodTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply.getPayload()).isEqualTo("TEST");
		assertThat(reply.getHeaders().get("history").toString())
				.isEqualTo("replyingHandlerWithStandardMethodTestInputChannel," +
						"replyingHandlerWithStandardMethodTestService");
		StackTraceElement[] st = (StackTraceElement[]) reply.getHeaders().get("callStack");
		assertThat(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel",
				"MethodInvokerHelper", st)).isTrue(); // close to the metal
	}

	@Test
	public void testReplyingMessageHandlerWithOtherMethod() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.replyingHandlerWithOtherMethodTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply.getPayload()).isEqualTo("bar");
		assertThat(reply.getHeaders().get("history").toString())
				.isEqualTo("replyingHandlerWithOtherMethodTestInputChannel,replyingHandlerWithOtherMethodTestService");
	}

	@Test
	public void testMessageHandler() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.handlerTestInputChannel.send(message);
	}

	@Test
	public void testMessageProcessor() {
		Object processor = TestUtils.getPropertyValue(processorTestService, "handler.processor");
		assertThat(processor).isSameAs(testMessageProcessor);

		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("bar")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "foo")
				.build();
		this.processorTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply.getPayload()).isEqualTo("foo:bar");
		assertThat(reply.getHeaders()).doesNotContainKey("foo");
	}

	@Test
	public void testFailOnDoubleReference() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-fail-context.xml", getClass()))
				.withMessageContaining("An AbstractMessageProducingMessageHandler may only be referenced once")
				.withRootCauseExactlyInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testAsync() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.asyncIn.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply).isNull();
		this.asyncService.future.set(this.asyncService.payload.toUpperCase());
		reply = replyChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("TEST");
	}

	@Test
	public void testAsyncWithDirectReply() {
		DirectChannel replyChannel = new DirectChannel();
		final AtomicReference<Message<?>> reply = new AtomicReference<>();
		replyChannel.subscribe(reply::set);

		Message<?> message = MessageBuilder.withPayload("testing").setReplyChannel(replyChannel).build();
		this.asyncIn.send(message);
		assertThat(reply.get()).isNull();
		this.asyncService.future.set(this.asyncService.payload.toUpperCase());
		assertThat(reply.get()).isNotNull();
		assertThat(reply.get().getPayload()).isEqualTo("TESTING");
	}

	@Test
	public void testAsyncError() {
		QueueChannel errorChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setErrorChannel(errorChannel).build();
		this.asyncIn.send(message);
		this.asyncService.future.setException(new RuntimeException("intended"));
		Message<?> error = errorChannel.receive(0);
		assertThat(error).isNotNull();
		assertThat(error).isInstanceOf(ErrorMessage.class);
		assertThat(error.getPayload()).isInstanceOf(MessagingException.class);
		assertThat(((MessagingException) error.getPayload()).getCause()).isInstanceOf(RuntimeException.class);
		assertThat(((MessagingException) error.getPayload()).getCause().getMessage()).isEqualTo("intended");
		assertThat(((MessagingException) error.getPayload()).getFailedMessage().getPayload()).isEqualTo("test");
	}

	@Test
	public void testAsyncErrorNoHeader() {
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.asyncIn.send(message);
		this.asyncService.future.setException(new RuntimeException("intended"));
		Message<?> error = this.errorChannel.receive(0);
		assertThat(error).isNotNull();
		assertThat(error).isInstanceOf(ErrorMessage.class);
		assertThat(error.getPayload()).isInstanceOf(MessagingException.class);
		assertThat(((MessagingException) error.getPayload()).getCause()).isInstanceOf(RuntimeException.class);
		assertThat(((MessagingException) error.getPayload()).getCause().getMessage()).isEqualTo("intended");
		assertThat(((MessagingException) error.getPayload()).getFailedMessage().getPayload()).isEqualTo("test");
	}

	@Test
	public void testFunctionFromXml() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.processorViaFunctionChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("TEST");
	}

	public static void throwIllegalStateException(String message) {
		throw new IllegalStateException(message);
	}

	@SuppressWarnings("unused")
	private static class TestReplyingMessageHandler extends AbstractReplyProducingMessageHandler {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			Exception e = new RuntimeException();
			StackTraceElement[] st = e.getStackTrace();
			return MessageBuilder.withPayload(requestMessage.getPayload().toString().toUpperCase())
					.setHeader("callStack", st);
		}

		public String foo(String in) {
			Exception e = new RuntimeException();
			StackTraceElement[] st = e.getStackTrace();
			// use this to test that StackTraceUtils works as expected and returns false
			assertThat(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel",
					"MethodInvokerHelper", st)).isFalse();
			return "bar";
		}

	}

	@SuppressWarnings("unused")
	private static class TestMessageHandler implements MessageHandler {

		@Override
		public void handleMessage(Message<?> requestMessage) {
			Exception e = new RuntimeException();
			StackTraceElement[] st = e.getStackTrace();
			assertThat(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel",
					"MethodInvokerHelper", st)).isTrue(); // close to the metal
		}

	}

	private static class TestMessageProcessor implements MessageProcessor<String> {

		private String prefix;

		@SuppressWarnings("unused")
		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public String processMessage(Message<?> message) {
			return prefix + ":" + message.getPayload();
		}

	}

	private static class AsyncService {

		private volatile SettableListenableFuture<String> future;

		private volatile String payload;

		@SuppressWarnings("unused")
		public ListenableFuture<String> process(String payload) {
			this.future = new SettableListenableFuture<>();
			this.payload = payload;
			return this.future;
		}

	}

	public static class FunctionConfiguration {

		@Bean
		public Function<String, String> functionAsService() {
			return String::toUpperCase;
		}

	}


}
