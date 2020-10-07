/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.log.LogAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.condition.LogLevels;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Andriy Kryvtsun
 *
 * @since 2.0
 */
@SpringJUnitConfig
@LogLevels(categories = "test.logging.handler")
public class LoggingHandlerTests {

	@Autowired
	MessageChannel input;

	@Test
	public void logWithExpression() {
		TestBean bean = new TestBean("test", 55);
		input.send(MessageBuilder.withPayload(bean).setHeader("foo", "bar").build());
	}

	@Test
	public void assertMutuallyExclusive() {
		LoggingHandler loggingHandler = new LoggingHandler("INFO");
		loggingHandler.setLogExpressionString("'foo'");
		try {
			loggingHandler.setShouldLogFullMessage(true);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Cannot set both 'expression' AND 'shouldLogFullMessage' properties");
		}

		loggingHandler = new LoggingHandler("INFO");
		loggingHandler.setShouldLogFullMessage(true);
		try {
			loggingHandler.setLogExpressionString("'foo'");
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Cannot set both 'expression' AND 'shouldLogFullMessage' properties");
		}
	}

	@Test
	public void testDontEvaluateIfNotEnabled() {
		LoggingHandler loggingHandler = new LoggingHandler("INFO");
		loggingHandler.setLoggerName("test.logging.handler");
		loggingHandler.setBeanFactory(mock(BeanFactory.class));
		loggingHandler.afterPropertiesSet();

		LogAccessor logAccessor = TestUtils.getPropertyValue(loggingHandler, "messageLogger", LogAccessor.class);
		Log log = spy(logAccessor.getLog());
		when(log.isInfoEnabled()).thenReturn(false, true);
		new DirectFieldAccessor(logAccessor).setPropertyValue("log", log);
		Expression expression = spy(TestUtils.getPropertyValue(loggingHandler, "expression", Expression.class));
		loggingHandler.setLogExpression(expression);
		loggingHandler.handleMessage(new GenericMessage<>("foo"));
		verify(expression, never()).getValue(any(EvaluationContext.class), any(Message.class));
		loggingHandler.handleMessage(new GenericMessage<>("foo"));
		verify(expression, times(1)).getValue(any(EvaluationContext.class), any(Message.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testChangeLevel() {
		LoggingHandler loggingHandler = new LoggingHandler(Level.INFO);
		loggingHandler.setBeanFactory(mock(BeanFactory.class));
		loggingHandler.afterPropertiesSet();

		DirectFieldAccessor accessor = new DirectFieldAccessor(loggingHandler);
		LogAccessor log = (LogAccessor) accessor.getPropertyValue("messageLogger");
		log = spy(log);
		accessor.setPropertyValue("messageLogger", log);
		when(log.isInfoEnabled()).thenReturn(true);
		loggingHandler.handleMessage(new GenericMessage<>("foo"));
		verify(log, times(1)).info(any(Supplier.class));
		verify(log, never()).warn(any(Supplier.class));

		loggingHandler.setLevel(Level.WARN);
		loggingHandler.handleMessage(new GenericMessage<>("foo"));
		verify(log, times(1)).info(any(Supplier.class));
		verify(log, times(1)).warn(any(Supplier.class));
	}

	@Test
	public void testUsageWithoutSpringInitialization() {
		LoggingHandler loggingHandler = new LoggingHandler("ERROR");
		DirectFieldAccessor accessor = new DirectFieldAccessor(loggingHandler);
		LogAccessor log = (LogAccessor) accessor.getPropertyValue("messageLogger");
		log = spy(log);
		accessor.setPropertyValue("messageLogger", log);

		String testPayload = "TEST_PAYLOAD";
		Message<String> message = MessageBuilder.withPayload(testPayload).build();

		loggingHandler.handleMessage(message);

		verify(log)
				.error(ArgumentMatchers.<Supplier<? extends CharSequence>>argThat(logMessage ->
						logMessage.get().equals(testPayload)));
	}

	public static class TestBean {

		private final String name;

		private final int age;

		public TestBean(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String getName() {
			return this.name;
		}

		public int getAge() {
			return this.age;
		}

	}

}
