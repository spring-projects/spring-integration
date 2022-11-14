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

package org.springframework.integration.redis.config;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.redis.outbound.RedisQueueOutboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Liu
 * @author Gary Russell
 * since 4.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class RedisQueueOutboundGatewayParserTests {

	@Autowired
	@Qualifier("outboundGateway")
	private PollingConsumer consumer;

	@Autowired
	@Qualifier("outboundGateway.handler")
	private RedisQueueOutboundGateway defaultGateway;

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
		assertThat(TestUtils.getPropertyValue(this.defaultGateway, "extractPayload", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.defaultGateway, "serializer")).isSameAs(this.serializer);
		assertThat(TestUtils.getPropertyValue(this.defaultGateway, "serializerExplicitlySet", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.defaultGateway, "order")).isEqualTo(2);
		assertThat(TestUtils.getPropertyValue(this.defaultGateway, "outputChannel")).isSameAs(this.receiveChannel);
		assertThat(TestUtils.getPropertyValue(this.consumer, "inputChannel")).isSameAs(this.requestChannel);
		assertThat(TestUtils.getPropertyValue(this.defaultGateway, "requiresReply", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.defaultGateway, "receiveTimeout")).isEqualTo(2000);
		assertThat(TestUtils.getPropertyValue(this.consumer, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.consumer, "phase")).isEqualTo(3);
	}

}
