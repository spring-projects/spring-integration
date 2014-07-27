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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.redis.inbound.RedisQueueMessageDrivenEndpoint;
import org.springframework.integration.redis.outbound.RedisQueueGateway;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import reactor.util.Assert;

/**
 * @author David Liu
 * @since 4.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisQueueIntegrationTests {

	@Autowired
	@Qualifier("redisConnectionFactory")
	private RedisConnectionFactory connectionFactory;

	@Autowired
	@Qualifier("customRedisConnectionFactory")
	private RedisConnectionFactory customRedisConnectionFactory;

	@Autowired
	@Qualifier("defaultAdapter")
	private EventDrivenConsumer defaultEndpoint;

	@Autowired
	@Qualifier("defaultAdapter.handler")
	private RedisQueueGateway defaultAdapter;

	@Autowired
	private RedisSerializer<?> serializer;
	
	@Autowired
	@Qualifier("sendChannel")
	private DirectChannel sendChannel;
	
	@Autowired
	@Qualifier("outputChannel")
	private DirectChannel outputChannel;

	@Autowired
	@Qualifier("defaultInAdapter.adapter")
	private RedisQueueMessageDrivenEndpoint defaultInAdapter;
	
	@Test
	public void testInt1DefaultConfig() throws Exception {
		defaultInAdapter.start();
		sendChannel.send(new GenericMessage<String>("test1"));
//		Assert.notNull(outputChannel);
	}


}
