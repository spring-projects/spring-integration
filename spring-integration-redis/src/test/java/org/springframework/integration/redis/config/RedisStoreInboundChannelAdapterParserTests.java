/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.redis.inbound.RedisStoreMessageSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Vozhdayenko
 * @since 2.2
 */
@SpringJUnitConfig
@DirtiesContext
class RedisStoreInboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private RedisTemplate<?, ?> redisTemplate;

	@Test
	void validateWithStringTemplate() {
		RedisStoreMessageSource withStringTemplate =
				TestUtils.getPropertyValue(context.getBean("withStringTemplate"), "source", RedisStoreMessageSource.class);
		assertThat(((SpelExpression) TestUtils.getPropertyValue(withStringTemplate, "keyExpression"))
				.getExpressionString()).isEqualTo("'presidents'");
		assertThat(TestUtils.getPropertyValue(withStringTemplate, "collectionType"))
				.hasToString("LIST");
		assertThat(TestUtils.getPropertyValue(withStringTemplate, "redisTemplate"))
				.isInstanceOf(StringRedisTemplate.class);
	}

	@Test
	void validateWithExternalTemplate() {
		RedisStoreMessageSource withExternalTemplate =
				TestUtils.getPropertyValue(context.getBean("withExternalTemplate"), "source", RedisStoreMessageSource.class);
		assertThat(((SpelExpression) TestUtils.getPropertyValue(withExternalTemplate, "keyExpression"))
				.getExpressionString()).isEqualTo("'presidents'");
		assertThat((TestUtils.getPropertyValue(withExternalTemplate, "collectionType")))
				.hasToString("LIST");
		assertThat(TestUtils.getPropertyValue(withExternalTemplate, "redisTemplate")).isSameAs(redisTemplate);
	}

	@Test
	void testTemplateAndCfMutualExclusivity() {
		assertThatThrownBy(() -> new ClassPathXmlApplicationContext("inbound-template-cf-fail.xml", this.getClass()))
				.isInstanceOf(BeanDefinitionParsingException.class);
	}

}
