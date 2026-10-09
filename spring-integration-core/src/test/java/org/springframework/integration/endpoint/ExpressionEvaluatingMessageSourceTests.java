/*
 * Copyright 2002-present the original author or authors.
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
