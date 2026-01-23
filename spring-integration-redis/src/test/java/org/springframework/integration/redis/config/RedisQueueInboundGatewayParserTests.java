/*
 * Copyright 2014-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.integration.redis.inbound.RedisQueueInboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Liu
 * @author Artem Bilan
 * @author Matthias Jeschke
 * @author Glenn Renfro
 *
 * since 4.1
 */
@SpringJUnitConfig
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
	public void testDefaultConfig() {
		assertThat(TestUtils.<Boolean>getPropertyValue(this.defaultGateway, "extractPayload")).isFalse();
		assertThat(TestUtils.<RedisSerializer<?>>getPropertyValue(this.defaultGateway, "serializer"))
				.isSameAs(this.serializer);
		assertThat(TestUtils.<Boolean>getPropertyValue(this.defaultGateway, "serializerExplicitlySet")).isTrue();
		assertThat(this.defaultGateway.getReplyChannel()).isSameAs(this.receiveChannel);
		assertThat(this.defaultGateway.getRequestChannel()).isSameAs(this.requestChannel);
		assertThat(TestUtils.<Long>getPropertyValue(this.defaultGateway, "messagingTemplate.receiveTimeout"))
				.isEqualTo(2000L);
		assertThat(TestUtils.<Object>getPropertyValue(this.defaultGateway, "taskExecutor")).isNotNull();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.defaultGateway, "autoStartup")).isFalse();
		assertThat(TestUtils.<Integer>getPropertyValue(this.defaultGateway, "phase")).isEqualTo(3);
	}

	@Test
	public void testZeroReceiveTimeoutConfig() {
		assertThat(TestUtils.<Long>getPropertyValue(this.zeroReceiveTimeoutGateway, "receiveTimeout"))
				.isEqualTo(0L);
	}

}
