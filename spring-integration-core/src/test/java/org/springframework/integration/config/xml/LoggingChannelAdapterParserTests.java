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

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @author Glenn Renfro
 *
 * @since 2.1
 */
@SpringJUnitConfig
public class LoggingChannelAdapterParserTests {

	@Autowired
	@Qualifier("logger.adapter")
	private EventDrivenConsumer loggerConsumer;

	@Autowired
	@Qualifier("loggerWithExpression.adapter")
	private EventDrivenConsumer loggerWithExpression;

	@Test
	public void verifyConfig() {
		LoggingHandler loggingHandler = TestUtils.getPropertyValue(loggerConsumer, "handler");
		assertThat(TestUtils.<String>getPropertyValue(loggingHandler, "messageLogger.log.logger.name"))
				.isEqualTo("org.springframework.integration.test.logger");
		assertThat(TestUtils.<Integer>getPropertyValue(loggingHandler, "order")).isEqualTo(1);
		assertThat(TestUtils.<LoggingHandler.Level>getPropertyValue(loggingHandler, "level"))
				.isEqualTo(LoggingHandler.Level.WARN);
		assertThat(TestUtils.<Object>getPropertyValue(loggingHandler, "expression"))
				.isInstanceOf(FunctionExpression.class);
	}

	@Test
	public void verifyExpressionAndOtherDefaultConfig() {
		LoggingHandler loggingHandler =
				TestUtils.<LoggingHandler>getPropertyValue(loggerWithExpression, "handler");
		assertThat(TestUtils.<String>getPropertyValue(loggingHandler, "messageLogger.log.logger.name"))
				.isEqualTo("org.springframework.integration.handler.LoggingHandler");
		assertThat(TestUtils.<Integer>getPropertyValue(loggingHandler, "order"))
				.isEqualTo(Ordered.LOWEST_PRECEDENCE);
		assertThat(TestUtils.<LoggingHandler.Level>getPropertyValue(loggingHandler, "level"))
				.isEqualTo(LoggingHandler.Level.INFO);
		assertThat(TestUtils.<String>getPropertyValue(loggingHandler, "expression.expression"))
				.isEqualTo("payload.foo");
		assertThat(TestUtils.<Object>getPropertyValue(loggingHandler, "evaluationContext.beanResolver")).isNotNull();
	}

	@Test
	public void failConfigLogFullMessageAndExpression() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"LoggingChannelAdapterParserTests-fail-context.xml",
								getClass()))
				.withMessageContaining("The 'expression' and 'log-full-message' attributes are mutually exclusive.");
	}

}
