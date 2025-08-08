/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;

/**
 * A Message Router implementation that evaluates the specified SpEL
 * expression. The result of evaluation will typically be a String to be
 * resolved to a channel name or a Collection (or Array) of strings.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class ExpressionEvaluatingRouter extends AbstractMessageProcessingRouter {

	/**
	 * Construct an instance by parsing the supplied expression string.
	 * @param expressionString the expression string.
	 */
	public ExpressionEvaluatingRouter(String expressionString) {
		this(EXPRESSION_PARSER.parseExpression(expressionString));
	}

	/**
	 * Construct an instance with the supplied {@link Expression}.
	 * @param expression the expression.
	 */
	public ExpressionEvaluatingRouter(Expression expression) {
		super(new ExpressionEvaluatingMessageProcessor<Object>(expression));
		setPrimaryExpression(expression);
	}

}
