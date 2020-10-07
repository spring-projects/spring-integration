/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.amqp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.PublisherCallbackChannelImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ReflectionUtils;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gunnar Hillert
 *
 * @since 2.1
 */
@SpringJUnitConfig
@DirtiesContext
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
		assertThat(channel.getClass()).isEqualTo(DirectChannel.class);
		assertThat(adapter.getClass()).isEqualTo(EventDrivenConsumer.class);
		MessageHandler handler = TestUtils.getPropertyValue(adapter, "handler", MessageHandler.class);
		assertThat(handler instanceof NamedComponent).isTrue();
		assertThat(((NamedComponent) handler).getComponentType()).isEqualTo("amqp:outbound-channel-adapter");
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
		assertThat(TestUtils.getPropertyValue(handler, "lazyConnect", Boolean.class)).isTrue();
	}

	@Test
	public void withHeaderMapperCustomHeaders() {
		Object eventDrivenConsumer = context.getBean("withHeaderMapperCustomHeaders");

		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivenConsumer, "handler",
				AmqpOutboundEndpoint.class);
		assertThat(TestUtils.getPropertyValue(endpoint, "defaultDeliveryMode")).isNotNull();
		assertThat(TestUtils.getPropertyValue(endpoint, "lazyConnect", Boolean.class)).isFalse();
		assertThat(TestUtils
				.getPropertyValue(endpoint, "delayExpression", org.springframework.expression.Expression.class)
				.getExpressionString()).isEqualTo("42");
		assertThat(TestUtils.getPropertyValue(endpoint, "headersMappedLast", Boolean.class)).isFalse();

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "rabbitTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "rabbitTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);
		final AtomicBoolean shouldBePersistent = new AtomicBoolean();

		Mockito.doAnswer(invocation -> {
			Object[] args = invocation.getArguments();
			org.springframework.amqp.core.Message amqpMessage = (org.springframework.amqp.core.Message) args[2];
			MessageProperties properties = amqpMessage.getMessageProperties();
			assertThat(properties.getHeaders().get("foo")).isEqualTo("foo");
			assertThat(properties.getHeaders().get("foobar")).isEqualTo("foobar");
			assertThat(properties.getHeaders().get("bar")).isNull();
			assertThat(properties.getDeliveryMode()).isEqualTo(shouldBePersistent.get() ?
					MessageDeliveryMode.PERSISTENT
					: MessageDeliveryMode.NON_PERSISTENT);
			return null;
		})
				.when(amqpTemplate).send(Mockito.any(String.class), Mockito.any(String.class),
				Mockito.any(org.springframework.amqp.core.Message.class), Mockito.any(CorrelationData.class));
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = context.getBean("requestChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello")
				.setHeader("foo", "foo")
				.setHeader("bar", "bar")
				.setHeader("foobar", "foobar")
				.build();
		requestChannel.send(message);
		Mockito.verify(amqpTemplate, Mockito.times(1)).send(anyString(),
				isNull(), Mockito.any(org.springframework.amqp.core.Message.class), isNull());

		shouldBePersistent.set(true);
		message = MessageBuilder.withPayload("hello")
				.setHeader("foo", "foo")
				.setHeader("bar", "bar")
				.setHeader("foobar", "foobar")
				.setHeader(AmqpHeaders.DELIVERY_MODE, MessageDeliveryMode.PERSISTENT)
				.build();
		requestChannel.send(message);
	}

	@Test
	public void parseWithPublisherConfirms() {
		Object eventDrivenConsumer = context.getBean("withPublisherConfirms");
		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivenConsumer, "handler",
				AmqpOutboundEndpoint.class);
		NullChannel nullChannel = context.getBean(NullChannel.class);
		MessageChannel ackChannel = context.getBean("ackChannel", MessageChannel.class);
		assertThat(TestUtils.getPropertyValue(endpoint, "confirmAckChannel")).isSameAs(ackChannel);
		assertThat(TestUtils.getPropertyValue(endpoint, "confirmNackChannel")).isSameAs(nullChannel);
		assertThat(TestUtils.getPropertyValue(endpoint, "errorMessageStrategy")).isSameAs(context.getBean("ems"));
		assertThat(TestUtils.getPropertyValue(endpoint, "waitForConfirm", Boolean.class)).isFalse();
	}

	@Test
	public void parseWithPublisherConfirms2() {
		Object eventDrivenConsumer = context.getBean("withPublisherConfirms2");
		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivenConsumer, "handler",
				AmqpOutboundEndpoint.class);
		MessageChannel nackChannel = context.getBean("nackChannel", MessageChannel.class);
		MessageChannel ackChannel = context.getBean("ackChannel", MessageChannel.class);
		assertThat(TestUtils.getPropertyValue(endpoint, "confirmAckChannel")).isSameAs(ackChannel);
		assertThat(TestUtils.getPropertyValue(endpoint, "confirmNackChannel")).isSameAs(nackChannel);
		assertThat(TestUtils.getPropertyValue(endpoint, "confirmTimeout")).isEqualTo(Duration.ofMillis(2000));
		assertThat(TestUtils.getPropertyValue(endpoint, "errorMessageStrategy")).isSameAs(context.getBean("ems"));
		assertThat(TestUtils.getPropertyValue(endpoint, "waitForConfirm", Boolean.class)).isTrue();
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void amqpOutboundChannelAdapterWithinChain() {
		Object eventDrivenConsumer = context.getBean("chainWithRabbitOutbound");

		List chainHandlers = TestUtils.getPropertyValue(eventDrivenConsumer, "handler.handlers", List.class);

		AmqpOutboundEndpoint endpoint = (AmqpOutboundEndpoint) chainHandlers.get(0);
		assertThat(TestUtils.getPropertyValue(endpoint, "defaultDeliveryMode")).isNull();

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "rabbitTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "rabbitTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(invocation -> {
			Object[] args = invocation.getArguments();
			org.springframework.amqp.core.Message amqpMessage = (org.springframework.amqp.core.Message) args[2];
			MessageProperties properties = amqpMessage.getMessageProperties();
			assertThat(new String(amqpMessage.getBody())).isEqualTo("hello");
			assertThat(properties.getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
			return null;
		})
				.when(amqpTemplate).send(Mockito.any(String.class), Mockito.any(String.class),
				Mockito.any(org.springframework.amqp.core.Message.class),
				Mockito.any(CorrelationData.class));
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = context.getBean("amqpOutboundChannelAdapterWithinChain", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").build();
		requestChannel.send(message);
		Mockito.verify(amqpTemplate, Mockito.times(1)).send(Mockito.any(String.class),
				isNull(), Mockito.any(org.springframework.amqp.core.Message.class), isNull());
	}

	@Test
	public void testInt2718FailForOutboundAdapterChannelAttribute() {
		try {
			new ClassPathXmlApplicationContext("AmqpOutboundChannelAdapterWithinChainParserTests-fail-context.xml",
					this.getClass()).close();
			fail("Expected BeanDefinitionParsingException");
		}
		catch (BeansException e) {
			assertThat(e instanceof BeanDefinitionParsingException).isTrue();
			assertThat(e.getMessage().contains("The 'channel' attribute isn't allowed for " +
					"'amqp:outbound-channel-adapter' when it is used as a nested element")).isTrue();
		}
	}

	@Test
	public void testInt2773UseDefaultAmqpTemplateExchangeAndRoutingLey() throws IOException {
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		Connection mockConnection = mock(Connection.class);
		Channel mockChannel = mock(Channel.class);

		when(connectionFactory.createConnection()).thenReturn(mockConnection);
		PublisherCallbackChannelImpl publisherCallbackChannel = new PublisherCallbackChannelImpl(mockChannel,
				mock(ExecutorService.class));
		when(mockConnection.createChannel(false)).thenReturn(publisherCallbackChannel);

		MessageChannel requestChannel = context.getBean("toRabbitOnlyWithTemplateChannel", MessageChannel.class);
		requestChannel.send(MessageBuilder.withPayload("test").build());
		Mockito.verify(mockChannel, Mockito.times(1)).basicPublish(Mockito.eq("default.test.exchange"),
				Mockito.eq("default.routing.key"),
				Mockito.anyBoolean(), Mockito.any(BasicProperties.class), Mockito.any(byte[].class));
	}

	@Test
	public void testInt2773WithDefaultAmqpTemplateExchangeAndRoutingKey() throws IOException {
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		Connection mockConnection = mock(Connection.class);
		Channel mockChannel = mock(Channel.class);

		when(connectionFactory.createConnection()).thenReturn(mockConnection);
		PublisherCallbackChannelImpl publisherCallbackChannel = new PublisherCallbackChannelImpl(mockChannel,
				mock(ExecutorService.class));
		when(mockConnection.createChannel(false)).thenReturn(publisherCallbackChannel);

		MessageChannel requestChannel = context.getBean("withDefaultAmqpTemplateExchangeAndRoutingKey",
				MessageChannel.class);
		requestChannel.send(MessageBuilder.withPayload("test").build());
		Mockito.verify(mockChannel, Mockito.times(1)).basicPublish(Mockito.eq(""), Mockito.eq(""),
				Mockito.anyBoolean(), Mockito.any(BasicProperties.class), Mockito.any(byte[].class));
	}

	@Test
	public void testInt2773WithOverrideToDefaultAmqpTemplateExchangeAndRoutingLey() throws IOException {
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		Connection mockConnection = mock(Connection.class);
		Channel mockChannel = mock(Channel.class);

		when(connectionFactory.createConnection()).thenReturn(mockConnection);
		PublisherCallbackChannelImpl publisherCallbackChannel = new PublisherCallbackChannelImpl(mockChannel,
				mock(ExecutorService.class));
		when(mockConnection.createChannel(false)).thenReturn(publisherCallbackChannel);

		MessageChannel requestChannel = context.getBean("overrideTemplateAttributesToEmpty", MessageChannel.class);
		requestChannel.send(MessageBuilder.withPayload("test").build());
		Mockito.verify(mockChannel, Mockito.times(1)).basicPublish(Mockito.eq(""), Mockito.eq(""),
				Mockito.anyBoolean(), Mockito.any(BasicProperties.class), Mockito.any(byte[].class));
	}

	@Test
	public void testInt2971HeaderMapperAndMappedHeadersExclusivity() {
		try {
			new ClassPathXmlApplicationContext("AmqpOutboundChannelAdapterParserTests-headerMapper-fail-context.xml",
					this.getClass()).close();
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage().startsWith("Configuration problem: The 'header-mapper' attribute " +
					"is mutually exclusive with 'mapped-request-headers' or 'mapped-reply-headers'")).isTrue();
		}
	}

	@Test
	public void testInt2971AmqpOutboundChannelAdapterWithCustomHeaderMapper() {
		AmqpHeaderMapper headerMapper = TestUtils.getPropertyValue(this.amqpMessageHandlerWithCustomHeaderMapper,
				"headerMapper", AmqpHeaderMapper.class);
		assertThat(headerMapper).isSameAs(this.context.getBean("customHeaderMapper"));
		assertThat(TestUtils.getPropertyValue(this.amqpMessageHandlerWithCustomHeaderMapper,
				"headersMappedLast", Boolean.class)).isTrue();
	}

	@Test
	public void testInt3430FailForNotLazyConnect() {
		RabbitTemplate amqpTemplate = spy(new RabbitTemplate());
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RuntimeException toBeThrown = new RuntimeException("Test Connection Exception");
		doThrow(toBeThrown).when(connectionFactory).createConnection();
		when(amqpTemplate.getConnectionFactory()).thenReturn(connectionFactory);
		AmqpOutboundEndpoint handler = new AmqpOutboundEndpoint(amqpTemplate);
		LogAccessor logger = spy(TestUtils.getPropertyValue(handler, "logger", LogAccessor.class));
		new DirectFieldAccessor(handler).setPropertyValue("logger", logger);
		doNothing().when(logger).error(toBeThrown, "Failed to eagerly establish the connection.");
		ApplicationContext context = mock(ApplicationContext.class);
		handler.setApplicationContext(context);
		handler.setBeanFactory(context);
		handler.afterPropertiesSet();
		handler.start();
		handler.stop();
		verify(logger, never()).error(any(RuntimeException.class), anyString());
		handler.setLazyConnect(false);
		handler.start();
		verify(logger).error(toBeThrown, "Failed to eagerly establish the connection.");
		handler.stop();
	}


	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}
