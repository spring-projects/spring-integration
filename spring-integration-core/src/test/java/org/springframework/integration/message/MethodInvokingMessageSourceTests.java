/*
 * Copyright 2002-2024 the original author or authors.
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
