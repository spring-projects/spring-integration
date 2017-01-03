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

package org.springframework.integration.config.annotation;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

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


	@Before
	public void init() {
		context.registerChannel("input", inputChannel);
		context.registerChannel("output", outputChannel);
		context.registerChannel("routingChannel", routingChannel);
		context.registerChannel("integerChannel", integerChannel);
		context.registerChannel("stringChannel", stringChannel);
	}


	@Test
	public void testRouter() {
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		TestRouter testRouter = new TestRouter();
		postProcessor.postProcessAfterInitialization(testRouter, "test");
		context.refresh();
		inputChannel.send(new GenericMessage<String>("foo"));
		Message<?> replyMessage = outputChannel.receive(0);
		assertEquals("foo", replyMessage.getPayload());
		context.stop();
	}

	@Test
	public void testRouterWithListParam() {
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		TestRouter testRouter = new TestRouter();
		postProcessor.postProcessAfterInitialization(testRouter, "test");
		context.refresh();

		routingChannel.send(new GenericMessage<>(Collections.singletonList("foo")));
		Message<?> replyMessage = stringChannel.receive(0);
		assertEquals(Collections.singletonList("foo"), replyMessage.getPayload());

		// The SpEL ReflectiveMethodExecutor does a conversion of a single value to a List
		routingChannel.send(new GenericMessage<Integer>(2));
		replyMessage = integerChannel.receive(0);
		assertEquals(2, replyMessage.getPayload());
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
