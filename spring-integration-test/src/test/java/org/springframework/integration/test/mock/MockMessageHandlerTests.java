/*
 * Copyright 2017 the original author or authors.
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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasHeader;
import static org.springframework.integration.test.matcher.PayloadMatcher.hasPayload;
import static org.springframework.integration.test.mock.MockIntegration.mockMessageHandler;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.test.context.MockIntegrationContext;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MockMessageHandlerTests.Config.class)
@SpringIntegrationTest
@DirtiesContext
public class MockMessageHandlerTests {

	@Autowired
	private MockIntegrationContext mockIntegrationContext;

	@Autowired
	private MessageChannel mockHandlerChannel;

	@Autowired
	private MessageChannel pojoServiceChannel;

	@Autowired
	private QueueChannel results;

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
	}

	@Test
	public void testMockMessageHandlerPojoService() {
		this.pojoServiceChannel.send(new GenericMessage<>("bar"));

		Message<?> receive = this.results.receive(10000);

		assertNotNull(receive);
		assertEquals("barbar", receive.getPayload());

		MessageHandler mockMessageHandler =
				mockMessageHandler(hasPayload("foo"))
						.andReply(m -> m.getPayload().toString().toUpperCase());

		this.mockIntegrationContext.instead("mockMessageHandlerTests.Config.myService.serviceActivator",
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

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public PollableChannel results() {
			return new QueueChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "mockHandlerChannel")
		public MessageHandler mockHandler() {
			return mockMessageHandler(hasHeader("bar", "BAR"))
					.andReply()
					.expect(hasHeader("baz", "BAZ"))
					.andReply()
					.expect(hasPayload("foo"))
					.andReply();
		}

		@ServiceActivator(inputChannel = "pojoServiceChannel", outputChannel = "results")
		public String myService(String payload) {
			return payload + payload;
		}

	}

}
