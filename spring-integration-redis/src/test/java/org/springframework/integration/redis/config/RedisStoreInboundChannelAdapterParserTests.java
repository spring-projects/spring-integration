/*
 * Copyright 2002-present the original author or authors.
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
