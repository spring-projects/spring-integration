/*
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
 * @author Glenn Renfro
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
		assertThat(TestUtils.<RedisConnectionFactory>getPropertyValue(this.defaultAdapter,
				"template.connectionFactory")).isSameAs(this.connectionFactory);
		assertThat(TestUtils.<Expression>getPropertyValue(this.defaultAdapter, "queueNameExpression")
				.getExpressionString()).isEqualTo("foo");
		assertThat(TestUtils.<Boolean>getPropertyValue(this.defaultAdapter, "extractPayload")).isTrue();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.defaultAdapter, "serializerExplicitlySet")).isFalse();

		Object handler = TestUtils.getPropertyValue(this.defaultEndpoint, "handler");

		assertThat(AopUtils.isAopProxy(handler)).isTrue();

		assertThat(this.defaultAdapter).isSameAs(((Advised) handler).getTargetSource().getTarget());

		assertThat(((Advised) handler).getAdvisors()[0].getAdvice()).isInstanceOf(RequestHandlerRetryAdvice.class);
		assertThat(TestUtils.<Boolean>getPropertyValue(this.defaultAdapter, "leftPush")).isTrue();
	}

	@Test
	public void testInt3017CustomConfig() {
		assertThat(TestUtils.<RedisConnectionFactory>getPropertyValue(this.customAdapter,
				"template.connectionFactory")).isSameAs(this.customRedisConnectionFactory);
		assertThat(TestUtils.<String>getPropertyValue(this.customAdapter, "queueNameExpression.expression"))
				.isEqualTo("headers['redis_queue']");
		assertThat(TestUtils.<Boolean>getPropertyValue(this.customAdapter, "extractPayload")).isFalse();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.customAdapter, "serializerExplicitlySet")).isTrue();
		assertThat(TestUtils.<RedisSerializer<?>>getPropertyValue(this.customAdapter, "serializer"))
				.isSameAs(this.serializer);
		assertThat(TestUtils.<Boolean>getPropertyValue(this.customAdapter, "leftPush")).isFalse();
	}

}
