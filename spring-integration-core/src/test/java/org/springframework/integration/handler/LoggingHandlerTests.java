/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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
		loggingHandler.setExpression("'foo'");
		try {
			loggingHandler.setShouldLogFullMessage(true);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Cannot set both 'expression' AND 'shouldLogFullMessage' properties", e.getMessage());
		}

		loggingHandler = new LoggingHandler("INFO");
		loggingHandler.setShouldLogFullMessage(true);
		try {
			loggingHandler.setExpression("'foo'");
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Cannot set both 'expression' AND 'shouldLogFullMessage' properties", e.getMessage());
		}
	}

	@Test
	public void testDontEvaluateIfNotEnabled() {
		LoggingHandler loggingHandler = new LoggingHandler("INFO");
		DirectFieldAccessor accessor = new DirectFieldAccessor(loggingHandler);
		Log log = (Log) accessor.getPropertyValue("messageLogger");
		log = spy(log);
		accessor.setPropertyValue("messageLogger", log);
		Expression expression = (Expression) accessor.getPropertyValue("expression");
		expression = spy(expression);
		accessor.setPropertyValue("expression", expression);
		when(log.isInfoEnabled()).thenReturn(false);
		loggingHandler.handleMessage(new GenericMessage<String>("foo"));
		verify(expression, never()).getValue(Mockito.any(EvaluationContext.class), Mockito.any());

		when(log.isInfoEnabled()).thenReturn(true);
		loggingHandler.handleMessage(new GenericMessage<String>("foo"));
		verify(expression, times(1)).getValue(Mockito.any(EvaluationContext.class), Mockito.any());
	}

	@Test
	public void testChangeLevel() {
		LoggingHandler loggingHandler = new LoggingHandler("INFO");
		DirectFieldAccessor accessor = new DirectFieldAccessor(loggingHandler);
		Log log = (Log) accessor.getPropertyValue("messageLogger");
		log = spy(log);
		accessor.setPropertyValue("messageLogger", log);
		when(log.isInfoEnabled()).thenReturn(true);
		loggingHandler.handleMessage(new GenericMessage<String>("foo"));
		verify(log, times(1)).info(Mockito.anyString());
		verify(log, never()).warn(Mockito.anyString());

		loggingHandler.setLevel(Level.WARN);
		loggingHandler.handleMessage(new GenericMessage<String>("foo"));
		verify(log, times(1)).info(Mockito.anyString());
		verify(log, times(1)).warn(Mockito.anyString());
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
