/*
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
 * @author Glenn Renfro
 *
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
				TestUtils.<RedisStoreMessageSource>getPropertyValue(context.getBean("withStringTemplate"), "source");
		assertThat(((SpelExpression) TestUtils.getPropertyValue(withStringTemplate, "keyExpression"))
				.getExpressionString()).isEqualTo("'presidents'");
		assertThat(TestUtils.<Object>getPropertyValue(withStringTemplate, "collectionType"))
				.hasToString("LIST");
		assertThat(TestUtils.<Object>getPropertyValue(withStringTemplate, "redisTemplate"))
				.isInstanceOf(StringRedisTemplate.class);
	}

	@Test
	void validateWithExternalTemplate() {
		RedisStoreMessageSource withExternalTemplate =
				TestUtils.getPropertyValue(context.getBean("withExternalTemplate"), "source");
		assertThat(((SpelExpression) TestUtils.getPropertyValue(withExternalTemplate, "keyExpression"))
				.getExpressionString()).isEqualTo("'presidents'");
		assertThat((TestUtils.<Object>getPropertyValue(withExternalTemplate,
				"collectionType"))).hasToString("LIST");
		assertThat(TestUtils.<Object>getPropertyValue(withExternalTemplate, "redisTemplate"))
				.isSameAs(redisTemplate);
	}

	@Test
	void testTemplateAndCfMutualExclusivity() {
		assertThatThrownBy(() -> new ClassPathXmlApplicationContext("inbound-template-cf-fail.xml", this.getClass()))
				.isInstanceOf(BeanDefinitionParsingException.class);
	}

}
