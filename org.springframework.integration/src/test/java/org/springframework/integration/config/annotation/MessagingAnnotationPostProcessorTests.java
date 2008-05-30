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
import static org.junit.Assert.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.annotation.Concurrency;
import org.springframework.integration.annotation.Handler;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.MessageSource;
import org.springframework.integration.annotation.MessageTarget;
import org.springframework.integration.annotation.Polled;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.endpoint.HandlerEndpoint;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;

/**
 * @author Mark Fisher
 */
public class MessagingAnnotationPostProcessorTests {

	@Test
	public void testHandlerAnnotation() {
		MessageBus messageBus = new MessageBus();
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		HandlerAnnotatedBean bean = new HandlerAnnotatedBean();
		Object result = postProcessor.postProcessAfterInitialization(bean, "testBean");
		assertTrue(result instanceof MessageHandler);
	}

	@Test
	public void testCustomHandlerAnnotation() {
		MessageBus messageBus = new MessageBus();
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		CustomHandlerAnnotatedBean bean = new CustomHandlerAnnotatedBean();
		Object result = postProcessor.postProcessAfterInitialization(bean, "testBean");
		assertTrue(result instanceof MessageHandler);
	}

	@Test
	public void testSimpleHandlerWithContext() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"handlerAnnotationPostProcessorTests.xml", this.getClass());
		MessageHandler handler = (MessageHandler) context.getBean("simpleHandler");
		Message<?> reply = handler.handle(new StringMessage("world"));
		assertEquals("hello world", reply.getPayload());
	}

	@Test
	public void testSimpleHandlerEndpointWithContext() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"handlerAnnotationPostProcessorTests.xml", this.getClass());
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		MessageChannel outputChannel = (MessageChannel) context.getBean("outputChannel");
		inputChannel.send(new StringMessage("foo"));
		Message<?> reply = outputChannel.receive(1000);
		assertEquals("hello foo", reply.getPayload());
	}

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
	public void testTargetAnnotation() throws InterruptedException {
		MessageBus messageBus = new MessageBus();
		QueueChannel testChannel = new QueueChannel();
		messageBus.registerChannel("testChannel", testChannel);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		CountDownLatch latch = new CountDownLatch(1);
		TargetAnnotationTestBean testBean = new TargetAnnotationTestBean(latch);
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
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		ConcurrencyAnnotationTestBean testBean = new ConcurrencyAnnotationTestBean();
		postProcessor.postProcessAfterInitialization(testBean, "testBean");
		HandlerEndpoint endpoint = (HandlerEndpoint) messageBus.lookupEndpoint("testBean.MessageHandler.endpoint");
		ConcurrencyPolicy concurrencyPolicy = endpoint.getConcurrencyPolicy();
		assertEquals(17, concurrencyPolicy.getCoreSize());
		assertEquals(42, concurrencyPolicy.getMaxSize());
		assertEquals(11, concurrencyPolicy.getQueueCapacity());
		assertEquals(123, concurrencyPolicy.getKeepAliveSeconds());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testPostProcessorWithNullMessageBus() {
		new MessagingAnnotationPostProcessor(null);
	}

	@Test
	public void testChannelRegistryAwareBean() {
		MessageBus messageBus = new MessageBus();
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		AnnotatedEndpointWithNoHandlerMethod endpoint = new AnnotatedEndpointWithNoHandlerMethod();
		postProcessor.postProcessAfterInitialization(endpoint, "endpoint");
	}

	@Test
	public void testEndpointWithPolledAnnotation() {
		MessageBus messageBus = new MessageBus();
		QueueChannel testChannel = new QueueChannel();
		messageBus.registerChannel("testChannel", testChannel);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		AnnotatedEndpointWithPolledAnnotation endpoint = new AnnotatedEndpointWithPolledAnnotation();
		postProcessor.postProcessAfterInitialization(endpoint, "testBean");
		HandlerEndpoint processedEndpoint = (HandlerEndpoint) messageBus.lookupEndpoint("testBean.MessageHandler.endpoint");
		Schedule schedule = processedEndpoint.getSubscription().getSchedule();
		assertEquals(PollingSchedule.class, schedule.getClass());
		PollingSchedule pollingSchedule = (PollingSchedule) schedule;
		assertEquals(1234, pollingSchedule.getPeriod());
		assertEquals(5678, pollingSchedule.getInitialDelay());
		assertEquals(true, pollingSchedule.getFixedRate());
		assertEquals(TimeUnit.SECONDS, pollingSchedule.getTimeUnit());
	}

	@Test
	public void testMessageSourceAnnotation() {
		MessageBus messageBus = new MessageBus();
		QueueChannel testChannel = new QueueChannel();
		messageBus.registerChannel("testChannel", testChannel);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
		postProcessor.afterPropertiesSet();
		MessageSourceAnnotationTestBean testBean = new MessageSourceAnnotationTestBean();
		postProcessor.postProcessAfterInitialization(testBean, "testBean");
		messageBus.start();
		Message<?> message = testChannel.receive(1000);
		assertEquals("test", message.getPayload());
		messageBus.stop();
	}


	@MessageEndpoint(input="testChannel")
	private static class TargetAnnotationTestBean {

		private String messageText;

		private CountDownLatch latch;


		public TargetAnnotationTestBean(CountDownLatch latch) {
			this.latch = latch;
		}

		public String getMessageText() {
			return this.messageText;
		}

		@MessageTarget
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


	@MessageEndpoint(input="inputChannel", output="outputChannel")
	private static interface SimpleAnnotatedEndpointInterface {
		String test(String input);
	}


	private static class SimpleAnnotatedEndpointImplementation implements SimpleAnnotatedEndpointInterface {

		@Handler
		public String test(String input) {
			return "test-"  + input;
		}
	}


	@MessageEndpoint(input="input", output="output")
	private static class SplitterAnnotationTestEndpoint {

		@Splitter
		public String[] split(String input) {
			return input.split("\\.");
		}
	}


	@MessageEndpoint(input="testChannel")
	private static class AnnotatedEndpointWithNoHandlerMethod {
	}


	@MessageEndpoint(input="testChannel")
	@Polled(period=1234, initialDelay=5678, fixedRate=true, timeUnit=TimeUnit.SECONDS)
	private static class AnnotatedEndpointWithPolledAnnotation {

		@Handler
		public String prependFoo(String s) {
			return "foo" + s;
		}
	}


	private static class HandlerAnnotatedBean {

		@Handler
		public String test(String s) {
			return s + s;
		}

	}


	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@Handler
	private static @interface CustomHandler {		
	}


	private static class CustomHandlerAnnotatedBean {

		@CustomHandler
		public String test(String s) {
			return s + s;
		}

	}


	@MessageEndpoint(output="testChannel")
	@Polled(period=100)
	private static class MessageSourceAnnotationTestBean {

		@MessageSource
		public String test() {
			return "test";
		}

	}

}
