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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.bus.DefaultMessageBus;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class SplitterAnnotationPostProcessorTests {

	private MessageBus messageBus;

	private DirectChannel inputChannel;

	private QueueChannel outputChannel;


	@Before
	public void init() {
		inputChannel = new DirectChannel();
		outputChannel = new QueueChannel();
		inputChannel.setBeanName("input");
		outputChannel.setBeanName("output");
		messageBus = new DefaultMessageBus();
		messageBus.registerChannel(inputChannel);
		messageBus.registerChannel(outputChannel);
	}


	@Test
	public void testSplitterAnnotation() throws InterruptedException {
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		TestSplitter splitter = new TestSplitter();
		postProcessor.postProcessAfterInitialization(splitter, "testSplitter");
		messageBus.start();
		inputChannel.send(new StringMessage("this.is.a.test"));
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
		messageBus.stop();
	}


	@MessageEndpoint
	public static class TestSplitter {

		@Splitter(inputChannel="input", outputChannel="output")
		public String[] split(String s) {
			return s.split("\\.");
		}
	}

}
