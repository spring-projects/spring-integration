/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.bus;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.annotation.MessagingAnnotationPostProcessor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;

/**
 * @author Mark Fisher
 */
public class DirectChannelSubscriptionTests {

	private TestApplicationContext context = TestUtils.createTestApplicationContext();

	private DirectChannel sourceChannel = new DirectChannel();

	private PollableChannel targetChannel = new QueueChannel();


	@Before
	public void setupChannels() {
		context.registerChannel("sourceChannel", sourceChannel);
		context.registerChannel("targetChannel", targetChannel);
	}


	@Test
	public void sendAndReceiveForRegisteredEndpoint() {
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(new TestBean(), "handle");
		serviceActivator.setOutputChannel(targetChannel);
		EventDrivenConsumer endpoint = new EventDrivenConsumer(sourceChannel, serviceActivator);
		context.registerEndpoint("testEndpoint", endpoint);
		context.refresh();
		this.sourceChannel.send(new GenericMessage<String>("foo"));
		Message<?> response = this.targetChannel.receive();
		assertEquals("foo!", response.getPayload());
		context.stop();
	}

	@Test
	public void sendAndReceiveForAnnotatedEndpoint() {
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		TestEndpoint endpoint = new TestEndpoint();
		postProcessor.postProcessAfterInitialization(endpoint, "testEndpoint");
		context.refresh();
		this.sourceChannel.send(new GenericMessage<String>("foo"));
		Message<?> response = this.targetChannel.receive();
		assertEquals("foo-from-annotated-endpoint", response.getPayload());
		context.stop();
	}

	@Test(expected = MessagingException.class)
	public void exceptionThrownFromRegisteredEndpoint() {
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			public Object handleRequestMessage(Message<?> message) {
				throw new RuntimeException("intentional test failure");
			}
		};
		handler.setOutputChannel(targetChannel);
		EventDrivenConsumer endpoint = new EventDrivenConsumer(sourceChannel, handler);
		context.registerEndpoint("testEndpoint", endpoint);
		context.refresh();
		try {
			this.sourceChannel.send(new GenericMessage<String>("foo"));
		}
		finally {
			context.stop();
		}
	}

	@Test(expected = MessagingException.class)
	public void exceptionThrownFromAnnotatedEndpoint() {
		QueueChannel errorChannel = new QueueChannel();
		context.registerChannel(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME, errorChannel);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		FailingTestEndpoint endpoint = new FailingTestEndpoint();
		postProcessor.postProcessAfterInitialization(endpoint, "testEndpoint");
		context.refresh();
		try {
			this.sourceChannel.send(new GenericMessage<String>("foo"));
		}
		finally {
			context.stop();
		}
	}


	static class TestBean {

		public Message<?> handle(Message<?> message) {
			return new GenericMessage<String>(message.getPayload() + "!");
		}
	}


	@MessageEndpoint
	public static class TestEndpoint {

		@ServiceActivator(inputChannel="sourceChannel", outputChannel="targetChannel")
		public Message<?> handle(Message<?> message) {
			return new GenericMessage<String>(message.getPayload() + "-from-annotated-endpoint");
		}
	}


	@MessageEndpoint
	public static class FailingTestEndpoint {

		@ServiceActivator(inputChannel="sourceChannel", outputChannel="targetChannel")
		public Message<?> handle(Message<?> message) {
			throw new RuntimeException("intentional test failure");
		}
	}

}
