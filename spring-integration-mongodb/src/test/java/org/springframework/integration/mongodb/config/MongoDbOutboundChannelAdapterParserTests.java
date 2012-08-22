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
package org.springframework.integration.mongodb.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.mongodb.outbound.MongoDbStoringMessageHandler;
import org.springframework.integration.test.util.TestUtils;
/**
 * @author Oleg Zhurakousky
 */
public class MongoDbOutboundChannelAdapterParserTests {

	@Test
	public void minimalConfig(){
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-parser-config.xml", this.getClass());
		MongoDbStoringMessageHandler handler =
				TestUtils.getPropertyValue(context.getBean("minimalConfig.adapter"), "handler", MongoDbStoringMessageHandler.class);
		assertEquals("minimalConfig.adapter", TestUtils.getPropertyValue(handler, "componentName"));
		assertEquals(false, TestUtils.getPropertyValue(handler, "shouldTrack"));
		assertNotNull(TestUtils.getPropertyValue(handler, "mongoTemplate"));
		assertEquals(context.getBean("mongoDbFactory"), TestUtils.getPropertyValue(handler, "mongoDbFactory"));
		assertNotNull(TestUtils.getPropertyValue(handler, "evaluationContext"));
		assertTrue(TestUtils.getPropertyValue(handler, "collectionNameExpression") instanceof LiteralExpression);
		assertEquals("data", TestUtils.getPropertyValue(handler, "collectionNameExpression.literalValue"));
	}

	@Test
	public void fullConfigWithCollectionExpression(){
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-parser-config.xml", this.getClass());
		MongoDbStoringMessageHandler handler =
				TestUtils.getPropertyValue(context.getBean("fullConfigWithCollectionExpression.adapter"), "handler", MongoDbStoringMessageHandler.class);
		assertEquals("fullConfigWithCollectionExpression.adapter", TestUtils.getPropertyValue(handler, "componentName"));
		assertEquals(false, TestUtils.getPropertyValue(handler, "shouldTrack"));
		assertNotNull(TestUtils.getPropertyValue(handler, "mongoTemplate"));
		assertEquals(context.getBean("mongoDbFactory"), TestUtils.getPropertyValue(handler, "mongoDbFactory"));
		assertNotNull(TestUtils.getPropertyValue(handler, "evaluationContext"));
		assertTrue(TestUtils.getPropertyValue(handler, "collectionNameExpression") instanceof SpelExpression);
		assertEquals("headers.collectionName", TestUtils.getPropertyValue(handler, "collectionNameExpression.expression"));
	}

	@Test
	public void fullConfigWithCollection(){
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-parser-config.xml", this.getClass());
		MongoDbStoringMessageHandler handler =
				TestUtils.getPropertyValue(context.getBean("fullConfigWithCollection.adapter"), "handler", MongoDbStoringMessageHandler.class);
		assertEquals("fullConfigWithCollection.adapter", TestUtils.getPropertyValue(handler, "componentName"));
		assertEquals(false, TestUtils.getPropertyValue(handler, "shouldTrack"));
		assertNotNull(TestUtils.getPropertyValue(handler, "mongoTemplate"));
		assertEquals(context.getBean("mongoDbFactory"), TestUtils.getPropertyValue(handler, "mongoDbFactory"));
		assertNotNull(TestUtils.getPropertyValue(handler, "evaluationContext"));
		assertTrue(TestUtils.getPropertyValue(handler, "collectionNameExpression") instanceof LiteralExpression);
		assertEquals("foo", TestUtils.getPropertyValue(handler, "collectionNameExpression.literalValue"));
	}

	@Test
	public void fullConfigWithMongoTemplate(){
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-parser-config.xml", this.getClass());
		MongoDbStoringMessageHandler handler =
				TestUtils.getPropertyValue(context.getBean("fullConfigWithMongoTemplate.adapter"), "handler", MongoDbStoringMessageHandler.class);
		assertEquals("fullConfigWithMongoTemplate.adapter", TestUtils.getPropertyValue(handler, "componentName"));
		assertEquals(false, TestUtils.getPropertyValue(handler, "shouldTrack"));
		assertNotNull(TestUtils.getPropertyValue(handler, "mongoTemplate"));
		assertNotNull(TestUtils.getPropertyValue(handler, "evaluationContext"));
		assertTrue(TestUtils.getPropertyValue(handler, "collectionNameExpression") instanceof LiteralExpression);
		assertEquals("foo", TestUtils.getPropertyValue(handler, "collectionNameExpression.literalValue"));
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void templateAndFactoryFail(){
		new ClassPathXmlApplicationContext("outbound-adapter-parser-fail-template-factory-config.xml", this.getClass());
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void templateAndConverterFail(){
		new ClassPathXmlApplicationContext("outbound-adapter-parser-fail-template-converter-config.xml", this.getClass());
	}
}
