/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.inbound.RedisQueueInboundGateway;
import org.springframework.integration.redis.outbound.RedisQueueOutboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Liu
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 4.1
 */
@SpringJUnitConfig
@DirtiesContext
class RedisQueueGatewayIntegrationTests implements RedisContainerTest {

	@Value("#{redisQueue.toString().bytes}")
	private byte[] queueName;

	@Autowired
	@Qualifier("sendChannel")
	private DirectChannel sendChannel;

	@Autowired
	@Qualifier("outputChannel")
	private QueueChannel outputChannel;

	@Autowired
	private RedisQueueInboundGateway inboundGateway;

	@Autowired
	private RedisQueueOutboundGateway outboundGateway;

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;

	@BeforeEach
	void setup() {
		redisConnectionFactory.getConnection().keyCommands().del(this.queueName);
		this.inboundGateway.start();
	}

	@AfterEach
	void tearDown() {
		this.inboundGateway.stop();
	}

	@Test
	void testRequestWithReply() {
		this.sendChannel.send(new GenericMessage<>(1));
		Message<?> receive = this.outputChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(2);
	}

	@Test
	void testInboundGatewayStop() {
		Integer receiveTimeout = TestUtils.getPropertyValue(this.outboundGateway, "receiveTimeout", Integer.class);
		this.outboundGateway.setReceiveTimeout(1);
		this.inboundGateway.stop();
		try {
			this.sendChannel.send(new GenericMessage<>("test1"));
		}
		catch (Exception e) {
			assertThat(e.getMessage()).contains("No reply produced");
		}
		finally {
			this.outboundGateway.setReceiveTimeout(receiveTimeout);
		}
	}

	@Test
	void testNullSerializer() {
		Integer receiveTimeout = TestUtils.getPropertyValue(this.outboundGateway, "receiveTimeout", Integer.class);
		this.outboundGateway.setReceiveTimeout(1);
		this.inboundGateway.setSerializer(null);
		try {
			this.sendChannel.send(new GenericMessage<>("test1"));
		}
		catch (Exception e) {
			assertThat(e.getMessage()).contains("No reply produced");
		}
		finally {
			this.inboundGateway.setSerializer(new StringRedisSerializer());
			this.outboundGateway.setReceiveTimeout(receiveTimeout);
		}
	}

	@Test
	void testRequestReplyWithMessage() {
		this.inboundGateway.setSerializer(new JdkSerializationRedisSerializer());
		this.inboundGateway.setExtractPayload(false);
		this.outboundGateway.setSerializer(new JdkSerializationRedisSerializer());
		this.outboundGateway.setExtractPayload(false);
		this.sendChannel.send(new GenericMessage<>(2));
		Message<?> receive = this.outputChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(3);
		this.inboundGateway.setSerializer(new StringRedisSerializer());
		this.inboundGateway.setExtractPayload(true);
		this.outboundGateway.setSerializer(new StringRedisSerializer());
		this.outboundGateway.setExtractPayload(true);
	}

}
