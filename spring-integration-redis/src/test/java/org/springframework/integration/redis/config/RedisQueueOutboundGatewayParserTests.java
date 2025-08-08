/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
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
