/*
 * Copyright 2007-2012 the original author or authors
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
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean.CollectionType;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.redis.outbound.RedisCollectionPopulatingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
/**
 *
 * @author Oleg Zhurakousky
 */
public class RedisCollectionsOutboundChannelAdapterParserTests {

	@Test
	public void validateFullConfiguration(){
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter-parser.xml", this.getClass());
		RedisCollectionPopulatingMessageHandler handler =
				TestUtils.getPropertyValue(context.getBean("storeAdapter.adapter"), "handler", RedisCollectionPopulatingMessageHandler.class);
		assertEquals(TestUtils.getPropertyValue(handler, "keySerializer"), context.getBean("keySerializer"));
		assertEquals(TestUtils.getPropertyValue(handler, "valueSerializer"), context.getBean("valueSerializer"));
		assertEquals(TestUtils.getPropertyValue(handler, "hashKeySerializer"), context.getBean("hashKeySerializer"));
		assertEquals(TestUtils.getPropertyValue(handler, "hashValueSerializer"), context.getBean("hashValueSerializer"));
		assertEquals("pepboys", ((LiteralExpression)TestUtils.getPropertyValue(handler, "keyExpression")).getExpressionString());
		assertEquals("PROPERTIES", ((CollectionType)TestUtils.getPropertyValue(handler, "collectionType")).toString());
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void validateFailureIfTemplateAndSerializers(){
		new ClassPathXmlApplicationContext("store-outbound-adapter-parser-fail.xml", this.getClass());
	}
}
