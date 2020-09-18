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

package org.springframework.integration.jmx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.StackTraceUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

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
@DirtiesContext
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
	private MessageProcessor<?> testMessageProcessor;

	@Test
	public void testGateway() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.gatewayTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply.getHeaders().get("history").toString())
				.isEqualTo("gatewayTestInputChannel,gatewayTestService," +
						"gateway#exchange(Message),requestChannel,bridge,replyChannel");
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
		assertThat(StackTraceUtils
				.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel", "MethodInvokerHelper", st))
				.isTrue();
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
		assertThat(StackTraceUtils
				.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel", "MethodInvokerHelper", st))
				.isTrue();
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
		assertThat(StackTraceUtils
				.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel", "MethodInvokerHelper", st))
				.isTrue();
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

	//	INT-2399
	@Test
	public void testMessageProcessor() {
		Object processor = TestUtils.getPropertyValue(processorTestService, "handler.processor");
		assertThat(processor).isSameAs(testMessageProcessor);

		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("bar").setReplyChannel(replyChannel).build();
		this.processorTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply.getPayload()).isEqualTo("foo:bar");
		assertThat(reply.getHeaders().get("history").toString())
				.isEqualTo("processorTestInputChannel,processorTestService");
	}

	@Test
	public void testFailOnDoubleReference() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-fail-context.xml", getClass()))
				.withMessageContaining("An AbstractMessageProducingMessageHandler may only be referenced once")
				.withRootCauseExactlyInstanceOf(IllegalArgumentException.class);
	}

	private interface Foo {

		String foo(String in);

	}

	@SuppressWarnings("unused")
	private static class TestReplyingMessageHandler extends AbstractReplyProducingMessageHandler implements Foo {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			Exception e = new RuntimeException();
			StackTraceElement[] st = e.getStackTrace();
			return MessageBuilder.withPayload(requestMessage.getPayload().toString().toUpperCase())
					.setHeader("callStack", st);
		}

		@Override
		public String foo(String in) {
			Exception e = new RuntimeException();
			StackTraceElement[] st = e.getStackTrace();
			// use this to test that StackTraceUtils works as expected and returns false
			assertThat(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel",
					"MethodInvokerHelper", st))
					.isFalse();
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
					"MethodInvokerHelper", st))
					.isTrue();
		}

	}

	@SuppressWarnings("unused")
	private static class TestMessageProcessor implements MessageProcessor<String> {

		private String prefix;

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public String processMessage(Message<?> message) {
			return prefix + ":" + message.getPayload();
		}

	}

}
