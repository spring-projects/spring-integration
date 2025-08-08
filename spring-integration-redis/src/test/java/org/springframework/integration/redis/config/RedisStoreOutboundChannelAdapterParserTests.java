/*
 * Copyright © 2007 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2007-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.redis.outbound.RedisStoreWritingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class RedisStoreOutboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private RedisTemplate<?, ?> redisTemplate;

	@Test
	public void validateWithStringTemplate() throws Exception {
		RedisStoreWritingMessageHandler withStringTemplate = context.getBean("withStringTemplate.handler",
				RedisStoreWritingMessageHandler.class);
		assertThat(((LiteralExpression) TestUtils.getPropertyValue(withStringTemplate,
				"keyExpression")).getExpressionString()).isEqualTo("pepboys");
		assertThat((TestUtils.getPropertyValue(withStringTemplate, "collectionType")).toString())
				.isEqualTo("PROPERTIES");
		assertThat(TestUtils.getPropertyValue(withStringTemplate, "redisTemplate") instanceof StringRedisTemplate)
				.isTrue();

		Object handler = TestUtils.getPropertyValue(context.getBean("withStringTemplate.adapter"), "handler");

		assertThat(AopUtils.isAopProxy(handler)).isTrue();

		assertThat(withStringTemplate).isSameAs(((Advised) handler).getTargetSource().getTarget());

		assertThat(((Advised) handler).getAdvisors()[0].getAdvice()).isInstanceOf(RequestHandlerRetryAdvice.class);

		assertThat(TestUtils.getPropertyValue(withStringTemplate, "zsetIncrementScoreExpression",
				Expression.class).getExpressionString()).isEqualTo("true");
	}

	@Test
	public void validateWithStringObjectTemplate() {
		RedisStoreWritingMessageHandler withStringObjectTemplate =
				TestUtils.getPropertyValue(context.getBean("withStringObjectTemplate.adapter"), "handler",
						RedisStoreWritingMessageHandler.class);
		assertThat(((LiteralExpression) TestUtils.getPropertyValue(withStringObjectTemplate,
				"keyExpression")).getExpressionString()).isEqualTo("pepboys");
		assertThat((TestUtils.getPropertyValue(withStringObjectTemplate, "collectionType")).toString())
				.isEqualTo("PROPERTIES");
		assertThat(TestUtils.getPropertyValue(withStringObjectTemplate, "redisTemplate") instanceof StringRedisTemplate)
				.isFalse();
		assertThat(TestUtils.getPropertyValue(withStringObjectTemplate,
				"redisTemplate.keySerializer") instanceof StringRedisSerializer).isTrue();
		assertThat(TestUtils.getPropertyValue(withStringObjectTemplate,
				"redisTemplate.hashKeySerializer") instanceof StringRedisSerializer).isTrue();
		assertThat(TestUtils.getPropertyValue(withStringObjectTemplate,
				"redisTemplate.valueSerializer") instanceof JdkSerializationRedisSerializer).isTrue();
		assertThat(TestUtils.getPropertyValue(withStringObjectTemplate,
				"redisTemplate.hashValueSerializer") instanceof JdkSerializationRedisSerializer).isTrue();
	}

	@Test
	public void validateWithExternalTemplate() {
		RedisStoreWritingMessageHandler withExternalTemplate =
				TestUtils.getPropertyValue(context.getBean("withExternalTemplate.adapter"), "handler",
						RedisStoreWritingMessageHandler.class);
		assertThat(((LiteralExpression) TestUtils.getPropertyValue(withExternalTemplate,
				"keyExpression")).getExpressionString()).isEqualTo("pepboys");
		assertThat((TestUtils.getPropertyValue(withExternalTemplate, "collectionType")).toString())
				.isEqualTo("PROPERTIES");
		assertThat(TestUtils.getPropertyValue(withExternalTemplate, "redisTemplate")).isSameAs(redisTemplate);
	}

}
