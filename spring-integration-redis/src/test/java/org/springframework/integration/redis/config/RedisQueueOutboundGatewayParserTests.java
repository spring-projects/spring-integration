/*
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.redis.outbound.RedisQueueOutboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Liu
 * @author Gary Russell
 * @author Glenn Renfro
 *
 * @since 4.1
 */
@SpringJUnitConfig
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
		assertThat(TestUtils.<Boolean>getPropertyValue(this.defaultGateway, "extractPayload")).isFalse();
		assertThat(TestUtils.<RedisSerializer<?>>getPropertyValue(this.defaultGateway, "serializer"))
				.isSameAs(this.serializer);
		assertThat(TestUtils.<Boolean>getPropertyValue(this.defaultGateway, "serializerExplicitlySet")).isTrue();
		assertThat(TestUtils.<Integer>getPropertyValue(this.defaultGateway, "order")).isEqualTo(2);
		assertThat(TestUtils.<MessageChannel>getPropertyValue(this.defaultGateway, "outputChannel"))
				.isSameAs(this.receiveChannel);
		assertThat(TestUtils.<MessageChannel>getPropertyValue(this.consumer, "inputChannel"))
				.isSameAs(this.requestChannel);
		assertThat(TestUtils.<Boolean>getPropertyValue(this.defaultGateway, "requiresReply")).isFalse();
		assertThat(TestUtils.<Duration>getPropertyValue(this.defaultGateway, "receiveTimeout").toMillis())
				.isEqualTo(2000L);
		assertThat(TestUtils.<Boolean>getPropertyValue(this.consumer, "autoStartup")).isFalse();
		assertThat(TestUtils.<Integer>getPropertyValue(this.consumer, "phase")).isEqualTo(3);
	}

}
