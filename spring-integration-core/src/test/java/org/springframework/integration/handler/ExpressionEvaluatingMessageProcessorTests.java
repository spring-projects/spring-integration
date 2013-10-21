/*
 * Copyright 2002-2013 the original author or authors.
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
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.config.IntegrationEvaluationContextFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class ExpressionEvaluatingMessageProcessorTests {

	private static final Log logger = LogFactory.getLog(ExpressionEvaluatingMessageProcessorTests.class);

	private static final ExpressionParser expressionParser = new SpelExpressionParser(new SpelParserConfiguration(true, true));


	@Rule
	public ExpectedException expected = ExpectedException.none();


	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testProcessMessage() {
		Expression expression = expressionParser.parseExpression("payload");
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor(expression);
		processor.setBeanFactory(mock(BeanFactory.class));
		assertEquals("foo", processor.processMessage(new GenericMessage<String>("foo")));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testProcessMessageWithParameterCoercion() throws Exception {
		@SuppressWarnings("unused")
		class TestTarget {
			public String stringify(int number) {
				return number+"";
			}
		}
		Expression expression = expressionParser.parseExpression("#target.stringify(payload)");
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor(expression);
		processor.setBeanFactory(mock(BeanFactory.class));
		processor.afterPropertiesSet();
		EvaluationContext evaluationContext = TestUtils.getPropertyValue(processor, "evaluationContext", EvaluationContext.class);
		evaluationContext.setVariable("target", new TestTarget());
		assertEquals("2", processor.processMessage(new GenericMessage<String>("2")));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testProcessMessageWithVoidResult() throws Exception {
		@SuppressWarnings("unused")
		class TestTarget {
			public void ping(String input) {
			}
		}
		Expression expression = expressionParser.parseExpression("#target.ping(payload)");
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor(expression);
		processor.setBeanFactory(mock(BeanFactory.class));
		processor.afterPropertiesSet();
		EvaluationContext evaluationContext = TestUtils.getPropertyValue(processor, "evaluationContext", EvaluationContext.class);
		evaluationContext.setVariable("target", new TestTarget());
		assertEquals(null, processor.processMessage(new GenericMessage<String>("2")));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testProcessMessageWithParameterCoercionToNonPrimitive() throws Exception {
		class TestTarget {
			@SuppressWarnings("unused")
			public String find(Resource[] resources) {
				return Arrays.asList(resources).toString();
			}

		}
		Expression expression = expressionParser.parseExpression("#target.find(payload)");
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor(expression);
		AbstractApplicationContext applicationContext = new GenericApplicationContext();
		processor.setBeanFactory(applicationContext);
		IntegrationEvaluationContextFactoryBean factoryBean = new IntegrationEvaluationContextFactoryBean();
		factoryBean.setApplicationContext(applicationContext);
		applicationContext.getBeanFactory().registerSingleton(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				factoryBean.getObject());
		applicationContext.refresh();

		processor.afterPropertiesSet();
		EvaluationContext evaluationContext = TestUtils.getPropertyValue(processor, "evaluationContext", EvaluationContext.class);
		evaluationContext.setVariable("target", new TestTarget());
		String result = (String) processor.processMessage(new GenericMessage<String>("classpath*:*.properties"));
		assertTrue("Wrong result: "+result, result.contains("log4j.properties"));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testProcessMessageWithDollarInBrackets() {
		Expression expression = expressionParser.parseExpression("headers['$foo_id']");
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor(expression);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("foo").setHeader("$foo_id", "abc").build();
		assertEquals("abc", processor.processMessage(message));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testProcessMessageWithDollarPropertyAccess() {
		Expression expression = expressionParser.parseExpression("headers.$foo_id");
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor(expression);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("foo").setHeader("$foo_id", "xyz").build();
		assertEquals("xyz", processor.processMessage(message));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testProcessMessageWithStaticKey() {
		Expression expression = expressionParser.parseExpression("headers[headers.ID]");
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor(expression);
		processor.setBeanFactory(mock(BeanFactory.class));
		GenericMessage<String> message = new GenericMessage<String>("foo");
		assertEquals(message.getHeaders().getId(), processor.processMessage(message));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testProcessMessageWithBeanAsMethodArgument() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("bar");
		context.registerBeanDefinition("testString", beanDefinition);
		context.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				new RootBeanDefinition(IntegrationEvaluationContextFactoryBean.class));
		context.refresh();

		Expression expression = expressionParser.parseExpression("payload.concat(@testString)");
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor(expression);
		processor.setBeanFactory(context);
		processor.afterPropertiesSet();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		assertEquals("foobar", processor.processMessage(message));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testProcessMessageWithMethodCallOnBean() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("bar");
		context.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				new RootBeanDefinition(IntegrationEvaluationContextFactoryBean.class));
		context.registerBeanDefinition("testString", beanDefinition);
		context.refresh();

		Expression expression = expressionParser.parseExpression("@testString.concat(payload)");
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor(expression);
		processor.setBeanFactory(context);
		processor.afterPropertiesSet();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		assertEquals("barfoo", processor.processMessage(message));
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
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
		Expression expression = expressionParser.parseExpression("payload.fixMe()");
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor(expression);
		processor.setBeanFactory(mock(BeanFactory.class));
		assertEquals("foo", processor.processMessage(new GenericMessage<String>("foo")));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
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
		Expression expression = expressionParser.parseExpression("payload.throwRuntimeException()");
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor(expression);
		processor.setBeanFactory(mock(BeanFactory.class));
		assertEquals("foo", processor.processMessage(new GenericMessage<TestPayload>(new TestPayload())));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
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
		Expression expression = expressionParser.parseExpression("payload.throwCheckedException()");
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor(expression);
		processor.setBeanFactory(mock(BeanFactory.class));
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
