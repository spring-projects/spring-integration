/*
 * Copyright 2017 the original author or authors.
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.AcknowledgmentCallback;
import org.springframework.integration.support.AcknowledgmentCallback.Status;
import org.springframework.messaging.Message;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;

/**
 * @author Gary Russell
 * @since 5.0.1
 *
 */
public class AmqpMessageSourceTests {

	@Test
	public void testAck() throws Exception {
		Channel channel = mock(Channel.class);
		willReturn(true).given(channel).isOpen();
		Envelope envelope = new Envelope(123L, false, "ex", "rk");
		BasicProperties props = new BasicProperties.Builder().build();
		GetResponse getResponse = new GetResponse(envelope, props, "bar".getBytes(), 0);
		willReturn(getResponse).given(channel).basicGet("foo", false);
		Connection connection = mock(Connection.class);
		willReturn(true).given(connection).isOpen();
		willReturn(channel).given(connection).createChannel();
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		willReturn(connection).given(connectionFactory).newConnection((ExecutorService) isNull(), anyString());

		CachingConnectionFactory ccf = new CachingConnectionFactory(connectionFactory);
		AmqpMessageSource source = new AmqpMessageSource(ccf, "foo");
		Message<?> received = source.receive();
		// make sure channel is not cached
		org.springframework.amqp.rabbit.connection.Connection conn = ccf.createConnection();
		Channel notCached = conn.createChannel(false); // should not have been "closed"
		verify(connection, times(2)).createChannel();
		received.getHeaders()
				.get(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, AcknowledgmentCallback.class)
				.acknlowlege(Status.ACCEPT);
		verify(channel).basicAck(123L, false);
		Channel cached = conn.createChannel(false); // should have been "closed"
		verify(connection, times(2)).createChannel();
		notCached.close();
		cached.close();
		ccf.destroy();
		verify(channel, times(2)).close();
		verify(connection).close(30000);
	}

	@Test
	public void testNAck() throws Exception {
		testNackOrRequeue(false);
	}

	@Test
	public void testRequeue() throws Exception {
		testNackOrRequeue(false);
	}

	private void testNackOrRequeue(boolean requeue) throws IOException, TimeoutException {
		Channel channel = mock(Channel.class);
		willReturn(true).given(channel).isOpen();
		Envelope envelope = new Envelope(123L, false, "ex", "rk");
		BasicProperties props = new BasicProperties.Builder().build();
		GetResponse getResponse = new GetResponse(envelope, props, "bar".getBytes(), 0);
		willReturn(getResponse).given(channel).basicGet("foo", false);
		Connection connection = mock(Connection.class);
		willReturn(true).given(connection).isOpen();
		willReturn(channel).given(connection).createChannel();
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		willReturn(connection).given(connectionFactory).newConnection((ExecutorService) isNull(), anyString());

		CachingConnectionFactory ccf = new CachingConnectionFactory(connectionFactory);
		AmqpMessageSource source = new AmqpMessageSource(ccf, "foo");
		Message<?> received = source.receive();
		verify(connection).createChannel();
		received.getHeaders()
				.get(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, AcknowledgmentCallback.class)
				.acknlowlege(requeue ? Status.REQUEUE : Status.REJECT);
		verify(channel).basicReject(123L, requeue);
		verify(connection).createChannel();
		ccf.destroy();
		verify(channel).close();
		verify(connection).close(30000);
	}

}
