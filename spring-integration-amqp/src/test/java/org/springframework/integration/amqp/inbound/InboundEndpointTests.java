/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.integration.amqp.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.amqp.AmqpHeaders;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.Transformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

import com.rabbitmq.client.Channel;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
public class InboundEndpointTests {

	@Test
	public void testInt2809JavaTypePropertiesToAmqp() throws Exception {
		Connection connection = mock(Connection.class);
		doAnswer(new Answer<Channel>() {
			@Override
			public Channel answer(InvocationOnMock invocation) throws Throwable {
				return mock(Channel.class);
			}
		}).when(connection).createChannel(anyBoolean());
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		when(connectionFactory.createConnection()).thenReturn(connection);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setAcknowledgeMode(AcknowledgeMode.MANUAL);

		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(container);
		adapter.setMessageConverter(new JsonMessageConverter());

		PollableChannel channel = new QueueChannel();

		adapter.setOutputChannel(channel);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();

		Object payload = new Foo("bar1");

		Transformer objectToJsonTransformer = new ObjectToJsonTransformer();
		Message<?> jsonMessage = objectToJsonTransformer.transform(new GenericMessage<Object>(payload));

		MessageProperties amqpMessageProperties = new MessageProperties();
		amqpMessageProperties.setDeliveryTag(123L);
		org.springframework.amqp.core.Message amqpMessage =
				new SimpleMessageConverter().toMessage(jsonMessage.getPayload(), amqpMessageProperties);
		new DefaultAmqpHeaderMapper().fromHeadersToRequest(jsonMessage.getHeaders(), amqpMessageProperties);

		ChannelAwareMessageListener listener = (ChannelAwareMessageListener) container.getMessageListener();
		Channel rabbitChannel = mock(Channel.class);
		listener.onMessage(amqpMessage, rabbitChannel);

		Message<?> result = channel.receive(1000);
		assertEquals(payload, result.getPayload());

		assertSame(rabbitChannel, result.getHeaders().get(AmqpHeaders.CHANNEL));
		assertEquals(123L, result.getHeaders().get(AmqpHeaders.DELIVERY_TAG));
	}

	@Test
	public void testInt2809JavaTypePropertiesFromAmqp() throws Exception {
		Connection connection = mock(Connection.class);
		doAnswer(new Answer<Channel>() {
			@Override
			public Channel answer(InvocationOnMock invocation) throws Throwable {
				return mock(Channel.class);
			}
		}).when(connection).createChannel(anyBoolean());
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		when(connectionFactory.createConnection()).thenReturn(connection);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);

		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(container);

		PollableChannel channel = new QueueChannel();

		adapter.setOutputChannel(channel);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();

		Object payload = new Foo("bar1");

		MessageProperties amqpMessageProperties = new MessageProperties();
		org.springframework.amqp.core.Message amqpMessage = new JsonMessageConverter().toMessage(payload, amqpMessageProperties);

		ChannelAwareMessageListener listener = (ChannelAwareMessageListener) container.getMessageListener();
		listener.onMessage(amqpMessage, null);

		Message<?> receive = channel.receive(1000);

		Message<?> result = new JsonToObjectTransformer().transform(receive);

		assertEquals(payload, result.getPayload());
	}

	@Test
	public void testMessageConverterJsonHeadersHavePrecedenceOverMessageHeaders() throws Exception {
		Connection connection = mock(Connection.class);
		doAnswer(new Answer<Channel>() {
			@Override
			public Channel answer(InvocationOnMock invocation) throws Throwable {
				return mock(Channel.class);
			}
		}).when(connection).createChannel(anyBoolean());
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		when(connectionFactory.createConnection()).thenReturn(connection);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setAcknowledgeMode(AcknowledgeMode.MANUAL);

		DirectChannel channel = new DirectChannel();

		final Channel rabbitChannel = mock(Channel.class);

		channel.subscribe(new MessageTransformingHandler(new Transformer() {

			@Override
			public Message<?> transform(Message<?> message) {
				assertSame(rabbitChannel, message.getHeaders().get(AmqpHeaders.CHANNEL));
				assertEquals(123L, message.getHeaders().get(AmqpHeaders.DELIVERY_TAG));
				return MessageBuilder.fromMessage(message)
						.setHeader(JsonHeaders.TYPE_ID, "foo")
						.setHeader(JsonHeaders.CONTENT_TYPE_ID, "bar")
						.setHeader(JsonHeaders.KEY_TYPE_ID, "baz")
						.build();
			}
		}));

		AmqpInboundGateway gateway = new AmqpInboundGateway(container);
		gateway.setMessageConverter(new JsonMessageConverter());

		gateway.setRequestChannel(channel);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();

		RabbitTemplate rabbitTemplate = Mockito.spy(TestUtils.getPropertyValue(gateway, "amqpTemplate", RabbitTemplate.class));

		Mockito.doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				org.springframework.amqp.core.Message message = (org.springframework.amqp.core.Message) invocation.getArguments()[2];
				Map<String,Object> headers = message.getMessageProperties().getHeaders();
				assertTrue(headers.containsKey(JsonHeaders.TYPE_ID.replaceFirst(JsonHeaders.PREFIX, "")));
				assertNotEquals("foo", headers.get(JsonHeaders.TYPE_ID.replaceFirst(JsonHeaders.PREFIX, "")));
				assertFalse(headers.containsKey(JsonHeaders.CONTENT_TYPE_ID.replaceFirst(JsonHeaders.PREFIX, "")));
				assertFalse(headers.containsKey(JsonHeaders.KEY_TYPE_ID.replaceFirst(JsonHeaders.PREFIX, "")));
				assertFalse(headers.containsKey(JsonHeaders.TYPE_ID));
				assertFalse(headers.containsKey(JsonHeaders.KEY_TYPE_ID));
				assertFalse(headers.containsKey(JsonHeaders.CONTENT_TYPE_ID));
				return null;
			}
		}

		).when(rabbitTemplate).send(Mockito.anyString(), Mockito.anyString(),
				Mockito.any(org.springframework.amqp.core.Message.class), Mockito.any(CorrelationData.class));

		DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(gateway);
		directFieldAccessor.setPropertyValue("amqpTemplate", rabbitTemplate);

		Object payload = new Foo("bar1");

		MessageProperties amqpMessageProperties = new MessageProperties();
		amqpMessageProperties.setReplyTo("test");
		amqpMessageProperties.setDeliveryTag(123L);
		org.springframework.amqp.core.Message amqpMessage = new JsonMessageConverter().toMessage(payload, amqpMessageProperties);

		ChannelAwareMessageListener listener = (ChannelAwareMessageListener) container.getMessageListener();
		listener.onMessage(amqpMessage, rabbitChannel);

	}



	public static class Foo {

		private String bar;

		public Foo() {
		}

		public Foo(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return bar;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			Foo foo = (Foo) o;

			if (bar != null ? !bar.equals(foo.bar) : foo.bar != null) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			return bar != null ? bar.hashCode() : 0;
		}

	}

}
