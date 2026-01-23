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

package org.springframework.integration.handler;

import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.log.LogAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.condition.LogLevels;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Andriy Kryvtsun
 * @author Glenn Renfro
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
@LogLevels(categories = "test.logging.handler")
public class LoggingHandlerTests implements TestApplicationContextAware {

	@Autowired
	@Qualifier("input.handler")
	LoggingHandler loggingHandler;

	@Autowired
	MessageChannel input;

	@Test
	public void logWithExpression() {
		DirectFieldAccessor accessor = new DirectFieldAccessor(loggingHandler);
		LogAccessor log = (LogAccessor) accessor.getPropertyValue("messageLogger");
		log = spy(log);
		accessor.setPropertyValue("messageLogger", log);

		TestBean bean = new TestBean("test", 55);
		input.send(MessageBuilder.withPayload(bean).setHeader("foo", "bar").build());

		verify(log)
				.info(ArgumentMatchers.<Supplier<? extends CharSequence>>argThat(logMessage ->
						logMessage.get().equals("test:55")));
	}

	@Test
	public void assertMutuallyExclusive() {
		LoggingHandler loggingHandler = new LoggingHandler("INFO");
		loggingHandler.setLogExpressionString("'test'");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> loggingHandler.setShouldLogFullMessage(true))
				.withMessage("Cannot set both 'expression' AND 'shouldLogFullMessage' properties");

		LoggingHandler loggingHandler2 = new LoggingHandler("INFO");
		loggingHandler2.setShouldLogFullMessage(true);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> loggingHandler2.setLogExpressionString("'test'"))
				.withMessage("Cannot set both 'expression' AND 'shouldLogFullMessage' properties");
	}

	@Test
	public void testDontEvaluateIfNotEnabled() {
		LoggingHandler loggingHandler = new LoggingHandler("INFO");
		loggingHandler.setLoggerName("test.logging.handler");
		loggingHandler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		loggingHandler.afterPropertiesSet();

		LogAccessor logAccessor = TestUtils.getPropertyValue(loggingHandler, "messageLogger");
		Log log = spy(logAccessor.getLog());
		when(log.isInfoEnabled()).thenReturn(false, true);
		new DirectFieldAccessor(logAccessor).setPropertyValue("log", log);
		Expression expression = spy(TestUtils.<Expression>getPropertyValue(loggingHandler, "expression"));
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
		loggingHandler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
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

	public record TestBean(String name, int age) {

	}

}
