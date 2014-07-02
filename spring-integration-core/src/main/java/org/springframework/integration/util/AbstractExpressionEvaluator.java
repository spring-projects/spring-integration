/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;

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

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile StandardEvaluationContext evaluationContext;

	private final ExpressionParser expressionParser = new SpelExpressionParser();

	private final BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();

	private volatile BeanFactory beanFactory;

	private volatile MessageBuilderFactory messageBuilderFactory;

	/**
	 * Specify a BeanFactory in order to enable resolution via <code>@beanName</code> in the expression.
	 */
	@Override
	public void setBeanFactory(final BeanFactory beanFactory) {
		if (beanFactory != null) {
			this.beanFactory = beanFactory;
			this.typeConverter.setBeanFactory(beanFactory);
			if (this.evaluationContext != null && this.evaluationContext.getBeanResolver() == null) {
				this.evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
			}
			this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(beanFactory);
		}
	}

	protected BeanFactory getBeanFactory() {
		return beanFactory;
	}

	public void setConversionService(ConversionService conversionService) {
		if (conversionService != null) {
			this.typeConverter.setConversionService(conversionService);
		}
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		if (this.messageBuilderFactory == null) {
			this.messageBuilderFactory = new DefaultMessageBuilderFactory();
		}
		return this.messageBuilderFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		getEvaluationContext();
		if (this.messageBuilderFactory == null) {
			this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
		}
	}

	protected StandardEvaluationContext getEvaluationContext() {
		return this.getEvaluationContext(true);
	}

	/**
	 * Emits a WARN log if the beanFactory field is null, unless the argument is false.
	 * @param beanFactoryRequired set to false to suppress the warning.
	 * @return The evaluation context.
	 */
	protected final StandardEvaluationContext getEvaluationContext(boolean beanFactoryRequired) {
		if (this.evaluationContext == null) {
			if (this.beanFactory == null && !beanFactoryRequired) {
				this.evaluationContext = ExpressionUtils.createStandardEvaluationContext();
			}
			else {
				this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.beanFactory);
			}
			this.evaluationContext.setTypeConverter(this.typeConverter);
		}
		return this.evaluationContext;
	}

	protected <T> T evaluateExpression(Expression expression, Message<?> message, Class<T> expectedType) {
		try {
			return evaluateExpression(expression, (Object) message, expectedType);
		}
		catch (EvaluationException e) {
			Throwable cause = e.getCause();
			if (this.logger.isDebugEnabled()) {
				logger.debug("SpEL Expression evaluation failed with EvaluationException.", e);
			}
			throw new MessageHandlingException(message, "Expression evaluation failed: "
					+ expression.getExpressionString(), cause == null ? e : cause);
		}
		catch (Exception e) {
			if (this.logger.isDebugEnabled()) {
				logger.debug("SpEL Expression evaluation failed with Exception." + e);
			}
			throw new MessageHandlingException(message, "Expression evaluation failed: "
					+ expression.getExpressionString(), e);
		}
	}

	protected Object evaluateExpression(String expression, Object input) {
		return this.evaluateExpression(expression, input, null);
	}

	protected <T> T evaluateExpression(String expression, Object input, Class<T> expectedType) {
		return this.expressionParser.parseExpression(expression)
				.getValue(this.getEvaluationContext(), input, expectedType);
	}

	protected Object evaluateExpression(Expression expression, Object input) {
		return this.evaluateExpression(expression, input, null);
	}

	protected <T> T evaluateExpression(Expression expression, Class<T> expectedType) {
		return expression.getValue(this.getEvaluationContext(), expectedType);
	}

	protected Object evaluateExpression(Expression expression) {
		return expression.getValue(this.getEvaluationContext());
	}

	protected <T> T evaluateExpression(Expression expression, Object input, Class<T> expectedType) {
		return expression.getValue(this.getEvaluationContext(), input, expectedType);
	}

}
