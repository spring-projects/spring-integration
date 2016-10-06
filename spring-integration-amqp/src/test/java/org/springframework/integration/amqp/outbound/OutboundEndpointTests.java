/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.amqp.outbound;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 3.0
 */
public class OutboundEndpointTests {

	@Test
	public void testDelayExpression() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RabbitTemplate amqpTemplate = spy(new RabbitTemplate(connectionFactory));
		AmqpOutboundEndpoint endpoint = new AmqpOutboundEndpoint(amqpTemplate);
		willDoNothing()
				.given(amqpTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
		willAnswer(invocation -> invocation.getArgumentAt(2, Message.class))
				.given(amqpTemplate)
					.sendAndReceive(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
		endpoint.setExchangeName("foo");
		endpoint.setRoutingKey("bar");
		endpoint.setDelayExpressionString("42");
		endpoint.setBeanFactory(mock(BeanFactory.class));
		endpoint.afterPropertiesSet();
		endpoint.handleMessage(new GenericMessage<>("foo"));
		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(amqpTemplate).send(eq("foo"), eq("bar"), captor.capture(), any(CorrelationData.class));
		assertThat(captor.getValue().getMessageProperties().getDelay(), equalTo(42));
		endpoint.setExpectReply(true);
		endpoint.setOutputChannel(new NullChannel());
		endpoint.handleMessage(new GenericMessage<>("foo"));
		verify(amqpTemplate).sendAndReceive(eq("foo"), eq("bar"), captor.capture(), any(CorrelationData.class));
		assertThat(captor.getValue().getMessageProperties().getDelay(), equalTo(42));
	}

	@Test
	public void testAsyncDelayExpression() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		AsyncRabbitTemplate amqpTemplate = spy(new AsyncRabbitTemplate(new RabbitTemplate(connectionFactory),
				new SimpleMessageListenerContainer(connectionFactory), "replyTo"));
		amqpTemplate.setTaskScheduler(mock(TaskScheduler.class));
		AsyncAmqpOutboundGateway gateway = new AsyncAmqpOutboundGateway(amqpTemplate);
		willAnswer(
				invocation -> amqpTemplate.new RabbitMessageFuture("foo", invocation.getArgumentAt(2, Message.class)))
					.given(amqpTemplate).sendAndReceive(anyString(), anyString(), any(Message.class));
		gateway.setExchangeName("foo");
		gateway.setRoutingKey("bar");
		gateway.setDelayExpressionString("42");
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setOutputChannel(new NullChannel());
		gateway.afterPropertiesSet();
		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		gateway.handleMessage(new GenericMessage<>("foo"));
		verify(amqpTemplate).sendAndReceive(eq("foo"), eq("bar"), captor.capture());
		assertThat(captor.getValue().getMessageProperties().getDelay(), equalTo(42));
	}

	@Test
	public void testHeaderMapperWinsAdapter() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RabbitTemplate amqpTemplate = spy(new RabbitTemplate(connectionFactory));
		AmqpOutboundEndpoint endpoint = new AmqpOutboundEndpoint(amqpTemplate);
		final AtomicReference<Message> amqpMessage =
				new AtomicReference<Message>();
		willAnswer(invocation -> {
			amqpMessage.set((Message) invocation.getArguments()[2]);
			return null;
		}).given(amqpTemplate).send(anyString(), anyString(), any(Message.class),
				any(CorrelationData.class));
		org.springframework.messaging.Message<?> message = MessageBuilder.withPayload("foo")
				.setHeader(MessageHeaders.CONTENT_TYPE, "bar")
				.build();
		endpoint.handleMessage(message);
		assertNotNull(amqpMessage.get());
		assertEquals("bar", amqpMessage.get().getMessageProperties().getContentType());
	}

	@Test
	public void testHeaderMapperWinsGateway() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		TestRabbitTemplate amqpTemplate = spy(new TestRabbitTemplate(connectionFactory));
		AmqpOutboundEndpoint endpoint = new AmqpOutboundEndpoint(amqpTemplate);
		endpoint.setExpectReply(true);
		DefaultAmqpHeaderMapper mapper = DefaultAmqpHeaderMapper.inboundMapper();
		mapper.setRequestHeaderNames("*");
		endpoint.setHeaderMapper(mapper);
		final AtomicReference<Message> amqpMessage =
				new AtomicReference<Message>();
		willAnswer(invocation -> {
			amqpMessage.set((Message) invocation.getArguments()[2]);
			return null;
		}).given(amqpTemplate)
				.doSendAndReceiveWithTemporary(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
		org.springframework.messaging.Message<?> message = MessageBuilder.withPayload("foo")
				.setHeader(MessageHeaders.CONTENT_TYPE, "bar")
				.setReplyChannel(new QueueChannel())
				.build();
		endpoint.handleMessage(message);
		assertNotNull(amqpMessage.get());
		assertEquals("bar", amqpMessage.get().getMessageProperties().getContentType());
		assertNull(amqpMessage.get().getMessageProperties().getHeaders().get(MessageHeaders.REPLY_CHANNEL));
	}

	/**
	 * Increase method visibility
	 */
	private class TestRabbitTemplate extends RabbitTemplate {

		private TestRabbitTemplate(ConnectionFactory connectionFactory) {
			super(connectionFactory);
		}

		@Override
		public org.springframework.amqp.core.Message doSendAndReceiveWithTemporary(String exchange,
				String routingKey, org.springframework.amqp.core.Message message, CorrelationData correlationData) {
			return super.doSendAndReceiveWithTemporary(exchange, routingKey, message, correlationData);
		}

	}

}
