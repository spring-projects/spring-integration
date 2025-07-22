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

package org.springframework.integration.message;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.handler.ExpressionEvaluatingMessageHandler;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.1
 */
public class ExpressionEvaluatingMessageHandlerTests implements TestApplicationContextAware {

	private ExpressionParser parser;

	@BeforeEach
	public void setup() {
		parser = new SpelExpressionParser();
	}

	@Test
	public void validExpression() {
		Expression expression = parser.parseExpression("T(System).out.println(payload)");
		ExpressionEvaluatingMessageHandler handler = new ExpressionEvaluatingMessageHandler(expression);
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<String>("test"));
	}

	@Test
	public void validExpressionWithNoArgs() {
		Expression expression = parser.parseExpression("T(System).out.println()");
		ExpressionEvaluatingMessageHandler handler = new ExpressionEvaluatingMessageHandler(expression);
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<String>("test"));
	}

	@Test
	public void validExpressionWithSomeArgs() {
		Expression expression = parser.parseExpression("T(System).out.write(payload.bytes, 0, headers.offset)");
		ExpressionEvaluatingMessageHandler handler = new ExpressionEvaluatingMessageHandler(expression);
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();
		HashMap<String, Object> headers = new HashMap<String, Object>();
		headers.put("offset", 4);
		handler.handleMessage(new GenericMessage<String>("testtest", headers));
	}

	@Test
	public void expressionWithReturnValue() {
		Message<?> message = new GenericMessage<Float>(.1f);
		Expression expression = parser.parseExpression("T(System).out.printf('$%4.2f', payload)");
		ExpressionEvaluatingMessageHandler handler = new ExpressionEvaluatingMessageHandler(expression);
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> handler.handleMessage(message))
				.extracting(MessagingException::getFailedMessage)
				.isEqualTo(message);
	}

}
