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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHeaders;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 3.0
 */
public class OutboundEndpointTests {

	@Test
	public void testHeaderMapperWinsAdapter() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RabbitTemplate amqpTemplate = spy(new RabbitTemplate(connectionFactory));
		AmqpOutboundEndpoint endpoint = new AmqpOutboundEndpoint(amqpTemplate);
		final AtomicReference<Message> amqpMessage =
				new AtomicReference<Message>();
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				amqpMessage.set((Message) invocation.getArguments()[2]);
				return null;
			}
		}).when(amqpTemplate).send(anyString(), anyString(), any(Message.class),
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
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				amqpMessage.set((Message) invocation.getArguments()[2]);
				return null;
			}
		}).when(amqpTemplate)
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
