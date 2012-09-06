/*
 * Copyright 2002-2012 the original author or authors.
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

import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean.CollectionType;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.redis.inbound.RedisStoreMessageSource;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class RedisCollectionsInboundChannelAdapterParserTests {
	@Test
	public void validateFullConfiguration(){
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("inbound-store-adapter-parser.xml", this.getClass());
		RedisStoreMessageSource source =
				TestUtils.getPropertyValue(context.getBean("adapterWithConnectionFactory"), "source", RedisStoreMessageSource.class);
		assertEquals(TestUtils.getPropertyValue(source, "keySerializer"), context.getBean("serializer"));
		assertEquals(TestUtils.getPropertyValue(source, "valueSerializer"), context.getBean("serializer"));
		assertEquals(TestUtils.getPropertyValue(source, "hashKeySerializer"), context.getBean("serializer"));
		assertEquals(TestUtils.getPropertyValue(source, "hashValueSerializer"), context.getBean("serializer"));
		assertEquals("'presidents'", ((SpelExpression)TestUtils.getPropertyValue(source, "keyExpression")).getExpressionString());
		assertEquals("LIST", ((CollectionType)TestUtils.getPropertyValue(source, "collectionType")).toString());
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void validateFailureIfTemplateAndSerializers(){
		new ClassPathXmlApplicationContext("inbound-store-adapter-parser-fail.xml", this.getClass());
	}
}
