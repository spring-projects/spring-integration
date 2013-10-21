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

package org.springframework.integration.config.annotation;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;

/**
 * @author Mark Fisher
 */
public class RouterAnnotationPostProcessorTests {

	private TestApplicationContext context = TestUtils.createTestApplicationContext();

	private DirectChannel inputChannel = new DirectChannel();

	private QueueChannel outputChannel = new QueueChannel();


	@Before
	public void init() {
		context.registerChannel("input", inputChannel);
		context.registerChannel("output", outputChannel);
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


	@MessageEndpoint
	public static class TestRouter {

		@Router(inputChannel="input", defaultOutputChannel="output")
		public String test(String s) {
			return null;
		}
	}

}
