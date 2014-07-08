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

package org.springframework.integration.amqp.channel;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;
import org.apache.commons.logging.Log;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageDeliveryException;


/**
 * @author Gary Russell
 * @since 2.1
 *
 */
public class DispatcherHasNoSubscribersTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testPtP() throws Exception {
		final Channel channel = mock(Channel.class);
		DeclareOk declareOk = mock(DeclareOk.class);
		when(declareOk.getQueue()).thenReturn("noSubscribersChannel");
		when(channel.queueDeclare(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), any(Map.class)))
				.thenReturn(declareOk);
		Connection connection = mock(Connection.class);
		doAnswer(new Answer<Channel>() {
			@Override
			public Channel answer(InvocationOnMock invocation) throws Throwable {
				return channel;
			}}).when(connection).createChannel(anyBoolean());
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

		MessageListener listener = (MessageListener) container.getMessageListener();
		try {
			listener.onMessage(new Message("Hello world!".getBytes(), null));
			fail("Exception expected");
		}
		catch (MessageDeliveryException e) {
			assertEquals("Dispatcher has no subscribers for amqp-channel 'noSubscribersChannel'.", e.getMessage());
		}
	}

	@Test
	public void testPubSub() {
		final Channel channel = mock(Channel.class);
		Connection connection = mock(Connection.class);
		doAnswer(new Answer<Channel>() {
			@Override
			public Channel answer(InvocationOnMock invocation) throws Throwable {
				return channel;
			}}).when(connection).createChannel(anyBoolean());
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		when(connectionFactory.createConnection()).thenReturn(connection);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);
		final Queue queue = new Queue("noSubscribersQueue");
		PublishSubscribeAmqpChannel amqpChannel = new PublishSubscribeAmqpChannel("noSubscribersChannel",
				container, amqpTemplate) {
					@Override
					protected String obtainQueueName(AmqpAdmin admin,
							String channelName) {
						return queue.getName();
					}};
		amqpChannel.setBeanName("noSubscribersChannel");
		amqpChannel.setBeanFactory(mock(BeanFactory.class));
		amqpChannel.afterPropertiesSet();

		List<String> logList = insertMockLoggerInListener(amqpChannel);
		MessageListener listener = (MessageListener) container.getMessageListener();
		listener.onMessage(new Message("Hello world!".getBytes(), null));
		verifyLogReceived(logList);
	}

	private List<String> insertMockLoggerInListener(
			PublishSubscribeAmqpChannel channel) {
		SimpleMessageListenerContainer container = TestUtils.getPropertyValue(
				channel, "container", SimpleMessageListenerContainer.class);
		Log logger = mock(Log.class);
		final ArrayList<String> logList = new ArrayList<String>();
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation)
					throws Throwable {
				String message = (String) invocation.getArguments()[0];
				if (message.startsWith("Dispatcher has no subscribers")) {
					logList.add(message);
				}
				return null;
			}}).when(logger).warn(anyString(), any(Exception.class));
		when(logger.isWarnEnabled()).thenReturn(true);
		Object listener = container.getMessageListener();
		DirectFieldAccessor dfa = new DirectFieldAccessor(listener);
		dfa.setPropertyValue("logger", logger);
		return logList;
	}

	private void verifyLogReceived(final List<String> logList) {
		assertTrue("Failed to get expected exception", logList.size() > 0);
		boolean expectedExceptionFound = false;
		while (logList.size() > 0) {
			String message = logList.remove(0);
			assertNotNull("Failed to get expected exception", message);
			if (message.startsWith("Dispatcher has no subscribers")) {
				expectedExceptionFound = true;
				assertEquals("Dispatcher has no subscribers for amqp-channel 'noSubscribersChannel'.", message);
				break;
			}
		}
		assertTrue("Failed to get expected exception", expectedExceptionFound);
	}

}
