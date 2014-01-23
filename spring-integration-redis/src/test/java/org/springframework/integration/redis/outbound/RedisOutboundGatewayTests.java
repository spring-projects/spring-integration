/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.redis.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.support.atomic.RedisAtomicInteger;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.lambdaworks.redis.protocol.CommandType;

/**
 * @author Artem Bilan
 * @since 4.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisOutboundGatewayTests extends RedisAvailableTests {

	@Autowired
	private PollableChannel replyChannel;

	@Autowired
	private MessageChannel pingChannel;

	@Autowired
	private MessageChannel leftPushChannel;

	@Autowired
	private MessageChannel rightPopChannel;

	@Autowired
	private MessageChannel incrementAtomicIntegerChannel;

	@Autowired
	private RedisAtomicInteger atomicInteger;

	@Autowired
	private MessageChannel getCommandChannel;

	@Autowired
	private MessageChannel mgetCommandChannel;

	@Test
	@RedisAvailable
	public void testDefaults() {
		this.pingChannel.send(MessageBuilder.withPayload("foo").setHeader(RedisHeaders.COMMAND, CommandType.PING).build());
		Message<?> receive = this.replyChannel.receive(1000);
		assertNotNull(receive);
		assertTrue(Arrays.equals("PONG".getBytes(), (byte[]) receive.getPayload()));
	}

	@Test
	@RedisAvailable
	public void testPushAndPopCommands() {
		final String queueName = "si.test.testRedisOutboundGateway";
		String payload = "testing";
		this.leftPushChannel.send(MessageBuilder.withPayload(new String[] {queueName, payload}).build());
		Message<?> receive = this.replyChannel.receive(1000);
		assertNotNull(receive);

		this.rightPopChannel.send(MessageBuilder.withPayload(queueName).build());
		receive = this.replyChannel.receive(1000);
		assertNotNull(receive);
		assertTrue(Arrays.equals(payload.getBytes(), (byte[]) receive.getPayload()));
	}

	@Test
	@RedisAvailable
	public void testIncrementAtomicCommand() {
		this.incrementAtomicIntegerChannel.send(MessageBuilder.withPayload(CommandType.INCR).build());
		System.out.println();
		Message<?> receive = this.replyChannel.receive(1000);
		assertNotNull(receive);
		assertEquals(11L, receive.getPayload());
		assertEquals(11, this.atomicInteger.get());
	}

	@Test
	@RedisAvailable
	public void testGetCommand() {
		this.getConnectionFactoryForTest().getConnection().set("foo".getBytes(), "bar".getBytes());
		this.getCommandChannel.send(MessageBuilder.withPayload("foo").build());
		Message<?> receive = this.replyChannel.receive(1000);
		assertNotNull(receive);
		assertTrue(Arrays.equals("bar".getBytes(), (byte[]) receive.getPayload()));
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testMGetCommand() {
		RedisConnection connection = this.getConnectionFactoryForTest().getConnection();
		byte[] value1 = "bar1".getBytes();
		byte[] value2 = "bar2".getBytes();
		connection.set("foo1".getBytes(), value1);
		connection.set("foo2".getBytes(), value2);
		this.mgetCommandChannel.send(MessageBuilder.withPayload(new String [] {"foo1", "foo2"}).build());
		Message<?> receive = this.replyChannel.receive(1000);
		assertNotNull(receive);
		assertThat((List<byte[]>) receive.getPayload(), Matchers.contains(value1, value2));
	}

}
