/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * See INT-1688 for background.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
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
	private MessageProcessor<?> testMessageProcessor;

	@Test
	public void testGateway() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.gatewayTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertEquals("gatewayTestInputChannel,gatewayTestService,gateway,requestChannel,bridge,replyChannel", reply.getHeaders().get("history").toString());
	}

	@Test
	public void testReplyingMessageHandler() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.replyingHandlerTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertEquals("TEST", reply.getPayload());
		assertEquals("replyingHandlerTestInputChannel,replyingHandlerTestService", reply.getHeaders().get("history").toString());
		StackTraceElement[] st = (StackTraceElement[]) reply.getHeaders().get("callStack");
		assertTrue(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel", "MethodInvokerHelper", st));
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
		assertTrue(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel", "MethodInvokerHelper", st));
	}

	@Test
	public void testReplyingMessageHandlerWithStandardMethod() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.replyingHandlerWithStandardMethodTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertEquals("TEST", reply.getPayload());
		assertEquals("replyingHandlerWithStandardMethodTestInputChannel,replyingHandlerWithStandardMethodTestService", reply.getHeaders().get("history").toString());
		StackTraceElement[] st = (StackTraceElement[]) reply.getHeaders().get("callStack");
		assertTrue(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel", "MethodInvokerHelper", st));
	}

	@Test
	public void testReplyingMessageHandlerWithOtherMethod() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.replyingHandlerWithOtherMethodTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertEquals("bar", reply.getPayload());
		assertEquals("replyingHandlerWithOtherMethodTestInputChannel,replyingHandlerWithOtherMethodTestService", reply.getHeaders().get("history").toString());
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
		Object processor = TestUtils.getPropertyValue(processorTestService, "handler.h.advised.targetSource.target.processor");
		assertSame(testMessageProcessor, processor);

		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("bar").setReplyChannel(replyChannel).build();
		this.processorTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertEquals("foo:bar", reply.getPayload());
		assertEquals("processorTestInputChannel,processorTestService", reply.getHeaders().get("history").toString());
	}

	@Test
	public void testFailOnDoubleReference() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail-context.xml",
					this.getClass());
			fail("Expected exception due to 2 endpoints referencing the same bean");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(BeanCreationException.class));
			assertThat(e.getCause(), Matchers.instanceOf(BeanCreationException.class));
			assertThat(e.getCause().getCause(), Matchers.instanceOf(IllegalArgumentException.class));
			assertThat(e.getCause().getCause().getMessage(),
					Matchers.containsString("An AbstractReplyProducingMessageHandler may only be referenced once"));
		}

	}

	private interface Foo {

		public String foo(String in);

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
			assertFalse(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel", "MethodInvokerHelper", st));
			return "bar";
		}

	}

	@SuppressWarnings("unused")
	private static class TestMessageHandler implements MessageHandler {

		@Override
		public void handleMessage(Message<?> requestMessage) {
			Exception e = new RuntimeException();
			StackTraceElement[] st = e.getStackTrace();
			assertTrue(StackTraceUtils.isFrameContainingXBeforeFrameContainingY("AbstractSubscribableChannel", "MethodInvokerHelper", st));
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
