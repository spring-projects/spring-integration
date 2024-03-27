/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.config.annotation;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.IntegrationRegistrar;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class RouterAnnotationPostProcessorTests {

	private final TestApplicationContext context = TestUtils.createTestApplicationContext();

	private final DirectChannel inputChannel = new DirectChannel();

	private final QueueChannel outputChannel = new QueueChannel();

	private final DirectChannel routingChannel = new DirectChannel();

	private final QueueChannel integerChannel = new QueueChannel();

	private final QueueChannel stringChannel = new QueueChannel();

	@BeforeEach
	public void init() {
		new IntegrationRegistrar().registerBeanDefinitions(mock(), this.context.getDefaultListableBeanFactory());
		context.registerChannel("input", inputChannel);
		context.registerChannel("output", outputChannel);
		context.registerChannel("routingChannel", routingChannel);
		context.registerChannel("integerChannel", integerChannel);
		context.registerChannel("stringChannel", stringChannel);
	}

	@AfterEach
	public void tearDown() {
		this.context.close();
	}

	@Test
	public void testRouter() {
		context.registerEndpoint("test", new TestRouter());
		context.refresh();
		inputChannel.send(new GenericMessage<>("foo"));
		Message<?> replyMessage = outputChannel.receive(0);
		assertThat(replyMessage.getPayload()).isEqualTo("foo");
		context.stop();
	}

	@Test
	public void testRouterWithListParam() {
		context.registerEndpoint("test", new TestRouter());
		context.refresh();

		routingChannel.send(new GenericMessage<>(Collections.singletonList("foo")));
		Message<?> replyMessage = stringChannel.receive(0);
		assertThat(replyMessage.getPayload()).isEqualTo(Collections.singletonList("foo"));

		// The SpEL ReflectiveMethodExecutor does a conversion of a single value to a List
		routingChannel.send(new GenericMessage<>(2));
		replyMessage = integerChannel.receive(0);
		assertThat(replyMessage.getPayload()).isEqualTo(2);
		context.stop();
	}

	@MessageEndpoint
	public static class TestRouter {

		@Router(inputChannel = "input", defaultOutputChannel = "output")
		public String test(String s) {
			return null;
		}

		@Router(inputChannel = "routingChannel")
		public String route(List<?> payload) {
			if (payload.size() == 0) {
				return null;
			}
			if (payload.get(0) instanceof Integer) {
				return "integerChannel";
			}
			else {
				return "stringChannel";
			}
		}

	}

}
