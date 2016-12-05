/*
 * Copyright 2002-2016 the original author or authors.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.mongodb.inbound.MongoDbMessageSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class MongoDbInboundChannelAdapterParserTests {

	@Autowired
	private MongoDbFactory mongoDbFactory;

	@Autowired
	private MongoConverter mongoConverter;

	@Autowired
	private MongoTemplate mongoDbTemplate;

	@Autowired
	@Qualifier("minimalConfig.adapter")
	private SourcePollingChannelAdapter minimalConfigAdapter;

	@Autowired
	@Qualifier("fullConfigWithCollectionExpression.adapter")
	private SourcePollingChannelAdapter fullConfigWithCollectionExpressionAdapter;

	@Autowired
	@Qualifier("fullConfigWithQueryExpression.adapter")
	private SourcePollingChannelAdapter fullConfigWithQueryExpressionAdapter;

	@Autowired
	@Qualifier("fullConfigWithQuery.adapter")
	private SourcePollingChannelAdapter fullConfigWithQueryAdapter;

	@Autowired
	@Qualifier("fullConfigWithSpelQuery.adapter")
	private SourcePollingChannelAdapter fullConfigWithSpelQueryAdapter;

	@Autowired
	@Qualifier("fullConfigWithCollectionName.adapter")
	private SourcePollingChannelAdapter fullConfigWithCollectionNameAdapter;

	@Autowired
	@Qualifier("fullConfigWithMongoTemplate.adapter")
	private SourcePollingChannelAdapter fullConfigWithMongoTemplateAdapter;

	@Test
	public void minimalConfig() {
		MongoDbMessageSource source = TestUtils.getPropertyValue(this.minimalConfigAdapter, "source",
				MongoDbMessageSource.class);

		assertEquals(false, TestUtils.getPropertyValue(this.minimalConfigAdapter, "shouldTrack"));
		assertNotNull(TestUtils.getPropertyValue(source, "mongoTemplate"));
		assertEquals(this.mongoDbFactory, TestUtils.getPropertyValue(source, "mongoDbFactory"));
		assertNotNull(TestUtils.getPropertyValue(source, "evaluationContext"));
		assertTrue(TestUtils.getPropertyValue(source, "collectionNameExpression") instanceof LiteralExpression);
		assertEquals("data", TestUtils.getPropertyValue(source, "collectionNameExpression.literalValue"));
	}

	@Test
	public void fullConfigWithCollectionExpression() {
		MongoDbMessageSource source = assertMongoDbMessageSource(this.fullConfigWithCollectionExpressionAdapter);
		assertTrue(TestUtils.getPropertyValue(source, "collectionNameExpression") instanceof SpelExpression);
		assertEquals("'foo'", TestUtils.getPropertyValue(source, "collectionNameExpression.expression"));
	}
	@Test
	public void fullConfigWithQueryExpression() {
		MongoDbMessageSource source = assertMongoDbMessageSource(this.fullConfigWithQueryExpressionAdapter);
		assertTrue(TestUtils.getPropertyValue(source, "queryExpression") instanceof SpelExpression);
		assertEquals("new org.springframework.data.mongodb.core.query.BasicQuery('{''address.state'' : ''PA''}').limit(2)", TestUtils.getPropertyValue(source, "queryExpression.expression"));
	}
	@Test
	public void fullConfigWithSpelQuery() {
		MongoDbMessageSource source = assertMongoDbMessageSource(this.fullConfigWithSpelQueryAdapter);
		assertTrue(TestUtils.getPropertyValue(source, "queryExpression") instanceof LiteralExpression);
		assertEquals("{''address.state'' : ''PA''}", TestUtils.getPropertyValue(source, "queryExpression.literalValue"));
	}
	@Test
	public void fullConfigWithQuery() {
		MongoDbMessageSource source = assertMongoDbMessageSource(this.fullConfigWithQueryAdapter);
		assertTrue(TestUtils.getPropertyValue(source, "queryExpression") instanceof LiteralExpression);
		assertEquals("{'address.state' : 'PA'}", TestUtils.getPropertyValue(source, "queryExpression.literalValue"));
	}

	@Test
	public void fullConfigWithCollectionName() {
		MongoDbMessageSource source = assertMongoDbMessageSource(this.fullConfigWithCollectionNameAdapter);
		assertTrue(TestUtils.getPropertyValue(source, "collectionNameExpression") instanceof LiteralExpression);
		assertEquals("foo", TestUtils.getPropertyValue(source, "collectionNameExpression.literalValue"));
	}

	@Test
	public void fullConfigWithMongoTemplate() {
		MongoDbMessageSource source = TestUtils.getPropertyValue(this.fullConfigWithMongoTemplateAdapter, "source",
				MongoDbMessageSource.class);

		assertEquals(false, TestUtils.getPropertyValue(this.fullConfigWithMongoTemplateAdapter, "shouldTrack"));
		assertNotNull(TestUtils.getPropertyValue(source, "mongoTemplate"));
		assertSame(this.mongoDbTemplate, TestUtils.getPropertyValue(source, "mongoTemplate"));
		assertNotNull(TestUtils.getPropertyValue(source, "evaluationContext"));
		assertTrue(TestUtils.getPropertyValue(source, "collectionNameExpression") instanceof LiteralExpression);
		assertEquals("foo", TestUtils.getPropertyValue(source, "collectionNameExpression.literalValue"));
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void templateAndFactoryFail() {
		new ClassPathXmlApplicationContext("inbound-adapter-parser-fail-template-factory-config.xml", this.getClass())
				.close();
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void templateAndConverterFail() {
		new ClassPathXmlApplicationContext("inbound-adapter-parser-fail-template-converter-config.xml", this.getClass())
				.close();
	}
	private MongoDbMessageSource assertMongoDbMessageSource(Object testedBean) {
		MongoDbMessageSource source = TestUtils.getPropertyValue(testedBean, "source", MongoDbMessageSource.class);

		assertEquals(false, TestUtils.getPropertyValue(testedBean, "shouldTrack"));
		assertNotNull(TestUtils.getPropertyValue(source, "mongoTemplate"));
		assertEquals(this.mongoDbFactory, TestUtils.getPropertyValue(source, "mongoDbFactory"));
		assertEquals(this.mongoConverter, TestUtils.getPropertyValue(source, "mongoConverter"));
		assertNotNull(TestUtils.getPropertyValue(source, "evaluationContext"));
		return source;
	}
}
