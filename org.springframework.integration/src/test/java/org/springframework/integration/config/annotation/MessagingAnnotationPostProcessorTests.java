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
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.annotation.ChannelAdapter;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.bus.DefaultMessageBus;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.xml.MessageBusParser;
import org.springframework.integration.endpoint.PollingConsumerEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.scheduling.IntervalTrigger;
import org.springframework.integration.scheduling.Trigger;
import org.springframework.integration.util.TestUtils;

/**
 * @author Mark Fisher
 */
public class MessagingAnnotationPostProcessorTests {

	@Test
	public void testServiceActivatorAnnotation() {
		GenericApplicationContext context = new GenericApplicationContext();
		QueueChannel inputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		DefaultMessageBus messageBus = new DefaultMessageBus();
		context.getBeanFactory().registerSingleton(
				MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		ServiceActivatorAnnotatedBean bean = new ServiceActivatorAnnotatedBean();
		postProcessor.postProcessAfterInitialization(bean, "testBean");
		assertTrue(context.containsBean("testBean.test.serviceActivator"));
		Object endpoint = context.getBean("testBean.test.serviceActivator");
		assertTrue(endpoint instanceof org.springframework.integration.endpoint.MessageEndpoint);
	}

	@Test
	public void testServiceActivatorWithContext() throws Exception {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"serviceActivatorAnnotationPostProcessorTests.xml", this.getClass());
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		inputChannel.send(new StringMessage("world"));
		Message<?> reply = outputChannel.receive(0);
		assertEquals("hello world", reply.getPayload());
		context.stop();
	}

	@Test
	public void testSimpleHandlerEndpointWithContext() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"serviceActivatorAnnotationPostProcessorTests.xml", this.getClass());
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		inputChannel.send(new StringMessage("foo"));
		Message<?> reply = outputChannel.receive(1000);
		assertEquals("hello foo", reply.getPayload());
		context.stop();
	}

	@Test
	public void testSimpleHandler() throws InterruptedException {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("simpleAnnotatedEndpointTests.xml", this.getClass());
		context.start();
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
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
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
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
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		inputChannel.send(new StringMessage("123"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals(246, message.getPayload());
		context.stop();
	}

	@Test
	public void testTargetAnnotation() throws InterruptedException {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultMessageBus messageBus = new DefaultMessageBus();
		context.getBeanFactory().registerSingleton(
				MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
		messageBus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		CountDownLatch latch = new CountDownLatch(1);
		TargetAnnotationTestBean testBean = new TargetAnnotationTestBean(latch);
		postProcessor.postProcessAfterInitialization(testBean, "testBean");
		messageBus.start();
		MessageChannel testChannel = messageBus.lookupChannel("testChannel");
		testChannel.send(new StringMessage("foo"));
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals("foo", testBean.getMessageText());
		messageBus.stop();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPostProcessorWithoutBeanFactory() {
		MessagingAnnotationPostProcessor postProcessor =
				new MessagingAnnotationPostProcessor();
		postProcessor.afterPropertiesSet();
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void testPostProcessorWithoutMessageBus() {
		GenericApplicationContext context = new GenericApplicationContext();
		MessagingAnnotationPostProcessor postProcessor =
				new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
	}

	@Test
	public void testChannelResolution() {
		GenericApplicationContext context = new GenericApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		outputChannel.setBeanName("outputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		context.getBeanFactory().registerSingleton("outputChannel", outputChannel);
		DefaultMessageBus messageBus = new DefaultMessageBus();
		messageBus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		context.getBeanFactory().registerSingleton(
				MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		messageBus.start();
		ServiceActivatorAnnotatedBean bean = new ServiceActivatorAnnotatedBean();
		postProcessor.postProcessAfterInitialization(bean, "testBean");
		Message<?> message = MessageBuilder.withPayload("test").setReturnAddress("outputChannel").build();
		inputChannel.send(message);
		Message<?> reply = outputChannel.receive(0);
		assertNotNull(reply);
	}

	@Test
	public void testProxiedMessageEndpointAnnotation() {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultMessageBus messageBus = new DefaultMessageBus();
		context.getBeanFactory().registerSingleton(
				MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		outputChannel.setBeanName("outputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		context.getBeanFactory().registerSingleton("outputChannel", outputChannel);
		messageBus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		ProxyFactory proxyFactory = new ProxyFactory(new SimpleAnnotatedEndpoint());
		Object proxy = proxyFactory.getProxy();
		postProcessor.postProcessAfterInitialization(proxy, "proxy");
		messageBus.start();
		inputChannel.send(new StringMessage("world"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("hello world", message.getPayload());
		messageBus.stop();
	}

	@Test
	public void testMessageEndpointAnnotationInherited() {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultMessageBus messageBus = new DefaultMessageBus();
		context.getBeanFactory().registerSingleton(
				MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		outputChannel.setBeanName("outputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		context.getBeanFactory().registerSingleton("outputChannel", outputChannel);
		messageBus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		postProcessor.postProcessAfterInitialization(new SimpleAnnotatedEndpointSubclass(), "subclass");
		messageBus.start();
		inputChannel.send(new StringMessage("world"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("hello world", message.getPayload());
		messageBus.stop();
	}

	@Test
	public void testMessageEndpointAnnotationInheritedWithProxy() {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultMessageBus messageBus = new DefaultMessageBus();
		context.getBeanFactory().registerSingleton(
				MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		outputChannel.setBeanName("outputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		context.getBeanFactory().registerSingleton("outputChannel", outputChannel);
		messageBus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		ProxyFactory proxyFactory = new ProxyFactory(new SimpleAnnotatedEndpointSubclass());
		Object proxy = proxyFactory.getProxy();
		postProcessor.postProcessAfterInitialization(proxy, "proxy");
		messageBus.start();
		inputChannel.send(new StringMessage("world"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("hello world", message.getPayload());
		messageBus.stop();
	}

	@Test
	public void testMessageEndpointAnnotationInheritedFromInterface() {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultMessageBus messageBus = new DefaultMessageBus();
		context.getBeanFactory().registerSingleton(
				MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		outputChannel.setBeanName("outputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		context.getBeanFactory().registerSingleton("outputChannel", outputChannel);
		messageBus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		postProcessor.postProcessAfterInitialization(new SimpleAnnotatedEndpointImplementation(), "impl");
		messageBus.start();
		inputChannel.send(new StringMessage("ABC"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("test-ABC", message.getPayload());
		messageBus.stop();
	}

	@Test
	public void testMessageEndpointAnnotationInheritedFromInterfaceWithAutoCreatedChannels() {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultMessageBus messageBus = new DefaultMessageBus();
		context.getBeanFactory().registerSingleton(
				MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		outputChannel.setBeanName("outputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		context.getBeanFactory().registerSingleton("outputChannel", outputChannel);
		messageBus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		postProcessor.postProcessAfterInitialization(new SimpleAnnotatedEndpointImplementation(), "impl");
		messageBus.start();
		inputChannel.send(new StringMessage("ABC"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("test-ABC", message.getPayload());
		messageBus.stop();
	}

	@Test
	public void testMessageEndpointAnnotationInheritedFromInterfaceWithProxy() {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultMessageBus messageBus = new DefaultMessageBus();
		context.getBeanFactory().registerSingleton(
				MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		outputChannel.setBeanName("outputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		context.getBeanFactory().registerSingleton("outputChannel", outputChannel);
		messageBus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		ProxyFactory proxyFactory = new ProxyFactory(new SimpleAnnotatedEndpointImplementation());
		Object proxy = proxyFactory.getProxy();
		postProcessor.postProcessAfterInitialization(proxy, "proxy");
		messageBus.start();
		inputChannel.send(new StringMessage("ABC"));
		Message<?> message = outputChannel.receive(1000);
		assertEquals("test-ABC", message.getPayload());
		messageBus.stop();
	}

	@Test
	public void testEndpointWithPollerAnnotation() {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultMessageBus messageBus = new DefaultMessageBus();
		context.getBeanFactory().registerSingleton(
				MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
		QueueChannel testChannel = new QueueChannel();
		testChannel.setBeanName("testChannel");
		context.getBeanFactory().registerSingleton("testChannel", testChannel);
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		AnnotatedEndpointWithPolledAnnotation bean = new AnnotatedEndpointWithPolledAnnotation();
		postProcessor.postProcessAfterInitialization(bean, "testBean");
		PollingConsumerEndpoint endpoint =
				(PollingConsumerEndpoint) context.getBean("testBean.prependFoo.serviceActivator");
		Trigger trigger = (Trigger) new DirectFieldAccessor(endpoint).getPropertyValue("trigger");
		assertEquals(IntervalTrigger.class, trigger.getClass());
		DirectFieldAccessor triggerAccessor = new DirectFieldAccessor(trigger);
		assertEquals(new Long(123000), triggerAccessor.getPropertyValue("interval"));
		assertEquals(new Long(456000), triggerAccessor.getPropertyValue("initialDelay"));
		assertEquals(true, triggerAccessor.getPropertyValue("fixedRate"));
	}

	@Test
	public void testChannelAdapterAnnotation() throws InterruptedException {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultMessageBus messageBus = new DefaultMessageBus();
		context.getBeanFactory().registerSingleton(
				MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
		messageBus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		ChannelAdapterAnnotationTestBean testBean = new ChannelAdapterAnnotationTestBean();
		postProcessor.postProcessAfterInitialization(testBean, "testBean");
		messageBus.start();
		DirectChannel testChannel = (DirectChannel) messageBus.lookupChannel("testChannel");
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<Message<?>> receivedMessage = new AtomicReference<Message<?>>();
		testChannel.subscribe(new MessageConsumer() {
			public void onMessage(Message<?> message) {
				receivedMessage.set(message);
				latch.countDown();
			}
		});
		latch.await(3, TimeUnit.SECONDS);
		assertEquals(0, latch.getCount());
		assertNotNull(receivedMessage.get());
		assertEquals("test", receivedMessage.get().getPayload());
		messageBus.stop();
	}

	@Test
	public void testTransformer() {
		GenericApplicationContext context = new GenericApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		inputChannel.setBeanName("inputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		QueueChannel outputChannel = new QueueChannel();
		outputChannel.setBeanName("outputChannel");
		context.getBeanFactory().registerSingleton("outputChannel", outputChannel);
		DefaultMessageBus messageBus = new DefaultMessageBus();
		context.getBeanFactory().registerSingleton(MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
		messageBus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		TransformerAnnotationTestBean testBean = new TransformerAnnotationTestBean();
		postProcessor.postProcessAfterInitialization(testBean, "testBean");
		context.refresh();
		inputChannel.send(new StringMessage("foo"));
		Message<?> reply = outputChannel.receive(0);
		assertEquals("FOO", reply.getPayload());
	}


	@MessageEndpoint
	private static class TargetAnnotationTestBean {

		private String messageText;

		private CountDownLatch latch;


		public TargetAnnotationTestBean(CountDownLatch latch) {
			this.latch = latch;
		}

		public String getMessageText() {
			return this.messageText;
		}

		@ChannelAdapter("testChannel")
		public void countdown(String input) {
			this.messageText = input;
			latch.countDown();
		}
	}


	private static class SimpleAnnotatedEndpointSubclass extends SimpleAnnotatedEndpoint {
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
	private static class AnnotatedEndpointWithPolledAnnotation {

		@ServiceActivator(inputChannel="testChannel")
		@Poller(interval=123, initialDelay=456, fixedRate=true, timeUnit=TimeUnit.SECONDS)
		public String prependFoo(String s) {
			return "foo" + s;
		}
	}


	@MessageEndpoint
	private static class ServiceActivatorAnnotatedBean {

		@ServiceActivator(inputChannel="inputChannel")
		public String test(String s) {
			return s + s;
		}

	}


	@MessageEndpoint
	private static class ChannelAdapterAnnotationTestBean {

		@ChannelAdapter("testChannel")
		@Poller(interval=1000, initialDelay=0, maxMessagesPerPoll=1)
		public String test() {
			return "test";
		}
	}


	@MessageEndpoint
	private static class TransformerAnnotationTestBean {

		@Transformer(inputChannel="inputChannel", outputChannel="outputChannel")
		public String transformBefore(String input) {
			return input.toUpperCase();
		}
	}

}
