/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
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
		assertThat(TestUtils.getPropertyValue(this.defaultGateway, "extractPayload", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.defaultGateway, "serializer")).isSameAs(this.serializer);
		assertThat(TestUtils.getPropertyValue(this.defaultGateway, "serializerExplicitlySet", Boolean.class)).isTrue();
		assertThat(this.defaultGateway.getReplyChannel()).isSameAs(this.receiveChannel);
		assertThat(this.defaultGateway.getRequestChannel()).isSameAs(this.requestChannel);
		assertThat(TestUtils.getPropertyValue(this.defaultGateway, "messagingTemplate.receiveTimeout")).isEqualTo(2000L);
		assertThat(TestUtils.getPropertyValue(this.defaultGateway, "taskExecutor")).isNotNull();
		assertThat(TestUtils.getPropertyValue(this.defaultGateway, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.defaultGateway, "phase")).isEqualTo(3);
	}

	@Test
	public void testZeroReceiveTimeoutConfig() {
		assertThat(TestUtils.getPropertyValue(this.zeroReceiveTimeoutGateway, "receiveTimeout")).isEqualTo(0L);
	}

}
