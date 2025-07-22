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

package org.springframework.integration.endpoint;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class ExpressionEvaluatingMessageSourceTests implements TestApplicationContextAware {

	@Test
	public void literalExpression() {
		Expression expression = new LiteralExpression("foo");
		ExpressionEvaluatingMessageSource<String> source =
				new ExpressionEvaluatingMessageSource<String>(expression, String.class);
		source.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		Message<?> message = source.receive();
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
	}

	@Test
	public void unexpectedType() {
		Expression expression = new LiteralExpression("foo");
		ExpressionEvaluatingMessageSource<Integer> source =
				new ExpressionEvaluatingMessageSource<Integer>(expression, Integer.class);
		source.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThatThrownBy(() -> source.receive())
				.isInstanceOf(ConversionFailedException.class);
	}

}
