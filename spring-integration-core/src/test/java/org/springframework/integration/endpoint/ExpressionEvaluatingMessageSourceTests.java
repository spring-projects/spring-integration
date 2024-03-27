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

package org.springframework.integration.endpoint;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class ExpressionEvaluatingMessageSourceTests {

	@Test
	public void literalExpression() {
		Expression expression = new LiteralExpression("foo");
		ExpressionEvaluatingMessageSource<String> source =
				new ExpressionEvaluatingMessageSource<String>(expression, String.class);
		source.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = source.receive();
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
	}

	@Test(expected = ConversionFailedException.class)
	public void unexpectedType() {
		Expression expression = new LiteralExpression("foo");
		ExpressionEvaluatingMessageSource<Integer> source =
				new ExpressionEvaluatingMessageSource<Integer>(expression, Integer.class);
		source.setBeanFactory(mock(BeanFactory.class));
		source.receive();
	}

}
