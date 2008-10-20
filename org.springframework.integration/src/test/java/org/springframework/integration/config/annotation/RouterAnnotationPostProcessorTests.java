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

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.bus.ApplicationContextMessageBus;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.xml.MessageBusParser;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.util.TestUtils;

/**
 * @author Mark Fisher
 */
public class RouterAnnotationPostProcessorTests {

	private GenericApplicationContext context = new GenericApplicationContext();

	private ApplicationContextMessageBus messageBus = new ApplicationContextMessageBus();

	private DirectChannel inputChannel = new DirectChannel();

	private QueueChannel outputChannel = new QueueChannel();


	@Before
	public void init() {
		messageBus.setApplicationContext(context);
		messageBus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		inputChannel.setBeanName("input");
		outputChannel.setBeanName("output");
		context.getBeanFactory().registerSingleton("input", inputChannel);
		context.getBeanFactory().registerSingleton("output", outputChannel);
		context.getBeanFactory().registerSingleton(
				MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
	}


	@Test
	public void testRouter() {
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		TestRouter testRouter = new TestRouter();
		postProcessor.postProcessAfterInitialization(testRouter, "test");
		messageBus.start();
		inputChannel.send(new StringMessage("foo"));
		Message<?> replyMessage = outputChannel.receive(0);
		assertEquals("foo", replyMessage.getPayload());
	}


	@MessageEndpoint
	public static class TestRouter {

		@Router(inputChannel="input", defaultOutputChannel="output")
		public String test(String s) {
			return null;
		}
	}

}
