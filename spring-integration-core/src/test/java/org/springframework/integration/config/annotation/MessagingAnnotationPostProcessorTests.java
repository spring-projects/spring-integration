/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.IntegrationRegistrar;
import org.springframework.integration.config.MessagingAnnotationBeanPostProcessor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class MessagingAnnotationPostProcessorTests {

	@Test
	public void serviceActivatorAnnotation() {
		TestApplicationContext context = createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.refresh();

		MessagingAnnotationBeanPostProcessor postProcessor = context.getBean(MessagingAnnotationBeanPostProcessor.class);
		ServiceActivatorAnnotatedBean bean = new ServiceActivatorAnnotatedBean();
		postProcessor.postProcessAfterInitialization(bean, "testBean");
		assertThat(context.containsBean("testBean.test.serviceActivator")).isTrue();
		Object endpoint = context.getBean("testBean.test.serviceActivator");
		assertThat(endpoint instanceof AbstractEndpoint).isTrue();
		context.close();
	}

	@Test
	public void serviceActivatorInApplicationContext() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"serviceActivatorAnnotationPostProcessorTests.xml", this.getClass());
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		inputChannel.send(new GenericMessage<>("world"));
		Message<?> reply = outputChannel.receive(0);
		assertThat(reply.getPayload()).isEqualTo("hello world");
		context.close();
	}

	@Test
	public void testSimpleHandler() {
		AbstractApplicationContext context =
				new ClassPathXmlApplicationContext("simpleAnnotatedEndpointTests.xml", getClass());
		context.start();
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		GenericMessage<String> messageToSend = new GenericMessage<>("world");
		inputChannel.send(messageToSend);
		Message<?> message = outputChannel.receive(1000);
		assertThat(message.getPayload()).isEqualTo("hello world");

		inputChannel = context.getBean("advisedIn", MessageChannel.class);
		outputChannel = context.getBean("advisedOut", PollableChannel.class);
		inputChannel.send(messageToSend);
		message = outputChannel.receive(1000);
		assertThat(message.getPayload()).isEqualTo("hello world advised");
		context.close();
	}

	@Test
	public void messageAsMethodParameter() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"messageParameterAnnotatedEndpointTests.xml", this.getClass());
		context.start();
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		inputChannel.send(new GenericMessage<>("world"));
		Message<?> message = outputChannel.receive(1000);
		assertThat(message.getPayload()).isEqualTo("hello world");
		context.close();
	}

	@Test
	public void typeConvertingHandler() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"typeConvertingEndpointTests.xml", this.getClass());
		context.start();
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		inputChannel.send(new GenericMessage<>("123"));
		Message<?> message = outputChannel.receive(1000);
		assertThat(message.getPayload()).isEqualTo(246);
		context.close();
	}

	@Test
	public void outboundOnlyServiceActivator() throws InterruptedException {
		TestApplicationContext context = createTestApplicationContext();
		context.registerChannel("testChannel", new DirectChannel());
		CountDownLatch latch = new CountDownLatch(1);
		OutboundOnlyTestBean testBean = new OutboundOnlyTestBean(latch);
		context.registerEndpoint("testBean", testBean);
		context.refresh();
		DestinationResolver<MessageChannel> channelResolver = new BeanFactoryChannelResolver(context);
		MessageChannel testChannel = channelResolver.resolveDestination("testChannel");
		testChannel.send(new GenericMessage<>("foo"));
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(testBean.getMessageText()).isEqualTo("foo");
		context.close();
	}

	@Test
	public void testChannelResolution() {
		TestApplicationContext context = createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		DirectChannel eventBus = new DirectChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.registerChannel("outputChannel", outputChannel);
		context.registerChannel("eventBus", eventBus);
		ServiceActivatorAnnotatedBean bean = new ServiceActivatorAnnotatedBean();
		context.registerEndpoint("testBean", bean);
		context.refresh();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannelName("outputChannel").build();
		inputChannel.send(message);
		Message<?> reply = outputChannel.receive(0);
		assertThat(reply).isNotNull();

		eventBus.send(new GenericMessage<>("foo"));
		assertThat(bean.getInvoked()).isTrue();

		context.close();
	}

	@Test
	public void testProxiedMessageEndpointAnnotation() {
		TestApplicationContext context = createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.registerChannel("outputChannel", outputChannel);
		ProxyFactory proxyFactory = new ProxyFactory(new AnnotatedTestService());
		Object proxy = proxyFactory.getProxy();
		context.registerEndpoint("proxy", proxy);
		context.refresh();
		inputChannel.send(new GenericMessage<>("world"));
		Message<?> message = outputChannel.receive(1000);
		assertThat(message.getPayload()).isEqualTo("hello world");
		context.close();
	}

	@Test
	public void testMessageEndpointAnnotationInherited() {
		TestApplicationContext context = createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.registerChannel("outputChannel", outputChannel);
		context.registerEndpoint("subclass", new SimpleAnnotatedEndpointSubclass());
		context.refresh();
		inputChannel.send(new GenericMessage<>("world"));
		Message<?> message = outputChannel.receive(1000);
		assertThat(message.getPayload()).isEqualTo("hello world");
		context.close();
	}

	@Test
	public void testMessageEndpointAnnotationInheritedWithProxy() {
		TestApplicationContext context = createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.registerChannel("outputChannel", outputChannel);
		ProxyFactory proxyFactory = new ProxyFactory(new SimpleAnnotatedEndpointSubclass());
		Object proxy = proxyFactory.getProxy();
		context.registerEndpoint("proxy", proxy);
		context.refresh();
		inputChannel.send(new GenericMessage<>("world"));
		Message<?> message = outputChannel.receive(1000);
		assertThat(message.getPayload()).isEqualTo("hello world");
		context.close();
	}

	@Test
	public void testMessageEndpointAnnotationInheritedFromInterface() {
		TestApplicationContext context = createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.registerChannel("outputChannel", outputChannel);
		context.registerEndpoint("impl", new SimpleAnnotatedEndpointImplementation());
		context.refresh();
		inputChannel.send(new GenericMessage<>("ABC"));
		Message<?> message = outputChannel.receive(1000);
		assertThat(message.getPayload()).isEqualTo("test-ABC");
		context.close();
	}

	@Test
	public void testMessageEndpointAnnotationInheritedFromInterfaceWithAutoCreatedChannels() {
		TestApplicationContext context = createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.registerChannel("outputChannel", outputChannel);
		context.registerEndpoint("impl", new SimpleAnnotatedEndpointImplementation());
		context.refresh();
		inputChannel.send(new GenericMessage<>("ABC"));
		Message<?> message = outputChannel.receive(1000);
		assertThat(message.getPayload()).isEqualTo("test-ABC");
		context.close();
	}

	@Test
	public void testMessageEndpointAnnotationInheritedFromInterfaceWithProxy() {
		TestApplicationContext context = createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		context.registerChannel("inputChannel", inputChannel);
		context.registerChannel("outputChannel", outputChannel);
		ProxyFactory proxyFactory = new ProxyFactory(new SimpleAnnotatedEndpointImplementation());
		context.registerEndpoint("proxy", proxyFactory.getProxy());
		context.refresh();
		inputChannel.send(new GenericMessage<>("ABC"));
		Message<?> message = outputChannel.receive(1000);
		assertThat(message.getPayload()).isEqualTo("test-ABC");
		context.close();
	}

	@Test
	public void testTransformer() {
		TestApplicationContext context = createTestApplicationContext();
		DirectChannel inputChannel = new DirectChannel();
		context.registerChannel("inputChannel", inputChannel);
		QueueChannel outputChannel = new QueueChannel();
		context.registerChannel("outputChannel", outputChannel);
		context.registerEndpoint("testBean", new TransformerAnnotationTestBean());
		context.refresh();
		inputChannel.send(new GenericMessage<>("foo"));
		Message<?> reply = outputChannel.receive(0);
		assertThat(reply.getPayload()).isEqualTo("FOO");

		MessageChannel inputChannel2 = context.getBean("inputChannel2", MessageChannel.class);
		inputChannel2.send(new GenericMessage<>("test2"));
		reply = outputChannel.receive(0);
		assertThat(reply.getPayload()).isEqualTo("TEST2");

		context.close();
	}

	private static TestApplicationContext createTestApplicationContext() {
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		new IntegrationRegistrar().registerBeanDefinitions(mock(), context.getDefaultListableBeanFactory());
		return context;
	}

	@MessageEndpoint
	public static class OutboundOnlyTestBean {

		private String messageText;

		private final CountDownLatch latch;

		OutboundOnlyTestBean(CountDownLatch latch) {
			this.latch = latch;
		}

		public String getMessageText() {
			return this.messageText;
		}

		@ServiceActivator(inputChannel = "testChannel")
		public void countdown(String input) {
			this.messageText = input;
			latch.countDown();
		}

	}

	public static class SimpleAnnotatedEndpointSubclass extends AnnotatedTestService {

	}

	@MessageEndpoint
	public interface SimpleAnnotatedEndpointInterface {

		String test(String input);

	}

	public static class SimpleAnnotatedEndpointImplementation implements SimpleAnnotatedEndpointInterface {

		@Override
		@ServiceActivator(inputChannel = "inputChannel", outputChannel = "outputChannel")
		public String test(String input) {
			return "test-" + input;
		}

	}

	@MessageEndpoint
	public static class ServiceActivatorAnnotatedBean {

		public final AtomicBoolean invoked = new AtomicBoolean();

		@ServiceActivator(inputChannel = "inputChannel")
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
	public static class TransformerAnnotationTestBean {

		@Transformer(inputChannel = "inputChannel", outputChannel = "outputChannel")
		@Transformer(inputChannel = "inputChannel2", outputChannel = "outputChannel")
		public String transformBefore(String input) {
			return input.toUpperCase();
		}

	}

	public static class ServiceActivatorAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			return callback.execute() + " advised";
		}

	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@ServiceActivator(inputChannel = "eventBus")
	public @interface EventHandler {

	}

}
