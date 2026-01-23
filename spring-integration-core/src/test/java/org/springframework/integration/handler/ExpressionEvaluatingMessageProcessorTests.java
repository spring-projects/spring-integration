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

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

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
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.config.IntegrationEvaluationContextFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.0
 */
public class ExpressionEvaluatingMessageProcessorTests implements TestApplicationContextAware {

	private static final ExpressionParser expressionParser = new SpelExpressionParser();

	@Test
	public void testProcessMessage() {
		Expression expression = expressionParser.parseExpression("payload");
		ExpressionEvaluatingMessageProcessor<String> processor =
				new ExpressionEvaluatingMessageProcessor<>(expression);
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThat(processor.processMessage(new GenericMessage<>("foo"))).isEqualTo("foo");
	}

	@Test
	public void testProcessMessageWithParameterCoercion() {
		@SuppressWarnings("unused")
		class TestTarget {

			public String stringify(int number) {
				return number + "";
			}

		}

		Expression expression = expressionParser.parseExpression("#target.stringify(payload)");
		ExpressionEvaluatingMessageProcessor<String> processor =
				new ExpressionEvaluatingMessageProcessor<>(expression);
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		processor.afterPropertiesSet();
		EvaluationContext evaluationContext =
				TestUtils.<EvaluationContext>getPropertyValue(processor, "evaluationContext");
		evaluationContext.setVariable("target", new TestTarget());
		assertThat(processor.processMessage(new GenericMessage<>("2"))).isEqualTo("2");
	}

	@Test
	public void testProcessMessageWithVoidResult() {
		@SuppressWarnings("unused")
		class TestTarget {

			public void ping(String input) {
			}

		}

		Expression expression = expressionParser.parseExpression("#target.ping(payload)");
		ExpressionEvaluatingMessageProcessor<String> processor =
				new ExpressionEvaluatingMessageProcessor<>(expression);
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		processor.afterPropertiesSet();
		EvaluationContext evaluationContext =
				TestUtils.<EvaluationContext>getPropertyValue(processor, "evaluationContext");
		evaluationContext.setVariable("target", new TestTarget());
		assertThat(processor.processMessage(new GenericMessage<>("2"))).isEqualTo(null);
	}

	@Test
	public void testProcessMessageWithParameterCoercionToNonPrimitive() {
		class TestTarget {

			@SuppressWarnings("unused")
			public String find(Resource[] resources) {
				return Arrays.toString(resources);
			}

		}

		Expression expression = expressionParser.parseExpression("#target.find(payload)");
		ExpressionEvaluatingMessageProcessor<String> processor =
				new ExpressionEvaluatingMessageProcessor<>(expression);
		AbstractApplicationContext applicationContext = new GenericApplicationContext();
		processor.setBeanFactory(applicationContext);
		IntegrationEvaluationContextFactoryBean factoryBean = new IntegrationEvaluationContextFactoryBean();
		factoryBean.setApplicationContext(applicationContext);
		applicationContext.getBeanFactory()
				.registerSingleton(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
						factoryBean.getObject());
		applicationContext.refresh();

		processor.afterPropertiesSet();
		EvaluationContext evaluationContext =
				TestUtils.<EvaluationContext>getPropertyValue(processor, "evaluationContext");
		evaluationContext.setVariable("target", new TestTarget());
		String result = processor.processMessage(new GenericMessage<>("classpath*:*-test.xml"));
		assertThat(result).contains("log4j2-test.xml");
	}

	@Test
	public void testProcessMessageWithDollarInBrackets() {
		Expression expression = expressionParser.parseExpression("headers['$foo_id']");
		ExpressionEvaluatingMessageProcessor<String> processor =
				new ExpressionEvaluatingMessageProcessor<>(expression);
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		Message<String> message = MessageBuilder.withPayload("foo").setHeader("$foo_id", "abc").build();
		assertThat(processor.processMessage(message)).isEqualTo("abc");
	}

	@Test
	public void testProcessMessageWithDollarPropertyAccess() {
		Expression expression = expressionParser.parseExpression("headers.$foo_id");
		ExpressionEvaluatingMessageProcessor<String> processor =
				new ExpressionEvaluatingMessageProcessor<>(expression);
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		Message<String> message = MessageBuilder.withPayload("foo").setHeader("$foo_id", "xyz").build();
		assertThat(processor.processMessage(message)).isEqualTo("xyz");
	}

	@Test
	public void testProcessMessageWithStaticKey() {
		Expression expression = expressionParser.parseExpression("headers[headers.ID]");
		ExpressionEvaluatingMessageProcessor<UUID> processor = new ExpressionEvaluatingMessageProcessor<>(expression);
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		GenericMessage<String> message = new GenericMessage<>("foo");
		assertThat(processor.processMessage(message)).isEqualTo(message.getHeaders().getId());
	}

	@Test
	public void testProcessMessageWithBeanAsMethodArgument() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("bar");
		context.registerBeanDefinition("testString", beanDefinition);
		context.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				new RootBeanDefinition(IntegrationEvaluationContextFactoryBean.class));
		context.refresh();

		Expression expression = expressionParser.parseExpression("payload.concat(@testString)");
		ExpressionEvaluatingMessageProcessor<String> processor =
				new ExpressionEvaluatingMessageProcessor<>(expression);
		processor.setBeanFactory(context);
		processor.afterPropertiesSet();
		GenericMessage<String> message = new GenericMessage<>("foo");
		assertThat(processor.processMessage(message)).isEqualTo("foobar");
	}

	@Test
	public void testProcessMessageWithMethodCallOnBean() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("bar");
		context.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				new RootBeanDefinition(IntegrationEvaluationContextFactoryBean.class));
		context.registerBeanDefinition("testString", beanDefinition);
		context.refresh();

		Expression expression = expressionParser.parseExpression("@testString.concat(payload)");
		ExpressionEvaluatingMessageProcessor<String> processor =
				new ExpressionEvaluatingMessageProcessor<>(expression);
		processor.setBeanFactory(context);
		processor.afterPropertiesSet();
		GenericMessage<String> message = new GenericMessage<>("foo");
		assertThat(processor.processMessage(message)).isEqualTo("barfoo");
	}

	@Test
	public void testProcessMessageBadExpression() {
		Expression expression = expressionParser.parseExpression("payload.fixMe()");
		ExpressionEvaluatingMessageProcessor<String> processor =
				new ExpressionEvaluatingMessageProcessor<>(expression);
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> processor.processMessage(new GenericMessage<>("foo")))
				.withCauseInstanceOf(EvaluationException.class);
	}

	@Test
	public void testProcessMessageExpressionThrowsRuntimeException() {
		Expression expression = expressionParser.parseExpression("payload.throwRuntimeException()");
		ExpressionEvaluatingMessageProcessor<String> processor =
				new ExpressionEvaluatingMessageProcessor<>(expression);
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> processor.processMessage(new GenericMessage<>(new TestPayload())))
				.withCauseInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	public void testProcessMessageExpressionThrowsCheckedException() {
		Expression expression = expressionParser.parseExpression("payload.throwCheckedException()");
		ExpressionEvaluatingMessageProcessor<String> processor =
				new ExpressionEvaluatingMessageProcessor<>(expression);
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> processor.processMessage(new GenericMessage<>(new TestPayload())))
				.withCauseInstanceOf(CheckedException.class);
	}

	@SuppressWarnings("unused")
	private static class TestPayload {

		TestPayload() {
			super();
		}

		public String throwRuntimeException() {
			throw new UnsupportedOperationException("Expected test exception");
		}

		public String throwCheckedException() throws Exception {
			throw new CheckedException("Expected test exception");
		}

	}

	@SuppressWarnings("serial")
	private static final class CheckedException extends Exception {

		CheckedException(String string) {
			super(string);
		}

	}

}
