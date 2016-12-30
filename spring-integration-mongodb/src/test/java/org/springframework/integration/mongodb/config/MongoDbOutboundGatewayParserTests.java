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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.CollectionCallback;
import org.springframework.data.mongodb.core.convert.MongoConverter;
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

	@Autowired
	private MongoDbFactory mongoDbFactory;

	@Autowired
	private MongoConverter mongoConverter;

	@Test
	public void minimalConfig() {
		MongoDbOutboundGateway gateway =
				TestUtils.getPropertyValue(context.getBean("minimalConfig"), "handler", MongoDbOutboundGateway.class);

		assertNotNull(TestUtils.getPropertyValue(gateway, "mongoTemplate"));
		assertSame(this.mongoDbFactory, TestUtils.getPropertyValue(gateway, "mongoDbFactory"));
		assertNotNull(TestUtils.getPropertyValue(gateway, "evaluationContext"));
		assertThat(TestUtils.getPropertyValue(gateway, "collectionNameExpression"),
				instanceOf(LiteralExpression.class));
		assertEquals("foo", TestUtils.getPropertyValue(gateway, "collectionNameExpression.literalValue"));
	}

	@Test
	public void fullConfigWithCollectionExpression() {
		MongoDbOutboundGateway gateway = TestUtils.getPropertyValue(
				context.getBean("fullConfigWithCollectionExpression"), "handler", MongoDbOutboundGateway.class);

		assertNotNull(TestUtils.getPropertyValue(gateway, "mongoTemplate"));
		assertSame(this.mongoDbFactory, TestUtils.getPropertyValue(gateway, "mongoDbFactory"));
		assertSame(this.mongoConverter, TestUtils.getPropertyValue(gateway, "mongoConverter"));
		assertNotNull(TestUtils.getPropertyValue(gateway, "evaluationContext"));
		assertThat(TestUtils.getPropertyValue(gateway, "collectionNameExpression"),
				instanceOf(SpelExpression.class));
		assertEquals("headers.collectionName",
				TestUtils.getPropertyValue(gateway, "collectionNameExpression.expression"));
	}

	@Test
	public void fullConfigWithCollection() {
		MongoDbOutboundGateway gateway = TestUtils.getPropertyValue(
				context.getBean("fullConfigWithCollection"), "handler", MongoDbOutboundGateway.class);

		assertNotNull(TestUtils.getPropertyValue(gateway, "mongoTemplate"));
		assertSame(this.mongoDbFactory, TestUtils.getPropertyValue(gateway, "mongoDbFactory"));
		assertSame(this.mongoConverter, TestUtils.getPropertyValue(gateway, "mongoConverter"));
		assertNotNull(TestUtils.getPropertyValue(gateway, "evaluationContext"));
		assertThat(TestUtils.getPropertyValue(gateway, "collectionNameExpression"),
				instanceOf(LiteralExpression.class));
		assertEquals("foo", TestUtils.getPropertyValue(gateway, "collectionNameExpression.literalValue"));
	}

	@Test
	public void fullConfigWithMongoTemplate() {
		MongoDbOutboundGateway gateway = TestUtils.getPropertyValue(
				context.getBean("fullConfigWithTemplate"), "handler", MongoDbOutboundGateway.class);

		assertSame(context.getBean("mongoDbTemplate"), TestUtils.getPropertyValue(gateway, "mongoTemplate"));
		assertNull(TestUtils.getPropertyValue(gateway, "mongoDbFactory"));
		assertNull(TestUtils.getPropertyValue(gateway, "mongoConverter"));
		assertNotNull(TestUtils.getPropertyValue(gateway, "evaluationContext"));
		assertThat(TestUtils.getPropertyValue(gateway, "collectionNameExpression"),
				instanceOf(LiteralExpression.class));
		assertEquals("foo", TestUtils.getPropertyValue(gateway, "collectionNameExpression.literalValue"));
	}

	@Test
	public void fullConfigWithMongoDbCollectionCallback() {
		MongoDbOutboundGateway gateway = TestUtils.getPropertyValue(
				context.getBean("fullConfigWithMongoDbCollectionCallback"), "handler", MongoDbOutboundGateway.class);

		assertSame(context.getBean("mongoDbTemplate"), TestUtils.getPropertyValue(gateway, "mongoTemplate"));
		assertNull(TestUtils.getPropertyValue(gateway, "mongoDbFactory"));
		assertNull(TestUtils.getPropertyValue(gateway, "mongoConverter"));
		assertNotNull(TestUtils.getPropertyValue(gateway, "evaluationContext"));
		assertThat(TestUtils.getPropertyValue(gateway, "collectionNameExpression"),
				instanceOf(LiteralExpression.class));
		assertEquals("foo", TestUtils.getPropertyValue(gateway, "collectionNameExpression.literalValue"));
		assertThat(TestUtils.getPropertyValue(gateway, "collectionCallback"),
				instanceOf(CollectionCallback.class));
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

	@Test(expected = BeanDefinitionParsingException.class)
	public void collectionCallbackAndQueryFail() {
		new ClassPathXmlApplicationContext("outbound-gateway-fail-collection-callback-config.xml",
				this.getClass()).close();
	}

}
