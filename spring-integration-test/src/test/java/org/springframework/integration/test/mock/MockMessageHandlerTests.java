/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.integration.test.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.integration.test.mock.MockIntegration.mockMessageHandler;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Subscriber;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.EndpointId;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.endpoint.ReactiveStreamsConsumer;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.ExpressionEvaluatingMessageHandler;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.context.MockIntegrationContext;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.integration.test.predicate.MessagePredicate;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Artem Bilan
 * @author Yicheng Feng
 *
 * @since 5.0
 */
@SpringJUnitConfig
@ContextConfiguration(classes = MockMessageHandlerTests.Config.class)
@SpringIntegrationTest
@DirtiesContext
public class MockMessageHandlerTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MockIntegrationContext mockIntegrationContext;

	@Autowired
	private MessageChannel mockHandlerChannel;

	@Autowired
	private MessageChannel pojoServiceChannel;

	@Autowired
	private MessageChannel startChannel;

	@Autowired
	private MessageChannel rawChannel;

	@Autowired
	private QueueChannel results;

	@Autowired
	private ArgumentCaptor<Message<?>> argumentCaptorForOutputTest;

	@Autowired
	private ArgumentCaptor<Message<?>> messageArgumentCaptor;

	@AfterEach
	public void tearDown() {
		this.mockIntegrationContext.resetBeans();
		this.results.purge(null);
	}

	@Test
	public void testMockMessageHandler() {
		QueueChannel replies = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("bar", "BAR")
				.setHeader("baz", "BAZ")
				.setReplyChannel(replies)
				.build();

		this.mockHandlerChannel.send(message);
		this.mockHandlerChannel.send(message);
		this.mockHandlerChannel.send(message);

		Message<String> message1 = MessageBuilder.fromMessage(message)
				.removeHeaders("bar", "baz")
				.build();
		this.mockHandlerChannel.send(message1);

		for (int i = 0; i < 4; i++) {
			Message<?> receive = replies.receive(10000);
			assertThat(receive).isNotNull();
			assertThat(receive.getPayload()).isEqualTo("foo");
		}

		List<Message<?>> messages = this.messageArgumentCaptor.getAllValues();

		assertThat(messages).hasSize(4);

		assertThat(messages.get(0).getHeaders()).containsEntry("bar", "BAR");
		assertThat(messages.get(1))
				.matches(
						new MessagePredicate(
								MessageBuilder.withPayload("foo")
										.setHeader("baz", "BAZ")
										.build(),
								"bar", MessageHeaders.REPLY_CHANNEL));

		assertThat(messages.get(2).getPayload()).isEqualTo("foo");
		assertThat(messages.get(3).getPayload()).isEqualTo("foo");
	}

	@Test
	public void testMockMessageHandlerPojoService() {
		this.pojoServiceChannel.send(new GenericMessage<>("bar"));

		Message<?> receive = this.results.receive(10000);

		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("barbar");

		MessageHandler mockMessageHandler =
				mockMessageHandler()
						.handleNextAndReply(m -> m.getPayload().toString().toUpperCase());

		this.mockIntegrationContext
				.substituteMessageHandlerFor("mockMessageHandlerTests.Config.myService.serviceActivator",
						mockMessageHandler);

		this.pojoServiceChannel.send(new GenericMessage<>("foo"));
		receive = this.results.receive(10000);

		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("FOO");

		try {
			this.pojoServiceChannel.send(new GenericMessage<>("bar"));
			fail("AssertionError expected");
		}
		catch (Error e) {
			assertThat(e).isInstanceOf(AssertionError.class);
		}
	}

	@Test
	public void testMockRawHandler() {
		ArgumentCaptor<Message<?>> messageArgumentCaptor = MockIntegration.messageArgumentCaptor();
		MessageHandler mockMessageHandler =
				spy(mockMessageHandler(messageArgumentCaptor))
						.handleNext(m -> { });

		String endpointId = "rawHandlerConsumer";
		this.mockIntegrationContext.substituteMessageHandlerFor(endpointId, mockMessageHandler);

		Object endpoint = this.context.getBean(endpointId);
		assertThat(TestUtils.getPropertyValue(endpoint, "handler", MessageHandler.class)).isSameAs(mockMessageHandler);

		GenericMessage<String> message = new GenericMessage<>("foo");

		this.rawChannel.send(message);

		verify(mockMessageHandler)
				.handleMessage(message);

		assertThat(messageArgumentCaptor.getValue()).isSameAs(message);

		this.mockIntegrationContext.resetBeans(endpointId);

		MessageHandler mockMessageHandler2 =
				mockMessageHandler()
						.handleNextAndReply(m -> m);


		assertThatIllegalStateException()
				.isThrownBy(() ->
						this.mockIntegrationContext.substituteMessageHandlerFor(endpointId, mockMessageHandler2))
				.withMessageContaining("with replies can't replace simple MessageHandler");

		this.mockIntegrationContext.resetBeans();

		assertThat(TestUtils.getPropertyValue(endpoint, "handler", MessageHandler.class))
				.isNotSameAs(mockMessageHandler2);
		assertThat(TestUtils.getPropertyValue(endpoint, "subscriber", Subscriber.class))
				.isNotSameAs(mockMessageHandler2);
	}

	/**
	 * Test whether the output channel is working after the target service activator is substituted.
	 */
	@Test
	public void testHandlerSubstitutionWithOutputChannel() {
		this.mockIntegrationContext
				.substituteMessageHandlerFor("mockMessageHandlerTests.Config.mockService.serviceActivator",
						mockMessageHandler()
								.handleNextAndReply(m -> m));
		Message<String> message = MessageBuilder.withPayload("bar").build();
		this.startChannel.send(message);
		this.startChannel.send(message);
		List<Message<?>> list = argumentCaptorForOutputTest.getAllValues();
		assertThat(list.size()).isEqualTo(2);
	}

	@Autowired
	private MessageChannel logChannel;

	@Test
	@SuppressWarnings("unchecked")
	public void testMockIntegrationContextReset() {
		MockMessageHandler mockMessageHandler = mockMessageHandler();
		mockMessageHandler.handleNext(message -> { });

		this.mockIntegrationContext.substituteMessageHandlerFor("logEndpoint", mockMessageHandler);

		String endpointId = "mockMessageHandlerTests.Config.myService.serviceActivator";
		this.mockIntegrationContext.substituteMessageHandlerFor(endpointId, mockMessageHandler);

		this.logChannel.send(new GenericMessage<>(1));

		this.mockIntegrationContext.resetBeans("logEndpoint");

		this.logChannel.send(new GenericMessage<>(2));

		verify(mockMessageHandler).handleMessage(any(Message.class));

		assertThat(TestUtils.getPropertyValue(this.mockIntegrationContext, "beans", Map.class)).hasSize(1);

		assertThat(
				TestUtils.getPropertyValue(
						this.context.getBean("mockMessageHandlerTests.Config.myService.serviceActivator"), "handler"))
				.isSameAs(mockMessageHandler);
	}


	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public PollableChannel results() {
			return new QueueChannel();
		}

		@Bean
		public ArgumentCaptor<Message<?>> messageArgumentCaptor() {
			return MockIntegration.messageArgumentCaptor();
		}

		@Bean
		public PollableChannel mockHandlerChannel() {
			return new QueueChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "mockHandlerChannel",
				poller = @Poller(fixedDelay = "100"))
		public MessageHandler mockHandler() {
			return mockMessageHandler(messageArgumentCaptor())
					.handleNextAndReply(m -> m)
					.handleNextAndReply(m -> m)
					.handleNextAndReply(m -> "foo");
		}

		@ServiceActivator(inputChannel = "pojoServiceChannel", outputChannel = "results")
		public String myService(String payload) {
			return payload + payload;
		}

		@Bean
		public SubscribableChannel rawChannel() {
			return new DirectChannel();
		}

		@Bean
		public ReactiveStreamsConsumer rawHandlerConsumer() {
			return new ReactiveStreamsConsumer(rawChannel(),
					(MessageHandler) new ExpressionEvaluatingMessageHandler(new ValueExpression<>("test")));
		}

		@ServiceActivator(inputChannel = "startChannel", outputChannel = "nextChannel")
		public String mockService(String payload) {
			return payload + "test";
		}

		@Bean
		public ArgumentCaptor<Message<?>> argumentCaptorForOutputTest() {
			return MockIntegration.messageArgumentCaptor();
		}

		@Bean
		@ServiceActivator(inputChannel = "nextChannel")
		public MessageHandler handleNextInput() {
			return mockMessageHandler(argumentCaptorForOutputTest())
					.handleNext(m -> {
					});
		}

		@Bean
		@EndpointId("logEndpoint")
		@ServiceActivator(inputChannel = "logChannel")
		public MessageHandler logHandler() {
			return new LoggingHandler(LoggingHandler.Level.FATAL);
		}

	}

}
