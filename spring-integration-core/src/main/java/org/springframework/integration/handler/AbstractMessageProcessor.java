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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.util.BeanFactoryTypeConverter;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class AbstractMessageProcessor implements MessageProcessor, BeanFactoryAware {

	private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
	
	private final BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
	
	public AbstractMessageProcessor() {
		evaluationContext.setTypeConverter(typeConverter);
	}
	
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		typeConverter.setBeanFactory(beanFactory);
	}

	public void setConversionService(ConversionService conversionService) {
		if (conversionService != null) {
			typeConverter.setConversionService(conversionService);
		}
	}

	protected StandardEvaluationContext getEvaluationContext() {
		return this.evaluationContext;
	}

	protected Object evaluateExpression(Expression expression, Message<?> message, Class<?> expectedType) {
		try {
			return (expectedType != null)
					? expression.getValue(this.evaluationContext, message, expectedType)
					: expression.getValue(this.evaluationContext, message);
		}
		catch (EvaluationException e) {
			Throwable cause = e.getCause();
			throw new MessageHandlingException(message, "Expression evaluation failed: "+expression.getExpressionString(), cause==null ? e : cause);
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, "Expression evaluation failed: "+expression.getExpressionString(), e);
		}
	}

}
