/*
 * Copyright 2016 the original author or authors.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.mongodb.outbound.MongoDbOutboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Xavier Padró
 * @since 5.0
 */
@ContextConfiguration
@RunWith(SpringRunner.class)
@DirtiesContext
public class MongoDbOutboundGatewayParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void minimalConfig() {
		MongoDbOutboundGateway gateway =
				TestUtils.getPropertyValue(context.getBean("minimalConfig"), "handler", MongoDbOutboundGateway.class);

		assertNotNull(TestUtils.getPropertyValue(gateway, "mongoTemplate"));
		assertEquals(context.getBean("mongoDbFactory"), TestUtils.getPropertyValue(gateway, "mongoDbFactory"));
		assertNotNull(TestUtils.getPropertyValue(gateway, "evaluationContext"));
		assertTrue(TestUtils.getPropertyValue(gateway, "collectionNameExpression") instanceof LiteralExpression);
		assertEquals("foo", TestUtils.getPropertyValue(gateway, "collectionNameExpression.literalValue"));
	}

	@Test
	public void fullConfigWithCollectionExpression() {
		MongoDbOutboundGateway gateway = TestUtils.getPropertyValue(
				context.getBean("fullConfigWithCollectionExpression"), "handler", MongoDbOutboundGateway.class);

		assertNotNull(TestUtils.getPropertyValue(gateway, "mongoTemplate"));
		assertEquals(context.getBean("mongoDbFactory"), TestUtils.getPropertyValue(gateway, "mongoDbFactory"));
		assertEquals(context.getBean("mongoConverter"), TestUtils.getPropertyValue(gateway, "mongoConverter"));
		assertNotNull(TestUtils.getPropertyValue(gateway, "evaluationContext"));
		assertTrue(TestUtils.getPropertyValue(gateway, "collectionNameExpression") instanceof SpelExpression);
		assertEquals("headers.collectionName",
				TestUtils.getPropertyValue(gateway, "collectionNameExpression.expression"));
	}

	@Test
	public void fullConfigWithCollection() {
		MongoDbOutboundGateway gateway = TestUtils.getPropertyValue(
				context.getBean("fullConfigWithCollection"), "handler", MongoDbOutboundGateway.class);

		assertNotNull(TestUtils.getPropertyValue(gateway, "mongoTemplate"));
		assertEquals(context.getBean("mongoDbFactory"), TestUtils.getPropertyValue(gateway, "mongoDbFactory"));
		assertEquals(context.getBean("mongoConverter"), TestUtils.getPropertyValue(gateway, "mongoConverter"));
		assertNotNull(TestUtils.getPropertyValue(gateway, "evaluationContext"));
		assertTrue(TestUtils.getPropertyValue(gateway, "collectionNameExpression") instanceof LiteralExpression);
		assertEquals("foo", TestUtils.getPropertyValue(gateway, "collectionNameExpression.literalValue"));
	}

	@Test
	public void fullConfigWithMongoTemplate() {
		MongoDbOutboundGateway gateway = TestUtils.getPropertyValue(
				context.getBean("fullConfigWithTemplate"), "handler", MongoDbOutboundGateway.class);

		assertEquals(context.getBean("mongoDbTemplate"), TestUtils.getPropertyValue(gateway, "mongoTemplate"));
		assertNull(TestUtils.getPropertyValue(gateway, "mongoDbFactory"));
		assertNull(TestUtils.getPropertyValue(gateway, "mongoConverter"));
		assertNotNull(TestUtils.getPropertyValue(gateway, "evaluationContext"));
		assertTrue(TestUtils.getPropertyValue(gateway, "collectionNameExpression") instanceof LiteralExpression);
		assertEquals("foo", TestUtils.getPropertyValue(gateway, "collectionNameExpression.literalValue"));
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void templateAndFactoryFail() {
		new ClassPathXmlApplicationContext("outbound-gateway-fail-template-factory-config.xml", this.getClass())
				.close();
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void templateAndConverterFail() {
		new ClassPathXmlApplicationContext("outbound-gateway-fail-template-converter-config.xml",
				this.getClass()).close();
	}
}
