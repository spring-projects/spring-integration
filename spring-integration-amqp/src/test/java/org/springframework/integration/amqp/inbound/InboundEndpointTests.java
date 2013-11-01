/*
 * Copyright 2013 the original author or authors.
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
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.integration.Message;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.transformer.Transformer;

import com.rabbitmq.client.Channel;

/**
 * @author Artem Bilan
 * @since 3.0
 */
public class InboundEndpointTests {

	@Test
	public void testInt2809JavaTypePropertiesToAmqp() {
		Connection connection = mock(Connection.class);
		doAnswer(new Answer<Channel>() {
			public Channel answer(InvocationOnMock invocation) throws Throwable {
				return mock(Channel.class);
			}
		}).when(connection).createChannel(anyBoolean());
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		when(connectionFactory.createConnection()).thenReturn(connection);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);

		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(container);
		adapter.setMessageConverter(new JsonMessageConverter());

		PollableChannel channel = new QueueChannel();

		adapter.setOutputChannel(channel);
		adapter.afterPropertiesSet();

		Object payload = new Foo("bar1");

		Transformer objectToJsonTransformer = new ObjectToJsonTransformer();
		Message<?> jsonMessage = objectToJsonTransformer.transform(new GenericMessage<Object>(payload));

		MessageProperties amqpMessageProperties = new MessageProperties();
		org.springframework.amqp.core.Message amqpMessage =
				new SimpleMessageConverter().toMessage(jsonMessage.getPayload(), amqpMessageProperties);
		new DefaultAmqpHeaderMapper().fromHeadersToRequest(jsonMessage.getHeaders(), amqpMessageProperties);

		MessageListener listener = (MessageListener) container.getMessageListener();
		listener.onMessage(amqpMessage);

		Message<?> result = channel.receive(1000);
		assertEquals(payload, result.getPayload());
	}

	@Test
	public void testInt2809JavaTypePropertiesFromAmqp() {
		Connection connection = mock(Connection.class);
		doAnswer(new Answer<Channel>() {
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
		adapter.afterPropertiesSet();

		Object payload = new Foo("bar1");

		MessageProperties amqpMessageProperties = new MessageProperties();
		org.springframework.amqp.core.Message amqpMessage = new JsonMessageConverter().toMessage(payload, amqpMessageProperties);

		MessageListener listener = (MessageListener) container.getMessageListener();
		listener.onMessage(amqpMessage);

		Message<?> receive = channel.receive(1000);

		Message<?> result = new JsonToObjectTransformer().transform(receive);

		assertEquals(payload, result.getPayload());
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
