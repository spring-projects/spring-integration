/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.util.Assert;

/**
 * A SpEL expression based {@link AbstractFileListFilter} implementation.
 *
 * @param <F> The type that will be filtered.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class ExpressionFileListFilter<F> extends AbstractFileListFilter<F>
		implements BeanFactoryAware {

	private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	private final Expression expression;

	private BeanFactory beanFactory;

	private EvaluationContext evaluationContext;

	public ExpressionFileListFilter(String expression) {
		this(EXPRESSION_PARSER.parseExpression(expression));
		Assert.hasText(expression, "'expression' must not be empty");
	}

	public ExpressionFileListFilter(Expression expression) {
		Assert.notNull(expression, "'expression' must not be null");
		this.expression = expression;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public boolean accept(F file) {
		Boolean pass = this.expression.getValue(getEvaluationContext(), file, Boolean.class);
		return pass == null ? false : pass;
	}

	private EvaluationContext getEvaluationContext() {
		if (this.evaluationContext == null) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.beanFactory);
		}
		return this.evaluationContext;
	}

}
