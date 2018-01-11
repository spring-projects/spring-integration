/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class LoggingChannelAdapterParserTests {

	@Autowired @Qualifier("logger.adapter")
	private EventDrivenConsumer loggerConsumer;

	@Autowired @Qualifier("loggerWithExpression.adapter")
	private EventDrivenConsumer loggerWithExpression;


	@Test
	public void verifyConfig() {
		LoggingHandler loggingHandler = TestUtils.getPropertyValue(loggerConsumer, "handler", LoggingHandler.class);
		assertEquals("org.springframework.integration.test.logger",
				TestUtils.getPropertyValue(loggingHandler, "messageLogger.logger.name"));
		assertEquals(1, TestUtils.getPropertyValue(loggingHandler, "order"));
		assertEquals("WARN", TestUtils.getPropertyValue(loggingHandler, "level").toString());
		assertEquals("#root", TestUtils.getPropertyValue(loggingHandler, "expression.expression"));
	}

	@Test
	public void verifyExpressionAndOtherDefaultConfig() {
		LoggingHandler loggingHandler = TestUtils.getPropertyValue(loggerWithExpression, "handler", LoggingHandler.class);
		assertEquals("org.springframework.integration.handler.LoggingHandler",
				TestUtils.getPropertyValue(loggingHandler, "messageLogger.logger.name"));
		assertEquals(Ordered.LOWEST_PRECEDENCE, TestUtils.getPropertyValue(loggingHandler, "order"));
		assertEquals("INFO", TestUtils.getPropertyValue(loggingHandler, "level").toString());
		assertEquals("payload.foo", TestUtils.getPropertyValue(loggingHandler, "expression.expression"));
		assertNotNull(TestUtils.getPropertyValue(loggingHandler, "evaluationContext.beanResolver"));
	}

	@Test
	public void failConfigLogFullMessageAndExpression() {
		try {
			new ClassPathXmlApplicationContext("LoggingChannelAdapterParserTests-fail-context.xml", this.getClass())
					.close();
			fail("BeanDefinitionParsingException expected");
		}
		catch (BeansException e) {
			assertTrue(e instanceof BeanDefinitionParsingException);
			assertTrue(e.getMessage().contains("The 'expression' and 'log-full-message' attributes are mutually exclusive."));
		}
	}

}
