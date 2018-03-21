/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.integration.test.mock;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasHeader;
import static org.springframework.integration.test.matcher.PayloadAndHeaderMatcher.sameExceptIgnorableHeaders;
import static org.springframework.integration.test.matcher.PayloadMatcher.hasPayload;
import static org.springframework.integration.test.mock.MockIntegration.mockMessageHandler;

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.ExpressionEvaluatingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.context.MockIntegrationContext;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 * @author Yicheng Feng
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MockMessageHandlerTests.Config.class)
@SpringIntegrationTest
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

	@After
	public void tearDown() {
		this.mockIntegrationContext.resetBeans();
		results.purge(null);
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
			assertNotNull(receive);
			assertEquals("foo", receive.getPayload());
		}

		List<Message<?>> messages = this.messageArgumentCaptor.getAllValues();

		assertEquals(4, messages.size());

		assertThat(messages.get(0), hasHeader("bar", "BAR"));
		assertThat(messages.get(1),
				sameExceptIgnorableHeaders(MessageBuilder.withPayload("foo")
								.setHeader("baz", "BAZ")
								.build(),
						"bar", MessageHeaders.REPLY_CHANNEL));

		assertThat(messages.get(2), hasPayload("foo"));
		assertThat(messages.get(3), hasPayload("foo"));
	}

	@Test
	public void testMockMessageHandlerPojoService() {
		this.pojoServiceChannel.send(new GenericMessage<>("bar"));

		Message<?> receive = this.results.receive(10000);

		assertNotNull(receive);
		assertEquals("barbar", receive.getPayload());

		MessageHandler mockMessageHandler =
				mockMessageHandler()
						.handleNextAndReply(m -> m.getPayload().toString().toUpperCase());

		this.mockIntegrationContext
				.substituteMessageHandlerFor("mockMessageHandlerTests.Config.myService.serviceActivator",
						mockMessageHandler);

		this.pojoServiceChannel.send(new GenericMessage<>("foo"));
		receive = this.results.receive(10000);

		assertNotNull(receive);
		assertEquals("FOO", receive.getPayload());

		try {
			this.pojoServiceChannel.send(new GenericMessage<>("bar"));
			fail("AssertionError expected");
		}
		catch (Error e) {
			assertThat(e, instanceOf(AssertionError.class));
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
		assertSame(mockMessageHandler, TestUtils.getPropertyValue(endpoint, "handler", MessageHandler.class));

		GenericMessage<String> message = new GenericMessage<>("foo");

		this.rawChannel.send(message);

		verify(mockMessageHandler)
				.handleMessage(message);

		assertSame(message, messageArgumentCaptor.getValue());

		this.mockIntegrationContext.resetBeans(endpointId);

		mockMessageHandler =
				mockMessageHandler()
						.handleNextAndReply(m -> m);

		try {
			this.mockIntegrationContext.substituteMessageHandlerFor(endpointId, mockMessageHandler);
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalStateException.class));
			assertThat(e.getMessage(), containsString("with replies can't replace simple MessageHandler"));
		}
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
		assertEquals(2, list.size());
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
		public EventDrivenConsumer rawHandlerConsumer() {
			return new EventDrivenConsumer(rawChannel(),
					new ExpressionEvaluatingMessageHandler(new ValueExpression<>("test")));
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
					.handleNext(m -> { });
		}

	}

}
