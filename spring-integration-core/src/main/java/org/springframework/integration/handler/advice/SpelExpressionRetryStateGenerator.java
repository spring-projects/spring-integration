/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.handler.advice;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.classify.Classifier;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.messaging.Message;
import org.springframework.integration.expression.ExpressionUtils;
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

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(beanFactory);
	}

	public void setClassifier(Classifier<? super Throwable, Boolean> classifier) {
		this.classifier = classifier;
	}

	public RetryState determineRetryState(Message<?> message) {
		return new DefaultRetryState(this.keyExpression.getValue(this.evaluationContext, message),
				this.forceRefreshExpression == null ? false :
					this.forceRefreshExpression.getValue(this.evaluationContext, message, Boolean.class),
				this.classifier);
	}
}
