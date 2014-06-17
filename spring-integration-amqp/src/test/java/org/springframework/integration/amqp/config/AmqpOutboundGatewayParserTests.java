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
package org.springframework.integration.amqp.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.amqp.AmqpHeaders;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.ReflectionUtils;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gunnar Hillert
 *
 * @since 2.1
 *
 */
public class AmqpOutboundGatewayParserTests {

	private static volatile int adviceCalled;

	@Test
	public void testGatewayConfig(){
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"AmqpOutboundGatewayParserTests-context.xml", this.getClass());
		Object edc = context.getBean("rabbitGateway");
		AmqpOutboundEndpoint gateway = TestUtils.getPropertyValue(edc, "handler", AmqpOutboundEndpoint.class);
		assertEquals(5, gateway.getOrder());
		assertTrue(TestUtils.getPropertyValue(gateway, "requiresReply", Boolean.class));
		assertEquals(context.getBean("fromRabbit"), TestUtils.getPropertyValue(gateway, "outputChannel"));
		assertEquals("amqp:outbound-gateway", gateway.getComponentType());
		MessageChannel returnChannel = context.getBean("returnChannel", MessageChannel.class);
		assertSame(returnChannel, TestUtils.getPropertyValue(gateway, "returnChannel"));

		Long sendTimeout = TestUtils.getPropertyValue(gateway, "messagingTemplate.sendTimeout", Long.class);

		assertEquals(Long.valueOf(777), sendTimeout);
		assertTrue(TestUtils.getPropertyValue(gateway, "lazyConnect", Boolean.class));

		context.close();
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void withHeaderMapperCustomRequestResponse() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"AmqpOutboundGatewayParserTests-context.xml", this.getClass());
		Object eventDrivernConsumer = context.getBean("withHeaderMapperCustomRequestResponse");

		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivernConsumer, "handler", AmqpOutboundEndpoint.class);
		assertNotNull(TestUtils.getPropertyValue(endpoint, "defaultDeliveryMode"));
		assertFalse(TestUtils.getPropertyValue(endpoint, "lazyConnect", Boolean.class));

		assertFalse(TestUtils.getPropertyValue(endpoint, "requiresReply", Boolean.class));

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);
		final AtomicBoolean shouldBePersistent = new AtomicBoolean();

		Mockito.doAnswer(new Answer() {
		      @Override
			public Object answer(InvocationOnMock invocation) {
		          Object[] args = invocation.getArguments();
		          org.springframework.amqp.core.Message amqpRequestMessage = (org.springframework.amqp.core.Message) args[2];
		          MessageProperties properties = amqpRequestMessage.getMessageProperties();
		          assertEquals("foo", properties.getHeaders().get("foo"));
		          assertEquals(shouldBePersistent.get() ? MessageDeliveryMode.PERSISTENT
		              : MessageDeliveryMode.NON_PERSISTENT, properties.getDeliveryMode());
		          // mock reply AMQP message
		          MessageProperties amqpProperties = new MessageProperties();
		  		  amqpProperties.setAppId("test.appId");
		  		  amqpProperties.setHeader("foobar", "foobar");
		  		  amqpProperties.setHeader("bar", "bar");
		          org.springframework.amqp.core.Message amqpReplyMessage = new org.springframework.amqp.core.Message("hello".getBytes(), amqpProperties);
		          return amqpReplyMessage;
		      }})
		 .when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = context.getBean("toRabbit1", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);

		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));

		// verify reply
		QueueChannel queueChannel = context.getBean("fromRabbit", QueueChannel.class);
		Message<?> replyMessage = queueChannel.receive(0);
		assertNotNull(replyMessage);
		assertEquals("bar", replyMessage.getHeaders().get("bar"));
		assertEquals("foo", replyMessage.getHeaders().get("foo")); // copied from request Message
		assertNull(replyMessage.getHeaders().get("foobar"));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.APP_ID));

		shouldBePersistent.set(true);
		message = MessageBuilder.withPayload("hello")
				.setHeader("foo", "foo")
				.setHeader(AmqpHeaders.DELIVERY_MODE, MessageDeliveryMode.PERSISTENT)
				.build();
		requestChannel.send(message);
		replyMessage = queueChannel.receive(0);
		assertNotNull(replyMessage);

		context.close();
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void withHeaderMapperCustomAndStandardResponse() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"AmqpOutboundGatewayParserTests-context.xml", this.getClass());
		Object eventDrivernConsumer = context.getBean("withHeaderMapperCustomAndStandardResponse");

		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivernConsumer, "handler", AmqpOutboundEndpoint.class);
		assertNull(TestUtils.getPropertyValue(endpoint, "defaultDeliveryMode"));

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(new Answer() {
		      @Override
			public Object answer(InvocationOnMock invocation) {
		          Object[] args = invocation.getArguments();
		          org.springframework.amqp.core.Message amqpRequestMessage = (org.springframework.amqp.core.Message) args[2];
		          MessageProperties properties = amqpRequestMessage.getMessageProperties();
		          assertEquals("foo", properties.getHeaders().get("foo"));
		          // mock reply AMQP message
		          MessageProperties amqpProperties = new MessageProperties();
		  		  amqpProperties.setAppId("test.appId");
		  		  amqpProperties.setHeader("foobar", "foobar");
		  		  amqpProperties.setHeader("bar", "bar");
				  assertEquals(MessageDeliveryMode.PERSISTENT, properties.getDeliveryMode());
		          org.springframework.amqp.core.Message amqpReplyMessage = new org.springframework.amqp.core.Message("hello".getBytes(), amqpProperties);
		          return amqpReplyMessage;
		      }})
		 .when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = context.getBean("toRabbit2", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);

		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));

		// verify reply
		QueueChannel queueChannel = context.getBean("fromRabbit", QueueChannel.class);
		Message<?> replyMessage = queueChannel.receive(0);
		assertEquals("bar", replyMessage.getHeaders().get("bar"));
		assertEquals("foo", replyMessage.getHeaders().get("foo")); // copied from request Message
		assertNull(replyMessage.getHeaders().get("foobar"));
		assertNotNull(replyMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE));
		assertNotNull(replyMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
		assertNotNull(replyMessage.getHeaders().get(AmqpHeaders.APP_ID));
		context.close();
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void withHeaderMapperNothingToMap() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"AmqpOutboundGatewayParserTests-context.xml", this.getClass());
		Object eventDrivernConsumer = context.getBean("withHeaderMapperNothingToMap");

		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivernConsumer, "handler", AmqpOutboundEndpoint.class);

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(new Answer() {
		      @Override
			public Object answer(InvocationOnMock invocation) {
		          Object[] args = invocation.getArguments();
		          org.springframework.amqp.core.Message amqpRequestMessage = (org.springframework.amqp.core.Message) args[2];
		          MessageProperties properties = amqpRequestMessage.getMessageProperties();
		          assertNull(properties.getHeaders().get("foo"));
		          // mock reply AMQP message
		          MessageProperties amqpProperties = new MessageProperties();
		  		  amqpProperties.setAppId("test.appId");
		  		  amqpProperties.setHeader("foobar", "foobar");
		  		  amqpProperties.setHeader("bar", "bar");
		          org.springframework.amqp.core.Message amqpReplyMessage = new org.springframework.amqp.core.Message("hello".getBytes(), amqpProperties);
		          return amqpReplyMessage;
		      }})
		 .when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = context.getBean("toRabbit3", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);

		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));

		// verify reply
		QueueChannel queueChannel = context.getBean("fromRabbit", QueueChannel.class);
		Message<?> replyMessage = queueChannel.receive(0);
		assertNull(replyMessage.getHeaders().get("bar"));
		assertEquals("foo", replyMessage.getHeaders().get("foo")); // copied from request Message
		assertNull(replyMessage.getHeaders().get("foobar"));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.APP_ID));
		assertEquals(1, adviceCalled);
		context.close();
	}

	@Test //INT-1029
	public void amqpOutboundGatewayWithinChain() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"AmqpOutboundGatewayParserTests-context.xml", this.getClass());
		Object eventDrivenConsumer = context.getBean("chainWithRabbitOutboundGateway");

		List<?> chainHandlers = TestUtils.getPropertyValue(eventDrivenConsumer, "handler.handlers", List.class);

		AmqpOutboundEndpoint endpoint = (AmqpOutboundEndpoint) chainHandlers.get(0);

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(new Answer<org.springframework.amqp.core.Message>() {
			@Override
			public org.springframework.amqp.core.Message answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				org.springframework.amqp.core.Message amqpRequestMessage = (org.springframework.amqp.core.Message) args[2];
				MessageProperties properties = amqpRequestMessage.getMessageProperties();
				assertNull(properties.getHeaders().get("foo"));
				// mock reply AMQP message
				MessageProperties amqpProperties = new MessageProperties();
				amqpProperties.setAppId("test.appId");
				amqpProperties.setHeader("foobar", "foobar");
				amqpProperties.setHeader("bar", "bar");
				return new org.springframework.amqp.core.Message("hello".getBytes(), amqpProperties);
			}})
			.when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);


		MessageChannel requestChannel = context.getBean("toRabbit4", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);

		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));

		// verify reply
		QueueChannel queueChannel = context.getBean("fromRabbit", QueueChannel.class);
		Message<?> replyMessage = queueChannel.receive(0);
		assertEquals("hello", new String((byte[]) replyMessage.getPayload()));
		assertNull(replyMessage.getHeaders().get("bar"));
		assertEquals("foo", replyMessage.getHeaders().get("foo")); // copied from request Message
		assertNull(replyMessage.getHeaders().get("foobar"));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.APP_ID));
		context.close();

	}

	@Test
	public void testInt2971HeaderMapperAndMappedHeadersExclusivity() {
		try {
			new ClassPathXmlApplicationContext("AmqpOutboundGatewayParserTests-headerMapper-fail-context.xml", this.getClass());
		}
		catch (BeanDefinitionParsingException e) {
			assertTrue(e.getMessage().startsWith("Configuration problem: The 'header-mapper' attribute " +
					"is mutually exclusive with 'mapped-request-headers' or 'mapped-reply-headers'"));
		}
	}


	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return callback.execute();
		}

	}
}
