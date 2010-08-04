/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.handler;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.context.SimpleBeanResolver;
import org.springframework.util.Assert;

/**
 * A {@link MessageProcessor} implementation that evaluates a SpEL expression
 * with the Message itself as the root object within the evaluation context.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class ExpressionEvaluatingMessageProcessor extends AbstractMessageProcessor implements BeanFactoryAware {

	private final ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));

	private final Expression expression;

	private volatile Class<?> expectedType = null;


	/**
	 * Create an {@link ExpressionEvaluatingMessageProcessor} for the given expression String.
	 */
	public ExpressionEvaluatingMessageProcessor(String expression) {
		Assert.hasLength(expression, "The expression must be non empty");
		try {
			this.expression = parser.parseExpression(expression);
			this.getEvaluationContext().addPropertyAccessor(new MapAccessor());
		}
		catch (ParseException e) {
			throw new IllegalArgumentException("Failed to parse expression.", e);
		}
	}


	/**
	 * Set the result type expected from evaluation of the expression.
	 */
	public void setExpectedType(Class<?> expectedType) {
		this.expectedType = expectedType;
	}

	/**
	 * Specify a BeanFactory in order to enable resolution via <code>@beanName</code> in the expression.
	 */
	public void setBeanFactory(final BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		if (beanFactory != null) {
			this.getEvaluationContext().setBeanResolver(new SimpleBeanResolver(beanFactory));
		}
	}

	/**
	 * Processes the Message by evaluating the expression with that Message as the
	 * root object. The expression evaluation result Object will be returned.
	 */
	public Object processMessage(Message<?> message) {
		return this.evaluateExpression(this.expression, message, this.expectedType);
	}

}
