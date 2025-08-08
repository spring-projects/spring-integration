/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.filter;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;

/**
 * A {@link org.springframework.integration.core.MessageSelector} implementation that
 * evaluates a SpEL expression. The evaluation result of the expression must be a boolean
 * value.
 *
 * @author Mark Fisher
 * @author Liujiong
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ExpressionEvaluatingSelector extends AbstractMessageProcessingSelector {

	private static final ExpressionParser EXPRESSION_PARSER =
			new SpelExpressionParser(new SpelParserConfiguration(true, true));

	private final String expressionString;

	public ExpressionEvaluatingSelector(String expressionString) {
		super(new ExpressionEvaluatingMessageProcessor<Boolean>(EXPRESSION_PARSER.parseExpression(expressionString),
				Boolean.class));
		this.expressionString = expressionString;
	}

	public ExpressionEvaluatingSelector(Expression expression) {
		super(new ExpressionEvaluatingMessageProcessor<Boolean>(expression, Boolean.class));
		this.expressionString = expression.getExpressionString();
	}

	public String getExpressionString() {
		return this.expressionString;
	}

	@Override
	public String toString() {
		return "ExpressionEvaluatingSelector for: [" + this.expressionString + "]";
	}

}
