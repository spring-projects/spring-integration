/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.amqp.channel;

import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.beans.factory.BeanFactory;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 *
 */
public class DispatcherHasNoSubscribersTests {

	@Test
	public void testPtP() throws Exception {
		final Channel channel = mock();
		DeclareOk declareOk = mock();
		when(declareOk.getQueue()).thenReturn("noSubscribersChannel");
		when(channel.queueDeclare(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), isNull()))
				.thenReturn(declareOk);
		Connection connection = mock(Connection.class);
		doAnswer(invocation -> channel).when(connection).createChannel(anyBoolean());
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		when(connectionFactory.createConnection()).thenReturn(connection);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);

		PointToPointSubscribableAmqpChannel amqpChannel =
				new PointToPointSubscribableAmqpChannel("noSubscribersChannel", container, amqpTemplate);
		amqpChannel.setBeanName("noSubscribersChannel");
		amqpChannel.setBeanFactory(mock(BeanFactory.class));
		amqpChannel.afterPropertiesSet();

		MessageListener listener = container.getMessageListener();

		assertThatExceptionOfType(ListenerExecutionFailedException.class)
				.isThrownBy(() -> listener.onMessage(new Message("Hello world!".getBytes())))
				.withMessageContaining("Dispatcher has no subscribers for amqp-channel 'noSubscribersChannel'.");
	}

	@Test
	public void testPubSub() {
		final Channel channel = mock();
		Connection connection = mock();
		doAnswer(invocation -> channel).when(connection).createChannel(anyBoolean());
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		when(connectionFactory.createConnection()).thenReturn(connection);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);
		PublishSubscribeAmqpChannel amqpChannel =
				new PublishSubscribeAmqpChannel("noSubscribersChannel", container, amqpTemplate);
		amqpChannel.setBeanName("noSubscribersChannel");
		amqpChannel.setBeanFactory(mock(BeanFactory.class));
		amqpChannel.afterPropertiesSet();

		MessageListener listener = container.getMessageListener();
		assertThatExceptionOfType(ListenerExecutionFailedException.class)
				.isThrownBy(() -> listener.onMessage(new Message("Hello world!".getBytes())))
				.withMessageContaining("Dispatcher has no subscribers for amqp-channel 'noSubscribersChannel'.");
	}

}
