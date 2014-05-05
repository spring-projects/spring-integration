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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @since 4.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisOutboundGatewayTests extends RedisAvailableTests {

	@Autowired
	private BeanFactory beanFactory;

	@Autowired
	private PollableChannel replyChannel;

	@Autowired
	private MessageChannel pingChannel;

	@Autowired
	private MessageChannel leftPushRightPopChannel;

	@Autowired
	private MessageChannel incrementAtomicIntegerChannel;

	@Autowired
	private MessageChannel setDelCommandChannel;

	@Autowired
	private MessageChannel getCommandChannel;

	@Autowired
	private MessageChannel mgetCommandChannel;

	@Test
	@RedisAvailable
	public void testPingPongCommand() {
		this.pingChannel.send(MessageBuilder.withPayload("foo").setHeader(RedisHeaders.COMMAND, "PING").build());
		Message<?> receive = this.replyChannel.receive(1000);
		assertNotNull(receive);
		assertTrue(Arrays.equals("PONG".getBytes(), (byte[]) receive.getPayload()));
	}

	@Test
	@RedisAvailable
	public void testPushAndPopCommands() {
		final String queueName = "si.test.testRedisOutboundGateway";
		String payload = "testing";
		this.leftPushRightPopChannel.send(MessageBuilder.withPayload(payload)
				.setHeader(RedisHeaders.COMMAND, "LPUSH")
				.setHeader("queue", queueName)
				.build());
		Message<?> receive = this.replyChannel.receive(1000);
		assertNotNull(receive);

		this.leftPushRightPopChannel.send(MessageBuilder.withPayload(payload)
				.setHeader(RedisHeaders.COMMAND, "RPOP")
				.setHeader("queue", queueName)
				.build());
		receive = this.replyChannel.receive(1000);
		assertNotNull(receive);
		assertTrue(Arrays.equals(payload.getBytes(), (byte[]) receive.getPayload()));
	}

	@Test
	@RedisAvailable
	public void testIncrementAtomicCommand() {
		// Since 'atomicInteger' is lazy-init to avoid early Redis connection,
		// we have to initialize it before send the INCR command.
		this.beanFactory.getBean("atomicInteger");
		this.incrementAtomicIntegerChannel.send(MessageBuilder.withPayload("INCR").build());
		Message<?> receive = this.replyChannel.receive(1000);
		assertNotNull(receive);
		assertEquals(11L, receive.getPayload());

		this.getCommandChannel.send(MessageBuilder.withPayload("si.test.RedisAtomicInteger").build());
		receive = this.replyChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("11", new String((byte[]) receive.getPayload()));
		this.createStringRedisTemplate(this.getConnectionFactoryForTest()).delete("si.test.RedisAtomicInteger");
	}

	@Test
	@RedisAvailable
	public void testGetCommand() {
		this.setDelCommandChannel.send(MessageBuilder.withPayload(new String[]{"foo", "bar"})
				.setHeader(RedisHeaders.COMMAND, "SET").build());
		Message<?> receive = this.replyChannel.receive(1000);
		assertNotNull(receive);
		assertTrue(Arrays.equals("OK".getBytes(), (byte[]) receive.getPayload()));

		this.getCommandChannel.send(MessageBuilder.withPayload("foo").build());
		receive = this.replyChannel.receive(1000);
		assertNotNull(receive);
		assertTrue(Arrays.equals("bar".getBytes(), (byte[]) receive.getPayload()));

		this.setDelCommandChannel.send(MessageBuilder.withPayload("foo").setHeader(RedisHeaders.COMMAND, "DEL").build());
		receive = this.replyChannel.receive(1000);
		assertNotNull(receive);
		assertEquals(1L, receive.getPayload());

		try {
			this.getCommandChannel.send(MessageBuilder.withPayload("foo").build());
			fail("ReplyRequiredException expected");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(ReplyRequiredException.class));
		}
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
		connection.del("foo1".getBytes(), "foo2".getBytes());
	}

}
