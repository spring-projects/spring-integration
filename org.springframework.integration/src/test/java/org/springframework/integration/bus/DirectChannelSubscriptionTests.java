/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.ThreadLocalChannel;
import org.springframework.integration.config.annotation.MessagingAnnotationPostProcessor;
import org.springframework.integration.config.xml.MessageBusParser;
import org.springframework.integration.endpoint.AbstractReplyProducingMessageConsumer;
import org.springframework.integration.endpoint.ReplyMessageHolder;
import org.springframework.integration.endpoint.ServiceActivatingConsumer;
import org.springframework.integration.endpoint.SubscribingConsumerEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.util.TestUtils;

/**
 * @author Mark Fisher
 */
public class DirectChannelSubscriptionTests {

	private GenericApplicationContext context = new GenericApplicationContext();

	private DefaultMessageBus bus = new DefaultMessageBus();

	private DirectChannel sourceChannel = new DirectChannel();

	private ThreadLocalChannel targetChannel = new ThreadLocalChannel();


	@Before
	public void setupChannels() {
		sourceChannel.setBeanName("sourceChannel");
		targetChannel.setBeanName("targetChannel");
		context.getBeanFactory().registerSingleton("sourceChannel", sourceChannel);
		context.getBeanFactory().registerSingleton("targetChannel", targetChannel);
		context.getBeanFactory().registerSingleton(MessageBusParser.MESSAGE_BUS_BEAN_NAME, bus);
		bus.setApplicationContext(context);
		bus.setTaskScheduler(TestUtils.createTaskScheduler(10));
	}


	@Test
	public void sendAndReceiveForRegisteredEndpoint() {
		GenericApplicationContext context = new GenericApplicationContext();
		ServiceActivatingConsumer serviceActivator = new ServiceActivatingConsumer(new TestBean(), "handle");
		serviceActivator.setOutputChannel(targetChannel);
		SubscribingConsumerEndpoint endpoint = new SubscribingConsumerEndpoint(serviceActivator, sourceChannel);
		context.getBeanFactory().registerSingleton("testEndpoint", endpoint);
		bus.setApplicationContext(context);
		bus.start();
		this.sourceChannel.send(new StringMessage("foo"));
		Message<?> response = this.targetChannel.receive();
		assertEquals("foo!", response.getPayload());
		bus.stop();
	}

	@Test
	public void sendAndReceiveForAnnotatedEndpoint() {
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		TestEndpoint endpoint = new TestEndpoint();
		postProcessor.postProcessAfterInitialization(endpoint, "testEndpoint");
		bus.start();
		this.sourceChannel.send(new StringMessage("foo"));
		Message<?> response = this.targetChannel.receive();
		assertEquals("foo-from-annotated-endpoint", response.getPayload());
		bus.stop();
	}

	@Test(expected = MessagingException.class)
	public void exceptionThrownFromRegisteredEndpoint() {
		AbstractReplyProducingMessageConsumer consumer = new AbstractReplyProducingMessageConsumer() {
			public void onMessage(Message<?> message, ReplyMessageHolder replyHolder) {
				throw new RuntimeException("intentional test failure");
			}
		};
		consumer.setOutputChannel(targetChannel);
		SubscribingConsumerEndpoint endpoint = new SubscribingConsumerEndpoint(consumer, sourceChannel);
		bus.registerEndpoint(endpoint);
		bus.start();
		this.sourceChannel.send(new StringMessage("foo"));
	}

	@Test(expected = MessagingException.class)
	public void exceptionThrownFromAnnotatedEndpoint() {
		QueueChannel errorChannel = new QueueChannel();
		errorChannel.setBeanName(DefaultMessageBus.ERROR_CHANNEL_BEAN_NAME);
		context.getBeanFactory().registerSingleton(
				DefaultMessageBus.ERROR_CHANNEL_BEAN_NAME, errorChannel);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		FailingTestEndpoint endpoint = new FailingTestEndpoint();
		postProcessor.postProcessAfterInitialization(endpoint, "testEndpoint");
		bus.start();
		this.sourceChannel.send(new StringMessage("foo"));
	}


	private static class TestBean {

		public Message<?> handle(Message<?> message) {
			return new StringMessage(message.getPayload() + "!");
		}
	}


	@MessageEndpoint
	public static class TestEndpoint {

		@ServiceActivator(inputChannel="sourceChannel", outputChannel="targetChannel")
		public Message<?> handle(Message<?> message) {
			return new StringMessage(message.getPayload() + "-from-annotated-endpoint");
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
