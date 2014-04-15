/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.junit.Assert.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class MessagingAnnotationPostProcessorTests {

	@Test
	public void serviceActivatorAnnotation() {
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.refresh();

		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		ServiceActivatorAnnotatedBean bean = new ServiceActivatorAnnotatedBean();
		postProcessor.postProcessAfterInitialization(bean, "testBean");
		assertTrue(context.containsBean("testBean.test.serviceActivator"));
		Object endpoint = context.getBean("testBean.test.serviceActivator");
		assertTrue(endpoint instanceof AbstractEndpoint);
	}

	@Test
	public void serviceActivatorInApplicationContext() throws Exception {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"serviceActivatorAnnotationPostProcessorTests.xml", this.getClass());
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		inputChannel.send(new GenericMessage<String>("world"));
		Message<?> reply = outputChannel.receive(0);
		assertEquals("hello world", reply.getPayload());
		context.stop();
	}

	@Test
	public void testSimpleHandler() throws InterruptedException {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("simpleAnnotatedEndpointTests.xml", this.getClass());
		context.start();
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		GenericMessage<String> messageToSend = new GenericMessage<String>("world");
		inputChannel.send(messageToSend);
		Message<?> message = outputChannel.receive(1000);
		assertEquals("hello world", message.getPayload());

		inputChannel = context.getBean("advisedIn", MessageChannel.class);
		outputChannel = context.getBean("advisedOut", PollableChannel.class);
		inputChannel.send(messageToSend);
		message = outputChannel.receive(1000);
		assertEquals("hello world advised", message.getPayload());
		context.stop();
	}

	@Test
	public void messageAsMethodParameter() throws InterruptedException {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"messageParameterAnnotatedEndpointTests.xml", this.getClass());
		context.start();
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		inputChannel.send(new GenericMessage<String>("world"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("hello world", message.getPayload());
		context.stop();
	}

	@Test
	public void typeConvertingHandler() throws InterruptedException {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"typeConvertingEndpointTests.xml", this.getClass());
		context.start();
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		inputChannel.send(new GenericMessage<String>("123"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals(246, message.getPayload());
		context.stop();
	}

	@Test
	public void outboundOnlyServiceActivator() throws InterruptedException {
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		context.registerChannel("testChannel", new DirectChannel());
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		CountDownLatch latch = new CountDownLatch(1);
		OutboundOnlyTestBean testBean = new OutboundOnlyTestBean(latch);
		postProcessor.postProcessAfterInitialization(testBean, "testBean");
		context.refresh();
		DestinationResolver<MessageChannel> channelResolver = new BeanFactoryChannelResolver(context);
		MessageChannel testChannel = channelResolver.resolveDestination("testChannel");
		testChannel.send(new GenericMessage<String>("foo"));
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals("foo", testBean.getMessageText());
		context.stop();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPostProcessorWithoutBeanFactory() {
		MessagingAnnotationPostProcessor postProcessor =
				new MessagingAnnotationPostProcessor();
		postProcessor.afterPropertiesSet();
	}

	@Test
	public void testChannelResolution() {
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		DirectChannel eventBus = new DirectChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.registerChannel("outputChannel", outputChannel);
		context.registerChannel("eventBus", eventBus);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		ServiceActivatorAnnotatedBean bean = new ServiceActivatorAnnotatedBean();
		postProcessor.postProcessAfterInitialization(bean, "testBean");
		context.refresh();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannelName("outputChannel").build();
		inputChannel.send(message);
		Message<?> reply = outputChannel.receive(0);
		assertNotNull(reply);

		eventBus.send(new GenericMessage<String>("foo"));
		assertTrue(bean.getInvoked());

		context.stop();
	}

	@Test
	public void testProxiedMessageEndpointAnnotation() {
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.registerChannel("outputChannel", outputChannel);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		ProxyFactory proxyFactory = new ProxyFactory(new AnnotatedTestService());
		Object proxy = proxyFactory.getProxy();
		postProcessor.postProcessAfterInitialization(proxy, "proxy");
		context.refresh();
		inputChannel.send(new GenericMessage<String>("world"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("hello world", message.getPayload());
		context.stop();
	}

	@Test
	public void testMessageEndpointAnnotationInherited() {
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.registerChannel("outputChannel", outputChannel);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		postProcessor.postProcessAfterInitialization(new SimpleAnnotatedEndpointSubclass(), "subclass");
		context.refresh();
		inputChannel.send(new GenericMessage<String>("world"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("hello world", message.getPayload());
		context.stop();
	}

	@Test
	public void testMessageEndpointAnnotationInheritedWithProxy() {
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.registerChannel("outputChannel", outputChannel);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		ProxyFactory proxyFactory = new ProxyFactory(new SimpleAnnotatedEndpointSubclass());
		Object proxy = proxyFactory.getProxy();
		postProcessor.postProcessAfterInitialization(proxy, "proxy");
		context.refresh();
		inputChannel.send(new GenericMessage<String>("world"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("hello world", message.getPayload());
		context.stop();
	}

	@Test
	public void testMessageEndpointAnnotationInheritedFromInterface() {
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.registerChannel("outputChannel", outputChannel);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		postProcessor.postProcessAfterInitialization(new SimpleAnnotatedEndpointImplementation(), "impl");
		context.refresh();
		inputChannel.send(new GenericMessage<String>("ABC"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("test-ABC", message.getPayload());
		context.stop();
	}

	@Test
	public void testMessageEndpointAnnotationInheritedFromInterfaceWithAutoCreatedChannels() {
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.registerChannel("outputChannel", outputChannel);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		postProcessor.postProcessAfterInitialization(new SimpleAnnotatedEndpointImplementation(), "impl");
		context.refresh();
		inputChannel.send(new GenericMessage<String>("ABC"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("test-ABC", message.getPayload());
		context.stop();
	}

	@Test
	public void testMessageEndpointAnnotationInheritedFromInterfaceWithProxy() {
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.registerChannel("outputChannel", outputChannel);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		ProxyFactory proxyFactory = new ProxyFactory(new SimpleAnnotatedEndpointImplementation());
		Object proxy = proxyFactory.getProxy();
		postProcessor.postProcessAfterInitialization(proxy, "proxy");
		context.refresh();
		inputChannel.send(new GenericMessage<String>("ABC"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("test-ABC", message.getPayload());
		context.stop();
	}

	@Test
	public void testTransformer() {
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		context.registerChannel("inputChannel", inputChannel);
		QueueChannel outputChannel = new QueueChannel();
		context.registerChannel("outputChannel", outputChannel);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		TransformerAnnotationTestBean testBean = new TransformerAnnotationTestBean();
		postProcessor.postProcessAfterInitialization(testBean, "testBean");
		context.refresh();
		inputChannel.send(new GenericMessage<String>("foo"));
		Message<?> reply = outputChannel.receive(0);
		assertEquals("FOO", reply.getPayload());
		context.stop();
	}


	@MessageEndpoint
	private static class OutboundOnlyTestBean {

		private String messageText;

		private final CountDownLatch latch;


		public OutboundOnlyTestBean(CountDownLatch latch) {
			this.latch = latch;
		}

		public String getMessageText() {
			return this.messageText;
		}

		@ServiceActivator(inputChannel="testChannel")
		public void countdown(String input) {
			this.messageText = input;
			latch.countDown();
		}
	}


	private static class SimpleAnnotatedEndpointSubclass extends AnnotatedTestService {
	}


	@MessageEndpoint
	private static interface SimpleAnnotatedEndpointInterface {
		String test(String input);
	}


	private static class SimpleAnnotatedEndpointImplementation implements SimpleAnnotatedEndpointInterface {

		@ServiceActivator(inputChannel="inputChannel", outputChannel="outputChannel")
		public String test(String input) {
			return "test-"  + input;
		}
	}


	@MessageEndpoint
	private static class ServiceActivatorAnnotatedBean {

		public final AtomicBoolean invoked = new AtomicBoolean();

		@ServiceActivator(inputChannel="inputChannel")
		public String test(String s) {
			return s + s;
		}

		@EventHandler
		public void eventBus(Object payload) {
			invoked.set(true);
		}

		public Boolean getInvoked() {
			return invoked.get();
		}
	}


	@MessageEndpoint
	private static class TransformerAnnotationTestBean {

		@Transformer(inputChannel="inputChannel", outputChannel="outputChannel")
		public String transformBefore(String input) {
			return input.toUpperCase();
		}
	}

	public static class ServiceActivatorAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			return callback.execute() + " advised";
		}

	}

	@Target({ ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@ServiceActivator(inputChannel = "eventBus")
	public static @interface EventHandler {
	}

}
