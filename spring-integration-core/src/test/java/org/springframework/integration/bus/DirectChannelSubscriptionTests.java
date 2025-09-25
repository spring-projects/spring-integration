/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.bus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.IntegrationRegistrar;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class DirectChannelSubscriptionTests {

	private final GenericApplicationContext context = new GenericApplicationContext();

	private final DirectChannel sourceChannel = new DirectChannel();

	private final PollableChannel targetChannel = new QueueChannel();

	@BeforeEach
	public void setupChannels() {
		new IntegrationRegistrar().registerBeanDefinitions(mock(), this.context.getDefaultListableBeanFactory());
		TestUtils.registerBean("sourceChannel", this.sourceChannel, this.context);
		TestUtils.registerBean("targetChannel", this.targetChannel, this.context);
	}

	@AfterEach
	public void tearDown() {
		this.context.close();
	}

	@Test
	public void sendAndReceiveForRegisteredEndpoint() {
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(new TestBean(), "handle");
		serviceActivator.setOutputChannel(this.targetChannel);
		TestUtils.registerBean("testServiceActivator", serviceActivator, this.context);
		EventDrivenConsumer endpoint = new EventDrivenConsumer(this.sourceChannel, serviceActivator);
		TestUtils.registerBean("testEndpoint", endpoint, this.context);
		context.refresh();
		this.sourceChannel.send(new GenericMessage<>("foo"));
		Message<?> response = this.targetChannel.receive();
		assertThat(response.getPayload()).isEqualTo("foo!");
	}

	@Test
	public void sendAndReceiveForAnnotatedEndpoint() {
		TestUtils.registerBean("testEndpoint", new TestEndpoint(), this.context);
		this.context.refresh();

		this.sourceChannel.send(new GenericMessage<>("foo"));
		Message<?> response = this.targetChannel.receive();
		assertThat(response.getPayload()).isEqualTo("foo-from-annotated-endpoint");
	}

	@Test
	public void exceptionThrownFromRegisteredEndpoint() {
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			public Object handleRequestMessage(Message<?> message) {
				throw new RuntimeException("intentional test failure");
			}
		};
		handler.setOutputChannel(targetChannel);
		EventDrivenConsumer endpoint = new EventDrivenConsumer(sourceChannel, handler);
		TestUtils.registerBean("testEndpoint", endpoint, this.context);
		this.context.refresh();
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.sourceChannel.send(new GenericMessage<>("foo")));
	}

	@Test
	public void exceptionThrownFromAnnotatedEndpoint() {
		TestUtils.registerBean("testEndpoint", new FailingTestEndpoint(), this.context);
		this.context.refresh();
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.sourceChannel.send(new GenericMessage<>("foo")));
	}

	static class TestBean {

		public Message<?> handle(Message<?> message) {
			return new GenericMessage<>(message.getPayload() + "!");
		}

	}

	@MessageEndpoint
	public static class TestEndpoint {

		@ServiceActivator(inputChannel = "sourceChannel", outputChannel = "targetChannel")
		public Message<?> handle(Message<?> message) {
			return new GenericMessage<>(message.getPayload() + "-from-annotated-endpoint");
		}

	}

	@MessageEndpoint
	public static class FailingTestEndpoint {

		@ServiceActivator(inputChannel = "sourceChannel", outputChannel = "targetChannel")
		public Message<?> handle(Message<?> message) {
			throw new RuntimeException("intentional test failure");
		}

	}

}
