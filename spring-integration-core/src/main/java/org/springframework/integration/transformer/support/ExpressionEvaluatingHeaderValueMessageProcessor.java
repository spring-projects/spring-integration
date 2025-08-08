/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.transformer.support;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.messaging.Message;

/**
 * @param <T> ther paylaod type.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 3.0
 */
public class ExpressionEvaluatingHeaderValueMessageProcessor<T> extends AbstractHeaderValueMessageProcessor<T>
		implements BeanFactoryAware {

	private static final ExpressionParser EXPRESSION_PARSER =
			new SpelExpressionParser(new SpelParserConfiguration(true, true));

	private final ExpressionEvaluatingMessageProcessor<T> targetProcessor;

	/**
	 * Create a header value processor for the given Expression and the
	 * expected type of the expression evaluation result. The expectedType
	 * may be null if unknown.
	 * @param expression the {@link Expression} to evaluate.
	 * @param expectedType the type for return value of {@code expression} evaluation result.
	 */
	public ExpressionEvaluatingHeaderValueMessageProcessor(Expression expression, Class<T> expectedType) {
		this.targetProcessor = new ExpressionEvaluatingMessageProcessor<T>(expression, expectedType);
	}

	/**
	 * Create a header value processor for the given expression string and
	 * the expected type of the expression evaluation result. The
	 * expectedType may be null if unknown.
	 * @param expressionString the {@link java.lang.String} expression presentation to evaluate.
	 * @param expectedType the type for return value of {@code expression} evaluation result.
	 */
	public ExpressionEvaluatingHeaderValueMessageProcessor(String expressionString, Class<T> expectedType) {
		Expression expression = EXPRESSION_PARSER.parseExpression(expressionString);
		this.targetProcessor = new ExpressionEvaluatingMessageProcessor<T>(expression, expectedType);
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.targetProcessor.setBeanFactory(beanFactory);
	}

	public T processMessage(Message<?> message) {
		return this.targetProcessor.processMessage(message);
	}

}
