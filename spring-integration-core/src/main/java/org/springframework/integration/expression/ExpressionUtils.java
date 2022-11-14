/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.expression;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
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
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

/**
 * Utility class with static methods for helping with evaluation of SpEL expressions.
 *
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.2
 */
public final class ExpressionUtils {

	private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	private static final Log LOGGER = LogFactory.getLog(ExpressionUtils.class);

	private ExpressionUtils() {
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
	public static StandardEvaluationContext createStandardEvaluationContext(@Nullable BeanFactory beanFactory) {
		if (beanFactory == null) {
			LOGGER.warn("Creating EvaluationContext with no beanFactory", new RuntimeException("No beanFactory"));
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
	public static SimpleEvaluationContext createSimpleEvaluationContext(@Nullable BeanFactory beanFactory) {
		if (beanFactory == null) {
			LOGGER.warn("Creating EvaluationContext with no beanFactory", new RuntimeException("No beanFactory"));
		}
		return (SimpleEvaluationContext) doCreateContext(beanFactory, true);
	}

	private static EvaluationContext doCreateContext(@Nullable BeanFactory beanFactory, boolean simple) {
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
	private static EvaluationContext createEvaluationContext(@Nullable ConversionService conversionService,
			@Nullable BeanFactory beanFactory, boolean simple) {

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
	 * @param propertyName the property name the expression is evaluated for.
	 * @return the File.
	 * @since 5.0
	 */
	public static File expressionToFile(Expression expression, EvaluationContext evaluationContext,
			@Nullable Message<?> message, String propertyName) {

		Object value =
				message == null
						? expression.getValue(evaluationContext)
						: expression.getValue(evaluationContext, message);

		Assert.state(value != null, () ->
				String.format("The provided %s expression (%s) must not evaluate to null.",
						propertyName, expression.getExpressionString()));

		if (value instanceof File) {
			return (File) value;
		}
		else if (value instanceof String) {
			String path = (String) value;
			Assert.hasText(path, String.format("Unable to resolve %s for the provided Expression '%s'.", propertyName,
					expression.getExpressionString()));
			try {
				return ResourceUtils.getFile(path);
			}
			catch (FileNotFoundException ex) {
				throw new IllegalStateException(
						String.format("Unable to resolve %s for the provided Expression '%s'.",
								propertyName, expression.getExpressionString()),
						ex);
			}
		}
		else if (value instanceof Resource) {
			try {
				return ((Resource) value).getFile();
			}
			catch (IOException ex) {
				throw new IllegalStateException(
						String.format("Unable to resolve %s for the provided Expression '%s'.",
								propertyName, expression.getExpressionString()),
						ex);
			}
		}
		else {
			throw new IllegalStateException(String.format(
					"The provided %s expression (%s) must evaluate to type java.io.File, String " +
							"or org.springframework.core.io.Resource, not %s.", propertyName,
					expression.getExpressionString(), value.getClass().getName()));
		}
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
