/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.endpoint.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.annotation.DefaultOutput;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Polled;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.config.MessageEndpointAnnotationPostProcessor;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class MessageEndpointAnnotationPostProcessorTests {

	@Test
	public void testSimpleHandler() throws InterruptedException {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("simpleAnnotatedEndpointTests.xml", this.getClass());
		context.start();
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		MessageChannel outputChannel = (MessageChannel) context.getBean("outputChannel");
		inputChannel.send(new StringMessage("world"));
		Message<String> message = outputChannel.receive(1000);
		assertEquals("hello world", message.getPayload());
		context.stop();
	}

	@Test
	public void testMessageParameterHandler() throws InterruptedException {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("messageParameterAnnotatedEndpointTests.xml", this.getClass());
		context.start();
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		MessageChannel outputChannel = (MessageChannel) context.getBean("outputChannel");
		inputChannel.send(new StringMessage("world"));
		Message<String> message = outputChannel.receive(1000);
		assertEquals("hello world", message.getPayload());
		context.stop();
	}

	@Test
	public void testPolledAnnotation() throws InterruptedException {
		MessageBus messageBus = new MessageBus();
		SimpleChannel testChannel = new SimpleChannel();
		messageBus.registerChannel("testChannel", testChannel);
		MessageEndpointAnnotationPostProcessor postProcessor = new MessageEndpointAnnotationPostProcessor();
		postProcessor.setMessageBus(messageBus);
		PolledAnnotationTestBean testBean = new PolledAnnotationTestBean();
		postProcessor.postProcessAfterInitialization(testBean, "testBean");
		messageBus.start();
		Message<?> message = testChannel.receive(1000);
		assertEquals("test", message.getPayload());
		messageBus.stop();
	}

	@Test
	public void testDefaultOutputAnnotation() throws InterruptedException {
		MessageBus messageBus = new MessageBus();
		SimpleChannel testChannel = new SimpleChannel();
		messageBus.registerChannel("testChannel", testChannel);
		MessageEndpointAnnotationPostProcessor postProcessor = new MessageEndpointAnnotationPostProcessor();
		postProcessor.setMessageBus(messageBus);
		CountDownLatch latch = new CountDownLatch(1);
		DefaultOutputAnnotationTestBean testBean = new DefaultOutputAnnotationTestBean(latch);
		postProcessor.postProcessAfterInitialization(testBean, "testBean");
		messageBus.start();
		testChannel.send(new StringMessage("foo"));
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals("foo", testBean.getMessageText());
		messageBus.stop();
	}

	@Test
	public void testPostProcessorWithNoMessageBus() {
		MessageEndpointAnnotationPostProcessor postProcessor = new MessageEndpointAnnotationPostProcessor();
		PolledAnnotationTestBean testBean = new PolledAnnotationTestBean();
		Object result = postProcessor.postProcessAfterInitialization(testBean, "testBean");
		assertSame(testBean, result);
	}


	@MessageEndpoint(defaultOutput="testChannel")
	private static class PolledAnnotationTestBean {

		@Polled(period=100)
		public String poller() {
			return "test";
		}
	}


	@MessageEndpoint(input="testChannel")
	private static class DefaultOutputAnnotationTestBean {

		private String messageText;

		private CountDownLatch latch;


		public DefaultOutputAnnotationTestBean(CountDownLatch latch) {
			this.latch = latch;
		}

		public String getMessageText() {
			return this.messageText;
		}

		@DefaultOutput
		public void countdown(String input) {
			this.messageText = input;
			latch.countDown();
		}
	}

}
