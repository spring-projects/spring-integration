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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.annotation.Concurrency;
import org.springframework.integration.annotation.DefaultOutput;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Polled;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.config.MessageEndpointAnnotationPostProcessor;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.endpoint.DefaultMessageEndpoint;
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
		Message<?> message = outputChannel.receive(1000);
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
		Message<?> message = outputChannel.receive(1000);
		assertEquals("hello world", message.getPayload());
		context.stop();
	}

	@Test
	public void testTypeConvertingHandler() throws InterruptedException {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("typeConvertingEndpointTests.xml", this.getClass());
		context.start();
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		MessageChannel outputChannel = (MessageChannel) context.getBean("outputChannel");
		inputChannel.send(new StringMessage("123"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals(246, message.getPayload());
		context.stop();
	}

	@Test
	public void testPolledAnnotation() throws InterruptedException {
		MessageBus messageBus = new MessageBus();
		SimpleChannel testChannel = new SimpleChannel();
		messageBus.registerChannel("testChannel", testChannel);
		MessageEndpointAnnotationPostProcessor postProcessor =
				new MessageEndpointAnnotationPostProcessor(messageBus);
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
		MessageEndpointAnnotationPostProcessor postProcessor =
				new MessageEndpointAnnotationPostProcessor(messageBus);
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
	public void testConcurrencyAnnotationWithValues() {
		MessageBus messageBus = new MessageBus();
		MessageEndpointAnnotationPostProcessor postProcessor =
				new MessageEndpointAnnotationPostProcessor(messageBus);
		ConcurrencyAnnotationTestBean testBean = new ConcurrencyAnnotationTestBean();
		postProcessor.postProcessAfterInitialization(testBean, "testBean");
		DefaultMessageEndpoint endpoint = (DefaultMessageEndpoint) messageBus.lookupEndpoint("testBean-endpoint");
		ConcurrencyPolicy concurrencyPolicy = endpoint.getConcurrencyPolicy();
		assertEquals(17, concurrencyPolicy.getCoreSize());
		assertEquals(42, concurrencyPolicy.getMaxSize());
		assertEquals(11, concurrencyPolicy.getQueueCapacity());
		assertEquals(123, concurrencyPolicy.getKeepAliveSeconds());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testPostProcessorWithNullMessageBus() {
		new MessageEndpointAnnotationPostProcessor(null);
	}

	@Test
	public void testChannelRegistryAwareBean() {
		MessageBus messageBus = new MessageBus();
		MessageEndpointAnnotationPostProcessor postProcessor =
				new MessageEndpointAnnotationPostProcessor(messageBus);
		ChannelRegistryAwareTestBean testBean = new ChannelRegistryAwareTestBean();
		assertNull(testBean.getChannelRegistry());
		postProcessor.postProcessAfterInitialization(testBean, "testBean");
		ChannelRegistry channelRegistry = testBean.getChannelRegistry();
		assertNotNull(channelRegistry);
		assertEquals(messageBus, channelRegistry);
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


	@MessageEndpoint(input="inputChannel")
	@Concurrency(coreSize=17, maxSize=42, keepAliveSeconds=123, queueCapacity=11)
	private static class ConcurrencyAnnotationTestBean {
	}


	@MessageEndpoint(input="inputChannel")
	private static class ChannelRegistryAwareTestBean implements ChannelRegistryAware {

		private ChannelRegistry channelRegistry;

		public void setChannelRegistry(ChannelRegistry channelRegistry) {
			this.channelRegistry = channelRegistry;
		}

		public ChannelRegistry getChannelRegistry() {
			return this.channelRegistry;
		}
	}

}
