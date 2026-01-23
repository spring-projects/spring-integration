/*
 * Copyright 2002-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.mongodb.outbound.MongoDbStoringMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Glenn Renfro
 */
@SpringJUnitConfig
public class MongoDbOutboundChannelAdapterParserTests {

	@Autowired
	ApplicationContext context;

	@Test
	public void minimalConfig() {
		MongoDbStoringMessageHandler handler =
				TestUtils.getPropertyValue(context.getBean("minimalConfig.adapter"), "handler");
		assertThat(TestUtils.<String>getPropertyValue(handler, "componentName")).isEqualTo("minimalConfig.adapter");
		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "shouldTrack")).isEqualTo(false);
		assertThat(TestUtils.<Object>getPropertyValue(handler, "mongoTemplate")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(handler, "mongoDbFactory"))
				.isEqualTo(context.getBean("mongoDbFactory"));
		assertThat(TestUtils.<Object>getPropertyValue(handler, "evaluationContext")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(handler, "collectionNameExpression"))
				.isInstanceOf(LiteralExpression.class);
		assertThat(TestUtils.<String>getPropertyValue(handler, "collectionNameExpression.literalValue"))
				.isEqualTo("data");
	}

	@Test
	public void fullConfigWithCollectionExpression() {
		MongoDbStoringMessageHandler handler =
				TestUtils.getPropertyValue(context.getBean("fullConfigWithCollectionExpression.adapter"), "handler");
		assertThat(TestUtils.<String>getPropertyValue(handler, "componentName"))
				.isEqualTo("fullConfigWithCollectionExpression.adapter");
		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "shouldTrack")).isEqualTo(false);
		assertThat(TestUtils.<Object>getPropertyValue(handler, "mongoTemplate")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(handler, "mongoDbFactory"))
				.isEqualTo(context.getBean("mongoDbFactory"));
		assertThat(TestUtils.<Object>getPropertyValue(handler, "evaluationContext")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(handler, "collectionNameExpression"))
				.isInstanceOf(SpelExpression.class);
		assertThat(TestUtils.<String>getPropertyValue(handler, "collectionNameExpression.expression"))
				.isEqualTo("headers.collectionName");
	}

	@Test
	public void fullConfigWithCollection() {
		MongoDbStoringMessageHandler handler =
				TestUtils.getPropertyValue(context.getBean("fullConfigWithCollection.adapter"), "handler");
		assertThat(TestUtils.<String>getPropertyValue(handler, "componentName"))
				.isEqualTo("fullConfigWithCollection.adapter");
		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "shouldTrack")).isEqualTo(false);
		assertThat(TestUtils.<Object>getPropertyValue(handler, "mongoTemplate")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(handler, "mongoDbFactory"))
				.isEqualTo(context.getBean("mongoDbFactory"));
		assertThat(TestUtils.<Object>getPropertyValue(handler, "evaluationContext")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(handler, "collectionNameExpression"))
				.isInstanceOf(LiteralExpression.class);
		assertThat(TestUtils.<String>getPropertyValue(handler, "collectionNameExpression.literalValue"))
				.isEqualTo("foo");
	}

	@Test
	public void fullConfigWithMongoTemplate() {
		MongoDbStoringMessageHandler handler =
				TestUtils.getPropertyValue(context.getBean("fullConfigWithMongoTemplate.adapter"), "handler");
		assertThat(TestUtils.<String>getPropertyValue(handler, "componentName"))
				.isEqualTo("fullConfigWithMongoTemplate.adapter");
		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "shouldTrack")).isEqualTo(false);
		assertThat(TestUtils.<Object>getPropertyValue(handler, "mongoTemplate")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(handler, "evaluationContext")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(handler, "collectionNameExpression"))
				.isInstanceOf(LiteralExpression.class);
		assertThat(TestUtils.<String>getPropertyValue(handler, "collectionNameExpression.literalValue"))
				.isEqualTo("foo");
	}

	@Test
	public void templateAndFactoryFail() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("outbound-adapter-parser-fail-template-factory-config.xml",
								getClass()));
	}

	@Test
	public void templateAndConverterFail() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("outbound-adapter-parser-fail-template-converter-config.xml",
								getClass()));
	}

	@Test
	public void testInt3024PollerAndRequestHandlerAdviceChain() {
		AbstractEndpoint endpoint = context.getBean("pollableAdapter", AbstractEndpoint.class);
		assertThat(endpoint).isInstanceOf(PollingConsumer.class);
		MessageHandler handler = TestUtils.getPropertyValue(endpoint, "handler");
		assertThat(AopUtils.isAopProxy(handler)).isTrue();
		assertThat(((Advised) handler).getAdvisors()[0].getAdvice()).isInstanceOf(RequestHandlerRetryAdvice.class);
	}

}
