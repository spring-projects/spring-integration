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

package org.springframework.integration.config.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;

/**
 * @author Mark Fisher
 */
public class SplitterAnnotationPostProcessorTests {

	private TestApplicationContext context = TestUtils.createTestApplicationContext();

	private DirectChannel inputChannel = new DirectChannel();

	private QueueChannel outputChannel = new QueueChannel();


	@Before
	public void init() {
		context.registerChannel("input", inputChannel);
		context.registerChannel("output", outputChannel);
	}


	@Test
	public void testSplitterAnnotation() throws InterruptedException {
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		TestSplitter splitter = new TestSplitter();
		postProcessor.postProcessAfterInitialization(splitter, "testSplitter");
		context.refresh();
		inputChannel.send(new GenericMessage<String>("this.is.a.test"));
		Message<?> message1 = outputChannel.receive(500);
		assertNotNull(message1);
		assertEquals("this", message1.getPayload());
		Message<?> message2 = outputChannel.receive(500);
		assertNotNull(message2);
		assertEquals("is", message2.getPayload());
		Message<?> message3 = outputChannel.receive(500);
		assertNotNull(message3);
		assertEquals("a", message3.getPayload());
		Message<?> message4 = outputChannel.receive(500);
		assertNotNull(message4);
		assertEquals("test", message4.getPayload());
		assertNull(outputChannel.receive(0));
		context.stop();
	}


	@MessageEndpoint
	public static class TestSplitter {

		@Splitter(inputChannel="input", outputChannel="output")
		public String[] split(String s) {
			return s.split("\\.");
		}
	}

}
