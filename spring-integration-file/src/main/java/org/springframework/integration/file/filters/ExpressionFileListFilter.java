/*
 * Copyright 2017-2021 the original author or authors.
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
