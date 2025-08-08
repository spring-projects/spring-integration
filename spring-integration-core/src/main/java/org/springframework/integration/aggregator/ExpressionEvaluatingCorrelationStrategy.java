/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link CorrelationStrategy} implementation that evaluates an expression.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class ExpressionEvaluatingCorrelationStrategy implements CorrelationStrategy, BeanFactoryAware {

	private static final ExpressionParser EXPRESSION_PARSER =
			new SpelExpressionParser(new SpelParserConfiguration(true, true));

	private final ExpressionEvaluatingMessageProcessor<Object> processor;

	public ExpressionEvaluatingCorrelationStrategy(String expressionString) {
		Assert.hasText(expressionString, "expressionString must not be empty");
		Expression expression = EXPRESSION_PARSER.parseExpression(expressionString);
		this.processor = new ExpressionEvaluatingMessageProcessor<>(expression, Object.class);
	}

	public ExpressionEvaluatingCorrelationStrategy(Expression expression) {
		this.processor = new ExpressionEvaluatingMessageProcessor<>(expression, Object.class);
	}

	public Object getCorrelationKey(Message<?> message) {
		return this.processor.processMessage(message);
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.processor.setBeanFactory(beanFactory);
	}

}
