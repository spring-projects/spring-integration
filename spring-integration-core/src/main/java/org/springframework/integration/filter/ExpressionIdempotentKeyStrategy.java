/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.filter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 * @since 4.1
 */
public class ExpressionIdempotentKeyStrategy implements IdempotentKeyStrategy, BeanFactoryAware {

	private static final ExpressionParser PARSER = new SpelExpressionParser();

	private final MessageProcessor<String> processor;

	private final String expressionString;

	public ExpressionIdempotentKeyStrategy(String expressionString) {
		processor = new ExpressionEvaluatingMessageProcessor<String>(PARSER.parseExpression(expressionString));
		this.expressionString = expressionString;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		((BeanFactoryAware) this.processor).setBeanFactory(beanFactory);
	}

	@Override
	public String getIdempotentKey(Message<?> message) {
		return this.processor.processMessage(message);
	}

	@Override
	public String toString() {
		return "ExpressionEvaluatingSelector for: [" + this.expressionString + "]";
	}

}
