/*
 * Copyright 2016-present the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.mongodb.outbound.MessageCollectionCallback;
import org.springframework.integration.mongodb.outbound.MongoDbOutboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Xavier Padro
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
public class MongoDbOutboundGatewayParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MongoDatabaseFactory mongoDbFactory;

	@Autowired
	private MongoConverter mongoConverter;

	@Test
	public void minimalConfig() {
		AbstractEndpoint endpoint = this.context.getBean("minimalConfig", AbstractEndpoint.class);
		MongoDbOutboundGateway gateway = TestUtils.getPropertyValue(endpoint, "handler");

		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mongoTemplate")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mongoDbFactory")).isSameAs(this.mongoDbFactory);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "evaluationContext")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "collectionNameExpression"))
				.isInstanceOf(LiteralExpression.class);
		assertThat(TestUtils.<String>getPropertyValue(gateway, "collectionNameExpression.literalValue"))
				.isEqualTo("foo");
		assertThat(TestUtils.<Long>getPropertyValue(gateway, "messagingTemplate.sendTimeout")).isEqualTo(30000L);

		assertThat(endpoint).isInstanceOf(PollingConsumer.class);
		MessageHandler handler = TestUtils.getPropertyValue(endpoint, "handler");
		List<?> advices = TestUtils.getPropertyValue(handler, "adviceChain");
		assertThat(advices.get(0)).isInstanceOf(RequestHandlerRetryAdvice.class);
	}

	@Test
	public void fullConfigWithCollectionExpression() {
		MongoDbOutboundGateway gateway =
				TestUtils.getPropertyValue(context.getBean("fullConfigWithCollectionExpression"), "handler");

		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mongoTemplate")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mongoDbFactory")).isSameAs(this.mongoDbFactory);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mongoConverter")).isSameAs(this.mongoConverter);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "evaluationContext")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "collectionNameExpression")).isInstanceOf(SpelExpression.class);
		assertThat(TestUtils.<String>getPropertyValue(gateway, "collectionNameExpression.expression"))
				.isEqualTo("headers.collectionName");
	}

	@Test
	public void fullConfigWithCollection() {
		MongoDbOutboundGateway gateway =
				TestUtils.getPropertyValue(context.getBean("fullConfigWithCollection"), "handler");

		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mongoTemplate")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mongoDbFactory")).isSameAs(this.mongoDbFactory);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mongoConverter")).isSameAs(this.mongoConverter);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "evaluationContext")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "collectionNameExpression"))
				.isInstanceOf(LiteralExpression.class);
		assertThat(TestUtils.<String>getPropertyValue(gateway, "collectionNameExpression.literalValue"))
				.isEqualTo("foo");
	}

	@Test
	public void fullConfigWithMongoTemplate() {
		MongoDbOutboundGateway gateway =
				TestUtils.getPropertyValue(context.getBean("fullConfigWithTemplate"), "handler");

		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mongoTemplate"))
				.isSameAs(context.getBean("mongoDbTemplate"));
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mongoDbFactory")).isNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mongoConverter")).isNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "evaluationContext")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "collectionNameExpression"))
				.isInstanceOf(LiteralExpression.class);
		assertThat(TestUtils.<String>getPropertyValue(gateway, "collectionNameExpression.literalValue")).isEqualTo("foo");
	}

	@Test
	public void fullConfigWithMongoDbCollectionCallback() {
		MongoDbOutboundGateway gateway =
				TestUtils.getPropertyValue(context.getBean("fullConfigWithMongoDbCollectionCallback"), "handler");

		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mongoTemplate"))
				.isSameAs(context.getBean("mongoDbTemplate"));
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mongoDbFactory")).isNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mongoConverter")).isNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "evaluationContext")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "collectionNameExpression"))
				.isInstanceOf(LiteralExpression.class);
		assertThat(TestUtils.<String>getPropertyValue(gateway, "collectionNameExpression.literalValue"))
				.isEqualTo("foo");
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "collectionCallback"))
				.isInstanceOf(MessageCollectionCallback.class);
	}

	@Test
	public void templateAndFactoryFail() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("outbound-gateway-fail-template-factory-config.xml",
								getClass()));
	}

	@Test
	public void templateAndConverterFail() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("outbound-gateway-fail-template-converter-config.xml",
								this.getClass()));
	}

	@Test
	public void collectionCallbackAndQueryFail() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("outbound-gateway-fail-collection-callback-config.xml",
								this.getClass()));
	}

}
