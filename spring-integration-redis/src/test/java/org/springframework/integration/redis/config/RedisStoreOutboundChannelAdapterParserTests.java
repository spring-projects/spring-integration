/*
 * Copyright 2007-present the original author or authors.
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
 * @author Glenn Renfro
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

		assertThat(TestUtils.<Expression>getPropertyValue(withStringTemplate, "zsetIncrementScoreExpression")
				.getExpressionString()).isEqualTo("true");
	}

	@Test
	public void validateWithStringObjectTemplate() {
		RedisStoreWritingMessageHandler withStringObjectTemplate =
				TestUtils.<RedisStoreWritingMessageHandler>getPropertyValue(context.getBean("withStringObjectTemplate.adapter"), "handler");
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
				TestUtils.getPropertyValue(context.getBean("withExternalTemplate.adapter"), "handler");
		assertThat(((LiteralExpression) TestUtils.getPropertyValue(withExternalTemplate,
				"keyExpression")).getExpressionString()).isEqualTo("pepboys");
		assertThat((TestUtils.getPropertyValue(withExternalTemplate, "collectionType")).toString())
				.isEqualTo("PROPERTIES");
		assertThat(TestUtils.<Object>getPropertyValue(withExternalTemplate, "redisTemplate"))
				.isSameAs(redisTemplate);
	}

}
