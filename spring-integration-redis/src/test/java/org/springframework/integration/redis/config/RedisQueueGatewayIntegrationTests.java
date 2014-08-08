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

package org.springframework.integration.redis.config;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.redis.inbound.RedisQueueInboundGateway;
import org.springframework.integration.redis.outbound.RedisQueueOutboundGateway;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author David Liu
 * @since 4.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisQueueGatewayIntegrationTests {

	@Autowired
	@Qualifier("redisConnectionFactory")
	private JedisConnectionFactory connectionFactory;

	@Autowired
	@Qualifier("defaultOutboundGateway")
	private EventDrivenConsumer defaultEndpoint;

	@Autowired
	@Qualifier("defaultOutboundGateway.handler")
	private RedisQueueOutboundGateway defaultOutboundGateway;

	@Autowired
	private RedisSerializer<?> serializer;

	@Autowired
	@Qualifier("sendChannel")
	private DirectChannel sendChannel;

	@Autowired
	@Qualifier("outputChannel")
	private QueueChannel outputChannel;

	@Autowired
	@Qualifier("inboundOutputChannel")
	private QueueChannel inboundOutputChannel;

	@Autowired
	private RedisQueueInboundGateway defaultInboundGateway;

//	@Test
	public void testRequestWithReply() throws Exception {
		defaultInboundGateway.start();
		sendChannel.send(new GenericMessage<String>("test1"));
		Assert.assertEquals("test1".toUpperCase(), outputChannel.receive().getPayload());
		defaultInboundGateway.stop();
	}

//	@Test
	public void testInboundGatewayStop() throws Exception {
		defaultInboundGateway.stop();
		try {
			sendChannel.send(new GenericMessage<String>("test1"));
		}
		catch(Exception e) {
			assertTrue(e.getMessage().contains("No reply produced"));
		}
	}

	@Test
	public void testRedisServerStop() throws Exception {
		connectionFactory.destroy();
		defaultInboundGateway.start();

		try {
			sendChannel.send(new GenericMessage<String>("test1"));
		}
		catch(Exception e) {
		}
		Assert.assertEquals("test1".toUpperCase(), outputChannel.receive().getPayload());
		defaultInboundGateway.stop();
	}

}
