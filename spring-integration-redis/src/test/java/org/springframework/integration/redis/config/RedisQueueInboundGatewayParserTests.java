/*
 * Copyright 2014-2017 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.integration.redis.inbound.RedisQueueInboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author David Liu
 * @author Artem Bilan
 * @author Matthias Jeschke
 *
 * since 4.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class RedisQueueInboundGatewayParserTests {

	@Autowired
	@Qualifier("inboundGateway")
	private RedisQueueInboundGateway defaultGateway;

	@Autowired
	@Qualifier("zeroReceiveTimeoutGateway")
	private RedisQueueInboundGateway zeroReceiveTimeoutGateway;

	@Autowired
	@Qualifier("receiveChannel")
	private MessageChannel receiveChannel;

	@Autowired
	@Qualifier("requestChannel")
	private MessageChannel requestChannel;

	@Autowired
	private RedisSerializer<?> serializer;


	@Test
	public void testDefaultConfig() throws Exception {
		assertFalse(TestUtils.getPropertyValue(this.defaultGateway, "extractPayload", Boolean.class));
		assertSame(this.serializer, TestUtils.getPropertyValue(this.defaultGateway, "serializer"));
		assertTrue(TestUtils.getPropertyValue(this.defaultGateway, "serializerExplicitlySet", Boolean.class));
		assertSame(this.receiveChannel, this.defaultGateway.getReplyChannel());
		assertSame(this.requestChannel, this.defaultGateway.getRequestChannel());
		assertEquals(2000L, TestUtils.getPropertyValue(this.defaultGateway, "replyTimeout"));
		assertNotNull(TestUtils.getPropertyValue(this.defaultGateway, "taskExecutor"));
		assertFalse(TestUtils.getPropertyValue(this.defaultGateway, "autoStartup", Boolean.class));
		assertEquals(3, TestUtils.getPropertyValue(this.defaultGateway, "phase"));
	}

	@Test
	public void testZeroReceiveTimeoutConfig() throws Exception {
		assertEquals(0L, TestUtils.getPropertyValue(this.zeroReceiveTimeoutGateway, "receiveTimeout"));
	}

}
