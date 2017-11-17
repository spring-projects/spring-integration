/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.handler;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasHeaderKey;

import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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

	@Test
	public void testGateway() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.gatewayTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertEquals("gatewayTestInputChannel,gatewayTestService,gateway,requestChannel,replyChannel",
				reply.getHeaders().get("history").toString());

		message = MessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build();
		try {
			this.gatewayTestInputChannel.send(message);
			fail("Exception expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageHandlingException.class));
			assertThat(e.getCause(), instanceOf(MessageTransformationException.class));
			assertThat(e.getCause().getCause(), instanceOf(MessageHandlingException.class));
			assertThat(e.getCause().getCause().getCause(), instanceOf(java.lang.IllegalStateException.class));
			assertThat(e.getMessage(), containsString("Expression evaluation failed"));
			assertThat(e.getMessage(), containsString("Wrong payload"));
		}
	}

	@Test
	public void testReplyingMessageHandler() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.replyingHandlerTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertEquals("TEST", reply.getPayload());
		assertEquals("replyingHandlerTestInputChannel,replyingHandlerTestService",
				reply.getHeaders().get("history").toString());
		StackTraceElement[] st = (StackTraceElement[]) reply.getHeaders().get("callStack");
		assertTrue(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel",
				"MethodInvokerHelper", st)); // close to the metal
	}

	@Test
	public void testOptimizedReplyingMessageHandler() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.optimizedRefReplyingHandlerTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertEquals("TEST", reply.getPayload());
		assertEquals("optimizedRefReplyingHandlerTestInputChannel,optimizedRefReplyingHandlerTestService",
				reply.getHeaders().get("history").toString());
		StackTraceElement[] st = (StackTraceElement[]) reply.getHeaders().get("callStack");
		assertTrue(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel",
				"MethodInvokerHelper", st)); // close to the metal
	}

	@Test
	public void testReplyingMessageHandlerWithStandardMethod() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.replyingHandlerWithStandardMethodTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertEquals("TEST", reply.getPayload());
		assertEquals("replyingHandlerWithStandardMethodTestInputChannel,replyingHandlerWithStandardMethodTestService",
				reply.getHeaders().get("history").toString());
		StackTraceElement[] st = (StackTraceElement[]) reply.getHeaders().get("callStack");
		assertTrue(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel",
				"MethodInvokerHelper", st)); // close to the metal
	}

	@Test
	public void testReplyingMessageHandlerWithOtherMethod() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.replyingHandlerWithOtherMethodTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertEquals("bar", reply.getPayload());
		assertEquals("replyingHandlerWithOtherMethodTestInputChannel,replyingHandlerWithOtherMethodTestService",
				reply.getHeaders().get("history").toString());
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
		assertSame(testMessageProcessor, processor);

		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("bar")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "foo")
				.build();
		this.processorTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertEquals("foo:bar", reply.getPayload());
		assertThat(reply, not(hasHeaderKey("foo")));
	}

	@Test
	public void testFailOnDoubleReference() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail-context.xml",
					this.getClass()).close();
			fail("Expected exception due to 2 endpoints referencing the same bean");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(BeanCreationException.class));
			assertThat(e.getCause(), Matchers.instanceOf(BeanCreationException.class));
			assertThat(e.getCause().getCause(), Matchers.instanceOf(IllegalArgumentException.class));
			assertThat(e.getCause().getCause().getMessage(),
					Matchers.containsString("An AbstractMessageProducingMessageHandler may only be referenced once"));
		}

	}

	@Test
	public void testAsync() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.asyncIn.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertNull(reply);
		this.asyncService.future.set(this.asyncService.payload.toUpperCase());
		reply = replyChannel.receive(0);
		assertNotNull(reply);
		assertEquals("TEST", reply.getPayload());
	}

	@Test
	public void testAsyncWithDirectReply() {
		DirectChannel replyChannel = new DirectChannel();
		final AtomicReference<Message<?>> reply = new AtomicReference<Message<?>>();
		replyChannel.subscribe(reply::set);

		Message<?> message = MessageBuilder.withPayload("testing").setReplyChannel(replyChannel).build();
		this.asyncIn.send(message);
		assertNull(reply.get());
		this.asyncService.future.set(this.asyncService.payload.toUpperCase());
		assertNotNull(reply.get());
		assertEquals("TESTING", reply.get().getPayload());
	}

	@Test
	public void testAsyncError() {
		QueueChannel errorChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setErrorChannel(errorChannel).build();
		this.asyncIn.send(message);
		this.asyncService.future.setException(new RuntimeException("intended"));
		Message<?> error = errorChannel.receive(0);
		assertNotNull(error);
		assertThat(error, instanceOf(ErrorMessage.class));
		assertThat(error.getPayload(), instanceOf(MessagingException.class));
		assertThat(((MessagingException) error.getPayload()).getCause(), instanceOf(RuntimeException.class));
		assertThat(((MessagingException) error.getPayload()).getCause().getMessage(), equalTo("intended"));
		assertEquals("test", ((MessagingException) error.getPayload()).getFailedMessage().getPayload());
	}

	@Test
	public void testAsyncErrorNoHeader() {
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.asyncIn.send(message);
		this.asyncService.future.setException(new RuntimeException("intended"));
		Message<?> error = this.errorChannel.receive(0);
		assertNotNull(error);
		assertThat(error, instanceOf(ErrorMessage.class));
		assertThat(error.getPayload(), instanceOf(MessagingException.class));
		assertThat(((MessagingException) error.getPayload()).getCause(), instanceOf(RuntimeException.class));
		assertThat(((MessagingException) error.getPayload()).getCause().getMessage(), equalTo("intended"));
		assertEquals("test", ((MessagingException) error.getPayload()).getFailedMessage().getPayload());
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
			assertFalse(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel",
					"MethodInvokerHelper", st));
			return "bar";
		}

	}

	@SuppressWarnings("unused")
	private static class TestMessageHandler implements MessageHandler {

		@Override
		public void handleMessage(Message<?> requestMessage) {
			Exception e = new RuntimeException();
			StackTraceElement[] st = e.getStackTrace();
			assertTrue(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel",
					"MethodInvokerHelper", st)); // close to the metal
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
			this.future = new SettableListenableFuture<String>();
			this.payload = payload;
			return this.future;
		}

	}

}
