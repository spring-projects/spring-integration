/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.integration.transformer.support;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 3.0
 */
public class ExpressionEvaluatingHeaderValueMessageProcessor<T> extends AbstractHeaderValueMessageProcessor<T>
		implements BeanFactoryAware {

	private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser(new SpelParserConfiguration(
			true, true));

	private final ExpressionEvaluatingMessageProcessor<T> targetProcessor;

	/**
	 * Create a header value processor for the given Expression and the
	 * expected type of the expression evaluation result. The expectedType
	 * may be null if unknown.
	 * @param expression the {@link Expression} to evaluate.
	 * @param expectedType the type for return value of {@code expression} evaluation result.
	 */
	public ExpressionEvaluatingHeaderValueMessageProcessor(Expression expression, Class<T> expectedType) {
		this.targetProcessor = new ExpressionEvaluatingMessageProcessor<T>(expression, expectedType);
	}

	/**
	 * Create a header value processor for the given expression string and
	 * the expected type of the expression evaluation result. The
	 * expectedType may be null if unknown.
	 * @param expressionString the {@link java.lang.String} expression presentation to evaluate.
	 * @param expectedType the type for return value of {@code expression} evaluation result.
	 */
	public ExpressionEvaluatingHeaderValueMessageProcessor(String expressionString, Class<T> expectedType) {
		Expression expression = EXPRESSION_PARSER.parseExpression(expressionString);
		this.targetProcessor = new ExpressionEvaluatingMessageProcessor<T>(expression, expectedType);
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.targetProcessor.setBeanFactory(beanFactory);
	}

	public T processMessage(Message<?> message) {
		return this.targetProcessor.processMessage(message);
	}

}
