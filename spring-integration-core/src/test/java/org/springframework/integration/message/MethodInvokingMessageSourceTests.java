/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.message;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class MethodInvokingMessageSourceTests {

	@Test
	public void testValidMethod() {
		MethodInvokingMessageSource source = new MethodInvokingMessageSource();
		source.setBeanFactory(mock(BeanFactory.class));
		source.setObject(new TestBean());
		source.setMethodName("validMethod");
		Message<?> result = source.receive();
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isNotNull();
		assertThat(result.getPayload()).isEqualTo("valid");
	}

	@Test
	public void testHeaderExpressions() {
		Map<String, Expression> headerExpressions = new HashMap<String, Expression>();
		headerExpressions.put("foo", new LiteralExpression("abc"));
		headerExpressions.put("bar", new SpelExpressionParser().parseExpression("new Integer(123)"));
		MethodInvokingMessageSource source = new MethodInvokingMessageSource();
		source.setBeanFactory(mock(BeanFactory.class));
		source.setObject(new TestBean());
		source.setMethodName("validMethod");
		source.setHeaderExpressions(headerExpressions);
		Message<?> result = source.receive();
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isNotNull();
		assertThat(result.getPayload()).isEqualTo("valid");
		assertThat(result.getHeaders().get("foo")).isEqualTo("abc");
		assertThat(result.getHeaders().get("bar")).isEqualTo(123);
	}

	@Test(expected = MessagingException.class)
	public void testNoMatchingMethodName() {
		MethodInvokingMessageSource source = new MethodInvokingMessageSource();
		source.setBeanFactory(mock(BeanFactory.class));
		source.setObject(new TestBean());
		source.setMethodName("noMatchingMethod");
		source.receive();
	}

	@Test(expected = MessagingException.class)
	public void testInvalidMethodWithArg() {
		MethodInvokingMessageSource source = new MethodInvokingMessageSource();
		source.setBeanFactory(mock(BeanFactory.class));
		source.setObject(new TestBean());
		source.setMethodName("invalidMethodWithArg");
		source.receive();
	}

	@Test(expected = MessagingException.class)
	public void testInvalidMethodWithNoReturnValue() {
		MethodInvokingMessageSource source = new MethodInvokingMessageSource();
		source.setBeanFactory(mock(BeanFactory.class));
		source.setObject(new TestBean());
		source.setMethodName("invalidMethodWithNoReturnValue");
		source.receive();
	}

	@Test
	public void testNullReturningMethodReturnsNullMessage() {
		MethodInvokingMessageSource source = new MethodInvokingMessageSource();
		source.setBeanFactory(mock(BeanFactory.class));
		source.setObject(new TestBean());
		source.setMethodName("nullReturningMethod");
		Message<?> message = source.receive();
		assertThat(message).isNull();
	}

	@SuppressWarnings("unused")
	private static class TestBean {

		TestBean() {
			super();
		}

		public String validMethod() {
			return "valid";
		}

		public String invalidMethodWithArg(String arg) {
			return "invalid";
		}

		public void invalidMethodWithNoReturnValue() {
		}

		public Object nullReturningMethod() {
			return null;
		}

	}

}
