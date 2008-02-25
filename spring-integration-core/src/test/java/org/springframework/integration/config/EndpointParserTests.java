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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.context.Lifecycle;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.endpoint.DefaultMessageEndpoint;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.selector.MessageSelectorRejectedException;
import org.springframework.integration.util.ErrorHandler;

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
		channel.send(new GenericMessage<String>(1, "test"));
		handler.getLatch().await(50, TimeUnit.MILLISECONDS);
		assertEquals("test", handler.getMessageString());
	}

	@Test
	public void testEndpointWithChildHandler() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointWithHandlerChildElement.xml", this.getClass());
		context.start();
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		TestHandler handler = (TestHandler) context.getBean("testHandler");
		assertNull(handler.getMessageString());
		channel.send(new GenericMessage<String>(1, "test"));
		handler.getLatch().await(50, TimeUnit.MILLISECONDS);
		assertEquals("test", handler.getMessageString());
	}

	@Test
	public void testHandlerAdapterEndpoint() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"handlerAdapterEndpointTests.xml", this.getClass());
		context.start();
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		TestBean bean = (TestBean) context.getBean("testBean");
		assertNull(bean.getMessage());
		channel.send(new GenericMessage<String>(1, "test"));
		bean.getLatch().await(500, TimeUnit.MILLISECONDS);
		assertEquals("test", bean.getMessage());
	}

	@Test
	public void testHandlerChainEndpoint() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointWithHandlerChainElement.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		channel.send(new StringMessage("test"));
		Message<?> reply = replyChannel.receive(500);
		assertNotNull(reply);
		assertEquals("test-1-2-3", reply.getPayload());
	}

	@Test
	public void testDefaultConcurrency() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointConcurrencyTests.xml", this.getClass());
		DefaultMessageEndpoint endpoint = (DefaultMessageEndpoint) context.getBean("defaultConcurrencyEndpoint");
		ConcurrencyPolicy concurrencyPolicy = endpoint.getConcurrencyPolicy();
		assertEquals(ConcurrencyPolicy.DEFAULT_CORE_SIZE, concurrencyPolicy.getCoreSize());
		assertEquals(ConcurrencyPolicy.DEFAULT_MAX_SIZE, concurrencyPolicy.getMaxSize());
		assertEquals(ConcurrencyPolicy.DEFAULT_QUEUE_CAPACITY, concurrencyPolicy.getQueueCapacity());
		assertEquals(ConcurrencyPolicy.DEFAULT_KEEP_ALIVE_SECONDS, concurrencyPolicy.getKeepAliveSeconds());
	}

	@Test
	public void testConfiguredConcurrency() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointConcurrencyTests.xml", this.getClass());
		DefaultMessageEndpoint endpoint = (DefaultMessageEndpoint) context.getBean("configuredConcurrencyEndpoint");
		ConcurrencyPolicy concurrencyPolicy = endpoint.getConcurrencyPolicy();
		assertEquals(7, concurrencyPolicy.getCoreSize());
		assertEquals(77, concurrencyPolicy.getMaxSize());
		assertEquals(777, concurrencyPolicy.getQueueCapacity());
		assertEquals(7777, concurrencyPolicy.getKeepAliveSeconds());
	}

	@Test
	public void testEndpointWithSelectorAccepts() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointWithSelectors.xml", this.getClass());		
		MessageHandler endpoint = (MessageHandler) context.getBean("endpoint");
		((Lifecycle) endpoint).start();
		Message<?> message = new StringMessage("test");
		MessageChannel replyChannel = new SimpleChannel();
		message.getHeader().setReturnAddress(replyChannel);
		endpoint.handle(message);
		Message<?> reply = replyChannel.receive(500);
		assertNotNull(reply);
		assertEquals("foo", reply.getPayload());
	}

	@Test(expected=MessageSelectorRejectedException.class)
	public void testEndpointWithSelectorRejects() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointWithSelectors.xml", this.getClass());		
		MessageHandler endpoint = (MessageHandler) context.getBean("endpoint");
		((Lifecycle) endpoint).start();
		endpoint.handle(new GenericMessage<Integer>(123));
	}

	@Test
	public void testCustomErrorHandler() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointWithErrorHandler.xml", this.getClass());
		MessageHandler endpoint = (MessageHandler) context.getBean("endpoint");
		TestErrorHandler errorHandler = (TestErrorHandler) context.getBean("errorHandler");
		assertNull(errorHandler.getLastError());
		Message<?> message = new StringMessage("test");
		endpoint.handle(message);
		Throwable error = errorHandler.getLastError();
		assertEquals(MessageHandlingException.class, error.getClass());
		MessageHandlingException exception = (MessageHandlingException) error;
		assertEquals(message, exception.getFailedMessage());
	}

}
