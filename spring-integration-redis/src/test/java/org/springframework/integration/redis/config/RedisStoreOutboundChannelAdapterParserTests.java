/*
 * Copyright 2007-2014 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.redis.config;

import static org.junit.Assert.*;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.redis.outbound.RedisStoreWritingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
/**
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisStoreOutboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private RedisTemplate<?,?> redisTemplate;

	@Test
	public void validateWithStringTemplate() throws Exception {
		RedisStoreWritingMessageHandler withStringTemplate = context.getBean("withStringTemplate.handler",
						RedisStoreWritingMessageHandler.class);
		assertEquals("pepboys", ((LiteralExpression)TestUtils.getPropertyValue(withStringTemplate,
				"keyExpression")).getExpressionString());
		assertEquals("PROPERTIES", (TestUtils.getPropertyValue(withStringTemplate, "collectionType")).toString());
		assertTrue(TestUtils.getPropertyValue(withStringTemplate, "redisTemplate") instanceof StringRedisTemplate);

		Object handler = TestUtils.getPropertyValue(context.getBean("withStringTemplate.adapter"), "handler");

		assertTrue(AopUtils.isAopProxy(handler));

		assertSame(((Advised) handler).getTargetSource().getTarget(), withStringTemplate);

		assertThat(TestUtils.getPropertyValue(handler, "h.advised.advisors.first.item.advice"),
				Matchers.instanceOf(RequestHandlerRetryAdvice.class));
	}

	@Test
	public void validateWithStringObjectTemplate(){
		RedisStoreWritingMessageHandler withStringObjectTemplate =
				TestUtils.getPropertyValue(context.getBean("withStringObjectTemplate.adapter"), "handler",
						RedisStoreWritingMessageHandler.class);
		assertEquals("pepboys", ((LiteralExpression)TestUtils.getPropertyValue(withStringObjectTemplate,
				"keyExpression")).getExpressionString());
		assertEquals("PROPERTIES", (TestUtils.getPropertyValue(withStringObjectTemplate, "collectionType")).toString());
		assertFalse(TestUtils.getPropertyValue(withStringObjectTemplate, "redisTemplate") instanceof StringRedisTemplate);
		assertTrue(TestUtils.getPropertyValue(withStringObjectTemplate,
				"redisTemplate.keySerializer") instanceof StringRedisSerializer);
		assertTrue(TestUtils.getPropertyValue(withStringObjectTemplate,
				"redisTemplate.hashKeySerializer") instanceof StringRedisSerializer);
		assertTrue(TestUtils.getPropertyValue(withStringObjectTemplate,
				"redisTemplate.valueSerializer") instanceof JdkSerializationRedisSerializer);
		assertTrue(TestUtils.getPropertyValue(withStringObjectTemplate,
				"redisTemplate.hashValueSerializer") instanceof JdkSerializationRedisSerializer);
	}

	@Test
	public void validateWithExternalTemplate(){
		RedisStoreWritingMessageHandler withExternalTemplate =
				TestUtils.getPropertyValue(context.getBean("withExternalTemplate.adapter"), "handler",
						RedisStoreWritingMessageHandler.class);
		assertEquals("pepboys", ((LiteralExpression)TestUtils.getPropertyValue(withExternalTemplate,
				"keyExpression")).getExpressionString());
		assertEquals("PROPERTIES", (TestUtils.getPropertyValue(withExternalTemplate, "collectionType")).toString());
		assertSame(redisTemplate, TestUtils.getPropertyValue(withExternalTemplate, "redisTemplate"));
	}

}
