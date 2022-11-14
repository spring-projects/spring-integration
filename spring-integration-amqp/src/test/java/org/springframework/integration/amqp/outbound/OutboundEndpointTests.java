/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.amqp.outbound;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.RabbitMessageFuture;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 */
public class OutboundEndpointTests {

	@Test
	public void testDelayExpression() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RabbitTemplate amqpTemplate = spy(new RabbitTemplate(connectionFactory));
		AmqpOutboundEndpoint endpoint = new AmqpOutboundEndpoint(amqpTemplate);
		willDoNothing()
				.given(amqpTemplate).send(anyString(), anyString(), any(Message.class), isNull());
		willAnswer(invocation -> invocation.getArgument(2))
				.given(amqpTemplate)
				.sendAndReceive(anyString(), anyString(), any(Message.class), isNull());
		endpoint.setExchangeName("foo");
		endpoint.setRoutingKey("bar");
		endpoint.setDelayExpressionString("42");
		endpoint.setBeanFactory(mock(BeanFactory.class));
		endpoint.afterPropertiesSet();
		endpoint.handleMessage(new GenericMessage<>("foo"));
		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(amqpTemplate).send(eq("foo"), eq("bar"), captor.capture(), isNull());
		assertThat(captor.getValue().getMessageProperties().getDelay()).isEqualTo(42);
		endpoint.setExpectReply(true);
		endpoint.setOutputChannel(new NullChannel());
		endpoint.handleMessage(new GenericMessage<>("foo"));
		verify(amqpTemplate).sendAndReceive(eq("foo"), eq("bar"), captor.capture(), isNull());
		assertThat(captor.getValue().getMessageProperties().getDelay()).isEqualTo(42);

		endpoint.setDelay(23);
		endpoint.setRoutingKey("baz");
		endpoint.afterPropertiesSet();
		endpoint.handleMessage(new GenericMessage<>("foo"));
		verify(amqpTemplate).sendAndReceive(eq("foo"), eq("baz"), captor.capture(), isNull());
		assertThat(captor.getValue().getMessageProperties().getDelay()).isEqualTo(23);
	}

	@Test
	public void testAsyncDelayExpression() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		AsyncRabbitTemplate amqpTemplate = spy(new AsyncRabbitTemplate(new RabbitTemplate(connectionFactory),
				new SimpleMessageListenerContainer(connectionFactory), "replyTo"));
		amqpTemplate.setTaskScheduler(mock(TaskScheduler.class));
		AsyncAmqpOutboundGateway gateway = new AsyncAmqpOutboundGateway(amqpTemplate);
		willReturn(mock(RabbitMessageFuture.class))
				.given(amqpTemplate)
				.sendAndReceive(anyString(), anyString(), any(Message.class));
		gateway.setExchangeName("foo");
		gateway.setRoutingKey("bar");
		gateway.setDelayExpressionString("42");
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setOutputChannel(new NullChannel());
		gateway.afterPropertiesSet();
		gateway.start();
		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		gateway.handleMessage(new GenericMessage<>("foo"));
		verify(amqpTemplate).sendAndReceive(eq("foo"), eq("bar"), captor.capture());
		assertThat(captor.getValue().getMessageProperties().getDelay()).isEqualTo(42);
	}

	@Test
	public void testHeaderMapperWinsAdapter() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RabbitTemplate amqpTemplate = spy(new RabbitTemplate(connectionFactory));
		AmqpOutboundEndpoint endpoint = new AmqpOutboundEndpoint(amqpTemplate);
		endpoint.setHeadersMappedLast(true);
		final AtomicReference<Message> amqpMessage = new AtomicReference<>();
		willAnswer(invocation -> {
			amqpMessage.set(invocation.getArgument(2));
			return null;
		}).given(amqpTemplate).send(isNull(), isNull(), any(Message.class), isNull());
		org.springframework.messaging.Message<?> message = MessageBuilder.withPayload("foo")
				.setHeader(MessageHeaders.CONTENT_TYPE, "bar")
				.build();
		endpoint.handleMessage(message);
		assertThat(amqpMessage.get()).isNotNull();
		assertThat(amqpMessage.get().getMessageProperties().getContentType()).isEqualTo("bar");
	}

	@Test
	public void testHeaderMapperWinsGateway() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		TestRabbitTemplate amqpTemplate = spy(new TestRabbitTemplate(connectionFactory));
		amqpTemplate.setUseTemporaryReplyQueues(true);
		AmqpOutboundEndpoint endpoint = new AmqpOutboundEndpoint(amqpTemplate);
		endpoint.setHeadersMappedLast(true);
		endpoint.setExpectReply(true);
		DefaultAmqpHeaderMapper mapper = DefaultAmqpHeaderMapper.inboundMapper();
		mapper.setRequestHeaderNames("*");
		endpoint.setHeaderMapper(mapper);
		final AtomicReference<Message> amqpMessage = new AtomicReference<>();
		willAnswer(invocation -> {
			amqpMessage.set(invocation.getArgument(2));
			return null;
		}).given(amqpTemplate)
				.doSendAndReceiveWithTemporary(isNull(), isNull(), any(Message.class), isNull());
		org.springframework.messaging.Message<?> message = MessageBuilder.withPayload("foo")
				.setHeader(MessageHeaders.CONTENT_TYPE, "bar")
				.setReplyChannel(new QueueChannel())
				.build();
		endpoint.handleMessage(message);
		assertThat(amqpMessage.get()).isNotNull();
		assertThat(amqpMessage.get().getMessageProperties().getContentType()).isEqualTo("bar");
		assertThat(amqpMessage.get().getMessageProperties().getHeaders().get(MessageHeaders.REPLY_CHANNEL)).isNull();
	}

	@Test
	public void testReplyHeadersWin() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		TestRabbitTemplate amqpTemplate = spy(new TestRabbitTemplate(connectionFactory));
		amqpTemplate.setUseTemporaryReplyQueues(true);
		AmqpOutboundEndpoint endpoint = new AmqpOutboundEndpoint(amqpTemplate);
		endpoint.setExpectReply(true);
		willAnswer(invocation ->
				org.springframework.amqp.core.MessageBuilder.withBody(new byte[0])
						.setHeader(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME, String.class.getName())
						.build()
		).given(amqpTemplate)
				.doSendAndReceiveWithTemporary(isNull(), isNull(), any(Message.class), isNull());
		QueueChannel replyChannel = new QueueChannel();
		org.springframework.messaging.Message<?> message = MessageBuilder.withPayload("foo")
				.setHeader(JsonHeaders.RESOLVABLE_TYPE, ResolvableType.forClass(Date.class))
				.setReplyChannel(replyChannel)
				.build();
		endpoint.handleMessage(message);
		org.springframework.messaging.Message<?> receive = replyChannel.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders())
				.containsEntry(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME, String.class.getName())
				.containsEntry(JsonHeaders.TYPE_ID, String.class.getName())
				.containsEntry(JsonHeaders.RESOLVABLE_TYPE, ResolvableType.forClass(String.class));
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
