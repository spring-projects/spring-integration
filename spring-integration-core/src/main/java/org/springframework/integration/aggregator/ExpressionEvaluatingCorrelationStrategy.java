/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.aggregator;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link CorrelationStrategy} implementation that evaluates an expression.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class ExpressionEvaluatingCorrelationStrategy implements CorrelationStrategy, BeanFactoryAware {

	private static final ExpressionParser expressionParser = new SpelExpressionParser(new SpelParserConfiguration(true, true));

	private final ExpressionEvaluatingMessageProcessor<Object> processor;


	public ExpressionEvaluatingCorrelationStrategy(String expressionString) {
		Assert.hasText(expressionString, "expressionString must not be empty");
		Expression expression = expressionParser.parseExpression(expressionString);
		this.processor = new ExpressionEvaluatingMessageProcessor<Object>(expression, Object.class);
	}

	public ExpressionEvaluatingCorrelationStrategy(Expression expression) {
		this.processor = new ExpressionEvaluatingMessageProcessor<Object>(expression, Object.class);
	}

	public Object getCorrelationKey(Message<?> message) {
		return processor.processMessage(message);
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory != null){
			this.processor.setBeanFactory(beanFactory);
		}
	}

}
