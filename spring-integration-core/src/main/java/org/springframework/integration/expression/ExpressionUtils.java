/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.expression;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.DataBindingPropertyAccessor;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.spel.support.SimpleEvaluationContext.Builder;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Utility class with static methods for helping with evaluation of SpEL expressions.
 *
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.2
 */
public final class ExpressionUtils {

	private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	private static final Log logger = LogFactory.getLog(ExpressionUtils.class);

	private ExpressionUtils() {
		super();
	}

	/**
	 * Used to create a context with no BeanFactory, usually in tests.
	 * @return The evaluation context.
	 */
	public static StandardEvaluationContext createStandardEvaluationContext() {
		return (StandardEvaluationContext) doCreateContext(null, false);
	}

	/**
	 * Used to create a context with no BeanFactory, usually in tests.
	 * @return The evaluation context.
	 * @since 4.3.15
	 */
	public static SimpleEvaluationContext createSimpleEvaluationContext() {
		return (SimpleEvaluationContext) doCreateContext(null, true);
	}

	/**
	 * Obtains the context from the beanFactory if not null; emits a warning if the beanFactory
	 * is null.
	 * @param beanFactory The bean factory.
	 * @return The evaluation context.
	 */
	public static StandardEvaluationContext createStandardEvaluationContext(BeanFactory beanFactory) {
		if (beanFactory == null) {
			logger.warn("Creating EvaluationContext with no beanFactory", new RuntimeException("No beanFactory"));
		}
		return (StandardEvaluationContext) doCreateContext(beanFactory, false);
	}

	/**
	 * Obtains the context from the beanFactory if not null; emits a warning if the beanFactory
	 * is null.
	 * @param beanFactory The bean factory.
	 * @return The evaluation context.
	 * @since 4.3.15
	 */
	public static SimpleEvaluationContext createSimpleEvaluationContext(BeanFactory beanFactory) {
		if (beanFactory == null) {
			logger.warn("Creating EvaluationContext with no beanFactory", new RuntimeException("No beanFactory"));
		}
		return (SimpleEvaluationContext) doCreateContext(beanFactory, true);
	}

	private static EvaluationContext doCreateContext(BeanFactory beanFactory, boolean simple) {
		ConversionService conversionService = null;
		EvaluationContext evaluationContext = null;
		if (beanFactory != null) {
			evaluationContext =
					simple
							? IntegrationContextUtils.getSimpleEvaluationContext(beanFactory)
							: IntegrationContextUtils.getEvaluationContext(beanFactory);
		}
		if (evaluationContext == null) {
			if (beanFactory != null) {
				conversionService = IntegrationUtils.getConversionService(beanFactory);
			}
			evaluationContext = createEvaluationContext(conversionService, beanFactory, simple);
		}
		return evaluationContext;
	}

	/**
	 * Create a {@link StandardEvaluationContext} with a {@link MapAccessor} in its
	 * property accessor property and the supplied {@link ConversionService} in its
	 * conversionService property.
	 * @param conversionService the conversion service.
	 * @param beanFactory the bean factory.
	 * @param simple true if simple.
	 * @return the evaluation context.
	 */
	private static EvaluationContext createEvaluationContext(ConversionService conversionService,
			BeanFactory beanFactory, boolean simple) {

		if (simple) {
			Builder ecBuilder = SimpleEvaluationContext.forPropertyAccessors(
					new MapAccessor(), DataBindingPropertyAccessor.forReadOnlyAccess())
					.withInstanceMethods();
			if (conversionService != null) {
				ecBuilder.withConversionService(conversionService);
			}
			return ecBuilder.build();
		}
		else {
			StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
			evaluationContext.addPropertyAccessor(new MapAccessor());
			if (conversionService != null) {
				evaluationContext.setTypeConverter(new StandardTypeConverter(conversionService));
			}
			if (beanFactory != null) {
				evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
			}
			return evaluationContext;
		}
	}

	/**
	 * Evaluate an expression and return a {@link File} object; the expression can evaluate
	 * to a {@link String} or {@link File}.
	 * @param expression the expression.
	 * @param evaluationContext the evaluation context.
	 * @param message the message (if available).
	 * @param name the name of the result of the evaluation.
	 * @return the File.
	 * @since 5.0
	 */
	public static File expressionToFile(Expression expression, EvaluationContext evaluationContext, Message<?> message,
			String name) {
		File file;
		Object value = expression.getValue(evaluationContext, message);
		if (value == null) {
			throw new IllegalStateException(String.format("The provided %s expression (%s) must not evaluate to null.",
					name, expression.getExpressionString()));
		}
		else if (value instanceof File) {
			file = (File) value;
		}
		else if (value instanceof String) {
			String path = (String) value;
			Assert.hasText(path, String.format("Unable to resolve %s for the provided Expression '%s'.", name,
					expression.getExpressionString()));
			file = new File(path);
		}
		else {
			throw new IllegalStateException(String.format(
					"The provided %s expression (%s) must evaluate to type java.io.File or String, not %s.", name,
					expression.getExpressionString(), value.getClass().getName()));
		}
		return file;
	}

	/**
	 * Return a {@link ValueExpression} for a simple literal, otherwise
	 * a {@link org.springframework.expression.spel.standard.SpelExpression}.
	 * @param expression the expression string.
	 * @return the expression.
	 * @since 5.0
	 */
	public static Expression intExpression(String expression) {
		try {
			return new ValueExpression<>(Integer.parseInt(expression));
		}
		catch (NumberFormatException e) {
			// empty
		}
		return EXPRESSION_PARSER.parseExpression(expression);
	}

	/**
	 * Return a {@link ValueExpression} for a simple literal, otherwise
	 * a {@link org.springframework.expression.spel.standard.SpelExpression}.
	 * @param expression the expression string.
	 * @return the expression.
	 * @since 5.0
	 */
	public static Expression longExpression(String expression) {
		try {
			return new ValueExpression<>(Long.parseLong(expression));
		}
		catch (NumberFormatException e) {
			// empty
		}
		return EXPRESSION_PARSER.parseExpression(expression);
	}

}
