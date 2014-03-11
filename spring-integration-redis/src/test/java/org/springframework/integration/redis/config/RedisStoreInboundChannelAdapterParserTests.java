/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.redis.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean.CollectionType;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.redis.inbound.RedisStoreMessageSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.2
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisStoreInboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private RedisTemplate<?,?> redisTemplate;

	@Test
	public void validateWithStringTemplate(){
		RedisStoreMessageSource withStringTemplate =
				TestUtils.getPropertyValue(context.getBean("withStringTemplate"), "source", RedisStoreMessageSource.class);
		assertEquals("'presidents'", ((SpelExpression)TestUtils.getPropertyValue(withStringTemplate, "keyExpression")).getExpressionString());
		assertEquals("LIST", ((CollectionType)TestUtils.getPropertyValue(withStringTemplate, "collectionType")).toString());
		assertTrue(TestUtils.getPropertyValue(withStringTemplate, "redisTemplate") instanceof StringRedisTemplate);
	}

	@Test
	public void validateWithExternalTemplate(){
		RedisStoreMessageSource withExternalTemplate =
				TestUtils.getPropertyValue(context.getBean("withExternalTemplate"), "source", RedisStoreMessageSource.class);
		assertEquals("'presidents'", ((SpelExpression)TestUtils.getPropertyValue(withExternalTemplate, "keyExpression")).getExpressionString());
		assertEquals("LIST", ((CollectionType)TestUtils.getPropertyValue(withExternalTemplate, "collectionType")).toString());
		assertSame(redisTemplate, TestUtils.getPropertyValue(withExternalTemplate, "redisTemplate"));
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void testTemplateAndCfMutualExclusivity(){
		new ClassPathXmlApplicationContext("inbound-template-cf-fail.xml", this.getClass()).close();
	}

}
