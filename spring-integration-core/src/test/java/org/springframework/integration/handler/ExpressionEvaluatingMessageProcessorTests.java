/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.TypeSafeMatcher;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.expression.EvaluationException;
import org.springframework.integration.core.GenericMessage;
import org.springframework.integration.core.StringMessage;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @since 2.0
 */
public class ExpressionEvaluatingMessageProcessorTests {

	private static final Log logger = LogFactory.getLog(ExpressionEvaluatingMessageProcessorTests.class);


	@Rule
	public ExpectedException expected = ExpectedException.none();


	@Test
	public void testProcessMessage() {
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("payload");
		assertEquals("foo", processor.processMessage(new StringMessage("foo")));
	}

	@Test
	public void testProcessMessageWithParameterCoercion() {
		@SuppressWarnings("unused")
		class TestTarget {
			public String stringify(int number) {
				return number+"";
			}
		}
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("#target.stringify(payload)");
		processor.getEvaluationContext().setVariable("target", new TestTarget());
		assertEquals("2", processor.processMessage(new StringMessage("2")));
	}

	@Test
	public void testProcessMessageWithVoidResult() {
		@SuppressWarnings("unused")
		class TestTarget {
			public void ping(String input) {
			}
		}
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("#target.ping(payload)");
		processor.getEvaluationContext().setVariable("target", new TestTarget());
		assertEquals(null, processor.processMessage(new StringMessage("2")));
	}

	@Test
	public void testProcessMessageWithParameterCoercionToNonPrimitive() {
		class TestTarget {
			@SuppressWarnings("unused")
			public String find(Resource[] resources) {
				return Arrays.asList(resources).toString();
			}
			
		}
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("#target.find(payload)");
		processor.setBeanFactory(new GenericApplicationContext().getBeanFactory());
		processor.getEvaluationContext().setVariable("target", new TestTarget());
		String result = (String) processor.processMessage(new StringMessage("classpath:*.properties"));
		assertTrue("Wrong result: "+result, result.contains("log4j.properties"));
	}

	@Test
	public void testProcessMessageWithDollarInBrackets() {
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("headers['$id']");
		StringMessage message = new StringMessage("foo");
		assertEquals(message.getHeaders().getId(), processor.processMessage(message));
	}

	@Test
	public void testProcessMessageWithDollarPropertyAccess() {
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("headers.$id");
		StringMessage message = new StringMessage("foo");
		assertEquals(message.getHeaders().getId(), processor.processMessage(message));
	}

	@Test
	public void testProcessMessageWithStaticKey() {
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("headers[headers.ID]");
		StringMessage message = new StringMessage("foo");
		assertEquals(message.getHeaders().getId(), processor.processMessage(message));
	}

	@Test
	public void testProcessMessageWithBeanAsMethodArgument() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("bar");
		context.registerBeanDefinition("testString", beanDefinition);
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("payload.concat(@testString)");
		processor.setBeanFactory(context);
		StringMessage message = new StringMessage("foo");
		assertEquals("foobar", processor.processMessage(message));
	}

	@Test
	public void testProcessMessageWithMethodCallOnBean() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("bar");
		context.registerBeanDefinition("testString", beanDefinition);
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("@testString.concat(payload)");
		processor.setBeanFactory(context);
		StringMessage message = new StringMessage("foo");
		assertEquals("barfoo", processor.processMessage(message));
	}

	@Test
	public void testProcessMessageBadExpression() {
		expected.expect(new TypeSafeMatcher<Exception>(Exception.class) {
			private Throwable cause;
			@Override
			public boolean matchesSafely(Exception item) {
				logger.debug(item);
				cause = item.getCause();
				return cause instanceof EvaluationException;
			}
			public void describeTo(Description description) {
				description.appendText("cause to be EvaluationException but was ").appendValue(cause);
			}
		});
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("payload.fixMe()");
		assertEquals("foo", processor.processMessage(new StringMessage("foo")));
	}

	@Test
	public void testProcessMessageExpressionThrowsRuntimeException() {
		expected.expect(new TypeSafeMatcher<Exception>(Exception.class) {
			private Throwable cause;
			@Override
			public boolean matchesSafely(Exception item) {
				logger.debug(item);
				cause = item.getCause();
				return cause instanceof UnsupportedOperationException;
			}
			public void describeTo(Description description) {
				description.appendText("cause to be UnsupportedOperationException but was ").appendValue(cause);
			}
		});
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("payload.throwRuntimeException()");
		assertEquals("foo", processor.processMessage(new GenericMessage<TestPayload>(new TestPayload())));
	}

	@Test
	public void testProcessMessageExpressionThrowsCheckedException() {
		expected.expect(new TypeSafeMatcher<Exception>(Exception.class) {
			private Throwable cause;
			@Override
			public boolean matchesSafely(Exception item) {
				logger.debug(item);
				cause = item.getCause();
				return cause instanceof CheckedException;
			}
			public void describeTo(Description description) {
				description.appendText("cause to be CheckedException but was ").appendValue(cause);
			}
		});
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("payload.throwCheckedException()");
		assertEquals("foo", processor.processMessage(new GenericMessage<TestPayload>(new TestPayload())));
	}


	@SuppressWarnings("unused")
	private static class TestPayload {

		public String throwRuntimeException() {
			throw new UnsupportedOperationException("Expected test exception");
		}

		public String throwCheckedException() throws Exception {
			throw new CheckedException("Expected test exception");
		}
	}

	@SuppressWarnings("serial")
	private static final class CheckedException extends Exception {
		public CheckedException(String string) {
			super(string);
		}
	}
 
}
