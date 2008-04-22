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

package org.springframework.integration.endpoint.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.annotation.Concurrency;
import org.springframework.integration.annotation.DefaultOutput;
import org.springframework.integration.annotation.Handler;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Polled;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.MessageEndpointAnnotationPostProcessor;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.endpoint.HandlerEndpoint;
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
	public void testSimpleHandlerWithAutoCreatedChannels() throws InterruptedException {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleAnnotatedEndpointWithAutoCreateChannelTests.xml", this.getClass());
		context.start();
		ChannelRegistry channelRegistry = (ChannelRegistry) context.getBean("bus");
		MessageChannel inputChannel = channelRegistry.lookupChannel("inputChannel");
		MessageChannel outputChannel = channelRegistry.lookupChannel("outputChannel");
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
		QueueChannel testChannel = new QueueChannel();
		messageBus.registerChannel("testChannel", testChannel);
		MessageEndpointAnnotationPostProcessor postProcessor =
				new MessageEndpointAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
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
		QueueChannel testChannel = new QueueChannel();
		messageBus.registerChannel("testChannel", testChannel);
		MessageEndpointAnnotationPostProcessor postProcessor =
				new MessageEndpointAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
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
		postProcessor.afterPropertiesSet();
		ConcurrencyAnnotationTestBean testBean = new ConcurrencyAnnotationTestBean();
		postProcessor.postProcessAfterInitialization(testBean, "testBean");
		HandlerEndpoint endpoint = (HandlerEndpoint) messageBus.lookupEndpoint("testBean-endpoint");
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
		postProcessor.afterPropertiesSet();
		ChannelRegistryAwareTestBean testBean = new ChannelRegistryAwareTestBean();
		assertNull(testBean.getChannelRegistry());
		postProcessor.postProcessAfterInitialization(testBean, "testBean");
		ChannelRegistry channelRegistry = testBean.getChannelRegistry();
		assertNotNull(channelRegistry);
		assertEquals(messageBus, channelRegistry);
	}

	@Test
	public void testProxiedMessageEndpointAnnotation() {
		MessageBus messageBus = new MessageBus();
		messageBus.setAutoCreateChannels(true);
		MessageEndpointAnnotationPostProcessor postProcessor =
				new MessageEndpointAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		ProxyFactory proxyFactory = new ProxyFactory(new SimpleAnnotatedEndpoint());
		Object proxy = proxyFactory.getProxy();
		postProcessor.postProcessAfterInitialization(proxy, "proxy");
		messageBus.start();
		MessageChannel inputChannel = messageBus.lookupChannel("inputChannel");
		MessageChannel outputChannel = messageBus.lookupChannel("outputChannel");
		inputChannel.send(new StringMessage("world"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("hello world", message.getPayload());
	}

	@Test
	public void testMessageEndpointAnnotationInherited() {
		MessageBus messageBus = new MessageBus();
		messageBus.setAutoCreateChannels(true);
		MessageEndpointAnnotationPostProcessor postProcessor =
				new MessageEndpointAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		postProcessor.postProcessAfterInitialization(new SimpleAnnotatedEndpointSubclass(), "subclass");
		messageBus.start();
		MessageChannel inputChannel = messageBus.lookupChannel("inputChannel");
		MessageChannel outputChannel = messageBus.lookupChannel("outputChannel");
		inputChannel.send(new StringMessage("world"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("hello world", message.getPayload());
	}

	@Test
	public void testMessageEndpointAnnotationInheritedWithProxy() {
		MessageBus messageBus = new MessageBus();
		messageBus.setAutoCreateChannels(true);
		MessageEndpointAnnotationPostProcessor postProcessor =
				new MessageEndpointAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		ProxyFactory proxyFactory = new ProxyFactory(new SimpleAnnotatedEndpointSubclass());
		Object proxy = proxyFactory.getProxy();
		postProcessor.postProcessAfterInitialization(proxy, "proxy");
		messageBus.start();
		MessageChannel inputChannel = messageBus.lookupChannel("inputChannel");
		MessageChannel outputChannel = messageBus.lookupChannel("outputChannel");
		inputChannel.send(new StringMessage("world"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("hello world", message.getPayload());
	}

	@Test
	public void testMessageEndpointAnnotationInheritedFromInterface() {
		MessageBus messageBus = new MessageBus();
		MessageChannel inputChannel = new QueueChannel();
		MessageChannel outputChannel = new QueueChannel();
		messageBus.registerChannel("inputChannel", inputChannel);
		messageBus.registerChannel("outputChannel", outputChannel);
		MessageEndpointAnnotationPostProcessor postProcessor =
				new MessageEndpointAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		postProcessor.postProcessAfterInitialization(new SimpleAnnotatedEndpointImplementation(), "impl");
		messageBus.start();
		inputChannel.send(new StringMessage("ABC"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("test-ABC", message.getPayload());
	}

	@Test
	public void testMessageEndpointAnnotationInheritedFromInterfaceWithAutoCreatedChannels() {
		MessageBus messageBus = new MessageBus();
		messageBus.setAutoCreateChannels(true);
		MessageEndpointAnnotationPostProcessor postProcessor =
				new MessageEndpointAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		postProcessor.postProcessAfterInitialization(new SimpleAnnotatedEndpointImplementation(), "impl");
		messageBus.start();
		MessageChannel inputChannel = messageBus.lookupChannel("inputChannel");
		MessageChannel outputChannel = messageBus.lookupChannel("outputChannel");
		inputChannel.send(new StringMessage("ABC"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("test-ABC", message.getPayload());
	}

	@Test
	public void testMessageEndpointAnnotationInheritedFromInterfaceWithProxy() {
		MessageBus messageBus = new MessageBus();
		MessageChannel inputChannel = new QueueChannel();
		MessageChannel outputChannel = new QueueChannel();
		messageBus.registerChannel("inputChannel", inputChannel);
		messageBus.registerChannel("outputChannel", outputChannel);
		MessageEndpointAnnotationPostProcessor postProcessor =
				new MessageEndpointAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		ProxyFactory proxyFactory = new ProxyFactory(new SimpleAnnotatedEndpointImplementation());
		Object proxy = proxyFactory.getProxy();
		postProcessor.postProcessAfterInitialization(proxy, "proxy");
		messageBus.start();
		inputChannel.send(new StringMessage("ABC"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("test-ABC", message.getPayload());
	}

	@Test
	public void testSplitterAnnotation() throws InterruptedException {
		MessageBus messageBus = new MessageBus();
		QueueChannel input = new QueueChannel();
		QueueChannel output = new QueueChannel();
		messageBus.registerChannel("input", input);
		messageBus.registerChannel("output", output);
		MessageEndpointAnnotationPostProcessor postProcessor =
				new MessageEndpointAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		SplitterAnnotationTestEndpoint endpoint = new SplitterAnnotationTestEndpoint();
		postProcessor.postProcessAfterInitialization(endpoint, "endpoint");
		messageBus.start();
		input.send(new StringMessage("this.is.a.test"));
		Message<?> message1 = output.receive(500);
		assertNotNull(message1);
		assertEquals("this", message1.getPayload());
		Message<?> message2 = output.receive(500);
		assertNotNull(message2);
		assertEquals("is", message2.getPayload());
		Message<?> message3 = output.receive(500);
		assertNotNull(message3);
		assertEquals("a", message3.getPayload());
		Message<?> message4 = output.receive(500);
		assertNotNull(message4);
		assertEquals("test", message4.getPayload());
		assertNull(output.receive(500));
	}

	@Test(expected=ConfigurationException.class)
	public void testEndpointWithNoHandlerMethod() {
		MessageBus messageBus = new MessageBus();
		QueueChannel testChannel = new QueueChannel();
		messageBus.registerChannel("testChannel", testChannel);
		MessageEndpointAnnotationPostProcessor postProcessor =
				new MessageEndpointAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		AnnotatedEndpointWithNoHandlerMethod endpoint = new AnnotatedEndpointWithNoHandlerMethod();
		postProcessor.postProcessAfterInitialization(endpoint, "endpoint");
	}


	@MessageEndpoint(defaultOutput="testChannel")
	private static class PolledAnnotationTestBean {

		@Polled(period=100)
		public String poller() {
			return "test";
		}

		@Handler
		public Message<?> handle(Message<?> message) {
			return message;
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

		@Handler
		public Message<?> handle(Message<?> message) {
			return message;
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

		@Handler
		public Message<?> handle(Message<?> message) {
			return null;
		}
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

		@Handler
		public Message<?> handle(Message<?> message) {
			return null;
		}
	}


	private static class SimpleAnnotatedEndpointSubclass extends SimpleAnnotatedEndpoint {
	}


	@MessageEndpoint(input="inputChannel", defaultOutput="outputChannel", pollPeriod=25)
	private static interface SimpleAnnotatedEndpointInterface {
		String test(String input);
	}


	private static class SimpleAnnotatedEndpointImplementation implements SimpleAnnotatedEndpointInterface {

		@Handler
		public String test(String input) {
			return "test-"  + input;
		}
	}


	@MessageEndpoint(input="input", defaultOutput="output")
	private static class SplitterAnnotationTestEndpoint {

		@Splitter
		public String[] split(String input) {
			return input.split("\\.");
		}
	}


	@MessageEndpoint(input="testChannel")
	private static class AnnotatedEndpointWithNoHandlerMethod {
	}

}
