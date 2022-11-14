/*
 * Copyright 2018-2022 the original author or authors.
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

package org.springframework.integration.amqp.inbound;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import org.junit.Test;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.batch.MessageBatch;
import org.springframework.amqp.rabbit.batch.SimpleBatchingStrategy;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback.Status;
import org.springframework.integration.amqp.support.AmqpMessageHeaderErrorMessageStrategy;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Gary Russell
 *
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
		source.setRawMessageHeader(true);
		Message<?> received = source.receive();
		assertThat(received.getHeaders().get(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE))
				.isInstanceOf(org.springframework.amqp.core.Message.class);
		assertThat(received.getHeaders().get(IntegrationMessageHeaderAccessor.SOURCE_DATA))
				.isSameAs(received.getHeaders().get(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE));
		assertThat(received.getHeaders().get(AmqpHeaders.CONSUMER_QUEUE)).isEqualTo("foo");
		// make sure channel is not cached
		org.springframework.amqp.rabbit.connection.Connection conn = ccf.createConnection();
		Channel notCached = conn.createChannel(false); // should not have been "closed"
		verify(connection, times(2)).createChannel();
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(Status.ACCEPT);
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

	private void testNackOrRequeue(boolean requeue) throws Exception {
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
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(requeue ? Status.REQUEUE : Status.REJECT);
		verify(channel).basicReject(123L, requeue);
		verify(connection).createChannel();
		ccf.destroy();
		verify(channel).close();
		verify(connection).close(30000);
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void testBatch() throws Exception {
		SimpleBatchingStrategy bs = new SimpleBatchingStrategy(2, 10_000, 10_000L);
		MessageProperties messageProperties = new MessageProperties();
		messageProperties.setContentType("text/plain");
		org.springframework.amqp.core.Message message =
				new org.springframework.amqp.core.Message("test1".getBytes(), messageProperties);
		bs.addToBatch("foo", "bar", message);
		message = new org.springframework.amqp.core.Message("test2".getBytes(), messageProperties);
		MessageBatch batched = bs.addToBatch("foo", "bar", message);

		Channel channel = mock(Channel.class);
		willReturn(true).given(channel).isOpen();
		Envelope envelope = new Envelope(123L, false, "ex", "rk");
		BasicProperties props = new BasicProperties.Builder()
				.headers(batched.getMessage().getMessageProperties().getHeaders())
				.contentType("text/plain")
				.build();
		GetResponse getResponse = new GetResponse(envelope, props, batched.getMessage().getBody(), 0);
		willReturn(getResponse).given(channel).basicGet("foo", false);
		Connection connection = mock(Connection.class);
		willReturn(true).given(connection).isOpen();
		willReturn(channel).given(connection).createChannel();
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		willReturn(connection).given(connectionFactory).newConnection((ExecutorService) isNull(), anyString());

		CachingConnectionFactory ccf = new CachingConnectionFactory(connectionFactory);
		AmqpMessageSource source = new AmqpMessageSource(ccf, "foo");
		Message<?> received = source.receive();
		assertThat(received).isNotNull();
		assertThat(((List<String>) received.getPayload())).contains("test1", "test2");
	}

}
