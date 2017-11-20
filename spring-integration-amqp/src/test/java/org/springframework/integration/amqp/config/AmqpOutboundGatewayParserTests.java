/*
 * Copyright 2002-2017 the original author or authors.
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
import static org.mockito.ArgumentMatchers.isNull;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.amqp.outbound.AsyncAmqpOutboundGateway;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.context.Orderable;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
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
@RunWith(SpringRunner.class)
@DirtiesContext
public class AmqpOutboundGatewayParserTests {

	private static volatile int adviceCalled;

	@Autowired
	private ApplicationContext  context;

	@Test
	public void testGatewayConfig() {
		Object edc = this.context.getBean("rabbitGateway");
		assertFalse(TestUtils.getPropertyValue(edc, "autoStartup", Boolean.class));
		AmqpOutboundEndpoint gateway = TestUtils.getPropertyValue(edc, "handler", AmqpOutboundEndpoint.class);
		assertEquals("amqp:outbound-gateway", gateway.getComponentType());
		assertTrue(TestUtils.getPropertyValue(gateway, "requiresReply", Boolean.class));
		checkGWProps(this.context, gateway);

		AsyncAmqpOutboundGateway async = this.context.getBean("asyncGateway.handler", AsyncAmqpOutboundGateway.class);
		assertEquals("amqp:outbound-async-gateway", async.getComponentType());
		checkGWProps(this.context, async);
		assertSame(this.context.getBean("asyncTemplate"), TestUtils.getPropertyValue(async, "template"));
		assertSame(this.context.getBean("ems"), TestUtils.getPropertyValue(gateway, "errorMessageStrategy"));
	}

	protected void checkGWProps(ApplicationContext context, Orderable gateway) {
		assertEquals(5, gateway.getOrder());
		assertEquals(context.getBean("fromRabbit"), TestUtils.getPropertyValue(gateway, "outputChannel"));
		MessageChannel returnChannel = context.getBean("returnChannel", MessageChannel.class);
		assertSame(returnChannel, TestUtils.getPropertyValue(gateway, "returnChannel"));

		Long sendTimeout = TestUtils.getPropertyValue(gateway, "messagingTemplate.sendTimeout", Long.class);

		assertEquals(Long.valueOf(777), sendTimeout);
		assertTrue(TestUtils.getPropertyValue(gateway, "lazyConnect", Boolean.class));
		assertEquals("42",
				TestUtils.getPropertyValue(gateway, "delayExpression", org.springframework.expression.Expression.class)
						.getExpressionString());
	}

	@Test
	public void withHeaderMapperCustomRequestResponse() {
		Object eventDrivenConsumer = this.context.getBean("withHeaderMapperCustomRequestResponse");

		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivenConsumer, "handler",
				AmqpOutboundEndpoint.class);
		assertNotNull(TestUtils.getPropertyValue(endpoint, "defaultDeliveryMode"));
		assertFalse(TestUtils.getPropertyValue(endpoint, "lazyConnect", Boolean.class));

		assertFalse(TestUtils.getPropertyValue(endpoint, "requiresReply", Boolean.class));
		assertTrue(TestUtils.getPropertyValue(endpoint, "headersMappedLast", Boolean.class));

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);
		final AtomicBoolean shouldBePersistent = new AtomicBoolean();

		Mockito.doAnswer(invocation -> {
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
			return new org.springframework.amqp.core.Message("hello".getBytes(), amqpProperties);
		})
				.when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class),
				Mockito.any(org.springframework.amqp.core.Message.class), isNull());
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = this.context.getBean("toRabbit1", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);

		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class),
				Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class),
				isNull());

		// verify reply
		QueueChannel queueChannel = this.context.getBean("fromRabbit", QueueChannel.class);
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
	}

	@Test
	public void withHeaderMapperCustomAndStandardResponse() {
		Object eventDrivenConsumer = this.context.getBean("withHeaderMapperCustomAndStandardResponse");

		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivenConsumer, "handler",
				AmqpOutboundEndpoint.class);
		assertNull(TestUtils.getPropertyValue(endpoint, "defaultDeliveryMode"));
		assertFalse(TestUtils.getPropertyValue(endpoint, "headersMappedLast", Boolean.class));

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(invocation -> {
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
			amqpProperties.setReceivedDeliveryMode(properties.getDeliveryMode());
			return new org.springframework.amqp.core.Message("hello".getBytes(), amqpProperties);
		})
				.when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class),
				Mockito.any(org.springframework.amqp.core.Message.class), isNull());
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = this.context.getBean("toRabbit2", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);

		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class),
				Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class),
				isNull());

		// verify reply
		QueueChannel queueChannel = this.context.getBean("fromRabbit", QueueChannel.class);
		Message<?> replyMessage = queueChannel.receive(0);
		assertEquals("bar", replyMessage.getHeaders().get("bar"));
		assertEquals("foo", replyMessage.getHeaders().get("foo")); // copied from request Message
		assertNull(replyMessage.getHeaders().get("foobar"));
		assertNotNull(replyMessage.getHeaders().get(AmqpHeaders.RECEIVED_DELIVERY_MODE));
		assertNotNull(replyMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
		assertNotNull(replyMessage.getHeaders().get(AmqpHeaders.APP_ID));
	}

	@Test
	public void withHeaderMapperNothingToMap() {
		Object eventDrivenConsumer = this.context.getBean("withHeaderMapperNothingToMap");

		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivenConsumer, "handler",
				AmqpOutboundEndpoint.class);

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(invocation -> {
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
		})
				.when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class),
				Mockito.any(org.springframework.amqp.core.Message.class), isNull());
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = this.context.getBean("toRabbit3", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);

		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class),
				Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class),
				isNull());

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
	}

	@Test //INT-1029
	public void amqpOutboundGatewayWithinChain() {
		Object eventDrivenConsumer = this.context.getBean("chainWithRabbitOutboundGateway");

		List<?> chainHandlers = TestUtils.getPropertyValue(eventDrivenConsumer, "handler.handlers", List.class);

		AmqpOutboundEndpoint endpoint = (AmqpOutboundEndpoint) chainHandlers.get(0);

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(invocation -> {
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
		})
				.when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class),
				Mockito.any(org.springframework.amqp.core.Message.class), isNull());
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);


		MessageChannel requestChannel = this.context.getBean("toRabbit4", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);

		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class),
				Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class),
				isNull());

		// verify reply
		QueueChannel queueChannel = this.context.getBean("fromRabbit", QueueChannel.class);
		Message<?> replyMessage = queueChannel.receive(0);
		assertEquals("hello", new String((byte[]) replyMessage.getPayload()));
		assertNull(replyMessage.getHeaders().get("bar"));
		assertEquals("foo", replyMessage.getHeaders().get("foo")); // copied from request Message
		assertNull(replyMessage.getHeaders().get("foobar"));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.APP_ID));

	}

	@Test
	public void testInt2971HeaderMapperAndMappedHeadersExclusivity() {
		try {
			new ClassPathXmlApplicationContext("AmqpOutboundGatewayParserTests-headerMapper-fail-context.xml",
					this.getClass()).close();
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
