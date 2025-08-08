/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.handler.advice;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.classify.Classifier;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.messaging.Message;
import org.springframework.retry.RetryState;
import org.springframework.retry.support.DefaultRetryState;
import org.springframework.util.Assert;

/**
 * Creates a DefaultRetryState from a {@link Message}.
 * @author Gary Russell
 * @since 2.2
 *
 */
public class SpelExpressionRetryStateGenerator implements RetryStateGenerator, BeanFactoryAware {

	private volatile StandardEvaluationContext evaluationContext;

	private final Expression keyExpression;

	private final Expression forceRefreshExpression;

	private volatile Classifier<? super Throwable, Boolean> classifier;

	public SpelExpressionRetryStateGenerator(String keyExpression) {
		this(keyExpression, null);
	}

	public SpelExpressionRetryStateGenerator(String keyExpression, String forceRefreshExpression) {
		Assert.notNull(keyExpression, "keyExpression must not be null");
		this.keyExpression = new SpelExpressionParser().parseExpression(keyExpression);
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext();
		if (forceRefreshExpression == null) {
			this.forceRefreshExpression = null;
		}
		else {
			this.forceRefreshExpression = new SpelExpressionParser().parseExpression(forceRefreshExpression);
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(beanFactory);
	}

	public void setClassifier(Classifier<? super Throwable, Boolean> classifier) {
		this.classifier = classifier;
	}

	@Override
	public RetryState determineRetryState(Message<?> message) {
		Boolean forceRefresh = this.forceRefreshExpression == null
				? Boolean.FALSE
				: this.forceRefreshExpression.getValue(this.evaluationContext, message, Boolean.class);
		return new DefaultRetryState(this.keyExpression.getValue(this.evaluationContext, message),
				forceRefresh == null ? false : forceRefresh,
				this.classifier);
	}

}
