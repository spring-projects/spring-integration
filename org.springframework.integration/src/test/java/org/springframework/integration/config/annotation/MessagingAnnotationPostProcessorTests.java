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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.annotation.ChannelAdapter;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.bus.DefaultMessageBus;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.ChannelPoller;
import org.springframework.integration.endpoint.ServiceActivatorEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.integration.util.MethodInvoker;

/**
 * @author Mark Fisher
 */
public class MessagingAnnotationPostProcessorTests {

	@Test
	public void testHandlerAnnotation() {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultMessageBus messageBus = new DefaultMessageBus();
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		HandlerAnnotatedBean bean = new HandlerAnnotatedBean();
		Object result = postProcessor.postProcessAfterInitialization(bean, "testBean");
		assertTrue(result instanceof MethodInvoker);
	}

	@Test
	public void testSimpleHandlerWithContext() throws Exception {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"serviceActivatorAnnotationPostProcessorTests.xml", this.getClass());
		MethodInvoker invoker = (MethodInvoker) context.getBean("testBean");
		String reply = (String) invoker.invokeMethod(new StringMessage("world"));
		assertEquals("hello world", reply);
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
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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

	@Test(expected=IllegalArgumentException.class)
	public void testPostProcessorWithNullMessageBus() {
		new MessagingAnnotationPostProcessor(null);
	}

	@Test
	public void testChannelRegistryAwareBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultMessageBus messageBus = new DefaultMessageBus();
		messageBus.setApplicationContext(context);
		QueueChannel inputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		messageBus.registerChannel(inputChannel);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
		postProcessor.setBeanFactory(context.getBeanFactory());
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
		GenericApplicationContext context = new GenericApplicationContext();
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		outputChannel.setBeanName("outputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		context.getBeanFactory().registerSingleton("outputChannel", outputChannel);
		DefaultMessageBus messageBus = new DefaultMessageBus();
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		outputChannel.setBeanName("outputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		context.getBeanFactory().registerSingleton("outputChannel", outputChannel);
		DefaultMessageBus messageBus = new DefaultMessageBus();
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		outputChannel.setBeanName("outputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		context.getBeanFactory().registerSingleton("outputChannel", outputChannel);
		DefaultMessageBus messageBus = new DefaultMessageBus();
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		outputChannel.setBeanName("outputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		context.getBeanFactory().registerSingleton("outputChannel", outputChannel);
		DefaultMessageBus messageBus = new DefaultMessageBus();
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		outputChannel.setBeanName("outputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		context.getBeanFactory().registerSingleton("outputChannel", outputChannel);
		DefaultMessageBus messageBus = new DefaultMessageBus();
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel = new QueueChannel();
		inputChannel.setBeanName("inputChannel");
		outputChannel.setBeanName("outputChannel");
		context.getBeanFactory().registerSingleton("inputChannel", inputChannel);
		context.getBeanFactory().registerSingleton("outputChannel", outputChannel);
		DefaultMessageBus messageBus = new DefaultMessageBus();
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		QueueChannel testChannel = new QueueChannel();
		testChannel.setBeanName("testChannel");
		context.getBeanFactory().registerSingleton("testChannel", testChannel);
		DefaultMessageBus messageBus = new DefaultMessageBus();
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		AnnotatedEndpointWithPolledAnnotation endpoint = new AnnotatedEndpointWithPolledAnnotation();
		postProcessor.postProcessAfterInitialization(endpoint, "testBean");
		ServiceActivatorEndpoint processedEndpoint =
				(ServiceActivatorEndpoint) context.getBean("testBean.prependFoo.serviceActivator");
		processedEndpoint.afterPropertiesSet();
		DirectFieldAccessor accessor = new DirectFieldAccessor(processedEndpoint);
		ChannelPoller poller = (ChannelPoller) accessor.getPropertyValue("poller");
		Schedule schedule = (Schedule) new DirectFieldAccessor(poller).getPropertyValue("schedule");
		assertEquals(PollingSchedule.class, schedule.getClass());
		PollingSchedule pollingSchedule = (PollingSchedule) schedule;
		assertEquals(1234, pollingSchedule.getPeriod());
		assertEquals(5678, pollingSchedule.getInitialDelay());
		assertEquals(true, pollingSchedule.getFixedRate());
		assertEquals(TimeUnit.SECONDS, pollingSchedule.getTimeUnit());
	}

	@Test
	public void testChannelAdapterAnnotation() throws InterruptedException {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultMessageBus messageBus = new DefaultMessageBus();
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
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
		DefaultMessageBus messageBus = new DefaultMessageBus();
		messageBus.setApplicationContext(context);
		MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor(messageBus);
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
		TransformerAnnotationTestBean testBean = new TransformerAnnotationTestBean();
		org.springframework.integration.transformer.Transformer transformer =
				(org.springframework.integration.transformer.Transformer) postProcessor.postProcessAfterInitialization(testBean, "testBean");
		Message<?> reply = transformer.transform(new StringMessage("foo"));
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


	@MessageEndpoint
	private static class ChannelRegistryAwareTestBean implements ChannelRegistryAware {

		private ChannelRegistry channelRegistry;

		public void setChannelRegistry(ChannelRegistry channelRegistry) {
			this.channelRegistry = channelRegistry;
		}

		public ChannelRegistry getChannelRegistry() {
			return this.channelRegistry;
		}

		@ServiceActivator(inputChannel="inputChannel")
		public Message<?> handle(Message<?> message) {
			return null;
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
		@Poller(period=1234, initialDelay=5678, fixedRate=true, timeUnit=TimeUnit.SECONDS)
		public String prependFoo(String s) {
			return "foo" + s;
		}
	}


	@MessageEndpoint
	private static class HandlerAnnotatedBean {

		@ServiceActivator
		public String test(String s) {
			return s + s;
		}

	}


	@MessageEndpoint
	private static class ChannelAdapterAnnotationTestBean {

		@ChannelAdapter("testChannel")
		@Poller(period = 1000, initialDelay = 0, maxMessagesPerPoll = 1)
		public String test() {
			return "test";
		}
	}


	@MessageEndpoint
	private static class TransformerAnnotationTestBean {

		@Transformer
		public String transformBefore(String input) {
			return input.toUpperCase();
		}
	}

}
