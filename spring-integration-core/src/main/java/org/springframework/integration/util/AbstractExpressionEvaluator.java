/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.util;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.log.LogAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
public abstract class AbstractExpressionEvaluator implements BeanFactoryAware, InitializingBean {

	protected final LogAccessor logger = new LogAccessor(this.getClass()); // NOSONAR final

	protected static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	private final BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();

	private boolean simpleEvaluationContext;

	private volatile EvaluationContext evaluationContext;

	private volatile BeanFactory beanFactory;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	/**
	 * Specify a BeanFactory in order to enable resolution via <code>@beanName</code> in the expression.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.typeConverter.setBeanFactory(beanFactory);
	}

	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	public void setConversionService(ConversionService conversionService) {
		if (conversionService != null) {
			this.typeConverter.setConversionService(conversionService);
		}
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		return this.messageBuilderFactory;
	}

	/**
	 * The flag to indicate that a {@link org.springframework.expression.spel.support.SimpleEvaluationContext}
	 * must be used for expression evaluations.
	 * @param simpleEvaluationContext true to use the
	 * {@link org.springframework.expression.spel.support.SimpleEvaluationContext}
	 * @since 6.4
	 */
	public void setSimpleEvaluationContext(boolean simpleEvaluationContext) {
		this.simpleEvaluationContext = simpleEvaluationContext;
	}

	@Override
	public final void afterPropertiesSet() {
		getEvaluationContext();
		if (this.beanFactory != null) {
			this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
		}

		onInit();
	}

	protected EvaluationContext getEvaluationContext() {
		return getEvaluationContext(true);
	}

	/**
	 * Emits a WARN log if the beanFactory field is null, unless the argument is false.
	 * @param beanFactoryRequired set to {@code false} to suppress the warning.
	 * @return The evaluation context.
	 */
	protected final EvaluationContext getEvaluationContext(boolean beanFactoryRequired) {
		if (this.evaluationContext == null) {
			this.evaluationContext = obtainEvaluationContext(beanFactoryRequired);
			if (this.evaluationContext instanceof StandardEvaluationContext standardEvaluationContext) {
				standardEvaluationContext.setTypeConverter(this.typeConverter);
			}
			if (this.beanFactory != null) {
				ConversionService conversionService = IntegrationUtils.getConversionService(this.beanFactory);
				if (conversionService != null) {
					this.typeConverter.setConversionService(conversionService);
				}
			}
		}
		return this.evaluationContext;
	}

	private EvaluationContext obtainEvaluationContext(boolean beanFactoryRequired) {
		if (this.beanFactory == null && !beanFactoryRequired) {
			return
					this.simpleEvaluationContext
							? ExpressionUtils.createSimpleEvaluationContext()
							: ExpressionUtils.createStandardEvaluationContext();
		}
		else {
			return this.simpleEvaluationContext
					? ExpressionUtils.createSimpleEvaluationContext(this.beanFactory)
					: ExpressionUtils.createStandardEvaluationContext(this.beanFactory);
		}
	}

	@Nullable
	protected <T> T evaluateExpression(Expression expression, Message<?> message, @Nullable Class<T> expectedType) {
		try {
			return evaluateExpression(expression, (Object) message, expectedType);
		}
		catch (Exception ex) {
			this.logger.debug(ex, "SpEL Expression evaluation failed with Exception.");
			Throwable cause = null;
			if (ex instanceof EvaluationException) { // NOSONAR
				cause = ex.getCause();
			}
			throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(message,
					() -> "Expression evaluation failed: " + expression.getExpressionString(),
					cause == null ? ex : cause);
		}
	}

	@Nullable
	protected Object evaluateExpression(String expression, Object input) {
		return evaluateExpression(expression, input, null);
	}

	@Nullable
	protected <T> T evaluateExpression(String expression, Object input, @Nullable Class<T> expectedType) {
		return EXPRESSION_PARSER.parseExpression(expression)
				.getValue(getEvaluationContext(), input, expectedType);
	}

	@Nullable
	protected Object evaluateExpression(Expression expression, Object input) {
		return evaluateExpression(expression, input, null);
	}

	@Nullable
	protected <T> T evaluateExpression(Expression expression, @Nullable Class<T> expectedType) {
		return expression.getValue(getEvaluationContext(), expectedType);
	}

	@Nullable
	protected Object evaluateExpression(Expression expression) {
		return expression.getValue(getEvaluationContext());
	}

	@Nullable
	protected <T> T evaluateExpression(Expression expression, Object input, @Nullable Class<T> expectedType) {
		return expression.getValue(getEvaluationContext(), input, expectedType);
	}

	protected void onInit() {

	}

}
