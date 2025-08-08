/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.redis.outbound.RedisQueueOutboundChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Rainer Frey
 *
 * @since 3.0
 */
@SpringJUnitConfig
@DirtiesContext
public class RedisQueueOutboundChannelAdapterParserTests {

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
	private RedisQueueOutboundChannelAdapter defaultAdapter;

	@Autowired
	@Qualifier("customAdapter.handler")
	private RedisQueueOutboundChannelAdapter customAdapter;

	@Autowired
	private RedisSerializer<?> serializer;

	@Test
	public void testInt3017DefaultConfig() throws Exception {
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "template.connectionFactory"))
				.isSameAs(this.connectionFactory);
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "queueNameExpression", Expression.class)
				.getExpressionString()).isEqualTo("foo");
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "extractPayload", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "serializerExplicitlySet", Boolean.class)).isFalse();

		Object handler = TestUtils.getPropertyValue(this.defaultEndpoint, "handler");

		assertThat(AopUtils.isAopProxy(handler)).isTrue();

		assertThat(this.defaultAdapter).isSameAs(((Advised) handler).getTargetSource().getTarget());

		assertThat(((Advised) handler).getAdvisors()[0].getAdvice()).isInstanceOf(RequestHandlerRetryAdvice.class);
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "leftPush", Boolean.class)).isTrue();
	}

	@Test
	public void testInt3017CustomConfig() {
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "template.connectionFactory"))
				.isSameAs(this.customRedisConnectionFactory);
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "queueNameExpression.expression"))
				.isEqualTo("headers['redis_queue']");
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "extractPayload", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "serializerExplicitlySet", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "serializer")).isSameAs(this.serializer);
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "leftPush", Boolean.class)).isFalse();
	}

}
