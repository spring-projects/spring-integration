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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class EndpointParserTests {

	@Test
	public void testSimpleEndpoint() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleEndpointTests.xml", this.getClass());
		context.start();
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		TestHandler handler = (TestHandler) context.getBean("testHandler");
		assertNull(handler.getMessageString());
		channel.send(new GenericMessage<String>("test"));
		handler.getLatch().await(500, TimeUnit.MILLISECONDS);
		assertEquals("test", handler.getMessageString());
	}

	@Test
	public void testEndpointWithSelectorAccepts() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointWithSelector.xml", this.getClass());		
		MessageConsumer endpoint = (MessageConsumer) context.getBean("endpoint");
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.fromPayload("test")
				.setReturnAddress(replyChannel).build();
		endpoint.onMessage(message);
		Message<?> reply = replyChannel.receive(500);
		assertNotNull(reply);
		assertEquals("foo", reply.getPayload());
	}

	@Test(expected=MessageRejectedException.class)
	public void testEndpointWithSelectorRejects() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointWithSelector.xml", this.getClass());		
		MessageConsumer endpoint = (MessageConsumer) context.getBean("endpoint");
		MessageChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.fromPayload(123)
				.setReturnAddress(replyChannel).build();
		endpoint.onMessage(message);
	}

	@Test
	public void testEndpointWithErrorHandler() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointWithErrorHandler.xml", this.getClass());		
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		TestErrorHandler errorHandler = (TestErrorHandler) context.getBean("errorHandler");
		assertNull(errorHandler.getLastError());
		channel.send(new StringMessage("test"));
		assertNotNull(errorHandler.getLastError());
		assertEquals("intentional test failure", errorHandler.getLastError().getMessage());
	}

}
