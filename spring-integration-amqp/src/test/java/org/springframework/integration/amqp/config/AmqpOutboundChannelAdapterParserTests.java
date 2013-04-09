/*
 * Copyright 2002-2013 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.rabbit.support.PublisherCallbackChannel;
import org.springframework.amqp.rabbit.support.PublisherCallbackChannelImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.amqp.AmqpHeaders;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.context.NamedComponent;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AmqpOutboundChannelAdapterParserTests {

	private static volatile int adviceCalled;

	@Autowired
	private ApplicationContext context;

	@Autowired
	@Qualifier("withCustomHeaderMapper.handler")
	private MessageHandler amqpMessageHandlerWithCustomHeaderMapper;

	@Test
	public void verifyIdAsChannel() {
		Object channel = context.getBean("rabbitOutbound");
		Object adapter = context.getBean("rabbitOutbound.adapter");
		assertEquals(DirectChannel.class, channel.getClass());
		assertEquals(EventDrivenConsumer.class, adapter.getClass());
		MessageHandler handler = TestUtils.getPropertyValue(adapter, "handler", MessageHandler.class);
		assertTrue(handler instanceof NamedComponent);
		assertEquals("amqp:outbound-channel-adapter", ((NamedComponent) handler).getComponentType());
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@Test
	public void withHeaderMapperCustomHeaders() {
		Object eventDrivenConsumer = context.getBean("withHeaderMapperCustomHeaders");

		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivenConsumer, "handler", AmqpOutboundEndpoint.class);

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				org.springframework.amqp.core.Message amqpReplyMessage = (org.springframework.amqp.core.Message) args[2];
				MessageProperties properties = amqpReplyMessage.getMessageProperties();
				assertEquals("foo", properties.getHeaders().get("foo"));
				assertEquals("foobar", properties.getHeaders().get("foobar"));
				assertNull(properties.getHeaders().get("bar"));
				return null;
			}
		})
				.when(amqpTemplate).send(Mockito.any(String.class), Mockito.any(String.class),
				Mockito.any(org.springframework.amqp.core.Message.class), Mockito.any(CorrelationData.class));
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = context.getBean("requestChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").setHeader("bar", "bar").setHeader("foobar", "foobar").build();
		requestChannel.send(message);
		Mockito.verify(amqpTemplate, Mockito.times(1)).send(Mockito.any(String.class), Mockito.any(String.class),
				Mockito.any(org.springframework.amqp.core.Message.class), Mockito.any(CorrelationData.class));
	}

	@Test
	public void parseWithPublisherConfirms() {
		Object eventDrivenConsumer = context.getBean("withPublisherConfirms");
		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivenConsumer, "handler", AmqpOutboundEndpoint.class);
		NullChannel nullChannel = context.getBean(NullChannel.class);
		MessageChannel ackChannel = context.getBean("ackChannel", MessageChannel.class);
		assertSame(ackChannel, TestUtils.getPropertyValue(endpoint, "confirmAckChannel"));
		assertSame(nullChannel, TestUtils.getPropertyValue(endpoint, "confirmNackChannel"));
	}

	@Test
	public void withPublisherConfirms() throws Exception {
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		Connection mockConnection = mock(Connection.class);
		Channel mockChannel = mock(Channel.class);

		when(connectionFactory.createConnection()).thenReturn(mockConnection);
		PublisherCallbackChannelImpl publisherCallbackChannel = new PublisherCallbackChannelImpl(mockChannel);
		when(mockConnection.createChannel(false)).thenReturn(publisherCallbackChannel);

		MessageChannel requestChannel = context.getBean("pcRequestChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello")
				.setHeader("amqp_confirmCorrelationData", "foo")
				.build();
		requestChannel.send(message);
		PollableChannel ackChannel = context.getBean("ackChannel", PollableChannel.class);
		publisherCallbackChannel.handleAck(0, false);
		Message<?> ack = ackChannel.receive(1000);
		assertNotNull(ack);
		assertEquals("foo", ack.getPayload());
		assertEquals(Boolean.TRUE, ack.getHeaders().get(AmqpHeaders.PUBLISH_CONFIRM));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void amqpOutboundChannelAdapterWithinChain() {
		Object eventDrivernConsumer = context.getBean("chainWithRabbitOutbound");

		List chainHandlers = TestUtils.getPropertyValue(eventDrivernConsumer, "handler.handlers", List.class);

		AmqpOutboundEndpoint endpoint = (AmqpOutboundEndpoint) chainHandlers.get(0);

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(new Answer() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				org.springframework.amqp.core.Message amqpReplyMessage = (org.springframework.amqp.core.Message) args[2];
				assertEquals("hello", new String(amqpReplyMessage.getBody()));
				return null;
			}
		})
				.when(amqpTemplate).send(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class),
				Mockito.any(CorrelationData.class));
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = context.getBean("amqpOutboundChannelAdapterWithinChain", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").build();
		requestChannel.send(message);
		Mockito.verify(amqpTemplate, Mockito.times(1)).send(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class),
				Mockito.any(CorrelationData.class));
	}

	@Test
	public void withReturns() throws Exception {
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		Connection mockConnection = mock(Connection.class);
		Channel mockChannel = mock(Channel.class);

		when(connectionFactory.createConnection()).thenReturn(mockConnection);
		PublisherCallbackChannelImpl publisherCallbackChannel = new PublisherCallbackChannelImpl(mockChannel);
		when(mockConnection.createChannel(false)).thenReturn(publisherCallbackChannel);

		MessageChannel requestChannel = context.getBean("returnRequestChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").build();
		requestChannel.send(message);
		PollableChannel returnChannel = context.getBean("returnChannel", PollableChannel.class);
		RabbitTemplate template = context.getBean("amqpTemplate", RabbitTemplate.class);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put(PublisherCallbackChannel.RETURN_CORRELATION, template.getUUID());
		BasicProperties properties = mock(BasicProperties.class);
		when(properties.getHeaders()).thenReturn(headers);
		when(properties.getContentType()).thenReturn("text/plain");
		publisherCallbackChannel.handleReturn(123, "reply text", "anExchange", "bar", properties, "hello".getBytes());
		Message<?> returned = returnChannel.receive(1000);
		assertNotNull(returned);
		assertEquals(123, returned.getHeaders().get(AmqpHeaders.RETURN_REPLY_CODE));
		assertEquals("reply text", returned.getHeaders().get(AmqpHeaders.RETURN_REPLY_TEXT));
		assertEquals("anExchange", returned.getHeaders().get(AmqpHeaders.RETURN_EXCHANGE));
		assertEquals("bar", returned.getHeaders().get(AmqpHeaders.RETURN_ROUTING_KEY));
		assertEquals("hello", returned.getPayload());
	}

	@Test
	public void testInt2718FailForOutboundAdapterChannelAttribute() {
		try {
			new ClassPathXmlApplicationContext("AmqpOutboundChannelAdapterWithinChainParserTests-fail-context.xml", this.getClass());
			fail("Expected BeanDefinitionParsingException");
		}
		catch (BeansException e) {
			assertTrue(e instanceof BeanDefinitionParsingException);
			assertTrue(e.getMessage().contains("The 'channel' attribute isn't allowed for 'amqp:outbound-channel-adapter' " +
					"when it is used as a nested element"));
		}
	}

	@Test
	public void testInt2773UseDefaultAmqpTemplateExchangeAndRoutingLey() throws IOException {
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		Connection mockConnection = mock(Connection.class);
		Channel mockChannel = mock(Channel.class);

		when(connectionFactory.createConnection()).thenReturn(mockConnection);
		PublisherCallbackChannelImpl publisherCallbackChannel = new PublisherCallbackChannelImpl(mockChannel);
		when(mockConnection.createChannel(false)).thenReturn(publisherCallbackChannel);

		MessageChannel requestChannel = context.getBean("toRabbitOnlyWithTemplateChannel", MessageChannel.class);
		requestChannel.send(MessageBuilder.withPayload("test").build());
		Mockito.verify(mockChannel, Mockito.times(1)).basicPublish(Mockito.eq("default.test.exchange"), Mockito.eq("default.routing.key"),
				Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(BasicProperties.class), Mockito.any(byte[].class));
	}

	@Test
	public void testInt2773WithDefaultAmqpTemplateExchangeAndRoutingLey() throws IOException {
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		Connection mockConnection = mock(Connection.class);
		Channel mockChannel = mock(Channel.class);

		when(connectionFactory.createConnection()).thenReturn(mockConnection);
		PublisherCallbackChannelImpl publisherCallbackChannel = new PublisherCallbackChannelImpl(mockChannel);
		when(mockConnection.createChannel(false)).thenReturn(publisherCallbackChannel);

		MessageChannel requestChannel = context.getBean("withDefaultAmqpTemplateExchangeAndRoutingKey", MessageChannel.class);
		requestChannel.send(MessageBuilder.withPayload("test").build());
		Mockito.verify(mockChannel, Mockito.times(1)).basicPublish(Mockito.eq(""), Mockito.eq(""),
				Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(BasicProperties.class), Mockito.any(byte[].class));
	}

	@Test
	public void testInt2773WithOverrideToDefaultAmqpTemplateExchangeAndRoutingLey() throws IOException {
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		Connection mockConnection = mock(Connection.class);
		Channel mockChannel = mock(Channel.class);

		when(connectionFactory.createConnection()).thenReturn(mockConnection);
		PublisherCallbackChannelImpl publisherCallbackChannel = new PublisherCallbackChannelImpl(mockChannel);
		when(mockConnection.createChannel(false)).thenReturn(publisherCallbackChannel);

		MessageChannel requestChannel = context.getBean("overrideTemplateAttributesToEmpty", MessageChannel.class);
		requestChannel.send(MessageBuilder.withPayload("test").build());
		Mockito.verify(mockChannel, Mockito.times(1)).basicPublish(Mockito.eq(""), Mockito.eq(""),
				Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(BasicProperties.class), Mockito.any(byte[].class));
	}

	@Test
	public void testInt2971HeaderMapperAndMappedHeadersExclusivity() {
		try {
			new ClassPathXmlApplicationContext("AmqpOutboundChannelAdapterParserTests-headerMapper-fail-context.xml", this.getClass());
		}
		catch (BeanDefinitionParsingException e) {
			assertTrue(e.getMessage().startsWith("Configuration problem: The 'header-mapper' attribute " +
					"is mutually exclusive with 'mapped-request-headers' or 'mapped-reply-headers'"));
		}
	}

	@Test
	public void testInt2971AmqpOutboundChannelAdapterWithCustomHeaderMapper() {
		AmqpHeaderMapper headerMapper = TestUtils.getPropertyValue(this.amqpMessageHandlerWithCustomHeaderMapper, "headerMapper", AmqpHeaderMapper.class);
		assertSame(this.context.getBean("customHeaderMapper"), headerMapper);
	}


	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}
}
