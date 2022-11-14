/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.integration.redis.outbound;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Artem Vozhdayenko
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
class RedisOutboundGatewayTests implements RedisContainerTest {

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

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;

	@Test
	void testPingPongCommand() {
		this.pingChannel.send(MessageBuilder.withPayload("foo").setHeader(RedisHeaders.COMMAND, "PING").build());
		Message<?> receive = this.replyChannel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(Arrays.equals("PONG".getBytes(), (byte[]) receive.getPayload())).isTrue();
	}

	@Test
	void testPushAndPopCommands() {
		final String queueName = "si.test.testRedisOutboundGateway";
		String payload = "testing";
		this.leftPushRightPopChannel.send(MessageBuilder.withPayload(payload)
				.setHeader(RedisHeaders.COMMAND, "LPUSH")
				.setHeader("queue", queueName)
				.build());
		Message<?> receive = this.replyChannel.receive(1000);
		assertThat(receive).isNotNull();

		this.leftPushRightPopChannel.send(MessageBuilder.withPayload(payload)
				.setHeader(RedisHeaders.COMMAND, "RPOP")
				.setHeader("queue", queueName)
				.build());
		receive = this.replyChannel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(Arrays.equals(payload.getBytes(), (byte[]) receive.getPayload())).isTrue();
	}

	@Test
	void testIncrementAtomicCommand() {
		// Since 'atomicInteger' is lazy-init to avoid early Redis connection,
		// we have to initialize it before send the INCR command.
		this.beanFactory.getBean("atomicInteger");
		this.incrementAtomicIntegerChannel.send(MessageBuilder.withPayload("INCR").build());
		Message<?> receive = this.replyChannel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(11L);

		this.getCommandChannel.send(MessageBuilder.withPayload("si.test.RedisAtomicInteger").build());
		receive = this.replyChannel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(new String((byte[]) receive.getPayload())).isEqualTo("11");
		RedisContainerTest.createStringRedisTemplate(redisConnectionFactory).delete("si.test.RedisAtomicInteger");
	}

	@Test
	void testGetCommand() {
		this.setDelCommandChannel.send(MessageBuilder.withPayload(new String[] {"foo", "bar"})
				.setHeader(RedisHeaders.COMMAND, "SET").build());
		Message<?> receive = this.replyChannel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("OK");

		this.getCommandChannel.send(MessageBuilder.withPayload("foo").build());
		receive = this.replyChannel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(Arrays.equals("bar".getBytes(), (byte[]) receive.getPayload())).isTrue();

		this.setDelCommandChannel.send(MessageBuilder.withPayload("foo").setHeader(RedisHeaders.COMMAND, "DEL").build());
		receive = this.replyChannel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(1L);

		try {
			this.getCommandChannel.send(MessageBuilder.withPayload("foo").build());
			fail("ReplyRequiredException expected");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(ReplyRequiredException.class);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	void testMGetCommand() {
		RedisConnection connection = redisConnectionFactory.getConnection();
		byte[] value1 = "bar1".getBytes();
		byte[] value2 = "bar2".getBytes();
		connection.stringCommands().set("foo1".getBytes(), value1);
		connection.stringCommands().set("foo2".getBytes(), value2);
		this.mgetCommandChannel.send(MessageBuilder.withPayload(new String[] {"foo1", "foo2"}).build());
		Message<?> receive = this.replyChannel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat((List<byte[]>) receive.getPayload()).containsExactly(value1, value2);
		connection.keyCommands().del("foo1".getBytes(), "foo2".getBytes());
	}

}
