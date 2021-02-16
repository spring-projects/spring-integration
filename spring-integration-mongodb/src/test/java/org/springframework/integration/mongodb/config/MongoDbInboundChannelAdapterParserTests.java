/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.mongodb.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.mongodb.inbound.MongoDbMessageSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Yaron Yamin
 */
@SpringJUnitConfig
@DirtiesContext
public class MongoDbInboundChannelAdapterParserTests {

	@Autowired
	private MongoDatabaseFactory mongoDbFactory;

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

		assertThat(TestUtils.getPropertyValue(this.minimalConfigAdapter, "shouldTrack")).isEqualTo(false);
		assertThat(TestUtils.getPropertyValue(source, "mongoTemplate")).isNotNull();
		assertThat(TestUtils.getPropertyValue(source, "mongoDbFactory")).isEqualTo(this.mongoDbFactory);
		assertThat(TestUtils.getPropertyValue(source, "evaluationContext")).isNotNull();
		assertThat(TestUtils.getPropertyValue(source, "collectionNameExpression") instanceof LiteralExpression)
				.isTrue();
		assertThat(TestUtils.getPropertyValue(source, "collectionNameExpression.literalValue")).isEqualTo("data");
	}

	@Test
	public void fullConfigWithCollectionExpression() {
		MongoDbMessageSource source = assertMongoDbMessageSource(this.fullConfigWithCollectionExpressionAdapter);
		assertThat(TestUtils.getPropertyValue(source, "collectionNameExpression") instanceof SpelExpression).isTrue();
		assertThat(TestUtils.getPropertyValue(source, "collectionNameExpression.expression")).isEqualTo("'foo'");
	}

	@Test
	public void fullConfigWithQueryExpression() {
		MongoDbMessageSource source = assertMongoDbMessageSource(this.fullConfigWithQueryExpressionAdapter);
		assertThat(TestUtils.getPropertyValue(source, "queryExpression") instanceof SpelExpression).isTrue();
		assertThat(TestUtils.getPropertyValue(source, "queryExpression.expression"))
				.isEqualTo("new BasicQuery('{''address.state'' : ''PA''}').limit(2)");
		assertThat(TestUtils.getPropertyValue(source, "updateExpression.literalValue"))
				.isEqualTo("{ $set: {'address.state' : 'NJ'} }");
	}

	@Test
	public void fullConfigWithSpelQuery() {
		MongoDbMessageSource source = assertMongoDbMessageSource(this.fullConfigWithSpelQueryAdapter);
		assertThat(TestUtils.getPropertyValue(source, "queryExpression") instanceof LiteralExpression).isTrue();
		assertThat(TestUtils.getPropertyValue(source, "queryExpression.literalValue"))
				.isEqualTo("{''address.state'' : ''PA''}");
	}

	@Test
	public void fullConfigWithQuery() {
		MongoDbMessageSource source = assertMongoDbMessageSource(this.fullConfigWithQueryAdapter);
		assertThat(TestUtils.getPropertyValue(source, "queryExpression") instanceof LiteralExpression).isTrue();
		assertThat(TestUtils.getPropertyValue(source, "queryExpression.literalValue"))
				.isEqualTo("{'address.state' : 'PA'}");
	}

	@Test
	public void fullConfigWithCollectionName() {
		MongoDbMessageSource source = assertMongoDbMessageSource(this.fullConfigWithCollectionNameAdapter);
		assertThat(TestUtils.getPropertyValue(source, "collectionNameExpression") instanceof LiteralExpression)
				.isTrue();
		assertThat(TestUtils.getPropertyValue(source, "collectionNameExpression.literalValue")).isEqualTo("foo");
	}

	@Test
	public void fullConfigWithMongoTemplate() {
		MongoDbMessageSource source = TestUtils.getPropertyValue(this.fullConfigWithMongoTemplateAdapter, "source",
				MongoDbMessageSource.class);

		assertThat(TestUtils.getPropertyValue(this.fullConfigWithMongoTemplateAdapter, "shouldTrack")).isEqualTo(false);
		assertThat(TestUtils.getPropertyValue(source, "mongoTemplate")).isNotNull();
		assertThat(TestUtils.getPropertyValue(source, "mongoTemplate")).isSameAs(this.mongoDbTemplate);
		assertThat(TestUtils.getPropertyValue(source, "evaluationContext")).isNotNull();
		assertThat(TestUtils.getPropertyValue(source, "collectionNameExpression") instanceof LiteralExpression)
				.isTrue();
		assertThat(TestUtils.getPropertyValue(source, "collectionNameExpression.literalValue")).isEqualTo("foo");
	}

	@Test
	public void templateAndFactoryFail() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("inbound-adapter-parser-fail-template-factory-config.xml",
								getClass()));
	}

	@Test
	public void templateAndConverterFail() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("inbound-adapter-parser-fail-template-converter-config.xml",
								getClass()));
	}

	private MongoDbMessageSource assertMongoDbMessageSource(Object testedBean) {
		MongoDbMessageSource source = TestUtils.getPropertyValue(testedBean, "source", MongoDbMessageSource.class);

		assertThat(TestUtils.getPropertyValue(testedBean, "shouldTrack")).isEqualTo(false);
		assertThat(TestUtils.getPropertyValue(source, "mongoTemplate")).isNotNull();
		assertThat(TestUtils.getPropertyValue(source, "mongoDbFactory")).isEqualTo(this.mongoDbFactory);
		assertThat(TestUtils.getPropertyValue(source, "mongoConverter")).isEqualTo(this.mongoConverter);
		assertThat(TestUtils.getPropertyValue(source, "evaluationContext")).isNotNull();
		return source;
	}

}
