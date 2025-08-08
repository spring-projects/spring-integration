/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
