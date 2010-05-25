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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.TypeSafeMatcher;
import org.junit.rules.ExpectedException;
import org.springframework.expression.EvaluationException;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.StringMessage;

/**
 * @author Dave Syer
 * @since 2.0
 *
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
	public void testProcessMessageWithDollar() {
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("headers['$id']");
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
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("payload.error()");
		assertEquals("foo", processor.processMessage(new GenericMessage<ExpressionEvaluatingMessageProcessorTests>(this)));
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
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("payload.check()");
		assertEquals("foo", processor.processMessage(new GenericMessage<ExpressionEvaluatingMessageProcessorTests>(this)));
	}

	public String error() {
		throw new UnsupportedOperationException("Expected test exception");
	}

	public String check() throws Exception {
		throw new CheckedException("Expected test exception");
	}

	@SuppressWarnings("serial")
	private static final class CheckedException extends Exception {
		public CheckedException(String string) {
			super(string);
		}
	}

}
